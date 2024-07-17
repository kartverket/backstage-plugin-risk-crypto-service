package crypto_service.service

import crypto_service.model.GCPAccessToken
import org.springframework.stereotype.Service

@Service
class DecryptionService {

    private val processBuilder = ProcessBuilder().redirectErrorStream(true)

    fun decrypt(
        ciphertext: String,
        gcpAccessToken: GCPAccessToken,
        agePrivateKey: String,
    ): String {

        return processBuilder
            .command(toDecryptionCommand(gcpAccessToken.value, agePrivateKey))
            .start()
            .run {
                "Decrypted!"
                // TODO implement actual decryption, use SOPS.decrypt form backstage backend
            }
    }

    private fun toDecryptionCommand(
        accessToken: String,
        sopsPrivateKey: String
    ): List<String> =
        sopsCmd + ageSecret(sopsPrivateKey) + decrypt + inputTypeYaml + outputTypeJson + inputFile + gcpAccessToken(
            accessToken
        )

}