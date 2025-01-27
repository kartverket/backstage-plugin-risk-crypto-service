package cryptoservice.service

import cryptoservice.exception.exceptions.SOPSDecryptionException
import cryptoservice.model.GCPAccessToken
import cryptoservice.model.SopsConfig
import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.InputStreamReader

@Service
class DecryptionService {
    private val processBuilder = ProcessBuilder().redirectErrorStream(true)

    fun extractSopsConfig(ciphertext: String): String {
        val rootNode = YamlUtils.objectMapper.readTree(ciphertext)
        val sopsNode = rootNode.get("sops") ?: throw IllegalArgumentException("No sops configuration found in ciphertext")
        val sopsConfig = YamlUtils.objectMapper.treeToValue(sopsNode, SopsConfig::class.java)
        val cleanConfig = sopsConfig.copy(
            mac = null,
            gcpKms = sopsConfig.gcpKms?.map { it.copy(enc = null) },
            age = sopsConfig.age?.map { it.copy(enc = null) }
        )
        return YamlUtils.serialize(cleanConfig)
    }

    fun decryptWithSopsConfig(
        ciphertext: String,
        gcpAccessToken: GCPAccessToken,
        sopsAgeKey: String,
    ): Pair<String, String> =
        try {
            val sopsConfig = extractSopsConfig(ciphertext)
            val plaintext = decrypt(ciphertext, gcpAccessToken, sopsAgeKey)
            Pair(plaintext, sopsConfig)
        } catch (e: Exception) {
            throw e
        }

    fun decrypt(
        ciphertext: String,
        gcpAccessToken: GCPAccessToken,
        sopsAgeKey: String,
    ): String =
        try {
            processBuilder
                .command(
                    listOf(
                        "sh",
                        "-c",
                        "SOPS_AGE_KEY=$sopsAgeKey GOOGLE_OAUTH_ACCESS_TOKEN=${gcpAccessToken.value} " +
                            "sops decrypt --input-type yaml --output-type json /dev/stdin"
                    )
                ).start()
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
