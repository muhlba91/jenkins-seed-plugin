package net.nemerosa.jenkins.seed.triggering.connector.bitbucket;

public enum BitBucketEventType {
    PUSH("repo:refs_changed"),
    PR_OPEN("pr:opened"),
    PR_MODIFIED("pr:modified"),
    PR_DELETED("pr:deleted"),
    PR_MERGED("pr:merged"),
    DIAGNOSTICS_PING("diagnostics:ping"),
    NONE("none");

    final String name;

    BitBucketEventType(final String name) {
        this.name = name;
    }

    static BitBucketEventType findByName(final String name) {
        for (final BitBucketEventType value : values()) {
            if (value.name.equals(name)) {
                return value;
            }
        }
        return NONE;
    }
}
