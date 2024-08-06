package cryptoservice.service

import cryptoservice.exception.exceptions.SopsEncryptionException
import cryptoservice.model.GCPAccessToken
import cryptoservice.model.sensor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.File
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
            val tempFile = File.createTempFile("sopsConfig-$riScId-${System.currentTimeMillis()}", ".yaml")
            println("Temporary file created here: ${tempFile.absolutePath}")
            tempFile.writeText(config)
            tempFile.deleteOnExit()

            processBuilder
                .command("sh", "-c", "GOOGLE_CREDENTIALS_ACCESS_TOKEN=${gcpAccessToken.value} sops --encrypt --input-type json --output-type yaml --config ${tempFile.absolutePath} /dev/stdin")
                .start()
                .run {
                    outputStream.buffered().also { it.write(text.toByteArray()) }.close()
                    val result = BufferedReader(InputStreamReader(inputStream)).readText()
                    when (waitFor()) {
                        EXECUTION_STATUS_OK -> result
                        else -> {
                            logger.error("IOException from encrypting yaml with error code ${exitValue()}: $result")
                            throw SopsEncryptionException(
                                message = "Failed when encrypting RiSc with ID: $riScId by running sops command: sops --encrypt --input-type json --output-type yaml --config ${tempFile.absolutePath} /dev/stdin with error message: $result",
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
}
