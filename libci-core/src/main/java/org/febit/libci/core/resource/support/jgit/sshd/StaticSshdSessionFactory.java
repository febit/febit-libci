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
package org.febit.libci.core.resource.support.jgit.sshd;

import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.febit.lang.util.Lists;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;

@RequiredArgsConstructor(staticName = "create")
public class StaticSshdSessionFactory extends SshdSessionFactory {

    private final Iterable<KeyPair> keys;
    private final ServerKeyDatabase serverKeyDatabase;

    public static StaticSshdSessionFactory create(EncodedKey encodedKey, ServerKeyDatabase db) {
        return StaticSshdSessionFactory.create(parseSshPrivateKey(encodedKey), db);
    }

    @Override
    protected Iterable<KeyPair> getDefaultKeys(File sshDir) {
        return keys;
    }

    @Override
    protected List<Path> getDefaultIdentities(File sshDir) {
        throw new UnsupportedOperationException("StaticSshdSessionFactory does not support default identities");
    }

    @Override
    protected ServerKeyDatabase getServerKeyDatabase(File homeDir, File sshDir) {
        return serverKeyDatabase;
    }

    private static List<KeyPair> parseSshPrivateKey(EncodedKey key) {
        List<KeyPair> keys;
        try {
            keys = Lists.collect(
                    EncodedKeyIdentityProvider.create(key)
                            .loadKeys(null)
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot parse SSH private key: " + key.name(), e);
        } catch (GeneralSecurityException e) {
            throw new IllegalArgumentException("Cannot parse SSH private key: " + key.name(), e);
        }
        if (keys.isEmpty()) {
            throw new IllegalArgumentException("No SSH private key could be parsed: " + key.name());
        }
        return keys;
    }
}

