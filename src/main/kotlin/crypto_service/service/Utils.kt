package crypto_service.service

val sopsCmd: List<String> = listOf("sops")
val encrypt: List<String> = listOf("encrypt")
val decrypt: List<String> = listOf("decrypt")
val inputTypeYaml: List<String> = listOf("--input-type", "yaml")
val inputTypeJson: List<String> = listOf("--input-type", "json")
val outputTypeYaml: List<String> = listOf("--output-type", "yaml")
val outputTypeJson: List<String> = listOf("--output-type", "json")
val encryptConfig: List<String> = listOf("--encrypt-config")
val inputFile: List<String> = listOf("/dev/stdin")

const val EXECUTION_STATUS_OK = 0

fun gcpAccessToken(accessToken: String): List<String> = listOf("--gcp-access-token", accessToken)

fun ageSecret(sopsPrivateKey: String): List<String> = listOf("--age", sopsPrivateKey)
