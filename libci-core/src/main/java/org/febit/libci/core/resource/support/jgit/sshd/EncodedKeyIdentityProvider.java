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
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.sshd.common.NamedResource;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.keyprovider.KeyIdentityProvider;
import org.apache.sshd.common.session.SessionContext;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.febit.lang.util.Lists;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@RequiredArgsConstructor(staticName = "create")
public class EncodedKeyIdentityProvider implements KeyIdentityProvider {

    private final AtomicReference<@Nullable List<KeyPair>> resolved = new AtomicReference<>(null);

    @Nullable
    private final EncodedKey key;

    @Override
    public Iterable<KeyPair> loadKeys(@Nullable SessionContext session) throws IOException, GeneralSecurityException {
        var keys = this.resolved.get();
        if (keys != null) {
            return keys;
        }
        keys = resolve(session);
        this.resolved.set(keys);
        return keys;
    }

    private List<KeyPair> resolve(@Nullable SessionContext session)
            throws GeneralSecurityException, IOException {
        if (key == null) {
            return List.of();
        }

        var in = ReaderInputStream.builder()
                .setCharset(StandardCharsets.UTF_8)
                .setReader(new StringReader(key.encoded()))
                .get();

        return Lists.collect(SecurityUtils.loadKeyPairIdentities(
                session,
                NamedResource.ofName(key.name()),
                in,
                FilePasswordProvider.of(key.passphrase())
        ));
    }
}
