package net.nemerosa.seed.config;

/**
 * Configuration for a project or a class.
 */
public interface ProjectProperties {

    /**
     * Path where to look for the pipeline Groovy definition file. It defaults to <code>seed/seed.properties</code>.
     */
    String PIPELINE_GENERATOR_PROPERTY_PATH = "pipeline-generator-property-path";

    /**
     * List of extensions to add in a branch pipeline seed.
     */
    String PIPELINE_GENERATOR_EXTENSIONS = "pipeline-generator-extensions";

    /**
     * Branching strategy to use
     */
    String BRANCH_STRATEGY = "branch-strategy";

    /**
     * Specific path to the branch generator. Optional and defaults to the path defined
     * by the branching strategy.
     */
    String SEED = "seed";

    /**
     * Boolean to enable the creation of a destructor job.
     */
    String PIPELINE_DESTRUCTOR = "project-destructor";

    /**
     * Specific path to the branch seed. Optional and defaults to the path defined by the branching strategy.
     */
    String PIPELINE_SEED = "pipeline-seed";

    /**
     * Specific path to the branch pipeline starting job. Optional and defaults to the path defined by the branching strategy.
     */
    String PIPELINE_START = "pipeline-start";

    /**
     * Defines the way a pipeline is managed in case a deletion event is fired.
     *
     * If yes (default), the entire pipeline is deleted (the complete branch folder).
     *
     * If no, only the branch seed job is deleted but the rest of the pipeline remains intact.
     */
    String PIPELINE_DELETE = "pipeline-delete";

    /**
     * When a Seed event is received (update of the pipeline definition), defines the behaviour to adopt.
     *
     * If yes (default), regenerate the pipeline.
     *
     * If no, do not regenerate the pipeline.
     */
    String PIPELINE_AUTO = "pipeline-auto";

    /**
     * When a commit event is received (commit, push), defines the behaviour to adopt.
     *
     * If yes (default), triggers the pipeline.
     *
     * If no, does not trigger the pipeline.
     */
    String PIPELINE_TRIGGER = "pipeline-trigger";

    /**
     * When a comment event is received, defines the parameter which contains the revision/commit to build.
     */
    String PIPELINE_COMMIT = "pipeline-commit";

    /**
     * Property which defines if the pipeline must be started after it has been generated or regenerated.
     *
     * If yes (default), the pipeline is started after generation.
     *
     * If no, it's not started.
     *
     * Note that changing this property needs the branch seed to be regenerated by the branch generator.
     */
    String PIPELINE_START_AUTO = "pipeline-start-auto";
}
