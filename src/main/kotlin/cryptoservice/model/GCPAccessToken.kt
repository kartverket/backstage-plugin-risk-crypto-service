package cryptoservice.model

data class GCPAccessToken(
    val value: String,
)

fun GCPAccessToken.sensor() =
    GCPAccessToken(
        value = value.slice(IntRange(0, 3)) + "****",
    )
