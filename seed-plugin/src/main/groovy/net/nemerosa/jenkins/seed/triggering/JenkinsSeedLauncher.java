package net.nemerosa.jenkins.seed.triggering;

import hudson.model.*;
import hudson.security.ACL;
import jenkins.model.Jenkins;
import net.nemerosa.jenkins.seed.CannotDeleteItemException;
import net.nemerosa.jenkins.seed.CannotFindJobException;
import org.acegisecurity.context.SecurityContext;
import org.acegisecurity.context.SecurityContextHolder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class JenkinsSeedLauncher implements SeedLauncher {

    private static final Logger LOGGER = Logger.getLogger(JenkinsSeedLauncher.class.getName());

    @Override
    public void launch(final SeedChannel channel, final String path, final Map<String, String> parameters) {
        LOGGER.info(String.format("Launching job at %s with parameters %s", path, parameters));

        SecurityContext orig = ACL.impersonate(ACL.SYSTEM);
        try {
            // Gets the job using its path
            final Queue.Task job = findJob(path);
            // Launches the job
            if (parameters != null && !parameters.isEmpty()) {
                // List of parameters
                List<ParameterValue> parameterValues = new ArrayList<>();
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    parameterValues.add(new StringParameterValue(entry.getKey(), entry.getValue()));
                }
                // Scheduling
                Jenkins.getInstance().getQueue()
                        .schedule2(job,
                                   0,
                                   new ParametersAction(parameterValues),
                                   new CauseAction(getCause(channel)));
            } else {
                Jenkins.getInstance().getQueue().schedule2(job,
                                                           0,
                                                           new CauseAction(getCause(channel)));
            }
        } finally {
            SecurityContextHolder.setContext(orig);
        }
    }

    @Override
    public void delete(final String path) {
        LOGGER.info(String.format("Deleting item at %s", path));

        try {
            SecurityContext orig = ACL.impersonate(ACL.SYSTEM);
            try {
                Item root = findItem(path);
                // Deletes all children
                for (Job job : root.getAllJobs()) {
                    LOGGER.info(String.format("\tDeleting item at %s", job.getName()));
                    job.delete();
                }
                // Deletes the root
                LOGGER.info(String.format("\tDeleting item at %s", root.getName()));
                root.delete();
            } finally {
                SecurityContextHolder.setContext(orig);
            }
        } catch (IOException | InterruptedException e) {
            throw new CannotDeleteItemException(path, e);
        }
    }

    private Cause getCause(final SeedChannel channel) {
        return new SeedCause(channel);
    }

    private Queue.Task findJob(final String path) {
        Item item = findItem(path);
        if (item instanceof Queue.Task) {
            return (Queue.Task) item;
        } else {
            throw new CannotFindJobException("", path);
        }
    }

    private Item findItem(final String path) {
        return Jenkins.getInstance().getItemByFullName(path);
    }
}
