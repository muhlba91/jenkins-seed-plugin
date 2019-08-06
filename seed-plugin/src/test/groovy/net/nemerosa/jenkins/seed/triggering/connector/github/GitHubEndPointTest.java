package net.nemerosa.jenkins.seed.triggering.connector.github;

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

public class GitHubEndPointTest {

    private static final SeedChannel GITHUB_CHANNEL = SeedChannel.of("github", "Seed GitHub end point");

    @Test
    public void ping() throws IOException {
        SeedService seedService = mock(SeedService.class);
        StaplerResponse response = mockStaplerResponse();
        StaplerRequest request = mockGitHubRequest("ping", "/github-payload-ping.json");

        getEndPoints(seedService).doDynamic(request, response);
        verify(seedService, never()).post(any(SeedEvent.class));
        verify(response, times(1)).setStatus(202);
    }


    @Test
    public void create_branch() throws IOException {
        SeedService seedService = mock(SeedService.class);
        StaplerResponse response = mockStaplerResponse();
        StaplerRequest request = mockGitHubRequest("create", "/github-payload-create.json");

        getEndPoints(seedService).doDynamic(request, response);
        verify(seedService, times(1)).post(
                new SeedEvent(
                        "nemerosa/seed-demo",
                        "test-4",
                        SeedEventType.CREATION,
                        GITHUB_CHANNEL));
    }

    /**
     * When a branch is created, the `create` event is preceded by the `push` which must be ignored.
     */
    @Test
    public void create_branch_push_event() throws IOException {
        SeedService seedService = mock(SeedService.class);
        StaplerResponse response = mockStaplerResponse();
        StaplerRequest request = mockGitHubRequest("push", "/github-payload-create-push.json");

        getEndPoints(seedService).doDynamic(request, response);
        verify(seedService, never()).post(any(SeedEvent.class));
    }

    @Test
    public void delete_branch() throws IOException {
        SeedService seedService = mock(SeedService.class);
        StaplerResponse response = mockStaplerResponse();
        StaplerRequest request = mockGitHubRequest("delete", "/github-payload-delete.json");

        getEndPoints(seedService).doDynamic(request, response);
        verify(seedService, times(1)).post(
                new SeedEvent(
                        "nemerosa/seed-demo",
                        "test-4",
                        SeedEventType.DELETION,
                        GITHUB_CHANNEL));
    }

    /**
     * When a branch is delete, the `delete` event is followed by the `push` which must be ignored.
     */
    @Test
    public void delete_branch_push_event() throws IOException {
        SeedService seedService = mock(SeedService.class);
        StaplerResponse response = mockStaplerResponse();
        StaplerRequest request = mockGitHubRequest("push", "/github-payload-delete-push.json");

        getEndPoints(seedService).doDynamic(request, response);
        verify(seedService, never()).post(any(SeedEvent.class));
    }

    @Test
    public void seed_event() throws IOException {
        SeedService seedService = mock(SeedService.class);
        StaplerResponse response = mockStaplerResponse();
        StaplerRequest request = mockGitHubRequest("push", "/github-payload-seed.json");

        getEndPoints(seedService).doDynamic(request, response);
        verify(seedService, times(1)).post(
                new SeedEvent(
                        "nemerosa/seed-demo",
                        "master",
                        SeedEventType.SEED,
                        GITHUB_CHANNEL));
    }

    @Test
    public void seed_mixed_event() throws IOException {
        SeedService seedService = mock(SeedService.class);
        StaplerResponse response = mockStaplerResponse();
        StaplerRequest request = mockGitHubRequest("push", "/github-payload-seed-mixed.json");

        getEndPoints(seedService).doDynamic(request, response);
        verify(seedService, times(1)).post(
                new SeedEvent(
                        "nemerosa/seed-demo",
                        "master",
                        SeedEventType.SEED,
                        GITHUB_CHANNEL));
    }

    @Test
    public void commit_event_without_signature() throws IOException {
        SeedService seedService = mock(SeedService.class);
        StaplerResponse response = mockStaplerResponse();
        StaplerRequest request = mockGitHubRequest("push", "/github-payload-commit.json");

        getEndPoints(seedService).doDynamic(request, response);
        verify(seedService, times(1)).post(
                new SeedEvent(
                        "nemerosa/seed-demo",
                        "master",
                        SeedEventType.COMMIT,
                        GITHUB_CHANNEL)
                        .withParam("commit", "a10c3027a04ab066adc7a2a3d4735a7026fc1c59"));
    }

    @Test
    public void commit_event_with_signature() throws IOException {
        SeedService seedService = mock(SeedService.class);
        StaplerResponse response = mockStaplerResponse();
        StaplerRequest request = mockGitHubRequest("push", "/github-payload-commit-signed.json");
        when(request.getHeader("X-Hub-Signature")).thenReturn("sha1=0cc69a00e5cb96e2459a740ce144e5ced4bd94ac");
        when(seedService.getSecretKey("nemerosa/seed-demo", "github")).thenReturn("ABCDEF123456");

        getEndPoints(seedService).doDynamic(request, response);
        verify(seedService, times(1)).post(
                new SeedEvent(
                        "nemerosa/seed-demo",
                        "master",
                        SeedEventType.COMMIT,
                        GITHUB_CHANNEL)
                        .withParam("commit", "a10c3027a04ab066adc7a2a3d4735a7026fc1c59"));
    }

    protected GitHubEndPoint getEndPoints(SeedService seedService) {
        return new GitHubEndPoint(seedService);
    }

    private StaplerRequest mockGitHubRequest(String event, String payload) throws IOException {
        StaplerRequest request = mock(StaplerRequest.class);
        when(request.getHeader("X-GitHub-Event")).thenReturn(event);
        when(request.getReader()).thenReturn(new BufferedReader(new InputStreamReader(
                getClass().getResourceAsStream(payload),
                StandardCharsets.UTF_8)));
        return request;
    }
}
