package cryptoservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import cryptoservice.exception.exceptions.SopsEncryptionException
import cryptoservice.model.GCPAccessToken
import cryptoservice.model.KeyGroup
import cryptoservice.model.SopsConfig
import cryptoservice.model.GcpKmsEntry
import cryptoservice.model.AgeEntry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.File

@Service
class EncryptionService {
    private val processBuilder = ProcessBuilder().redirectErrorStream(true)
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    private val logger = LoggerFactory.getLogger(EncryptionService::class.java)
    private val securityTeamPublicKey = "age145s860ux96jvx6d7nwvzar588qjmgv5p47sp6nmmt2jnmhqh4scqcuk0mg"
    private val backendPublicKey = "age18e0t6ve0vdxqzzjt7rxf0r6vzc37fhs5cad2qz40r02c3spzgvvq8uxz23"
    private val securityPlatformPublicKey = "age1kjpgclkjev08aa8l2uy277gn0cngrkrkazt240405ezqywkm5axqt3d3tq"

    private fun findGcpKmsEntry(keyGroups: List<KeyGroup>): GcpKmsEntry {
        return keyGroups.flatMap { it.gcp_kms ?: emptyList() }.first()
    }

    private fun buildSopsKeyArguments(config: SopsConfig): List<String> {
        // Create a set of our predefined keys for easy comparison
        val predefinedKeys = setOf(securityTeamPublicKey, backendPublicKey, securityPlatformPublicKey)
        
        // Find public keys from config that aren't our predefined keys
        val configPublicKeys = config.key_groups
            .flatMap { it.age ?: emptyList() }
            .map { it.recipient }
            .filter { it !in predefinedKeys }

        val keyGroups = listOfNotNull(
            KeyGroup(
                age = listOf(AgeEntry(recipient = securityTeamPublicKey)),
                gcp_kms = listOf(GcpKmsEntry(resource_id = findGcpKmsEntry(config.key_groups).resource_id))
            ),
            KeyGroup(
                age = listOf(
                    AgeEntry(recipient = backendPublicKey),
                    AgeEntry(recipient = securityPlatformPublicKey)
                )
            ),
            if (configPublicKeys.isNotEmpty()) {
                KeyGroup(
                    age = configPublicKeys.map { AgeEntry(recipient = it) }
                )
            } else {
                null
            }
        )
        
        val keyArgs = mutableListOf<String>()
        
        // Collect all GCP KMS keys
        val gcpKmsKeys = keyGroups.mapNotNull { it.gcp_kms }.flatten().map { it.resource_id }
        if (gcpKmsKeys.isNotEmpty()) {
            keyArgs.add("--gcp-kms")
            keyArgs.add(gcpKmsKeys.joinToString(","))
        }
        
        // Collect all Age keys
        val ageKeys = keyGroups.mapNotNull { it.age }.flatten().map { it.recipient }
        if (ageKeys.isNotEmpty()) {
            keyArgs.add("--age")
            keyArgs.add(ageKeys.joinToString(","))
        }
        
        if (keyArgs.isEmpty()) {
            throw SopsEncryptionException("No valid encryption keys found in configuration", "")
        }
        
        return keyArgs
    }

    fun encrypt(
        text: String,
        config: SopsConfig,
        gcpAccessToken: GCPAccessToken,
        riScId: String,
    ): String =
        try {
            // Create key groups configuration
            val keyGroups = listOfNotNull(
                mapOf(
                    "age" to listOf(securityTeamPublicKey),
                    "gcp_kms" to listOf(
                        mapOf("resource_id" to findGcpKmsEntry(config.key_groups).resource_id)
                    )
                ),
                mapOf(
                    "age" to listOf(backendPublicKey, securityPlatformPublicKey)
                ),
                if (config.key_groups.flatMap { it.age ?: emptyList() }
                        .map { it.recipient }
                        .filter { it !in setOf(securityTeamPublicKey, backendPublicKey, securityPlatformPublicKey) }
                        .isNotEmpty()
                ) {
                    mapOf(
                        "age" to config.key_groups
                            .flatMap { it.age ?: emptyList() }
                            .filter { it.recipient !in setOf(securityTeamPublicKey, backendPublicKey, securityPlatformPublicKey) }
                            .map { it.recipient }
                    )
                } else null
            )

            // Create SOPS config
            val sopsConfig = mapOf(
                "creation_rules" to listOf(
                    mapOf(
                        "shamir_threshold" to config.shamir_threshold,
                        "key_groups" to keyGroups
                    )
                )
            )

            // Create temporary config file
            val tempConfigFile = File.createTempFile("sopsConfig-$riScId-${System.currentTimeMillis()}", ".yaml")
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
                        "/dev/stdin"
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
