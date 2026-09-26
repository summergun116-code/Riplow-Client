package com.riplow.client.modules

enum class BedrockReleaseChannel {
    STABLE,
    PREVIEW,
    UNKNOWN
}

enum class BedrockSupportLevel {
    CURRENT_STABLE,
    KNOWN_STABLE,
    FUTURE_RELEASE,
    LEGACY_RELEASE,
    UNKNOWN
}

data class BedrockCompatibilityProfile(
    val version: BedrockVersion,
    val support: BedrockSupportLevel,
    val channel: BedrockReleaseChannel,
    val networkProtocol: Int?,
    val note: String
)

object BedrockCompatibilityCatalog {
    val CURRENT_STABLE = BedrockVersion(1, 26, 51)

    private val stableProtocols = mapOf(
        BedrockVersion(1, 26, 51) to 2193,
        BedrockVersion(1, 26, 50) to 2193,
        BedrockVersion(1, 26, 45) to 2169,
        BedrockVersion(1, 26, 44) to 2168,
        BedrockVersion(1, 26, 43) to 2168
    )

    fun inspect(version: BedrockVersion): BedrockCompatibilityProfile {
        val knownProtocol = stableProtocols[version]
        if (knownProtocol != null) {
            return BedrockCompatibilityProfile(
                version = version,
                support = if (version == CURRENT_STABLE) {
                    BedrockSupportLevel.CURRENT_STABLE
                } else {
                    BedrockSupportLevel.KNOWN_STABLE
                },
                channel = BedrockReleaseChannel.STABLE,
                networkProtocol = knownProtocol,
                note = if (version == CURRENT_STABLE) {
                    "Current stable Bedrock release"
                } else {
                    "Known stable Bedrock release"
                }
            )
        }

        return if (version >= CURRENT_STABLE) {
            BedrockCompatibilityProfile(
                version = version,
                support = BedrockSupportLevel.FUTURE_RELEASE,
                channel = BedrockReleaseChannel.UNKNOWN,
                networkProtocol = null,
                note = "Newer Bedrock release; generic-safe mode"
            )
        } else {
            BedrockCompatibilityProfile(
                version = version,
                support = BedrockSupportLevel.LEGACY_RELEASE,
                channel = BedrockReleaseChannel.UNKNOWN,
                networkProtocol = null,
                note = "Older or unlisted Bedrock release"
            )
        }
    }

    fun expectedNetworkProtocol(version: BedrockVersion?): Int? =
        version?.let { stableProtocols[it] }
}
