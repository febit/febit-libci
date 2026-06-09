/*
 * Copyright 2025-present febit.org (support@febit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.febit.libci.jenkins.workflow;

import com.cloudbees.jenkins.plugins.sshcredentials.SSHUserPrivateKey;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.IdCredentials;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import hudson.FilePath;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.Secret;
import org.febit.lang.UncheckedException;
import org.febit.libci.core.ProfileCompiler;
import org.febit.libci.core.ProfileDocument;
import org.febit.libci.core.ProfileLoader;
import org.febit.libci.core.VarsHeap;
import org.febit.libci.core.document.yaml.YamlUtils;
import org.febit.libci.core.predefined.Predefined;
import org.febit.libci.core.resource.loader.GenericPathResourceLoader;
import org.febit.libci.core.resource.loader.JgitRepositoryResourceLoader;
import org.febit.libci.core.resource.source.PathSource;
import org.febit.libci.runtime.PipelineEvaluator;
import org.febit.libci.runtime.PipelinePlanner;
import org.febit.libci.runtime.plan.PipelinePlan;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Serial;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;

import static org.febit.libci.core.spec.support.SpecMapper.jsonify;

public class PipelineResolveExecution extends SynchronousStepExecution<PipelinePlan> {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient String entry;
    private final transient VarsHeap<?> inputVars;
    private final transient PipelineResolveStep.LibraryOptions library;
    private final transient PipelineResolveStep.ProfileOptions profiles;

    private final transient boolean isDebugEnabled;

    @lombok.Builder(builderClassName = "Builder")
    PipelineResolveExecution(
            @lombok.NonNull StepContext context,
            @lombok.NonNull String entry,
            @lombok.NonNull VarsHeap<?> inputVars,
            @lombok.NonNull PipelineResolveStep.LibraryOptions library,
            @lombok.NonNull PipelineResolveStep.ProfileOptions profiles
    ) {
        super(context);
        this.inputVars = inputVars;
        this.library = library;
        this.entry = entry;
        this.profiles = profiles;

        this.isDebugEnabled = Boolean.parseBoolean(inputVars.get(Predefined.LIBCI_DEBUG));
    }

    @Override
    protected PipelinePlan run() throws Exception {
        var workspace = getContext().get(FilePath.class);
        var logger = logger();

        if (isDebugEnabled) {
            DebugLogSupport.debugHeader(logger, "Input variables (" + inputVars.entries().size() + "):");
            inputVars.entries().forEach(e ->
                    logger.printf("  - %s=%s%n", e.name(), jsonify(e.expanded(), true))
            );
        }

        logger.printf("[PLAN] Loading profile document: %s%n", entry);
        var doc = loadDocument(workspace, inputVars, entry);
        doc.resolved();
        logger.printf("[PLAN] Profile document resolved from: %s%n", doc.resource().id());

        if (isDebugEnabled) {
            DebugLogSupport.debugHeader(logger, "Resolved profile document:");
            logger.println(YamlUtils.dump(doc.resolved()));
        }

        var profile = ProfileCompiler.compile(doc);
        logger.printf(
                "[PLAN] Compiled profile with %d stage(s) and %d job(s).%n",
                profile.stages().size(),
                profile.jobs().size()
        );
        if (isDebugEnabled) {
            DebugLogSupport.debugHeader(logger, "Compiled profile:");
            logger.println(jsonify(profile, true));
        }

        logger.println("[PLAN] Planning pipeline execution...");
        var workspaceApi = new JenkinsWorkspaceApi(workspace);
        var spec = PipelineEvaluator.builder()
                .profile(profile)
                .baseVars(inputVars)
                .workspaceApi(workspaceApi)
                .build()
                .evaluate();
        return PipelinePlanner.create(spec, inputVars).plan();
    }

    private ProfileDocument loadDocument(
            FilePath workspace,
            VarsHeap<?> vars,
            String entry
    ) throws Exception {
        var local = loadWorkspaceYaml(workspace);
        var builder = ProfileLoader.loader()
                .entry(local.resource(entry))
                .vars(vars)
                .resourceLoader(GenericPathResourceLoader.get());

        try (var jgitLoader = createJgitLoader()) {
            builder.resourceLoader(jgitLoader);
            return builder.load();
        }
    }

    private PrintStream logger() {
        try {
            return getContext().get(TaskListener.class).getLogger();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UncheckedException(e);
        }
    }

    private PathSource loadWorkspaceYaml(FilePath workspace) throws IOException, InterruptedException {
        if (isDebugEnabled) {
            var logger = logger();
            DebugLogSupport.debugHeader(logger, "Workspace profile scan:");
            DebugLogSupport.printField(logger, "baseDir", workspace);
            DebugLogSupport.printField(logger, "includes", profiles.getIncludes());
            DebugLogSupport.printField(logger, "excludes", profiles.getExcludes());
        }
        var source = JenkinsFilePathSource.of(
                workspace,
                profiles.getIncludes(),
                profiles.getExcludes()
        );
        if (isDebugEnabled) {
            var logger = logger();
            var files = source.mapping().table().values().stream().toList();
            DebugLogSupport.debugHeader(logger, "Workspace profile files:");
            DebugLogSupport.printList(logger, "files", files);
        }
        return source;
    }

    private JgitRepositoryResourceLoader createJgitLoader() throws IOException, InterruptedException {
        var builder = JgitRepositoryResourceLoader.builder()
                .resolveRepoUrlByBaseUrl(
                        library.getBaseUrl(),
                        library.getRepoSuffix()
                );

        var credentialsId = library.getCredentialsId();
        if (credentialsId == null || credentialsId.isBlank()) {
            return builder.build();
        }
        if (isHttpBased(library.getBaseUrl())) {
            configureHttpCredentials(builder, credentialsId);
        } else {
            configureSshCredentials(builder, credentialsId);
        }
        return builder.build();
    }

    private static boolean isHttpBased(String baseUrl) {
        return baseUrl.startsWith("http://")
                || baseUrl.startsWith("https://");
    }

    private void configureHttpCredentials(
            JgitRepositoryResourceLoader.Builder builder,
            String credentialsId
    ) throws IOException, InterruptedException {
        var credentials = requireCredential(credentialsId, StandardUsernamePasswordCredentials.class);
        builder.usernameAndPassword(
                credentials.getUsername(),
                Secret.toString(credentials.getPassword())
        );
    }

    private void configureSshCredentials(
            JgitRepositoryResourceLoader.Builder builder,
            String credentialsId
    ) throws IOException, InterruptedException {
        var credentials = requireCredential(credentialsId, SSHUserPrivateKey.class);
        var privateKeys = credentials.getPrivateKeys();
        if (privateKeys.isEmpty()) {
            throw new IllegalArgumentException("SSH private key credential has no private keys: " + credentialsId);
        }
        builder.sshPrivateKey(
                privateKeys.getFirst(),
                credentials.getId(),
                credentials.getUsername(),
                Secret.toString(credentials.getPassphrase())
        );
    }

    private <C extends IdCredentials & Serializable> C requireCredential(
            String credentialsId,
            Class<C> type
    ) throws IOException, InterruptedException {
        var run = getContext().get(Run.class);
        var credentials = CredentialsProvider.findCredentialById(
                credentialsId,
                type,
                run,
                Collections.emptyList()
        );
        if (credentials != null) {
            return credentials;
        }

        credentials = CredentialsProvider.lookupCredentialsInItem(type, run.getParent(), null, List.of())
                .stream()
                .filter(cred -> credentialsId.equals(cred.getId()))
                .findFirst()
                .orElse(null);
        if (credentials == null) {
            throw new IllegalArgumentException(
                    "Cannot find credential '" + credentialsId + "' as " + type.getSimpleName());
        }
        return credentials;
    }
}
