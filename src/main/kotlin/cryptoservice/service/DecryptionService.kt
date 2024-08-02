package cryptoservice.service

import cryptoservice.exception.exceptions.SOPSDecryptionException
import cryptoservice.model.GCPAccessToken
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader

@Service
class DecryptionService {
    private val processBuilder = ProcessBuilder().redirectErrorStream(true)

    fun decrypt(
        ciphertext: String,
        gcpAccessToken: GCPAccessToken,
    ): String {
        return try {
            processBuilder
                .command(toDecryptionCommand(gcpAccessToken.value))
                .start()
                .run {
                    outputStream.buffered().also { it.write(ciphertext.toByteArray()) }.close()
                    val result = BufferedReader(InputStreamReader(inputStream)).readText()
                    when (waitFor()) {
                        EXECUTION_STATUS_OK -> result

                        else -> {
                            throw SOPSDecryptionException(
                                message = "Decrypting message failed with error: $result",
                            )
                        }
                    }
                }
        } catch (e: Exception) {
            throw e
        }
    }

    private fun toDecryptionCommand(accessToken: String): List<String> =
        sopsCmd + decrypt + inputTypeYaml + outputTypeJson + inputFile +
            gcpAccessToken(
                accessToken,
            )
}
