package net.nemerosa.jenkins.seed.triggering.connector.bitbucket;

import net.nemerosa.jenkins.seed.Constants;
import net.nemerosa.jenkins.seed.triggering.SeedChannel;
import net.nemerosa.jenkins.seed.triggering.SeedEvent;
import net.nemerosa.jenkins.seed.triggering.SeedEventType;
import net.nemerosa.jenkins.seed.triggering.SeedService;
import org.junit.Test;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static net.nemerosa.jenkins.seed.triggering.connector.EndPointTestSupport.mockStaplerResponse;
import static org.mockito.Mockito.*;

public class BitBucketEndPointTest {

    private static final SeedChannel BITBUCKET_CHANNEL = SeedChannel.of("bitbucket", "Seed BitBucket end point");

    @Test
    public void commit_event() throws IOException {
        SeedService seedService = mock(SeedService.class);
        StaplerResponse response = mockStaplerResponse();
        StaplerRequest request = mockBitBucketRequest("repo:refs_changed", "/bitbucket-payload-commit.json");

        getEndPoint(seedService).doDynamic(request, response);
        verify(seedService, times(1)).post(
                new SeedEvent(
                        "proj/repository",
                        "master",
                        SeedEventType.COMMIT,
                        BITBUCKET_CHANNEL)
                        .withParam(Constants.COMMIT_PARAMETER, "a083aca5efac42ee26ee5a554f0c57c7af3bc64c")
                        .withParam(Constants.PULL_REQUEST_ID_PARAMETER, "")
                        .withParam(Constants.TARGET_BRANCH_PARAMETER, "")
                        .withParam(Constants.AUTHOR_ID_PARAMETER, "admin")
                        .withParam(Constants.AUTHOR_NAME_PARAMETER, "Administrator"));
    }

    @Test
    public void create_branch() throws IOException {
        SeedService seedService = mock(SeedService.class);
        StaplerResponse response = mockStaplerResponse();
        StaplerRequest request = mockBitBucketRequest("repo:refs_changed", "/bitbucket-payload-create.json");

        getEndPoint(seedService).doDynamic(request, response);
        verify(seedService, times(1)).post(
                new SeedEvent(
                        "proj/repository",
                        "branch",
                        SeedEventType.CREATION,
                        BITBUCKET_CHANNEL)
                        .withParam(Constants.COMMIT_PARAMETER, "7800ed8324433c48bb59eaed6ed938b088576da5")
                        .withParam(Constants.PULL_REQUEST_ID_PARAMETER, "")
                        .withParam(Constants.TARGET_BRANCH_PARAMETER, "")
                        .withParam(Constants.AUTHOR_ID_PARAMETER, "admin")
                        .withParam(Constants.AUTHOR_NAME_PARAMETER, "Administrator"));
    }

    @Test
    public void create_tag() throws IOException {
        SeedService seedService = mock(SeedService.class);
        StaplerResponse response = mockStaplerResponse();
        StaplerRequest request = mockBitBucketRequest("repo:refs_changed", "/bitbucket-payload-create-tag.json");

        getEndPoint(seedService).doDynamic(request, response);
        verify(seedService, times(1)).post(
                new SeedEvent(
                        "proj/repository",
                        "tag",
                        SeedEventType.CREATION,
                        BITBUCKET_CHANNEL,
                        true)
                        .withParam(Constants.COMMIT_PARAMETER, "7800ed8324433c48bb59eaed6ed938b088576da5")
                        .withParam(Constants.PULL_REQUEST_ID_PARAMETER, "")
                        .withParam(Constants.TARGET_BRANCH_PARAMETER, "")
                        .withParam(Constants.AUTHOR_ID_PARAMETER, "admin")
                        .withParam(Constants.AUTHOR_NAME_PARAMETER, "Administrator"));
    }

    protected BitBucketEndPoint getEndPoint(SeedService seedService) {
        return new BitBucketEndPoint(seedService);
    }

    private StaplerRequest mockBitBucketRequest(String event, String payload) throws IOException {
        StaplerRequest request = mock(StaplerRequest.class);
        when(request.getHeader("X-Event-Key")).thenReturn(event);
        when(request.getReader()).thenReturn(
                new BufferedReader(new InputStreamReader(
                        getClass().getResourceAsStream(payload),
                        StandardCharsets.UTF_8)));
        return request;
    }
}
