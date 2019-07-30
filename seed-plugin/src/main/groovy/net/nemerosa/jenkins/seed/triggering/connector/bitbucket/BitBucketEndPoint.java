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
    private static final List<BitBucketEventType> ACCEPTED_EVENTS = Arrays.asList(BitBucketEventType.PUSH, BitBucketEventType.DIAGNOSTICS_PING);

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

        // check permissions
        checkSignature(req, payload, getProject(json));

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
            return pushEvent(json, change);
        } else if (isNewBranch) {
            return createEvent(json, change);
        } else if (isDelete) {
            return deleteEvent(json, change);
        } else {
            return null;
        }
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

    private SeedEvent createEvent(final JSONObject json, final JSONObject change) {
        return branchEvent(json, change, SeedEventType.CREATION);
    }

    private SeedEvent deleteEvent(final JSONObject json, final JSONObject change) {
        return branchEvent(json, change, SeedEventType.DELETION);
    }

    private SeedEvent branchEvent(final JSONObject json, final JSONObject change, final SeedEventType eventType) {
        String project = getProject(json);
        String branchName = change.getJSONObject("ref").getString("displayId");
        return addParameters(new SeedEvent(
                project,
                branchName,
                eventType,
                SEED_CHANNEL
        ), json.getJSONObject("actor"), change);
    }

    private SeedEvent pushEvent(final JSONObject json, final JSONObject change) {
        String branch = change.getJSONObject("ref").getString("displayId");
        return addParameters(new SeedEvent(
                getProject(json),
                branch,
                SeedEventType.COMMIT,
                SEED_CHANNEL
        ), json.getJSONObject("actor"), change);
    }

    private SeedEvent addParameters(final SeedEvent event, final JSONObject actor, final JSONObject change) {
        final boolean isTag = "tag".equals(change.getJSONObject("ref").optString("type", "").toLowerCase());
        return event.withParam(Constants.COMMIT_PARAMETER, change.getString("toHash"))
                .withParam(Constants.IS_TAG_PARAMETER, isTag)
                .withParam(Constants.AUTHOR_ID_PARAMETER, actor.getString("name"))
                .withParam(Constants.AUTHOR_NAME_PARAMETER, actor.getString("displayName"));
    }

    @Override
    public String getUrlName() {
        return "seed-bitbucket-api";
    }

    private String getProject(final JSONObject json) {
        final JSONObject repository = json.getJSONObject("repository");
        return repository.getJSONObject("project").getString("key").toLowerCase() + "/" + repository.getString("slug");
    }
}
