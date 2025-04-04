package cryptoservice.model

import com.fasterxml.jackson.annotation.JsonProperty

data class SopsConfig(
    @JsonProperty("shamir_threshold") val shamir_threshold: Int = 1,
    @JsonProperty("key_groups") val key_groups: List<KeyGroup>? = emptyList(),
    @JsonProperty("kms") val kms: List<Any>? = emptyList(),
    @JsonProperty("gcp_kms") val gcp_kms: List<GcpKmsEntry>? = emptyList(),
    @JsonProperty("azure_kv") val azureKv: List<Any>? = emptyList(),
    @JsonProperty("hc_vault") val hcVault: List<Any>? = emptyList(),
    @JsonProperty("age") val age: List<AgeEntry>? = emptyList(),
    @JsonProperty("lastmodified") val lastmodified: String? = null,
    @JsonProperty("mac") val mac: String? = null,
    @JsonProperty("pgp") val pgp: List<Any>? = null,
    @JsonProperty("unencrypted_suffix") val unencrypted_suffix: String? = null,
    @JsonProperty("version") val version: String? = null,
)

data class KeyGroup(
    @JsonProperty("gcp_kms") val gcp_kms: List<GcpKmsEntry>? = null,
    @JsonProperty("hc_vault") val hc_vault: List<Any>? = null,
    @JsonProperty("age") val age: List<AgeEntry>? = null,
)

data class GcpKmsEntry(
    @JsonProperty("resource_id") val resource_id: String,
    @JsonProperty("created_at") val created_at: String? = null,
    @JsonProperty("enc") val enc: String? = null,
)

data class AgeEntry(
    val recipient: String,
    val enc: String? = null,
)
