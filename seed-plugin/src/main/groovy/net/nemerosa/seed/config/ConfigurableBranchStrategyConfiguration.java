package net.nemerosa.seed.config;

import java.util.Collection;
import java.util.Map;

public class ConfigurableBranchStrategyConfiguration extends Configuration {

    private final String id;
    private final String seedExpression;
    private final String destructorExpression;
    private final String branchSeedExpression;
    private final String branchStartExpression;
    private final String branchNameExpression;
    private final Collection<String> branchNamePrefixes;
    private final String commitParameter;

    public ConfigurableBranchStrategyConfiguration(Map<String, ?> data) {
        super(data);
        this.id = getString("id");
        this.seedExpression = getString("seed-expression", false, "${project}/${project}-seed");
        this.destructorExpression = getString("destructor-expression", false, "${project}/${project}-destructor");
        this.branchSeedExpression = getString("branch-seed-expression", false, "${project}/${project}-*/${project}-*-seed");
        this.branchStartExpression = getString("branch-start-expression", false, "${project}/${project}-*/${project}-*-build");
        this.branchNameExpression = getString("branch-name-expression", false, "${branch}");
        this.branchNamePrefixes = getListString("branch-name-prefixes");
        this.commitParameter = getString("commit-parameter", false, "COMMIT");
    }

    public String getId() {
        return id;
    }

    public String getSeedExpression() {
        return seedExpression;
    }

    public String getDestructorExpression() {
        return destructorExpression;
    }

    public String getBranchSeedExpression() {
        return branchSeedExpression;
    }

    public String getBranchStartExpression() {
        return branchStartExpression;
    }

    public String getBranchNameExpression() {
        return branchNameExpression;
    }

    public Collection<String> getBranchNamePrefixes() {
        return branchNamePrefixes;
    }

    public String getCommitParameter() {
        return commitParameter;
    }

    public static ConfigurableBranchStrategyConfiguration of(Map<String, ?> input) {
        return new ConfigurableBranchStrategyConfiguration(input);
    }
}
