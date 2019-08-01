package net.nemerosa.jenkins.seed.triggering;

import net.nemerosa.jenkins.seed.Constants;
import net.nemerosa.jenkins.seed.cache.ProjectCachedConfig;
import net.nemerosa.jenkins.seed.cache.ProjectSeedCache;
import net.nemerosa.jenkins.seed.triggering.connector.RequestNonAuthorizedException;
import org.apache.commons.lang.StringUtils;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.String.format;

public class SeedServiceImpl implements SeedService {

    private static final Logger LOGGER = Logger.getLogger(SeedService.class.getName());

    private final SeedLauncher seedLauncher;
    private final ProjectSeedCache seedCache;

    @Inject
    public SeedServiceImpl(final SeedLauncher seedLauncher, final ProjectSeedCache seedCache) {
        this.seedLauncher = seedLauncher;
        this.seedCache = seedCache;
    }

    @Override
    public void post(final SeedEvent event) {
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(String.format("Event: project=%s, branch=%s, type=%s", event.getProject(), event.getBranch(), event.getType()));
        }

        // The project configuration is stored in the project seed
        // but this job is not accessible directly using the project name
        // since naming conventions can change from project to project
        ProjectCachedConfig config = getProjectCachedConfig(event.getProject());

        // Checks the channel
        checkChannel(event, config);

        // Dispatching
        post(event, seedLauncher, config);
    }

    private void post(final SeedEvent event, final SeedLauncher seedLauncher, final ProjectCachedConfig config) {
        switch (event.getType()) {
            case CREATION:
                create(event, seedLauncher, config);
                break;
            case DELETION:
                delete(event, seedLauncher, config);
                break;
            case SEED:
                seed(event, seedLauncher, config);
                break;
            case COMMIT:
                commit(event, seedLauncher, config);
                break;
            default:
                throw new UnsupportedSeedEventTypeException(event.getType());
        }
    }

    private void commit(final SeedEvent event, final SeedLauncher seedLauncher, final ProjectCachedConfig config) {
        if (config.isTrigger()) {
            // Gets the path to the branch start job
            String path = config.getBranchStartJob(event.getBranch());

            // Uses the commit (must be specified in the event)
            String commit = event.getCommitParameter();
            LOGGER.info(format("Commit %s for branch %s of project %s - starting the pipeline at %s", commit, event.getBranch(), event.getProject(), path));
            // Launching the job
            seedLauncher.launch(event.getChannel(), path, generateParameters(event));
        } else {
            LOGGER.finer(format("Commit events are not enabled for project %s", event.getProject()));
        }
    }

    private void seed(final SeedEvent event, final SeedLauncher seedLauncher, final ProjectCachedConfig config) {
        if (config.isAuto()) {
            // Gets the path to the branch seed job
            String path = config.getBranchSeedJob(event.getBranch());
            // Logging
            LOGGER.info(format("Seed files changed for branch %s of project %s - regenerating the pipeline at %s", event.getBranch(), event.getProject(), path));
            // Launches the job (no parameter)
            seedLauncher.launch(event.getChannel(), path, generateParameters(event));
        } else {
            LOGGER.finer(format("Seed events are not enabled for project %s", event.getProject()));
        }
    }

    private void delete(final SeedEvent event, final SeedLauncher seedLauncher, final ProjectCachedConfig config) {
        // Gets the path to the branch seed job
        String path = config.getBranchSeedJob(event.getBranch());
        // Deletes the whole branch folder
        if (config.isDelete()) {
            LOGGER.finer(format("Deletion of the branch means deletion of the pipeline for project %s", event.getProject()));
            // Gets the folder
            path = StringUtils.substringBeforeLast(path, "/");
            if (StringUtils.isNotBlank(path)) {
                seedLauncher.delete(path);
            }
        }
        // ... or deletes the seed job only
        else {
            LOGGER.finer(format("Deletion of the branch means deletion of the pipeline seed for project %s", event.getProject()));
            seedLauncher.delete(path);
        }

    }

    private void create(final SeedEvent event, final SeedLauncher seedLauncher, final ProjectCachedConfig config) {
        LOGGER.finer(format("New branch %s for project %s - creating a new pipeline", event.getBranch(), event.getProject()));
        // Gets the path to the project seed
        String path = config.getProjectSeedJob();
        // Launches the job
        seedLauncher.launch(event.getChannel(), path, generateParameters(event));
    }

    private Map<String, String> generateParameters(final SeedEvent event) {
        Map<String, String> parameters = new HashMap<>();
        parameters.put(Constants.BRANCH_PARAMETER, event.getBranch());
        parameters.put(Constants.IS_TAG_PARAMETER, Boolean.toString(event.isTag()));
        parameters.put(Constants.TARGET_BRANCH, "");
        parameters.put(Constants.PULL_REQUEST_ID, "");
        for (final Map.Entry<String, Object> entry : event.getParameters().entrySet()) {
            parameters.put(entry.getKey().toUpperCase(), entry.getValue().toString());
        }
        return parameters;
    }

    private ProjectCachedConfig getProjectCachedConfig(final String project) {
        // Using a cache, fed by the project seed itself
        ProjectCachedConfig config = seedCache.getProjectPipelineConfig(project);

        // If not found, use a default one
        if (config == null) {
            LOGGER.warning(String.format("Did not find any cache for project %s, using defaults.", project));
            config = new ProjectCachedConfig(project);
        }
        return config;
    }

    protected void checkChannel(final SeedEvent event, final ProjectCachedConfig config) {
        // System channel?
        if (StringUtils.equals(SeedChannel.SYSTEM.getId(), event.getChannel().getId())) {
            return;
        }

        // Enabled?
        boolean enabled = config.isChannelEnabled(event.getChannel());
        if (!enabled) {
            throw new RequestNonAuthorizedException();
        }
    }

    @Override
    public String getSecretKey(final String project, final String context) {
        // Gets the project's configuration
        ProjectCachedConfig config = getProjectCachedConfig(project);
        // Gets the secret key for this context
        return config.getSecretKey();
    }
}
