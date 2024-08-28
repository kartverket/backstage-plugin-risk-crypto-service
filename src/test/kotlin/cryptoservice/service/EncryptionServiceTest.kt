package cryptoservice.service

import cryptoservice.model.GCPAccessToken
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class EncryptionServiceTest {
    @Disabled
    @Test
    fun `when gcp access token is available data is successfully encrypted`() {
        val gcpAccessToken = GCPAccessToken("din-access-token")
        val configWithGCPResourceAndAge =
            "creation_rules:\n" +
                "    - key_groups:\n" +
                "        - age:\n" +
                "            - \"age1g9m644t5s95zk6px9mh2kctajqw3guuq2alntgfqu2au6fdz85lq4uupug\"\n" +
                "        - gcp_kms:\n" +
                "            - resource_id: projects/spire-ros-5lmr/locations/europe-west4/keyRings/rosene-team/cryptoKeys/ros-as-code-2"

        val encrypted =
            EncryptionService().encrypt(
                text = "{\"test\":\"test\"}",
                config = configWithGCPResourceAndAge,
                gcpAccessToken = gcpAccessToken,
                riScId = "ad-12314",
            )

        assertThat(encrypted).isNotNull()
        assertThat(encrypted).contains("sops")
    }

    @Test
    fun `when gcp access token is invalid an exception is thrown`() {
        val invalidGcpAccessToken = GCPAccessToken("feil")
        val configWithGCPResourceAndAge =
            "creation_rules:\n" +
                "    - key_groups:\n" +
                "        - age:\n" +
                "            - \"age1g9m644t5s95zk6px9mh2kctajqw3guuq2alntgfqu2au6fdz85lq4uupug\"\n" +
                "        - gcp_kms:\n" +
                "            - resource_id: projects/spire-ros-5lmr/locations/europe-west4/keyRings/rosene-team/cryptoKeys/ros-as-code-2"

        assertThrows<Exception> {
            EncryptionService().encrypt(
                text = "{\"test\":\"test\"}",
                config = configWithGCPResourceAndAge,
                gcpAccessToken = invalidGcpAccessToken,
                riScId = "ad-12314",
            )
        }
    }
}
