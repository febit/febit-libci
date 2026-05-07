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
package org.febit.libci.core.predefined;

import lombok.experimental.UtilityClass;

/**
 * <a href="https://docs.gitlab.com/ci/variables/predefined_variables/">...</a>
 */
@UtilityClass
public class Predefined {

    @SuppressWarnings({
            "java:S115", // naming convention
    })
    public static final String __LIBCI_ = "__LIBCI_"; // NOPMD

    public static final String LIBCI_DEBUG = "LIBCI_DEBUG";

    public static final String LIBCI_STAGE_IID = "LIBCI_STAGE_IID";
    public static final String LIBCI_STAGE_SLUG = "LIBCI_STAGE_SLUG";
    public static final String LIBCI_STAGE_STARTED_AT = "LIBCI_STAGE_STARTED_AT";

    public static final String LIBCI_JOB_IID = "LIBCI_JOB_IID";
    public static final String LIBCI_JOB_SLUG = "LIBCI_JOB_SLUG";
    public static final String LIBCI_JOB_MATRIX_IID = "LIBCI_JOB_MATRIX_IID";
    public static final String LIBCI_JOB_RETRY_ATTEMPT = "LIBCI_JOB_RETRY_ATTEMPT";
    public static final String LIBCI_JOB_RETRY_MAX = "LIBCI_JOB_RETRY_MAX";

    public static final String KUBE_NAMESPACE = "KUBE_NAMESPACE";

    public static final String CI = "CI";
    public static final String CI_DEBUG_TRACE = "CI_DEBUG_TRACE";
    public static final String CI_DEBUG_SERVICES = "CI_DEBUG_SERVICES";

    public static final String CI_CONFIG_PATH = "CI_CONFIG_PATH";
    public static final String CI_CONFIG_REF_URI = "CI_CONFIG_REF_URI";

    public static final String CI_JOB_ID = "CI_JOB_ID";
    public static final String CI_JOB_IMAGE = "CI_JOB_IMAGE";
    public static final String CI_JOB_NAME = "CI_JOB_NAME";
    public static final String CI_JOB_STAGE = "CI_JOB_STAGE";
    public static final String CI_JOB_STARTED_AT = "CI_JOB_STARTED_AT";
    public static final String CI_JOB_STATUS = "CI_JOB_STATUS";
    public static final String CI_JOB_TIMEOUT = "CI_JOB_TIMEOUT";
    public static final String CI_JOB_TOKEN = "CI_JOB_TOKEN";
    public static final String CI_JOB_URL = "CI_JOB_URL";

    public static final String CI_BUILDS_DIR = "CI_BUILDS_DIR";
    public static final String CI_CONCURRENT_ID = "CI_CONCURRENT_ID";
    public static final String CI_CONCURRENT_PROJECT_ID = "CI_CONCURRENT_PROJECT_ID";

    public static final String CI_PIPELINE_ID = "CI_PIPELINE_ID";
    public static final String CI_PIPELINE_IID = "CI_PIPELINE_IID";
    public static final String CI_PIPELINE_SOURCE = "CI_PIPELINE_SOURCE";
    public static final String CI_PIPELINE_TRIGGERED = "CI_PIPELINE_TRIGGERED";
    public static final String CI_PIPELINE_URL = "CI_PIPELINE_URL";
    public static final String CI_PIPELINE_CREATED_AT = "CI_PIPELINE_CREATED_AT";
    public static final String CI_PIPELINE_NAME = "CI_PIPELINE_NAME";
    public static final String CI_PIPELINE_SCHEDULE_DESCRIPTION = "CI_PIPELINE_SCHEDULE_DESCRIPTION";

    public static final String CI_RUNNER_ID = "CI_RUNNER_ID";
    public static final String CI_RUNNER_REVISION = "CI_RUNNER_REVISION";
    public static final String CI_RUNNER_SHORT_TOKEN = "CI_RUNNER_SHORT_TOKEN";
    public static final String CI_RUNNER_TAGS = "CI_RUNNER_TAGS";
    public static final String CI_RUNNER_VERSION = "CI_RUNNER_VERSION";

    public static final String CI_NODE_TOTAL = "CI_NODE_TOTAL";
    public static final String CI_NODE_INDEX = "CI_NODE_INDEX";
    public static final String CI_DISPOSABLE_ENVIRONMENT = "CI_DISPOSABLE_ENVIRONMENT";

    public static final String CI_SERVER = "CI_SERVER";
    public static final String CI_SERVER_FQDN = "CI_SERVER_FQDN";
    public static final String CI_SERVER_HOST = "CI_SERVER_HOST";
    public static final String CI_SERVER_NAME = "CI_SERVER_NAME";
    public static final String CI_SERVER_PORT = "CI_SERVER_PORT";
    public static final String CI_SERVER_PROTOCOL = "CI_SERVER_PROTOCOL";
    public static final String CI_SERVER_REVISION = "CI_SERVER_REVISION";
    public static final String CI_SERVER_SHELL_SSH_HOST = "CI_SERVER_SHELL_SSH_HOST";
    public static final String CI_SERVER_SHELL_SSH_PORT = "CI_SERVER_SHELL_SSH_PORT";
    public static final String CI_SERVER_TLS_CA_FILE = "CI_SERVER_TLS_CA_FILE";
    public static final String CI_SERVER_TLS_CERT_FILE = "CI_SERVER_TLS_CERT_FILE";
    public static final String CI_SERVER_TLS_KEY_FILE = "CI_SERVER_TLS_KEY_FILE";
    public static final String CI_SERVER_URL = "CI_SERVER_URL";
    public static final String CI_SERVER_VERSION = "CI_SERVER_VERSION";
    public static final String CI_SERVER_VERSION_MAJOR = "CI_SERVER_VERSION_MAJOR";
    public static final String CI_SERVER_VERSION_MINOR = "CI_SERVER_VERSION_MINOR";
    public static final String CI_SERVER_VERSION_PATCH = "CI_SERVER_VERSION_PATCH";

    public static final String CI_ENVIRONMENT_ACTION = "CI_ENVIRONMENT_ACTION";
    public static final String CI_ENVIRONMENT_ID = "CI_ENVIRONMENT_ID";
    public static final String CI_ENVIRONMENT_NAME = "CI_ENVIRONMENT_NAME";
    public static final String CI_ENVIRONMENT_SLUG = "CI_ENVIRONMENT_SLUG";
    public static final String CI_ENVIRONMENT_TIER = "CI_ENVIRONMENT_TIER";
    public static final String CI_ENVIRONMENT_URL = "CI_ENVIRONMENT_URL";

    public static final String CI_KUBERNETES_ACTIVE = "CI_KUBERNETES_ACTIVE";

    public static final String CI_REPOSITORY_URL = "CI_REPOSITORY_URL";
    public static final String CI_DEFAULT_BRANCH = "CI_DEFAULT_BRANCH";
    public static final String CI_DEFAULT_BRANCH_SLUG = "CI_DEFAULT_BRANCH_SLUG";

    public static final String CI_PROJECT_CLASSIFICATION_LABEL = "CI_PROJECT_CLASSIFICATION_LABEL";
    public static final String CI_PROJECT_DESCRIPTION = "CI_PROJECT_DESCRIPTION";
    public static final String CI_PROJECT_DIR = "CI_PROJECT_DIR";
    public static final String CI_PROJECT_ID = "CI_PROJECT_ID";
    public static final String CI_PROJECT_NAME = "CI_PROJECT_NAME";
    public static final String CI_PROJECT_NAMESPACE = "CI_PROJECT_NAMESPACE";
    public static final String CI_PROJECT_NAMESPACE_ID = "CI_PROJECT_NAMESPACE_ID";
    public static final String CI_PROJECT_NAMESPACE_SLUG = "CI_PROJECT_NAMESPACE_SLUG";
    public static final String CI_PROJECT_PATH = "CI_PROJECT_PATH";
    public static final String CI_PROJECT_PATH_SLUG = "CI_PROJECT_PATH_SLUG";
    public static final String CI_PROJECT_REPOSITORY_LANGUAGES = "CI_PROJECT_REPOSITORY_LANGUAGES";
    public static final String CI_PROJECT_ROOT_NAMESPACE = "CI_PROJECT_ROOT_NAMESPACE";
    public static final String CI_PROJECT_TITLE = "CI_PROJECT_TITLE";
    public static final String CI_PROJECT_TOPICS = "CI_PROJECT_TOPICS";
    public static final String CI_PROJECT_URL = "CI_PROJECT_URL";
    public static final String CI_PROJECT_VISIBILITY = "CI_PROJECT_VISIBILITY";

    public static final String CI_COMMIT_AUTHOR = "CI_COMMIT_AUTHOR";
    public static final String CI_COMMIT_AUTHOR_EMAIL = "CI_COMMIT_AUTHOR_EMAIL";
    public static final String CI_COMMIT_AUTHOR_NAME = "CI_COMMIT_AUTHOR_NAME";
    public static final String CI_COMMIT_BEFORE_SHA = "CI_COMMIT_BEFORE_SHA";
    public static final String CI_COMMIT_BRANCH = "CI_COMMIT_BRANCH";
    public static final String CI_COMMIT_DESCRIPTION = "CI_COMMIT_DESCRIPTION";
    public static final String CI_COMMIT_MESSAGE = "CI_COMMIT_MESSAGE";
    public static final String CI_COMMIT_REF_NAME = "CI_COMMIT_REF_NAME";
    public static final String CI_COMMIT_REF_PROTECTED = "CI_COMMIT_REF_PROTECTED";
    public static final String CI_COMMIT_REF_SLUG = "CI_COMMIT_REF_SLUG";
    public static final String CI_COMMIT_SHA = "CI_COMMIT_SHA";
    public static final String CI_COMMIT_SHORT_SHA = "CI_COMMIT_SHORT_SHA";
    public static final String CI_COMMIT_TIMESTAMP = "CI_COMMIT_TIMESTAMP";
    public static final String CI_COMMIT_TITLE = "CI_COMMIT_TITLE";

    public static final String CI_MERGE_REQUEST_APPROVED = "CI_MERGE_REQUEST_APPROVED";
    public static final String CI_MERGE_REQUEST_ASSIGNEES = "CI_MERGE_REQUEST_ASSIGNEES";
    public static final String CI_MERGE_REQUEST_DESCRIPTION = "CI_MERGE_REQUEST_DESCRIPTION";
    public static final String CI_MERGE_REQUEST_DESCRIPTION_IS_TRUNCATED = "CI_MERGE_REQUEST_DESCRIPTION_IS_TRUNCATED";
    public static final String CI_MERGE_REQUEST_DRAFT = "CI_MERGE_REQUEST_DRAFT";
    public static final String CI_MERGE_REQUEST_EVENT_TYPE = "CI_MERGE_REQUEST_EVENT_TYPE";
    public static final String CI_MERGE_REQUEST_ID = "CI_MERGE_REQUEST_ID";
    public static final String CI_MERGE_REQUEST_IID = "CI_MERGE_REQUEST_IID";
    public static final String CI_MERGE_REQUEST_LABELS = "CI_MERGE_REQUEST_LABELS";
    public static final String CI_MERGE_REQUEST_MILESTONE = "CI_MERGE_REQUEST_MILESTONE";
    public static final String CI_MERGE_REQUEST_REF_PATH = "CI_MERGE_REQUEST_REF_PATH";
    public static final String CI_MERGE_REQUEST_SQUASH_ON_MERGE = "CI_MERGE_REQUEST_SQUASH_ON_MERGE";
    public static final String CI_MERGE_REQUEST_TITLE = "CI_MERGE_REQUEST_TITLE";

    public static final String CI_MERGE_REQUEST_DIFF_BASE_SHA = "CI_MERGE_REQUEST_DIFF_BASE_SHA";
    public static final String CI_MERGE_REQUEST_DIFF_ID = "CI_MERGE_REQUEST_DIFF_ID";
    public static final String CI_MERGE_REQUEST_PROJECT_ID = "CI_MERGE_REQUEST_PROJECT_ID";
    public static final String CI_MERGE_REQUEST_PROJECT_PATH = "CI_MERGE_REQUEST_PROJECT_PATH";
    public static final String CI_MERGE_REQUEST_PROJECT_URL = "CI_MERGE_REQUEST_PROJECT_URL";
    public static final String CI_MERGE_REQUEST_SOURCE_BRANCH_NAME = "CI_MERGE_REQUEST_SOURCE_BRANCH_NAME";
    public static final String CI_MERGE_REQUEST_SOURCE_BRANCH_PROTECTED = "CI_MERGE_REQUEST_SOURCE_BRANCH_PROTECTED";
    public static final String CI_MERGE_REQUEST_SOURCE_BRANCH_SHA = "CI_MERGE_REQUEST_SOURCE_BRANCH_SHA";
    public static final String CI_MERGE_REQUEST_SOURCE_PROJECT_ID = "CI_MERGE_REQUEST_SOURCE_PROJECT_ID";
    public static final String CI_MERGE_REQUEST_SOURCE_PROJECT_PATH = "CI_MERGE_REQUEST_SOURCE_PROJECT_PATH";
    public static final String CI_MERGE_REQUEST_SOURCE_PROJECT_URL = "CI_MERGE_REQUEST_SOURCE_PROJECT_URL";
    public static final String CI_MERGE_REQUEST_TARGET_BRANCH_NAME = "CI_MERGE_REQUEST_TARGET_BRANCH_NAME";
    public static final String CI_MERGE_REQUEST_TARGET_BRANCH_PROTECTED = "CI_MERGE_REQUEST_TARGET_BRANCH_PROTECTED";
    public static final String CI_MERGE_REQUEST_TARGET_BRANCH_SHA = "CI_MERGE_REQUEST_TARGET_BRANCH_SHA";

}
