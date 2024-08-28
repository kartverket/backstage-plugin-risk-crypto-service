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
        sopsAgeKey: String
    ): String {
        return try {
            processBuilder
                .command("sh", "-c", "SOPS_AGE_KEY=$sopsAgeKey GOOGLE_OAUTH_ACCESS_TOKEN=${gcpAccessToken.value} sops decrypt --input-type yaml --output-type json /dev/stdin")
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
}
