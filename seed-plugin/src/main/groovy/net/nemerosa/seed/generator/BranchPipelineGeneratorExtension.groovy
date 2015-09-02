package net.nemerosa.seed.generator

import net.nemerosa.seed.config.Configuration
import net.nemerosa.seed.config.SeedNamingStrategyHelper
import net.nemerosa.seed.config.SeedProjectEnvironment

class BranchPipelineGeneratorExtension {

    static final String PIPELINE_GENERATOR_PROPERTY_PATH = 'pipeline-generator-property-path'
    static final String PIPELINE_GENERATOR_EXTENSIONS = 'pipeline-generator-extensions'

    private final SeedProjectEnvironment environment
    private final String branch

    BranchPipelineGeneratorExtension(SeedProjectEnvironment environment, String branch) {
        this.environment = environment
        this.branch = branch
    }

    String generate() {

        List<String> snippets = []

        // Gets the property file name
        String propertyPath = environment.getConfigurationValue(PIPELINE_GENERATOR_PROPERTY_PATH, 'seed/seed.properties')

        /**
         * Parameters
         */
        Map<String, String> parameters = environment.getParameters('branch-parameters')
        if (!parameters.empty) {
            snippets << """\
environmentVariables {
    ${parameters.collect { name, description -> "env('${name}', ${name})" }.join('\n')}
}
"""
        }

        /**
         * SCM parameters
         *
         * The BRANCH_SCM variable is available.
         */
        if (environment.getConfigurationBoolean('branch-scm', false)) {
            snippets << '''\
environmentVariables {
    env('BRANCH_SCM', BRANCH_SCM)
}
'''
        }

        /**
         * Extensions (injection of DSL steps)
         *
         * Gets the list of extension IDs from the project configuration.
         *
         * Gets the extension snippets from the configuration and applies them.
         */
        def extensionIds = environment.getConfigurationList(PIPELINE_GENERATOR_EXTENSIONS)
        extensionIds.each { String extensionId ->
            String extensionDsl = getExtension(extensionId)
            // Adds the extension DSL
            snippets << extensionDsl
        }

        /**
         * Reads information from the property file and generates a Gradle build file
         * for the purpose of download the DSL libraries and extracting the bootstrap
         * script file.
         */
        snippets << """\
configure { node ->
    node / 'builders' / 'net.nemerosa.seed.generator.SeedPipelineGeneratorBuilder' {
        'project' '${environment.id}'
        'projectClass' '${environment.projectClass}'
        'projectScmType' '${environment.scmType}'
        'projectScmUrl' '${environment.scmUrl}'
        'projectScmCredentials' '${environment.scmCredentials}'
        'branch' '${branch}'
        'propertyPath' '${propertyPath}'
    }
}
"""

        /**
         * Defines a Gradle step for the build file generated by the previous step:
         * - downloads the dependencies
         * - extract the DSL bootstrap script from the indicated JAR
         */
        snippets << """\
wrappers {
    injectPasswords()
}
steps {
    shell '''\\
#!/bin/bash
if [ "\${SEED_GRADLE}" == "yes" ]
then
    cd seed
    chmod u+x gradlew
    ./gradlew prepare --refresh-dependencies
fi
'''
}
"""

        /**
         * Runs the script DSL
         */
        snippets << """\
steps {
    dsl {
        external 'seed/\${${SeedPipelineGeneratorHelper.ENV_SEED_DSL_SCRIPT_LOCATION}}'
        removeAction 'DELETE'
        lookupStrategy 'SEED_JOB'
        ignoreExisting false
        additionalClasspath 'seed/lib/*.jar'
    }
}
"""

        /**
         * Fires the branch pipeline
         */
        String branchPipeline = SeedNamingStrategyHelper.getBranchPath(
                environment.namingStrategy.getBranchStart(environment.id),
                environment.namingStrategy.getBranchName(branch)
        )
        snippets << """\
steps {
    dsl {
        text "queue('${branchPipeline}')"
    }
}
"""

        // OK
        return snippets.join('\n')
    }

    protected String getExtension(String id) {
        return Configuration.getFieldInList(
                'extensions',
                environment.projectConfiguration,
                environment.globalConfiguration,
                'id', id,
                'dsl')
    }

}
