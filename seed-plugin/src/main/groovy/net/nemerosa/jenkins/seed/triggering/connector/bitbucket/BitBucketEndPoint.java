package net.nemerosa.jenkins.seed.triggering.connector.bitbucket;

import hudson.Extension;
import net.nemerosa.jenkins.seed.Constants;
import net.nemerosa.jenkins.seed.triggering.SeedChannel;
import net.nemerosa.jenkins.seed.triggering.SeedEvent;
import net.nemerosa.jenkins.seed.triggering.SeedEventType;
import net.nemerosa.jenkins.seed.triggering.SeedService;
import net.nemerosa.jenkins.seed.triggering.connector.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.kohsuke.stapler.StaplerRequest;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

@Extension
public class BitBucketEndPoint extends AbstractEndPoint {
    private static final String X_HUB_SIGNATURE = "X-Hub-Signature";
    private static final String X_EVENT_KEY = "X-Event-Key";
    private static final List<BitBucketEventType> ACCEPTED_EVENTS = Arrays.asList(BitBucketEventType.PUSH,
                                                                                  BitBucketEventType.PR_OPEN,
                                                                                  BitBucketEventType.PR_MODIFIED,
                                                                                  BitBucketEventType.PR_DELETED,
                                                                                  BitBucketEventType.PR_MERGED,
                                                                                  BitBucketEventType.DIAGNOSTICS_PING);
    private static final List<BitBucketEventType> PULL_REQUEST_DELETED_EVENTS = Arrays.asList(BitBucketEventType.PR_DELETED,
                                                                                              BitBucketEventType.PR_MERGED);

    private static final Logger LOGGER = Logger.getLogger(SeedService.class.getName());
    private static final SeedChannel SEED_CHANNEL = SeedChannel.of("bitbucket", "Seed BitBucket end point");

    public BitBucketEndPoint(final SeedService seedService) {
        super(seedService);
    }

    @SuppressWarnings("unused")
    public BitBucketEndPoint() {
        super();
    }

    @Override
    protected SeedEvent extractEvent(final StaplerRequest req) throws IOException {
        // Gets the event type sent by BitBucket
        BitBucketEventType eventType = BitBucketEventType.findByName(req.getHeader(X_EVENT_KEY));
        if (!ACCEPTED_EVENTS.contains(eventType)) {
            throw new UnknownRequestException(String.format("Unknown/Unaccepted event type in %s header - it was %s", X_EVENT_KEY, eventType.name));
        }

        // handle special event types
        if (BitBucketEventType.DIAGNOSTICS_PING == eventType) {
            return new SeedEvent("Test OK, but did not check token verification!", null, SeedEventType.TEST, SEED_CHANNEL);
        }

        // get payload
        String payload = IOUtils.toString(req.getReader());
        JSONObject json = JSONObject.fromObject(payload);
        final String project = getProject(json, eventType);

        // check permissions
        checkSignature(req, payload, project);

        SeedEvent seedEvent;
        switch (eventType) {
            case PUSH:
                seedEvent = getPushSeedEvent(project, json);
                break;
            case PR_OPEN:
            case PR_MODIFIED:
            case PR_DELETED:
                seedEvent = getPullRequestSeedEvent(project, json, eventType);
                break;
            default:
                seedEvent = null;
        }
        return seedEvent;
    }

    protected void checkSignature(final StaplerRequest req, final String payload, final String project) throws UnsupportedEncodingException {
        // Gets the secret key for the project
        String secretKey = seedService.getSecretKey(project, "bitbucket");
        if (StringUtils.isBlank(secretKey)) {
            return;
        }

        // Gets the signature header
        String ghSignature = req.getHeader(X_HUB_SIGNATURE);

        // Secret key specification
        SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HMACSha256");

        // HMac signature
        try {
            Mac mac = Mac.getInstance("HMACSha256");
            mac.init(keySpec);
            byte[] rawHmac = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String hmac = "sha256=" + Hex.encodeHexString(rawHmac);

            if (!StringUtils.equals(hmac, ghSignature)) {
                LOGGER.severe(X_HUB_SIGNATURE + " token is invalid.");
                throw new RequestNonAuthorizedException();
            }
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new CannotHandleRequestException(ex);
        }
    }

    private SeedEvent getPullRequestSeedEvent(final String project, final JSONObject json, final BitBucketEventType bitBucketEventType) {
        JSONObject pullRequest = json.getJSONObject("pullRequest");
        JSONObject fromRef = pullRequest.getJSONObject("fromRef");
        JSONObject toRef = pullRequest.getJSONObject("toRef");
        final boolean prDeleted = PULL_REQUEST_DELETED_EVENTS.contains(bitBucketEventType);
        return addParameters(new SeedEvent(
                                     project,
                                     fromRef.getString("displayId"),
                                     SeedEventType.SEED,
                                     SEED_CHANNEL
                             ),
                             json.getJSONObject("actor"),
                             fromRef.getString("latestCommit"),
                             prDeleted ? "" : toRef.getString("displayId"),
                             prDeleted ? "" : pullRequest.getString("id"));
    }

    private SeedEvent getPushSeedEvent(final String project, final JSONObject json) {
        // Gets the list of changes
        JSONArray changes = json.optJSONArray("changes");
        if (changes.size() == 0) {
            throw new RequestFormatException("At least one change is required.");
        }
        // Takes only the first change
        JSONObject change = changes.getJSONObject(0);

        // push information
        String changeType = change.optString("type", "");
        boolean isNewBranch = "add".equals(changeType.toLowerCase());
        boolean isCommit = "update".equals(changeType.toLowerCase());
        boolean isDelete = "delete".equals(changeType.toLowerCase());
        if (isCommit) {
            return pushEvent(project, json, change);
        } else if (isNewBranch) {
            return createEvent(project, json, change);
        } else if (isDelete) {
            return deleteEvent(project, json, change);
        } else {
            return null;
        }
    }

    private SeedEvent createEvent(final String project, final JSONObject json, final JSONObject change) {
        return branchEvent(project, json, change, SeedEventType.CREATION);
    }

    private SeedEvent deleteEvent(final String project, final JSONObject json, final JSONObject change) {
        return branchEvent(project, json, change, SeedEventType.DELETION);
    }

    private SeedEvent branchEvent(final String project, final JSONObject json, final JSONObject change, final SeedEventType eventType) {
        String branchName = change.getJSONObject("ref").getString("displayId");
        return addParameters(new SeedEvent(
                                     project,
                                     branchName,
                                     eventType,
                                     SEED_CHANNEL,
                                     isTag(change)
                             ),
                             json.getJSONObject("actor"),
                             change.getString("toHash"));
    }

    private SeedEvent pushEvent(final String project, final JSONObject json, final JSONObject change) {
        String branch = change.getJSONObject("ref").getString("displayId");
        return addParameters(new SeedEvent(
                                     project,
                                     branch,
                                     SeedEventType.COMMIT,
                                     SEED_CHANNEL,
                                     isTag(change)
                             ),
                             json.getJSONObject("actor"),
                             change.getString("toHash"));
    }

    private boolean isTag(final JSONObject change) {
        return "tag".equals(change.getJSONObject("ref").optString("type", "").toLowerCase());
    }

    private SeedEvent addParameters(final SeedEvent event, final JSONObject actor, final String commit) {
        return addParameters(event, actor, commit, null, null);
    }

    private SeedEvent addParameters(final SeedEvent event, final JSONObject actor, final String commit, final String targetBranch, final String pullRequestId) {
        return event.withParam(Constants.COMMIT_PARAMETER, commit)
                .withParam(Constants.PULL_REQUEST_ID_PARAMETER, ObjectUtils.defaultIfNull(pullRequestId, ""))
                .withParam(Constants.TARGET_BRANCH_PARAMETER, ObjectUtils.defaultIfNull(targetBranch, ""))
                .withParam(Constants.AUTHOR_ID_PARAMETER, actor.getString("name"))
                .withParam(Constants.AUTHOR_NAME_PARAMETER, actor.getString("displayName"));
    }

    @Override
    public String getUrlName() {
        return "seed-bitbucket-api";
    }

    private String getProject(final JSONObject json, final BitBucketEventType eventType) {
        final JSONObject repository = getRepository(json, eventType);
        return repository.getJSONObject("project").getString("key").toLowerCase() + "/" + repository.getString("slug");
    }

    private JSONObject getRepository(final JSONObject json, final BitBucketEventType eventType) {
        JSONObject repository;
        switch (eventType) {
            case PUSH:
                repository = json.getJSONObject("repository");
                break;
            case PR_OPEN:
            case PR_MODIFIED:
            case PR_DELETED:
                repository = json.getJSONObject("pullRequest").getJSONObject("fromRef").getJSONObject("repository");
                break;
            default:
                repository = null;
        }
        return repository;
    }
}
