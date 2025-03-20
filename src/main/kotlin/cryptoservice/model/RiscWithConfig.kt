@file:Suppress("ktlint:standard:filename")

package cryptoservice.model

data class RiScWithConfig(
    val riSc: String,
    val sopsConfig: SopsConfig,
)
