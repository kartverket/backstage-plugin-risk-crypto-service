package crypto_service.service

import crypto_service.exception.exceptions.SopsEncryptionException
import crypto_service.model.GCPAccessToken
import crypto_service.model.sensor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader

@Service
class EncryptionService {
    private val processBuilder = ProcessBuilder().redirectErrorStream(true)

    private val logger = LoggerFactory.getLogger(DecryptionService::class.java)

    fun encrypt(
        text: String,
        config: String,
        gcpAccessToken: GCPAccessToken,
        riScId: String,
    ): String {
        return try {
            processBuilder
                .command(toEncryptionCommand(config, gcpAccessToken.value))
                .start()
                .run {
                    outputStream.buffered().also { it.write(text.toByteArray()) }.close()
                    val result = BufferedReader(InputStreamReader(inputStream)).readText()
                    when (waitFor()) {
                        EXECUTION_STATUS_OK -> result
                        else -> {
                            logger.error("IOException from encrypting yaml with error code ${exitValue()}: $result")
                            throw SopsEncryptionException(
                                message = "Failed when encrypting RiSc with ID: $riScId by running sops command: ${
                                    toEncryptionCommand(
                                        config,
                                        gcpAccessToken.sensor().value,
                                    )
                                } with error message: $result",
                                riScId = riScId,
                            )
                        }
                    }
                }
        } catch (e: Exception) {
            logger.error("Decrypting failed.", e)
            throw e
        }
    }

    private fun toEncryptionCommand(
        config: String,
        accessToken: String,
    ): List<String> =
        sopsCmd + encrypt + inputTypeJson + outputTypeYaml + encryptConfig + config + inputFile +
            gcpAccessToken(
                accessToken,
            )
}
