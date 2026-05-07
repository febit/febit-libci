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

import lombok.extern.slf4j.Slf4j;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntry;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.febit.lang.util.Lists;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.security.KeyPair;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.febit.libci.core.resource.support.jgit.sshd.ServerKeyDatabases.acceptAny;
import static org.febit.libci.core.resource.support.jgit.sshd.StaticSshdSessionFactory.create;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class StaticSshdSessionFactoryTest {

    private static final File SSH_DIR = new File(".");

    private record EncodedKeyPair(
            String publicKey,
            EncodedKey privateKey
    ) {
    }

    private static final EncodedKeyPair KEY_ED25519_NO_PASS = new EncodedKeyPair(
            "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIL5i4su+9OKoKOoIRQ5tjmcNCQtgkH+cRl71t8z"
                    + "NdFu6 test@febit-libci",
            EncodedKey.builder()
                    .name("id_ed25519_test_no_pass")
                    .encoded("""
                            -----BEGIN OPENSSH PRIVATE KEY-----
                            b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAAAMwAAAAtzc2gtZW
                            QyNTUxOQAAACC+YuLLvvTiqCjqCEUObY5nDQkLYJB/nEZe9bfMzXRbugAAAJgUBCm7FAQp
                            uwAAAAtzc2gtZWQyNTUxOQAAACC+YuLLvvTiqCjqCEUObY5nDQkLYJB/nEZe9bfMzXRbug
                            AAAECXf7JFkPEZj4eqdf/DIDgNbn0hoVYcaMSivuMB1UlYoL5i4su+9OKoKOoIRQ5tjmcN
                            CQtgkH+cRl71t8zNdFu6AAAAEHJvb3RAamVua2lucy01MDIBAgMEBQ==
                            -----END OPENSSH PRIVATE KEY-----
                            """.stripIndent())
                    .build()
    );

    private static final EncodedKeyPair KEY_ED25519_PASS = new EncodedKeyPair(
            "",
            EncodedKey.builder()
                    .name("ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIPNmjQJSodHe78ahGfOfD4NsW4vZYQ5qoMdp"
                            + "fAxY2YQG test@febit-libci")
                    .passphrase("123abc")
                    .encoded("""
                            -----BEGIN OPENSSH PRIVATE KEY-----
                            b3BlbnNzaC1rZXktdjEAAAAACmFlczI1Ni1jdHIAAAAGYmNyeXB0AAAAGAAAABC8nCpYi/
                            43YGRpkXIN7UDvAAAAGAAAAAEAAAAzAAAAC3NzaC1lZDI1NTE5AAAAIPNmjQJSodHe78ah
                            GfOfD4NsW4vZYQ5qoMdpfAxY2YQGAAAAoLywh0BrAA4pHQsfw5Ihc9NoV1we+OU8iFE4g3
                            jEHMD5T+Ij0xLegmksyBuCly733n6Y5j77uv5oViFAYIX7t59tO13HExPUxx4v2oAhppqJ
                            ePP1MzbmhhMBdsZ9SFrngFuNxeERn1p2m4VZrCo72Y1n6pI1qGvfsM5mXX9fFo7m7EpN66
                            vf8wZH1WA8CtvscSs6xUgWo/k022FzyZy/DAU=
                            -----END OPENSSH PRIVATE KEY-----
                            """.stripIndent())
                    .build()
    );

    private static final EncodedKeyPair KEY_RSA_NO_PASS = new EncodedKeyPair(
            "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQCqDFotlOwlox3IfeNj/+hWK5oGzIMTDjMJ6Yvvu7/"
                    + "fxfc+zM3r4eA5EfXSChP+azqrHV4N4TRYcmWz1qo6RBpSW/RRYPGnuhaYa8hgTS2rPLnOy+PevsOjv"
                    + "NBYL/bTmpE1AkoYbWddpneOF2CmBL+SklakcZX6dYj75ufmta18LLjXHUwsQflN6U58MolPm2Py5Na"
                    + "AsPmJ9Hpo/GJ5TBUg5eae5QIMLX/xZSAkq8YFTJzvZnTPgPGiGD+4furE5Zb3ppmn1KeGZoJ1Dq7nn"
                    + "Xp4GsDulArR9rI3R2YO4byTSijD4vEUDALoy5+OfBOcb/TbOS0E/17qH1rUwlCJ6alLaMmFLraP7rx"
                    + "HmGr2vD7ZrzkgiSNYCHw1RdoHxZ42HXDXY5WR87Cm4GqeNw+qnP4MlQS4NF1O6owps4Ekg1N0baWek"
                    + "0bDLNTY6eH8UWaeKoS/w/azTIu3wSD42Cy16BcTNIb5fizoY58LXMMphBxJMwrHWI1ZMq7fJj63i45"
                    + "8w1c= test@febit-libci",
            EncodedKey.builder()
                    .name("id_rsa_test")
                    .encoded("""
                            -----BEGIN OPENSSH PRIVATE KEY-----
                            b3BlbnNzaC1rZXktdjEAAAAABG5vbmUAAAAEbm9uZQAAAAAAAAABAAABlwAAAAdzc2gtcn
                            NhAAAAAwEAAQAAAYEAqgxaLZTsJaMdyH3jY//oViuaBsyDEw4zCemL77u/38X3PszN6+Hg
                            ORH10goT/ms6qx1eDeE0WHJls9aqOkQaUlv0UWDxp7oWmGvIYE0tqzy5zsvj3r7Do7zQWC
                            /205qRNQJKGG1nXaZ3jhdgpgS/kpJWpHGV+nWI++bn5rWtfCy41x1MLEH5TelOfDKJT5tj
                            8uTWgLD5ifR6aPxieUwVIOXmnuUCDC1/8WUgJKvGBUyc72Z0z4Dxohg/uH7qxOWW96aZp9
                            SnhmaCdQ6u5516eBrA7pQK0fayN0dmDuG8k0oow+LxFAwC6MufjnwTnG/02zktBP9e6h9a
                            1MJQiempS2jJhS62j+68R5hq9rw+2a85IIkjWAh8NUXaB8WeNh1w12OVkfOwpuBqnjcPqp
                            z+DJUEuDRdTuqMKbOBJINTdG2lnpNGwyzU2Onh/FFmniqEv8P2s0yLt8Eg+NgstegXEzSG
                            +X4s6GOfC1zDKYQcSTMKx1iNWTKu3yY+t4uOfMNXAAAFiGan17Rmp9e0AAAAB3NzaC1yc2
                            EAAAGBAKoMWi2U7CWjHch942P/6FYrmgbMgxMOMwnpi++7v9/F9z7Mzevh4DkR9dIKE/5r
                            OqsdXg3hNFhyZbPWqjpEGlJb9FFg8ae6FphryGBNLas8uc7L496+w6O80Fgv9tOakTUCSh
                            htZ12md44XYKYEv5KSVqRxlfp1iPvm5+a1rXwsuNcdTCxB+U3pTnwyiU+bY/Lk1oCw+Yn0
                            emj8YnlMFSDl5p7lAgwtf/FlICSrxgVMnO9mdM+A8aIYP7h+6sTllvemmafUp4ZmgnUOru
                            edengawO6UCtH2sjdHZg7hvJNKKMPi8RQMAujLn458E5xv9Ns5LQT/XuofWtTCUInpqUto
                            yYUuto/uvEeYava8PtmvOSCJI1gIfDVF2gfFnjYdcNdjlZHzsKbgap43D6qc/gyVBLg0XU
                            7qjCmzgSSDU3RtpZ6TRsMs1Njp4fxRZp4qhL/D9rNMi7fBIPjYLLXoFxM0hvl+LOhjnwtc
                            wymEHEkzCsdYjVkyrt8mPreLjnzDVwAAAAMBAAEAAAGAPGI2g4kmchcCNHe/j3sIHdFN2K
                            w2v0WDijmMn4ykDepWac6AMQr8fEeMaxF5GBcrtievhm46EE5PHxVTTW1xi11r3Jn4Nf82
                            ltlvRgMh/HSL5oswV6CWCEa8FRSzKWDxv0kY1qPC2NaMDcDvaQS2om2kprBO+alTA2BzaN
                            GK0VVwLbMRoYWr6aa3YBLx/3rGezIXmYs+kWZnMXCt5zTFK/F3e2UG3RwrGqU2TM9Sef9C
                            myBeynCXXWAPAIzRASolZWgAuGwbUinAT9aG54OtAxYhDctPI7iyP8eXuje29AdhEeW4Hj
                            p1PPKFMrkki8WjbZGhRaMuXG+bwk0Of4bR4IaurKR+MXnADfgiHFHrBop4CA7doX5gPKi3
                            GIxPoI0aVl7O863IgyntaOmFbQe87JqB38tVP0mfcstvU5SQ3ASgkf7ymmtWOpC2N70ita
                            0TwQ7V2xRvIqyHprZ7DQAc/0jc1dtfbBNpRSZ7Le76AuWbQYxKUxSBhKIK+qJ7LRGBAAAA
                            wQDLv6O5DEj5TrSHnbA4Vv7QrhafVt1x30ZRPOrhrF7RO1nRicwrxVghdVFw1hDlBlR18R
                            dZJWZbJs0Lufql16CUxrj0EmGvjscOVmnoUPdJDM8mDr2JzkiI0v9r0i4c6oiWaAaMvmfr
                            D8v5Cxjop0k/yuGR25iMjoKhJQqsv5y06nbx/cI8c3ph93F7jcJeh80J0RoTgxpaJVKUPT
                            LwxxNknxFMDA+hLAPT8ysgjVcLHdGiqAEpfTcJoIK64+sNk2QAAADBANohXcYjKu2S9cmI
                            NTPMYVLQkKU0jKFSt1eoCm/dp3uC1z060Ln3OeHErdegRlqu4f+YBXeOMpZ0bBn7/TZ6N4
                            NUAKbMwK649uan2DrytrTEo/TQKAWmlUsfDwm8KzuE5ORpYR+W4mc+e9UBhbbhbJUhdvAc
                            RrBrKHymJtbSS6QzQ2O551TyGrrlTI5Tla9f2qVKGS6uapE/khKiIgELbzlkv7Zlug0il0
                            +qNidVs7VkpM9YMFCJHKqWgPa1KnUbiQAAAMEAx5ID0bYymw8DGq9/nsF40XoEPfkA4n70
                            AQsUJJw57MN6bujJRhOD3SP2JQ5pUdWs09RihbyisdpdJuvOTagaUGQsAt50ApBPGybIML
                            LVlOImb38weLUiMfOItl19ZdG3eOeqblmt8O5mnU65121P8d6U+PdiRc2jQN10txN07i04
                            iB7urxwu1Py+K4MzjY0L+6WG0HNsfZanPYfG5/TsJcgTqZ3NgV8+jX+ufS93E8Q11y5vK1
                            ksjLN5f+tcBs/fAAAAEHJvb3RAamVua2lucy01MDIBAg==
                            -----END OPENSSH PRIVATE KEY-----
                            """.stripIndent())
                    .build()
    );

    private static KeyPair parse(EncodedKeyPair keyPair) {
        try (var factory = create(keyPair.privateKey(), acceptAny())) {
            var keys = Lists.collect(factory.getDefaultKeys(SSH_DIR));
            assertEquals(1, keys.size(), "Expected exactly one key to be parsed");
            return keys.getFirst();
        }
    }

    @Test
    void parseEd25519NoPass() throws Exception {
        assertTrue(SecurityUtils.isEDDSACurveSupported());
        var key = parse(KEY_ED25519_NO_PASS);
        assertNotNull(key.getPrivate(), "Expected private key to be parsed");
        assertNotNull(key.getPublic(), "Expected public key to be parsed");
        assertTrue(SecurityUtils.compareEDDSAPPublicKeys(
                resolvePublicKey(KEY_ED25519_NO_PASS.publicKey()),
                key.getPublic()
        ));
    }

    @Test
    void parseEd25519Pass() throws Exception {
        assertTrue(SecurityUtils.isEDDSACurveSupported());
        var key = parse(KEY_ED25519_PASS);
        assertNotNull(key.getPrivate(), "Expected private key to be parsed");
        assertNotNull(key.getPublic(), "Expected public key to be parsed");
    }

    @Test
    void parseRsa() throws Exception {
        var key = parse(KEY_RSA_NO_PASS);
        assertNotNull(key.getPrivate(), "Expected private key to be parsed");
        assertNotNull(key.getPublic(), "Expected public key to be parsed");
        assertTrue(KeyUtils.compareRSAKeys(
                (RSAPublicKey) resolvePublicKey(KEY_RSA_NO_PASS.publicKey()),
                (RSAPublicKey) key.getPublic()
        ));
    }

    @Test
    void getDefaultIdentitiesUnsupported() {
        try (var factory = create(KEY_RSA_NO_PASS.privateKey(), acceptAny())) {
            assertThrows(UnsupportedOperationException.class, () -> factory.getDefaultIdentities(SSH_DIR));
        }
    }

    @Test
    void getServerKeyDatabaseReturnsConfiguredInstance() {
        var db = acceptAny();
        try (var factory = create(KEY_RSA_NO_PASS.privateKey(), db)) {
            assertThat(factory.getServerKeyDatabase(SSH_DIR, SSH_DIR))
                    .isSameAs(db);
        }
    }

    private static PublicKey resolvePublicKey(String encoded) throws Exception {
        return PublicKeyEntry.parsePublicKeyEntry(encoded)
                .resolvePublicKey(null, Map.of(), null);
    }

}
