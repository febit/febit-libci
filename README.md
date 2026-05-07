# Febit LibCI

`Febit LibCI` lets Jenkins execute GitLab-style CI YAML through a shared-library runtime.

Repository roles:

- `libci-core`: load, merge, normalize, and compile CI YAML.
- repo-root `src/` and `vars/`: Jenkins shared-library orchestration.
- `libci-jenkins-support-plugin`: Jenkins steps used by the shared library.

## Choose your usage mode

### 1. Full Jenkins runtime

Use the shared library plus the support plugin.

Best for:

- running `.libci.yml` on Jenkins;
- integrating with GitLab-style CI definitions;
- using Jenkins credentials, agents, and container execution.

### 2. Parse / compile only

Use only `libci-core`.

Best for:

- CI lint / validation tools;
- merged document inspection;
- compiling GitLab-style CI YAML into a typed model for another runtime.

## Quick start: Jenkins runtime

### 1. Build environment

Recommended environment:

- Java 21
- Gradle wrapper: `./gradlew`
- wrapper version: `9.x`
- Jenkins dependency baseline in this repository: BOM `2.561`

### 2. Build the support plugin

Build command:

```bash
./gradlew :febit-libci-jenkins-support-plugin:build
```

Plugin artifact:

- `libci-jenkins-support-plugin/build/libs/*.jpi`

Example output file:

- `libci-jenkins-support-plugin/build/libs/febit-libci-jenkins-support-plugin-<version>.jpi`

Install the generated `.jpi` into Jenkins using your normal plugin installation flow.

### 3. Register the shared library in Jenkins

In Jenkins global shared library configuration, point the library at this repository and expose it with a name such as
`libci`.

Recommended setup:

- use a **self-hosted mirror** or internal Git service when possible;
- pin the shared library to a **versioned tag** for production use;
- if you need a moving target, prefer a **stable release branch**.

Practical guidance:

- preferred: `v1.0.0`, `v1.0.1`, ...
- acceptable: `release/1.x`, `stable`
- avoid for production: `main`, feature branches

### 4. Keep versions aligned

Prefer building and deploying the shared library and the support plugin from the **same tag** or **same release
branch**.

This reduces the risk of API drift between Groovy orchestration code and Jenkins step implementations.

### 5. Minimal Jenkinsfile/Pipeline script

The shared library entrypoint is `vars/libci.groovy`, exposed as the global step `libci`.

```groovy
@Library('libci') _

libci([
    node: 'linux-with-docker',
    scm : [
        url          : 'https://repository.url/repo.git',
        ref          : 'main',
        credentialsId: 'git-credentials-id',
    ],
    vars: [
        APP_ENV: 'ci',
    ],
])
```

### 6. Minimal `.libci.yml`

```yaml
stages:
  - test

test:
  stage: test
  image: alpine:latest
  script:
    - echo "Running tests in $APP_ENV environment..."
```

## Runtime prerequisites

Before using the full Jenkins runtime mode, make sure:

- the support plugin is installed in Jenkins;
- the shared library and support plugin are built from the same tag or compatible release branch;
- Jenkins agents can access the target SCM repository;
- required Jenkins credentials are confi**gured;
- nodes that execute image-based jobs provide** a usable container runtime.

## Build commands by module

Use the module that matches what you are changing.

### Whole repository

```bash
make test
make build
```

### `libci-core`

```bash
./gradlew :febit-libci-core:build
```

### `libci-jenkins-support-plugin`

```bash
./gradlew :febit-libci-jenkins-support-plugin:build
```

### `libci-jenkins-shared-library`

```bash
./gradlew :febit-libci-jenkins-shared-library:build
```

Notes:

- repo-root `src/` and `vars/` are compiled by `:febit-libci-jenkins-shared-library`;
- if you change Jenkins step implementations, validate `:febit-libci-jenkins-support-plugin` too;
- if you only use parse-only mode, you only need `libci-core`.

## Quick validation commands

Recommended quick checks:

```bash
./gradlew :febit-libci-core:build
./gradlew :febit-libci-jenkins-support-plugin:build
./gradlew :febit-libci-jenkins-shared-library:build
```

Broader checks:

```bash
make test
make build
```

## Best practices

### 1. When using Jenkins GitLab Plugin

If Jenkins is triggered by GitLab Plugin events, prefer letting the plugin provide the GitLab event context.

Why:

- `LibciSetup` already reads GitLab Plugin environment variables such as `gitlabActionType`, branch data,
  repository URLs, and merge-request metadata;
- pipeline source and merge-request predefined variables are derived from those values;
- this keeps branch and MR context aligned with the GitLab trigger.

Practical recommendation:

- keep `scm.credentialsId` configured;
- let GitLab-triggered branch/ref context come from the trigger environment when possible;
- avoid hardcoding merge-request-only values in pipeline scripts.

### 2. When integrating with another CI/CD system

If Jenkins is called by another platform, use parameterized configuration and forward the external inputs into
`libci(...)`.

```groovy
libci([
    entry: params.LIBCI_ENTRY ?: '.libci.yml',
    scm : [
        url          : params.GIT_URL,
        ref          : params.GIT_REF,
        credentialsId: params.GIT_CREDENTIALS_ID,
    ],
    vars: [
        APP_ENV: params.APP_ENV,
    ],
])
```

This keeps the Jenkinsfile stable while letting an upstream CI/CD system decide the runtime inputs.

## Using only `libci-core`

If your goal is only to parse, merge, inspect, or compile GitLab-style CI configuration, you only need `libci-core`.

Minimal Java example:

```java
import org.febit.libci.core.ProfileCompiler;
import org.febit.libci.core.ProfileLoader;
import org.febit.libci.core.resource.loader.GenericPathResourceLoader;
import org.febit.libci.core.resource.source.FileSystemSource;

import java.nio.file.Path;

var source = FileSystemSource.create(Path.of("/path/to/repo"));
var doc = ProfileLoader.loader()
    .entry(source.resource(".libci.yml"))
    .resourceLoader(GenericPathResourceLoader.get())
    .load();

var profile = ProfileCompiler.compile(doc);
```

No Jenkins shared library or support plugin is required in this mode.

## Advanced parameters of `libci(...)`

The current shared-library configuration model is defined in `src/febit/libci/LibciConfig.groovy`.

```groovy
libci([
    // optional, Jenkins node label
    node                  : 'linux && docker',

    // optional, path to the entry CI YAML file in the repository
    entry                 : '.libci.yml',

    // Override or inject additional variables into the LibCI runtime. 
    vars                  : [
        LIBCI_DEBUG   : 'false', // default: 'false', set to 'true' to enable debug logging in LibCI
        CI_DEBUG_TRACE: 'false', // default: 'false', set to 'true' to enable debug trace logging in LibCI
        // for more Predefined variables, see `org.febit.libci.core.predefined.Predefined`
    ],

    // Expose Jenkins credentials as variables in the LibCI runtime.
    credentialsBindings    : [
        kind: 'usernamePassword', // credential type, e.g., 'usernamePassword', 'string', 'file', etc.
        id  : 'credentials-id', // Jenkins credentials ID
        var : 'VAR_NAME', // environment variable name to expose the 'variable' part of the credential.
        vars: [
            'username': 'USERNAME_VAR_NAME', // environment variable name to expose the 'username' part of the credential.
            'password': 'PASSWORD_VAR_NAME',  // environment variable name to expose the 'password' part of the credential.
            // for other credential types, the structure of 'vars' may differ based on the credential's data fields.
        ]
    ],

    // function to resolve Kubernetes credentials by ID, used for jobs that require Kubernetes access.
    kubeCredentialsIdLookup: { String id -> null },

    logs                  : [
        // whether to wrap job execution with Jenkins timestamps for better log readability
        // NOTICE: require Timestamper plugin installed in Jenkins when enabled
        timestamps: true,
    ],

    features              : [
        // whether to archive job artifacts at the end of each job execution when artifacts are defined in the CI YAML
        archiveArtifacts: true,
    ],

    jobs                  : [
        // whether to run jobs in parallel when the resolved pipeline allows it
        parallel: true,

        // maximum retry attempts for failed jobs when the CI YAML specifies `retry` behavior
        retryMax: 50,
    ],

    scm                   : [
        // required
        url          : 'https://repository.url/repo.git',

        // optional, no hardcoded setup default
        ref          : 'main',

        // optional, exact commit to checkout
        commitId     : null,

        // optional, but recommended
        credentialsId: 'git-credentials-id',
    ],

    // Options for loading CI YAML and includes from repositories
    library               : [
        baseUrl      : 'https://repository.url',
        credentialsId: 'git-credentials-id',
    ],

    // Options for CI YAML profile loading and merging
    profiles              : [
        includes: '**/*.yml,**/*.yaml',
        excludes: null,
    ],

    container             : [
        // user to run container commands as, in 'user:group' format
        user      : 'root:root',

        // home directory for the container user, used for variable setup and volume mounts
        homeDir   : '/root',

        // working directory for container execution
        projectDir: "/builds/${env.JOB_NAME}",

        // Shells to search for when executing container commands, in order of preference
        shells    : 'bash zsh ash dash sh',

        // whether to always pull container images before execution (once per pipeline run)
        pullAlways: false,

        // extra arguments to pass to the container runtime when launching job containers
        args      : [],

        // registry credentials mapping for authenticated pulls
        registries: [
            // example registry configuration for GitHub Container Registry (GHCR)
            'ghcr.io': [
                host         : 'ghcr.io',
                credentialsId: 'ghcr-credentials',
            ],
        ],
    ],
])
```
