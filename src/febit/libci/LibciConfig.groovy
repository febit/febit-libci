package febit.libci

import org.febit.libci.core.predefined.git.GitScmMetadata
import org.febit.libci.extern.GitMetadataParser

class LibciConfig {

    String node
    String entry

    ScmConfig scm
    LibraryOptions library

    Jobs jobs
    Logs logs
    Features features
    ContainerConfig container

    ProfileOptions profiles

    Map<String, String> vars
    List<CredentialsBinding> credentialsBindings

    Closure<String> kubeCredentialsIdLookup

    static class CredentialsBinding {
        String kind
        String id
        String var
        Map<String, String> vars
    }

    static class ContainerRegistry {
        String host
        String credentialsId
    }

    static class ContainerConfig {
        String user
        String homeDir
        String projectDir
        String shells
        Boolean pullAlways
        List<Object> args
        Map<String, ContainerRegistry> registries = [:]

        ContainerConfig registry(String host, String credentialsId) {
            this.registries[host] = new ContainerRegistry(
                host: host, credentialsId: credentialsId
            )
            return this
        }
    }

    static class ScmConfig {
        String url
        String ref
        String commitId
        String credentialsId

        private GitScmMetadata metadata

        GitScmMetadata getMetadata() {
            if (!metadata) {
                metadata = GitMetadataParser.fromRepoUrl(url)
            }
            return metadata
        }
    }

    static class Logs {
        Boolean timestamps
    }

    static class Jobs {
        Boolean parallel
        Integer retryMax
    }

    static class LibraryOptions {
        String baseUrl
        String credentialsId
    }

    static class Features {
        Boolean archiveArtifacts
    }

    static class ProfileOptions {
        String includes
        String excludes
    }
}
