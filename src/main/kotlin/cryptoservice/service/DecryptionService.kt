package cryptoservice.service

import cryptoservice.exception.exceptions.SOPSDecryptionException
import cryptoservice.model.GCPAccessToken
import cryptoservice.model.RiScWithConfig
import cryptoservice.model.SopsConfig
import org.springframework.boot.context.properties.ConfigurationProperties
import cryptoservice.service.validation.CryptoValidation
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader

@ConfigurationProperties(prefix = "sops.decryption")
@Service
class DecryptionService {
    private val processBuilder = ProcessBuilder().redirectErrorStream(true)
    lateinit var backendPublicKey: String
    lateinit var securityTeamPublicKey: String
    lateinit var securityPlatformPublicKey: String

    fun extractSopsConfig(ciphertext: String): SopsConfig {
        val rootNode = YamlUtils.objectMapper.readTree(ciphertext)
        val sopsNode =
            rootNode.get("sops")
                ?: throw IllegalArgumentException(
                    "No sops configuration found in ciphertext",
                )
        val sopsConfig =
            YamlUtils.objectMapper.treeToValue(sopsNode, SopsConfig::class.java)
        val cleanConfig =
            sopsConfig.copy(
                gcpKms =
                    sopsConfig.key_groups.flatMap { it.gcp_kms ?: emptyList() },
                age =
                    sopsConfig
                        .key_groups
                        .flatMap { it.age ?: emptyList() }
                        .filter {
                            it.recipient != securityTeamPublicKey
                        }.filter { it.recipient != backendPublicKey }
                        .filter {
                            it.recipient != securityPlatformPublicKey
                        },
                shamir_threshold = sopsConfig.shamir_threshold,
                lastmodified = sopsConfig.lastmodified,
                version = sopsConfig.version,
                key_groups = emptyList(),
            )
        return cleanConfig
    }

    fun decryptWithSopsConfig(
        ciphertext: String,
        gcpAccessToken: GCPAccessToken,
        sopsAgeKey: String,
    ): RiScWithConfig =
        try {
            val sopsConfig = extractSopsConfig(ciphertext)
            val plaintext = decrypt(ciphertext, gcpAccessToken, sopsAgeKey)
            RiScWithConfig(plaintext, sopsConfig)
        } catch (e: Exception) {
            throw e
        }

    fun decrypt(
        ciphertext: String,
        gcpAccessToken: GCPAccessToken,
        sopsAgeKey: String,
    ): String =
        try {
            if (!CryptoValidation.isValidGCPToken(gcpAccessToken.value)) {
                throw SOPSDecryptionException("Invalid GCP Token")
            }

            if (!CryptoValidation.isValidAgeSecretKey(sopsAgeKey)) {
                throw SOPSDecryptionException("Invalid age key")
            }

            processBuilder
                .command(
                    listOf(
                        "sh",
                        "-c",
                        "SOPS_AGE_KEY=$sopsAgeKey GOOGLE_OAUTH_ACCESS_TOKEN=${gcpAccessToken.value} " +
                            "sops decrypt --input-type yaml --output-type json /dev/stdin",
                    ),
                ).start()
                .run {
                    outputStream
                        .buffered()
                        .also { it.write(ciphertext.toByteArray()) }
                        .close()
                    val result =
                        BufferedReader(InputStreamReader(inputStream))
                            .readText()
                    when (waitFor()) {
                        EXECUTION_STATUS_OK -> result
                        else -> {
                            throw SOPSDecryptionException(
                                message =
                                    "Decrypting message failed with error: $result",
                            )
                        }
                    }
                }
        } catch (e: Exception) {
            throw e
        }
}
