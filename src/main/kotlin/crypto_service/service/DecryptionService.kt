package crypto_service.service

import crypto_service.exception.SOPSDecryptionException
import crypto_service.model.GCPAccessToken
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader

@Service
class DecryptionService {
    private val processBuilder = ProcessBuilder().redirectErrorStream(true)

    private val logger = LoggerFactory.getLogger(DecryptionService::class.java)

    fun decrypt(
        ciphertext: String,
        gcpAccessToken: GCPAccessToken,
        agePrivateKey: String,
    ): String {
        return try {
            processBuilder
                .command(toDecryptionCommand(gcpAccessToken.value, agePrivateKey))
                .start()
                .run {
                    outputStream.buffered().also { it.write(ciphertext.toByteArray()) }.close()
                    val result = BufferedReader(InputStreamReader(inputStream)).readText()
                    when (waitFor()) {
                        EXECUTION_STATUS_OK -> result

                        else -> {
                            logger.error("IOException from decrypting yaml with error code ${exitValue()}: $result")
                            throw SOPSDecryptionException(
                                message = result,
                            )
                        }
                    }
                }
        } catch (e: Exception) {
            logger.error("Decrypting failed.", e)
            throw e
        }
    }

    private fun toDecryptionCommand(
        accessToken: String,
        sopsPrivateKey: String,
    ): List<String> =
        sopsCmd + ageSecret(sopsPrivateKey) + decrypt + inputTypeYaml + outputTypeJson + inputFile +
            gcpAccessToken(
                accessToken,
            )
}
