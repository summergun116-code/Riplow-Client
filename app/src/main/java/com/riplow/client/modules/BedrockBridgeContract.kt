package com.riplow.client.modules

data class BedrockVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val build: Int? = null
) : Comparable<BedrockVersion> {
    override fun compareTo(other: BedrockVersion): Int =
        compareValuesBy(this, other, BedrockVersion::major, BedrockVersion::minor, BedrockVersion::patch)
            .takeIf { it != 0 } ?: compareValues(build ?: -1, other.build ?: -1)

    override fun toString(): String =
        listOf(major, minor, patch).joinToString(".") +
            (build?.let { ".$it" } ?: "")
}

object BedrockVersionParser {
    private val numericPattern =
        Regex("""^(?:v)?(\d+)\.(\d+)(?:\.(\d+))?(?:\.(\d+))?""")
    private val channelBuildPattern =
        Regex("""(?i)(?:preview|beta|alpha|rc)[.-]?(\d+)$""")

    /**
     * Accepts both legacy Bedrock strings such as 1.21.100 and the 2026
     * year-based release label such as 26.51. Mojang's protocol metadata
     * refers to the same release as 1.26.51.
     */
    fun parse(raw: String?): BedrockVersion? {
        val value = raw?.trim()?.removePrefix("v") ?: return null
        val match = numericPattern.find(value) ?: return null

        return runCatching {
            val first = match.groupValues[1].toInt()
            val second = match.groupValues[2].toInt()
            val third = match.groupValues[3].takeIf(String::isNotEmpty)?.toInt()
            val fourth = match.groupValues[4].takeIf(String::isNotEmpty)?.toInt()
            val channelBuild =
                channelBuildPattern.find(value)?.groupValues?.getOrNull(1)?.toIntOrNull()

            when {
                third == null && first in 20..99 ->
                    BedrockVersion(1, first, second, channelBuild)
                first == 1 && third != null ->
                    BedrockVersion(1, second, third, fourth ?: channelBuild)
                first in 20..99 && third != null ->
                    BedrockVersion(1, first, second, fourth ?: third)
                third != null ->
                    BedrockVersion(first, second, third, fourth ?: channelBuild)
                else -> null
            }
        }.getOrNull()
    }
}

enum class BedrockBridgeState {
    NOT_DETECTED,
    VERSION_RECOGNIZED_NO_BRIDGE,
    VERSION_PARSED_NO_ADAPTER,
    ADAPTER_READY,
    ATTACHED
}

data class BedrockBridgeInfo(
    val state: BedrockBridgeState,
    val version: BedrockVersion?,
    val adapterId: String? = null,
    val profile: BedrockCompatibilityProfile? = version?.let { BedrockCompatibilityCatalog.inspect(it) }
)

/**
 * Compatibility boundary for future version-matched Bedrock integration.
 *
 * No offsets, hooks, packet mutations, or game automation live here.
 * A real adapter must explicitly prove compatibility before the client can
 * expose bridge-dependent modules.
 */
interface BedrockBridgeAdapter {
    val id: String
    fun supports(version: BedrockVersion): Boolean
}

object BedrockBridgeRegistry {
    private val adapters = emptyList<BedrockBridgeAdapter>()

    fun inspect(version: BedrockVersion?): BedrockBridgeInfo {
        if (version == null) {
            return BedrockBridgeInfo(BedrockBridgeState.NOT_DETECTED, null)
        }

        val adapter = adapters.firstOrNull { it.supports(version) }
        return if (adapter == null) {
            BedrockBridgeInfo(
                state = BedrockBridgeState.VERSION_RECOGNIZED_NO_BRIDGE,
                version = version
            )
        } else {
            BedrockBridgeInfo(
                state = BedrockBridgeState.ADAPTER_READY,
                version = version,
                adapterId = adapter.id
            )
        }
    }
}
