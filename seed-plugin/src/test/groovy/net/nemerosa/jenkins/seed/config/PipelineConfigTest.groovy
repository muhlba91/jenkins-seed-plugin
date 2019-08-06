package net.nemerosa.jenkins.seed.config

import org.junit.Test

class PipelineConfigTest {

    @Test
    void 'Default config'() {
        def cfg = PipelineConfig.defaultConfig()
        assert cfg.authorisations == ''
        assert cfg.branchParameters == ''
        assert !cfg.branchSCMParameter
        assert !cfg.destructor

        def strategy = cfg.namingStrategy
        assert strategy != null
        assert strategy.projectFolderPath == ''
        assert strategy.projectSeedName == ''
        assert strategy.projectDestructorName == ''
        assert strategy.branchFolderPath == ''
        assert strategy.branchSeedName == ''
        assert strategy.branchStartName == ''

        def eventStrategy = cfg.eventStrategy
        assert eventStrategy.auto
        assert eventStrategy.delete
    }

    @Test
    void 'Default naming strategy'() {
        PipelineConfig config = new PipelineConfig()
        assert config.getProjectFolder('test') == "test"
        assert config.getProjectSeedJob('test') == "test-seed"
        assert config.getProjectDestructorJob('test') == "test-destructor"
        assert config.getBranchFolderPath('test', "master") == "test-master"
        assert config.getBranchSeedName('test', "master") == "test-master-seed"
        assert config.getBranchStartName('test', "master") == "test-master-build"
        assert config.getBranchName("master") == "master"
    }

    @Test
    void 'Default branch names'() {
        PipelineConfig config = new PipelineConfig()
        assert config.getBranchName("master") == "master"
        assert config.getBranchName("release/1.0") == "release-1.0"
        assert config.getBranchName("feature/great") == "feature-great"
        assert config.getBranchName("RELEASE-1.0") == "release-1.0"
    }

    @Test
    void 'Project authorisations'() {
        def parameters = new ProjectParameters(
                "test",
                "git",
                "https://github.com/nemerosa/ontrack.git",
                "",
                "",
                "",
                ""
        )
        PipelineConfig config = new PipelineConfig()
                .withAuthorisations('''\
                hudson.model.Item.Workspace:jenkins_*
                hudson.model.Item.Read:jenkins_*
                # Comments and empty lines are allowed

                hudson.model.Item.Discover:jenkins_*
                ''')
        def authorisations = config.getProjectAuthorisations(parameters)
        assert authorisations == [
                'hudson.model.Item.Workspace:jenkins_test',
                'hudson.model.Item.Read:jenkins_test',
                'hudson.model.Item.Discover:jenkins_test',
        ]
    }

}
