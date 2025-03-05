@file:Suppress("ktlint:standard:filename")

package cryptoservice.model

data class RiScWithConfig(
    val riSc: String,
    val isRequiresNewApproval: Boolean,
    val schemaVersion: String,
    val sopsConfig: SopsConfig,
)
