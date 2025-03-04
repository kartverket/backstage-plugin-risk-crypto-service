package cryptoservice.controller.models

import cryptoservice.model.SopsConfig

data class EncryptionRequest(
    val text: String,
    val config: SopsConfig,
    val gcpAccessToken: String,
    val riScId: String,
)
