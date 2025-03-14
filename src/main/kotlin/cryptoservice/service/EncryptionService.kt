package cryptoservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import cryptoservice.exception.exceptions.SopsEncryptionException
import cryptoservice.model.GCPAccessToken
import cryptoservice.model.SopsConfig
import cryptoservice.service.validation.CryptoValidation
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@ConfigurationProperties(prefix = "sops.encryption")
@Service
class EncryptionService {
    private val processBuilder = ProcessBuilder().redirectErrorStream(true)
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    private val logger = LoggerFactory.getLogger(EncryptionService::class.java)
    lateinit var backendPublicKey: String
    lateinit var securityTeamPublicKey: String
    lateinit var securityPlatformPublicKey: String

    fun encrypt(
        text: String,
        config: SopsConfig,
        gcpAccessToken: GCPAccessToken,
        riScId: String,
    ): String =
        try {
            if (!CryptoValidation.isValidGCPToken(gcpAccessToken.value)) {
                throw SopsEncryptionException("Invalid GCP Token", riScId)
            }

            // Create key groups configuration
            val keyGroups =
                listOfNotNull(
                    mapOf(
                        "age" to listOf(securityTeamPublicKey),
                        "gcp_kms" to
                            listOf(
                                mapOf("resource_id" to config.gcp_kms?.first()?.resource_id),
                            ),
                    ),
                    mapOf(
                        "age" to listOf(backendPublicKey, securityPlatformPublicKey),
                    ),
                    mapOf(
                        "age" to
                            config.age
                                ?.filter { it.recipient !in setOf(securityTeamPublicKey, backendPublicKey, securityPlatformPublicKey) },
                    ),
                )

            // Create SOPS config
            val sopsConfig =
                mapOf(
                    "creation_rules" to
                        listOf(
                            mapOf(
                                "shamir_threshold" to config.shamir_threshold,
                                "key_groups" to keyGroups,
                            ),
                        ),
                )

            // Create temporary config file
            val prefix = randomBech32("sopsConfig-", 6) + System.currentTimeMillis()
            val tempConfigFile = File.createTempFile(prefix, ".yaml")
            tempConfigFile.writeText(yamlMapper.writeValueAsString(sopsConfig))
            tempConfigFile.deleteOnExit()

            processBuilder
                .command(
                    "sh",
                    "-c",
                    "GOOGLE_OAUTH_ACCESS_TOKEN=${gcpAccessToken.value} " +
                        "sops --encrypt " +
                        "--input-type json " +
                        "--output-type yaml " +
                        "--config ${tempConfigFile.absolutePath} " +
                        "/dev/stdin",
                ).start()
                .run {
                    outputStream.buffered().also { it.write(text.toByteArray()) }.close()
                    val result = BufferedReader(InputStreamReader(inputStream)).readText()
                    when (waitFor()) {
                        EXECUTION_STATUS_OK -> {
                            tempConfigFile.delete()
                            result
                        }
                        else -> {
                            tempConfigFile.delete()
                            logger.error("IOException from encrypting yaml with error code ${exitValue()}: $result")
                            throw SopsEncryptionException(
                                message = "Failed when encrypting RiSc with ID: $riScId ",
                                riScId = riScId,
                            )
                        }
                    }
                }
        } catch (e: Exception) {
            logger.error("Encryption failed.", e)
            throw e
        }

    companion object {
        private const val EXECUTION_STATUS_OK = 0
    }
}
