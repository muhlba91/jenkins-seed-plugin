package net.nemerosa.jenkins.seed.triggering;

import net.nemerosa.jenkins.seed.Constants;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

public class SeedEvent {

    private final String project;
    private final String branch;
    private final SeedEventType type;
    private final SeedChannel channel;
    private final boolean isTag;
    private final String pullRequestId;
    private final String targetBanch;
    private final Map<String, Object> parameters = new LinkedHashMap<>();

    public SeedEvent(String project, String branch, SeedEventType type, SeedChannel channel) {
        this(project, branch, type, channel, false);
    }

    public SeedEvent(String project, String branch, SeedEventType type, SeedChannel channel, boolean isTag) {
        this(project, branch, type, channel, isTag, null, null);
    }

    public SeedEvent(String project, String branch, SeedEventType type, SeedChannel channel, String pullRequestId, String targetBanch) {
        this(project, branch, type, channel, false, pullRequestId, targetBanch);
    }

    public SeedEvent(String project, String branch, SeedEventType type, SeedChannel channel, boolean isTag, String pullRequestId, String targetBanch) {
        this.project = project;
        this.branch = branch;
        this.type = type;
        this.channel = channel;
        this.isTag = isTag;
        this.targetBanch = targetBanch;
        this.pullRequestId = pullRequestId;
    }

    public SeedEvent withParam(String name, Object value) {
        parameters.put(name, value);
        return this;
    }

    public String getProject() {
        return project;
    }

    public String getBranch() {
        return branch;
    }

    public SeedEventType getType() {
        return type;
    }

    public SeedChannel getChannel() {
        return channel;
    }

    public boolean isTag() {
        return isTag;
    }

    public String getPullRequestId() {
        return pullRequestId;
    }

    public String getTargetBanch() {
        return targetBanch;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SeedEvent event = (SeedEvent) o;

        return branch.equals(event.branch) &&
               parameters.equals(event.parameters)
               && project.equals(event.project)
               && type == event.type;
    }

    @Override
    public int hashCode() {
        int result = project.hashCode();
        result = 31 * result + branch.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + parameters.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SeedEvent{" + "project='" + project + '\'' +
               ", branch='" + branch + '\'' +
               ", type=" + type +
               ", channel=" + channel +
               ", isTag=" + isTag +
               ", pullRequestId=" + pullRequestId +
               ", targetBanch=" + targetBanch +
               ", parameters=" + parameters + '}';
    }

    public String getCommitParameter() {
        String commit = (String) parameters.get(Constants.COMMIT_PARAMETER);
        if (StringUtils.isBlank(commit)) {
            return "HEAD";
        } else {
            return commit;
        }
    }
}
