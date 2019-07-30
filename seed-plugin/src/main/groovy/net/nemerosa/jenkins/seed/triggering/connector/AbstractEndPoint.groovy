package net.nemerosa.jenkins.seed.triggering.connector

import com.google.inject.Guice
import hudson.model.UnprotectedRootAction
import net.nemerosa.jenkins.seed.generator.MissingParameterException
import net.nemerosa.jenkins.seed.triggering.SeedEvent
import net.nemerosa.jenkins.seed.triggering.SeedEventType
import net.nemerosa.jenkins.seed.triggering.SeedService
import net.nemerosa.jenkins.seed.triggering.SeedServiceModule
import net.sf.json.JSONSerializer
import org.apache.commons.lang.StringUtils
import org.kohsuke.stapler.StaplerRequest
import org.kohsuke.stapler.StaplerResponse
import org.kohsuke.stapler.interceptor.RequirePOST

import java.util.logging.Level
import java.util.logging.Logger

public abstract class AbstractEndPoint implements UnprotectedRootAction {

    private static final Logger LOGGER = Logger.getLogger(AbstractEndPoint.class.getName());

    protected final SeedService seedService;

    AbstractEndPoint(final SeedService seedService) {
        this.seedService = seedService;
    }

    AbstractEndPoint() {
        this(Guice.createInjector(new SeedServiceModule()).getInstance(SeedService.class));
    }

    @Override
    String getIconFileName() {
        return null;
    }

    @Override
    String getDisplayName() {
        return null;
    }

    @RequirePOST
    void doDynamic(final StaplerRequest req, final StaplerResponse rsp) throws IOException {
        LOGGER.info("Incoming POST")

        try {
            // Extracts the event
            SeedEvent event = extractEvent(req)
            if (event == null) {
                LOGGER.finer("Event not managed")
                sendError(rsp, StaplerResponse.SC_ACCEPTED, "Event not managed")
            } else {
                LOGGER.finer(String.format("Event to process: project=%s, branch=%s, type=%s, parameters=%s", event.getProject(), event.getBranch(), event.getType(), event.getParameters()))
                if (event.type == SeedEventType.TEST) {
                    sendError(rsp, StaplerResponse.SC_ACCEPTED, event.getProject())
                } else {
                    post(event)
                    sendOk(rsp, event)
                }
            }
        } catch (IOException ex) {
            throw ex
        } catch (RequestNonAuthorizedException ex) {
            sendError(rsp, StaplerResponse.SC_FORBIDDEN, ex.getMessage())
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, ex.getMessage(), ex)
            sendError(rsp, StaplerResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage())
        }
    }

    static String extractParameter(final StaplerRequest req, final String name) {
        return extractParameter(req, name, true)
    }

    static String extractParameter(final StaplerRequest req, final String name, final boolean required) {
        String value = req.getParameter(name)
        if (StringUtils.isNotBlank(value)) {
            return value
        } else if (required) {
            throw new MissingParameterException(name)
        } else {
            return null
        }
    }

    protected abstract SeedEvent extractEvent(StaplerRequest req) throws IOException;

    protected static void sendOk(final StaplerResponse rsp, final SeedEvent event) throws IOException {
        rsp.setStatus(getHttpCodeForEvent(event))
        rsp.setContentType("application/json")
        JSONSerializer.toJSON([
                status: 'OK',
                event: [
                        project: event.project,
                        branch: event.branch,
                        type: event.type,
                        parameters: event.parameters
                ],
        ]).write(rsp.writer)
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    protected static int getHttpCodeForEvent(final SeedEvent event) {
        return StaplerResponse.SC_ACCEPTED
    }

    protected static void sendError(final StaplerResponse rsp, final int httpCode, final String message) throws IOException {
        rsp.setStatus(httpCode)
        rsp.setContentType("application/json")
        JSONSerializer.toJSON([
                status: 'ERROR',
                message: message,
        ]).write(rsp.writer)
    }

    protected void post(final SeedEvent event) {
        seedService.post(event)
    }
}
