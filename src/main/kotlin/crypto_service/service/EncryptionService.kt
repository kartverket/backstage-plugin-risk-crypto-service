package crypto_service.service

import crypto_service.exception.SopsEncryptionException
import crypto_service.model.GCPAccessToken
import crypto_service.model.sensor
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader

@Service
class EncryptionService {

    private val processBuilder = ProcessBuilder().redirectErrorStream(true)

    fun encrypt(
        text: String,
        config: String,
        gcpAccessToken: GCPAccessToken,
        riScId: String
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
                        else -> throw SopsEncryptionException(
                            message = "Failed when encrypting RiSc with ID: $riScId by running sops command: ${toEncryptionCommand(config, gcpAccessToken.sensor().value)} with error message: $result",
                            riScId = riScId
                        )
                    }
                }
        } catch (e: Exception) {
            throw e
        }
    }

    private fun toEncryptionCommand(
        config: String,
        accessToken: String,
    ): List<String> = sopsCmd + encrypt + inputTypeJson + outputTypeYaml + encryptConfig + config + inputFile + gcpAccessToken(accessToken)

}