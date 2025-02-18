package cryptoservice.model

import cryptoservice.model.SopsConfig

data class RiScWithConfig(
    val riSc: String,
    val isRequiresNewApproval: Boolean,
    val schemaVersion: String,
    val sopsConfig: SopsConfig,
)
