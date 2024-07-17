package crypto_service.service

import crypto_service.model.GCPAccessToken
import org.springframework.stereotype.Service

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
                    "Encrypted!"
                    // TODO implement actual encryption, use SOPS.decrypt form backstage backend
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