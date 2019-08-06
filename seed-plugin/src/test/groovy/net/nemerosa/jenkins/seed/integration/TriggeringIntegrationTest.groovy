package net.nemerosa.jenkins.seed.integration

import net.nemerosa.jenkins.seed.config.EventStrategyConfig
import net.nemerosa.jenkins.seed.config.PipelineConfig
import net.nemerosa.jenkins.seed.integration.git.GitRepo
import net.nemerosa.jenkins.seed.test.JenkinsForbiddenException
import org.junit.Rule
import org.junit.Test

import static net.nemerosa.jenkins.seed.test.TestUtils.uid

/**
 * Testing the triggering of seeds and pipelines using the Seed plug-in.
 */
class TriggeringIntegrationTest {

    @Rule
    public SeedRule jenkins = new SeedRule()

    @Test
    void 'Default seed tree'() {
        // Project name
        def project = uid('p')
        // Git
        def git = GitRepo.prepare('std')
        // Configuration
        def seed = jenkins.defaultSeed()
        // Firing the seed job
        jenkins.fireJob(seed, [
                PROJECT: project,
                PROJECT_SCM_TYPE: 'git',
                PROJECT_SCM_URL: git,
                PROJECT_TRIGGER_IDENTIFIER: '',
                PROJECT_TRIGGER_TYPE: 'http',
                PROJECT_TRIGGER_SECRET: '',
        ]).checkSuccess()
        // Checks the project seed is created
        jenkins.checkJobExists("${project}/${project}-seed")
        // Fires the project seed for the `master` branch
        jenkins.post("seed-http-api/create?project=${project}&branch=master")
        // Checks the result of the project seed
        jenkins.getBuild("${project}/${project}-seed", 1).checkSuccess()
        // Checks the branch seed is created
        jenkins.checkJobExists("${project}/${project}-master/${project}-master-seed")
        // Checks the result of the branch seed
        jenkins.getBuild("${project}/${project}-master/${project}-master-seed", 1).checkSuccess()
        // Checks the branch pipeline is there
        jenkins.checkJobExists("${project}/${project}-master/${project}-master-build")
        jenkins.checkJobExists("${project}/${project}-master/${project}-master-ci")
        jenkins.checkJobExists("${project}/${project}-master/${project}-master-publish")
        // Fires the branch pipeline start
        jenkins.post("seed-http-api/commit?project=${project}&branch=master")
        // Checks the result of the pipeline (ci & publish must have been fired)
        jenkins.getBuild("${project}/${project}-master/${project}-master-build", 1).checkSuccess()
        jenkins.getBuild("${project}/${project}-master/${project}-master-ci", 1).checkSuccess()
        jenkins.getBuild("${project}/${project}-master/${project}-master-publish", 1).checkSuccess()
    }

    @Test
    void 'Default seed tree with trigger identifier'() {
        // Project name
        def project = uid('p')
        // Git
        def git = GitRepo.prepare('std')
        // Configuration
        def seed = jenkins.defaultSeed()
        // Firing the seed job
        jenkins.fireJob(seed, [
                PROJECT: project,
                PROJECT_SCM_TYPE: 'git',
                PROJECT_SCM_URL: git,
                PROJECT_TRIGGER_IDENTIFIER: "nemerosa/${project}",
                PROJECT_TRIGGER_TYPE: 'http',
                PROJECT_TRIGGER_SECRET: '',
        ]).checkSuccess()
        // Checks the project seed is created
        jenkins.checkJobExists("${project}/${project}-seed")
        // Fires the project seed for the `master` branch
        jenkins.post("seed-http-api/create?project=nemerosa/${project}&branch=master")
        // Checks the result of the project seed
        jenkins.getBuild("${project}/${project}-seed", 1).checkSuccess()
        // Checks the branch seed is created
        jenkins.checkJobExists("${project}/${project}-master/${project}-master-seed")
        // Checks the result of the branch seed
        jenkins.getBuild("${project}/${project}-master/${project}-master-seed", 1).checkSuccess()
        // Checks the branch pipeline is there
        jenkins.checkJobExists("${project}/${project}-master/${project}-master-build")
        jenkins.checkJobExists("${project}/${project}-master/${project}-master-ci")
        jenkins.checkJobExists("${project}/${project}-master/${project}-master-publish")
        // Fires the branch pipeline start
        jenkins.post("seed-http-api/commit?project=nemerosa/${project}&branch=master")
        // Checks the result of the pipeline (ci & publish must have been fired)
        jenkins.getBuild("${project}/${project}-master/${project}-master-build", 1).checkSuccess()
        jenkins.getBuild("${project}/${project}-master/${project}-master-ci", 1).checkSuccess()
        jenkins.getBuild("${project}/${project}-master/${project}-master-publish", 1).checkSuccess()
    }

    @Test(expected = JenkinsForbiddenException)
    void 'Default seed tree with trigger identifier, 403 on project name only'() {
        // Project name
        def project = uid('p')
        // Git
        def git = GitRepo.prepare('std')
        // Configuration
        def seed = jenkins.defaultSeed()
        // Firing the seed job
        jenkins.fireJob(seed, [
                PROJECT: project,
                PROJECT_SCM_TYPE: 'git',
                PROJECT_SCM_URL: git,
                PROJECT_TRIGGER_IDENTIFIER: "nemerosa/${project}",
                PROJECT_TRIGGER_TYPE: 'http',
                PROJECT_TRIGGER_SECRET: '',
        ]).checkSuccess()
        // Checks the project seed is created
        jenkins.checkJobExists("${project}/${project}-seed")
        // Fires the project seed for the `master` branch, using the project name only
        // and not the project trigger identifier, results in 403
        jenkins.post("seed-http-api/create?project=${project}&branch=master")
    }

    @Test(expected = JenkinsForbiddenException)
    void 'HTTP API not being enabled'() {
        // Project name
        def project = uid('p')
        // Git
        def git = GitRepo.prepare('std')
        // Configuration
        def seed = jenkins.defaultSeed()
        // Firing the seed job
        jenkins.fireJob(seed, [
                PROJECT: project,
                PROJECT_SCM_TYPE: 'git',
                PROJECT_SCM_URL: git,
                PROJECT_TRIGGER_IDENTIFIER: '',
                PROJECT_TRIGGER_TYPE: '',
                PROJECT_TRIGGER_SECRET: '',
        ]).checkSuccess()
        // Checks the project seed is created
        jenkins.checkJobExists("${project}/${project}-seed")
        // Fires the project seed for the `master` branch
        jenkins.post("seed-http-api/create?project=${project}&branch=master")
    }

    @Test
    void 'HTTP API being enabled'() {
        // Project name
        def project = uid('p')
        // Git
        def git = GitRepo.prepare('std')
        // Configuration
        def seed = jenkins.defaultSeed()
        // Firing the seed job
        jenkins.fireJob(seed, [
                PROJECT: project,
                PROJECT_SCM_TYPE: 'git',
                PROJECT_SCM_URL: git,
                PROJECT_TRIGGER_IDENTIFIER: '',
                PROJECT_TRIGGER_TYPE: 'http',
                PROJECT_TRIGGER_SECRET: '',
        ]).checkSuccess()
        // Checks the project seed is created
        jenkins.checkJobExists("${project}/${project}-seed")
        // Fires the project seed for the `master` branch
        jenkins.post("seed-http-api/create?project=${project}&branch=master")
        // Checks the result of the project seed
        jenkins.getBuild("${project}/${project}-seed", 1).checkSuccess()
    }

    @Test(expected = JenkinsForbiddenException)
    void 'Token not provided'() {
        // Project name
        def project = uid('p')
        // Git
        def git = GitRepo.prepare('std')
        // Configuration
        def seed = jenkins.defaultSeed()
        // Firing the seed job
        jenkins.fireJob(seed, [
                PROJECT: project,
                PROJECT_SCM_TYPE: 'git',
                PROJECT_SCM_URL: git,
                PROJECT_TRIGGER_IDENTIFIER: '',
                PROJECT_TRIGGER_TYPE: 'http',
                PROJECT_TRIGGER_SECRET: 'ABCDEF',
        ]).checkSuccess()
        // Checks the project seed is created
        jenkins.checkJobExists("${project}/${project}-seed")
        // Fires the project seed for the `master` branch
        jenkins.post("seed-http-api/create?project=${project}&branch=master")
    }

    @Test
    void 'Token provided'() {
        // Project name
        def project = uid('p')
        // Git
        def git = GitRepo.prepare('std')
        // Configuration
        def seed = jenkins.defaultSeed()
        // Firing the seed job
        jenkins.fireJob(seed, [
                PROJECT: project,
                PROJECT_SCM_TYPE: 'git',
                PROJECT_SCM_URL: git,
                PROJECT_TRIGGER_IDENTIFIER: '',
                PROJECT_TRIGGER_TYPE: 'http',
                PROJECT_TRIGGER_SECRET: 'ABCDEF',
        ]).checkSuccess()
        // Checks the project seed is created
        jenkins.checkJobExists("${project}/${project}-seed")
        // Fires the project seed for the `master` branch
        jenkins.post("seed-http-api/create?project=${project}&branch=master", '', [
                'X-Seed-Token': 'ABCDEF'
        ])
        // Checks the result of the project seed
        jenkins.getBuild("${project}/${project}-seed", 1).checkSuccess()
    }

    @Test
    void 'No pipeline generation when auto is set to false'() {
        // Project name
        def project = uid('p')
        // Git
        def git = GitRepo.prepare('std')
        // Configuration
        def seed = jenkins.seed(
                new PipelineConfig()
                        .withEventStrategy(new EventStrategyConfig().withAuto(false))
        )
        // Firing the seed job
        jenkins.fireJob(seed, [
                PROJECT: project,
                PROJECT_SCM_TYPE: 'git',
                PROJECT_SCM_URL: git,
                PROJECT_TRIGGER_IDENTIFIER: '',
                PROJECT_TRIGGER_TYPE: 'http',
                PROJECT_TRIGGER_SECRET: '',
        ]).checkSuccess()
        // Checks the project seed is created
        jenkins.checkJobExists("${project}/${project}-seed")
        // Fires the project seed for the `master` branch
        jenkins.post("seed-http-api/create?project=${project}&branch=master")
        // Checks the result of the project seed
        jenkins.getBuild("${project}/${project}-seed", 1).checkSuccess()
        // Checks that the master seed was NOT fired
        def build = jenkins.getBuild("${project}/${project}-master/${project}-master-seed", 2, 30)
        assert build == null: "The master branch automatic generation is not enabled"
    }
}
