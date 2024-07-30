package crypto_service.controller.models

data class EncryptionRequest(
    val text: String,
    val config: String,
    val gcpAccessToken: String,
    val riScId: String,
)
