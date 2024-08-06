package cryptoservice.service

import cryptoservice.model.GCPAccessToken
import cryptoservice.service.EncryptionService
import org.junit.jupiter.api.Test

class EncryptionServiceTest {
    @Test
    fun `encrypt data`() {
        val encrypted = EncryptionService().encrypt(
            text = "{\"test\":\"test\"}",
            config = "creation_rules:\n" +
                    "    - key_groups:\n" +
                    "        - age:\n" +
                    "            - \"age1g9m644t5s95zk6px9mh2kctajqw3guuq2alntgfqu2au6fdz85lq4uupug\"\n"+
                    "        - gcp_kms:\n" +
                    "            - resource_id: projects/spire-ros-5lmr/locations/europe-west4/keyRings/rosene-team/cryptoKeys/ros-as-code-2",
            gcpAccessToken = GCPAccessToken("xxx-din-gcp-token"),
            riScId = "ad-12314"
        )

        println(encrypted)
    }
}