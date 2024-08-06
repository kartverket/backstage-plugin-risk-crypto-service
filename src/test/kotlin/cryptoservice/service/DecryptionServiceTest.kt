package cryptoservice.service

import cryptoservice.model.GCPAccessToken
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

@Disabled
class DecryptionServiceTest {
    val ageKey = "AGE-SECRET-KEY-18TRT94XGD8SC06JSJX5Q9PFFA9XRR0SYKNCVGVLL0EJTS93YJFSQ89A8RP"
    val invalidAgeKey = ""
    val sopsFileWithShamir1 = "hei: ENC[AES256_GCM,data:r5bHVoU=,iv:M3YhjRcP7EFSUP5uyjsvUcUVttcnBmM4GPDgdxmUi2A=,tag:UZLHS679wF6jSWAfftz+iQ==,type:str]\n" +
            "hva: ENC[AES256_GCM,data:jgxACAo=,iv:fCpggS7M/eFNC1ce0Gmy/7wOgS6sF2igcJwqVqxDy80=,tag:GjWX6R+5GXFewJ50HVTZPQ==,type:str]\n" +
            "sops:\n" +
            "    shamir_threshold: 1\n" +
            "    kms: []\n" +
            "    gcp_kms:\n" +
            "        - resource_id: projects/spire-ros-5lmr/locations/europe-west4/keyRings/rosene-team/cryptoKeys/ros-as-code-2\n" +
            "          created_at: \"2024-08-06T11:07:17Z\"\n" +
            "          enc: CiQAMvdImh2ugjR7IPggRIHlAmnsgnkrmJJK7/VYS8RGA+SPlpYSSQAWhPfdQsTdmcTh2qD4Rgd5vW6lkdjUV3Hp/jN1ERDzPfQL4YHa0pcqE4WV1WLXHOBb6rlSet2xdyJcJ2FbrNPkynCl3o4+fg8=\n" +
            "    azure_kv: []\n" +
            "    hc_vault: []\n" +
            "    age:\n" +
            "        - recipient: age1g9m644t5s95zk6px9mh2kctajqw3guuq2alntgfqu2au6fdz85lq4uupug\n" +
            "          enc: |\n" +
            "            -----BEGIN AGE ENCRYPTED FILE-----\n" +
            "            YWdlLWVuY3J5cHRpb24ub3JnL3YxCi0+IFgyNTUxOSBvNWtXNDJQY2t5cVJUdEp0\n" +
            "            MXM5Zk1FTitucWo2VUtORk9ZVzNGNk83TERzCk9RMmx1M2RRNGZsd2JKamlHNmlK\n" +
            "            YjA0UjNIZTZ4Rk1rMExOcVRLM1FIcWsKLS0tIDY2YWN0d28reWdaTGp4NFUyekcy\n" +
            "            NjJodGNxRlNNbXdlYjM2Q0h3VWlSV1UKfA5PfjTJxu7fcCIvJV5Wd3KirAz+3Jak\n" +
            "            68mpwGaD5VisPKhe1TPrJdcZ/QjzmOIqotoKpYmrzsIm6jYQxGUypw==\n" +
            "            -----END AGE ENCRYPTED FILE-----\n" +
            "    lastmodified: \"2024-08-06T11:07:18Z\"\n" +
            "    mac: ENC[AES256_GCM,data:SZc4vSE4N3lkOpyqn3gLqhPl9E5/zfg+RHK8ZvC8PXxTHYgkVoZUy8o8mHL11wAfhVBlsXpomn/1ASESyzQJ7/dxYNKDUvrywDkiCdnF/OLQi0TElzDKRAhfJHZapCR77TQGGaIFAWZYEsiUiqPGh/f88e9jP8eRqFjAzyEJOME=,iv:wc5WCaQu8UKrml/hn2gCNUnRZeo/38Z8WnZ06S9hJ80=,tag:mexZoBUlgiVJRkugeYfjsA==,type:str]\n" +
            "    pgp: []\n" +
            "    unencrypted_suffix: _unencrypted\n" +
            "    version: 3.9.0"
    val sopsFileWithShamir2 = "hei: ENC[AES256_GCM,data:8DD8C0g=,iv:WegIEU7t+H4eFaBynjK9WpvoJSdzvmE81OVu5FxC62M=,tag:EEI+4l3Y85PhpwiBsfnEkw==,type:str]\n" +
            "hva: ENC[AES256_GCM,data:ybxaqB8=,iv:xRxprceL9Fj3QGS0RRTjg63R23xJxfjYsJw1pHuU4PE=,tag:ZPuwrwSK79OSUG/8nAts4A==,type:str]\n" +
            "sops:\n" +
            "    shamir_threshold: 2\n" +
            "    key_groups:\n" +
            "        - hc_vault: []\n" +
            "          age:\n" +
            "            - recipient: age1g9m644t5s95zk6px9mh2kctajqw3guuq2alntgfqu2au6fdz85lq4uupug\n" +
            "              enc: |\n" +
            "                -----BEGIN AGE ENCRYPTED FILE-----\n" +
            "                YWdlLWVuY3J5cHRpb24ub3JnL3YxCi0+IFgyNTUxOSB1b1dqSmo1YWJSUHVXejhB\n" +
            "                Wkxwa2ttWlZBV2szYTZDUnh3c2xMWS8rbFhzCm9vRjFHbDJUNjFtTE5WUVZoWVln\n" +
            "                Wm9wd05UZGlOSzNqRUdxYmdZY0NxNHMKLS0tIHBvZTQ5UFNDR1Q5MUhRMkdmS1hW\n" +
            "                TG5Gb0RRamtrWUM0L1VjRytXTlRmcE0KV38Q6hW4/6QfTifXG8dbaM3noPDSQtxo\n" +
            "                t6y58dqsEa9Gm0d+WTbZM2wkEaxxhI8TOM7V9ahwThs6MvYdhkYI3mA=\n" +
            "                -----END AGE ENCRYPTED FILE-----\n" +
            "        - gcp_kms:\n" +
            "            - resource_id: projects/spire-ros-5lmr/locations/eur4/keyRings/ROS/cryptoKeys/ros-as-code\n" +
            "              created_at: \"2024-08-06T10:44:38Z\"\n" +
            "              enc: CiQAMVUE/3YwwDvWmZ4OilZLbRcTWHFef1ICqj5SFfG9AOwioVISSgD2ZGIpiNAI+AiE5Ccc+r9IGR/ANzRH9UllvtLL8UaTh+MygyhCoEeYnTr1ievaGz2bjY/oR4sj5lkV6Qt6KRlVX8nJTMQrWBJo\n" +
            "          hc_vault: []\n" +
            "          age: []\n" +
            "    kms: []\n" +
            "    gcp_kms: []\n" +
            "    azure_kv: []\n" +
            "    hc_vault: []\n" +
            "    age: []\n" +
            "    lastmodified: \"2024-08-06T10:44:38Z\"\n" +
            "    mac: ENC[AES256_GCM,data:6KCDbaVisMHW8T/qrAjqG1ir61XGEF996eSTDpsK16VBSEQck6A+/AY5kq9s/UpCKOFKPWqoC6BemCxb6qRAJjpxuq4f5Nzqv9NfS9RFX9qUJPo83UdlDe/eCzUekD4aYjIuLy88qAqv70MuR2GcN0LLCSe8rI8JZNZQ6PXcWlg=,iv:me0zxeGzJPOEWv/fiEzpr82jracN2JjEOVf+AoL61pI=,tag:Ul8XzoMeAjd2yhAjvxRfSw==,type:str]\n" +
            "    pgp: []\n" +
            "    unencrypted_suffix: _unencrypted\n" +
            "    version: 3.9.0"

    // OBS! Remember to remove before committing
    val validGCPAccessToken = GCPAccessToken("ditt-access-token")
    val invalidGCPAccessToken = GCPAccessToken("feil")

    @Test
    fun `when age key is present and shamir is 1 the ciphertext is successfully decrypted`(){
        DecryptionService().decrypt(sopsFileWithShamir1, invalidGCPAccessToken, ageKey)
    }

    @Test
    fun `when gcp access token is valid and shamir is 1 the ciphertext is successfully decrypted`(){
        DecryptionService().decrypt(sopsFileWithShamir1, validGCPAccessToken, invalidAgeKey)
    }

    @Test
    fun `when age key and gcp access token is present and shamir is 2 the ciphertext is successfully decrypted`(){
        DecryptionService().decrypt(sopsFileWithShamir2, validGCPAccessToken, ageKey)
    }

    @Test
    fun `when age key is not present and gcp access token is valid and shamir is 2 the decryption fails`(){
        assertThrows<Exception> { DecryptionService().decrypt(sopsFileWithShamir2, validGCPAccessToken, invalidAgeKey) }
    }

    @Test
    fun `when age and key is present but gcp access token is invalid and shamir is 2 the decryption fails`(){
        assertThrows<Exception> { DecryptionService().decrypt(sopsFileWithShamir2, invalidGCPAccessToken, ageKey) }
    }
}