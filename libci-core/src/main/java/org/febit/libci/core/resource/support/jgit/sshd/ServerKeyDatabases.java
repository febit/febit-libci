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

import lombok.experimental.UtilityClass;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.List;

@UtilityClass
public class ServerKeyDatabases {

    public static ServerKeyDatabase acceptAny() {
        return new AcceptAny();
    }

    private static class AcceptAny implements ServerKeyDatabase {

        @Override
        public List<PublicKey> lookup(
                String connectAddress,
                InetSocketAddress remoteAddress,
                Configuration config
        ) {
            return List.of();
        }

        @Override
        public boolean accept(
                String connectAddress,
                InetSocketAddress remoteAddress,
                PublicKey serverKey,
                Configuration config,
                CredentialsProvider provider
        ) {
            return true;
        }
    }
}
