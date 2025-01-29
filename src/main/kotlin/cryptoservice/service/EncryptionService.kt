package cryptoservice.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import cryptoservice.exception.exceptions.SopsEncryptionException
import cryptoservice.model.GCPAccessToken
import cryptoservice.model.SopsConfig
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader

@Service
class EncryptionService {
    private val processBuilder = ProcessBuilder().redirectErrorStream(true)
    private val yamlMapper = ObjectMapper(YAMLFactory()).registerKotlinModule()

    private val logger = LoggerFactory.getLogger(DecryptionService::class.java)

    private fun extractGcpKmsResourceId(config: String): String {
        val sopsConfig = yamlMapper.readValue(config, SopsConfig::class.java)
        val gcpKmsConfig =
            sopsConfig.keyGroups?.firstOrNull { it.gcpKms?.isNotEmpty() == true }
                ?: throw SopsEncryptionException("No GCP KMS configuration found", "")

        return gcpKmsConfig.gcpKms?.firstOrNull()?.resourceId
            ?: throw SopsEncryptionException("No resource_id found in GCP KMS configuration", "")
    }

    fun encrypt(
        text: String,
        config: String,
        gcpAccessToken: GCPAccessToken,
        riScId: String,
    ): String =
        try {
            val gcpKmsResourceId = extractGcpKmsResourceId(config)

            processBuilder
                .command(
                    listOf(
                        "sh",
                        "-c",
                        "GOOGLE_OAUTH_ACCESS_TOKEN=${gcpAccessToken.value} " +
                            "sops --encrypt " +
                            "--gcp-kms '$gcpKmsResourceId' " +
                            "--input-type json --output-type yaml /dev/stdin",
                    ),
                ).start()
                .run {
                    outputStream.buffered().also { it.write(text.toByteArray()) }.close()
                    val result = BufferedReader(InputStreamReader(inputStream)).readText()
                    when (waitFor()) {
                        EXECUTION_STATUS_OK -> result
                        else -> {
                            logger.error("IOException from encrypting yaml with error code ${exitValue()}: $result")
                            throw SopsEncryptionException(
                                message =
                                    "Failed when encrypting RiSc with ID: $riScId" +
                                        "by running sops command: sops --encrypt --gcp-kms '$gcpKmsResourceId' " +
                                        "--input-type json --output-type yaml /dev/stdin with error message: $result",
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
