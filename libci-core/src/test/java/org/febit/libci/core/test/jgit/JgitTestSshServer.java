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
package org.febit.libci.core.test.jgit;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.junit.ssh.SshTestGitServer;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;
import org.febit.libci.core.resource.support.jgit.sshd.ServerKeyDatabases;
import org.febit.libci.core.resource.support.jgit.sshd.StaticSshdSessionFactory;

import java.io.IOException;
import java.security.KeyPair;
import java.util.List;

@Slf4j
@Getter
public class JgitTestSshServer implements AutoCloseable {

    private final SshTestGitServer server;
    private final KeyPair hostKey;
    private final String baseUrl;
    private final int port;

    private final String testUser = "test";
    private final KeyPair testKey;
    private final Repository repository;

    public JgitTestSshServer(Repository repo) throws IOException {
        this.repository = repo;
        this.testKey = JgitTestUtils.generateKeyPair();
        this.hostKey = JgitTestUtils.generateKeyPair();
        this.server = new SshTestGitServer(this.testUser, this.testKey.getPublic(), repo, this.hostKey);
        this.port = this.server.start();
        this.baseUrl = "ssh://" + this.testUser + "@127.0.0.1:" + this.port + "/";
        log.info("JGit testing SSH server is started on auto-allocated port: {}", this.port);
    }

    public void configure(Transport transport) {
        if (!(transport instanceof SshTransport sshTransport)) {
            throw new IllegalStateException("Expected SshTransport, but got: " + transport.getClass());
        }

        var sessionFactory = StaticSshdSessionFactory.create(
                List.of(testKey),
                ServerKeyDatabases.acceptAny()
        );

        sshTransport.setSshSessionFactory(sessionFactory);
    }

    @Override
    public void close() throws IOException {
        this.server.stop();
    }
}
