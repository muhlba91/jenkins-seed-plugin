package net.nemerosa.jenkins.seed.config;

import lombok.Data;
import lombok.experimental.Wither;
import org.kohsuke.stapler.DataBoundConstructor;

@Data
public class EventStrategyConfig {

    /**
     * Defines the way a pipeline is managed in case a deletion event is fired. If yes (default), the
     * entire pipeline is deleted (the complete branch folder). If no, only the branch seed job is
     * deleted but the rest of the pipeline remains intact.
     */
    @Wither
    private final boolean delete;

    /**
     * When a Seed event is received (update of the pipeline definition), defines the behaviour to adopt.
     * If yes (default), regenerate the pipeline. If no, do not regenerate the pipeline.
     */
    @Wither
    private final boolean auto;

    /**
     * When a commit event is received (commit, push), defines the behaviour to adopt. If yes (default),
     * triggers the pipeline. If no, does not trigger the pipeline.
     */
    @Wither
    private final boolean trigger;

    @DataBoundConstructor
    public EventStrategyConfig(boolean delete, boolean auto, boolean trigger) {
        this.delete = delete;
        this.auto = auto;
        this.trigger = trigger;
    }

    public EventStrategyConfig() {
        this(true, true, true);
    }
}
