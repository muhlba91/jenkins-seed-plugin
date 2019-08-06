package net.nemerosa.jenkins.seed.generator.scm

import net.nemerosa.jenkins.seed.generator.scm.git.GitSCMService

class SCMServiceRegistryImpl implements SCMServiceRegistry {

    private final Map<String, SCMService> scmServices = [
            git: new GitSCMService(),
    ]

    @Override
    SCMService getScm(String id) {
        SCMService service = scmServices[id]
        if (service) {
            return service
        } else {
            throw new SCMServiceNotDefinedException(id)
        }
    }
}
