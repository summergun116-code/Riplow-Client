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
        listOfNotNull(major, minor, patch, build).joinToString(".")
}

object BedrockVersionParser {
    private val pattern = Regex("""^(\d+)\.(\d+)\.(\d+)(?:\.(\d+))?.*$""")

    fun parse(raw: String?): BedrockVersion? {
        val match = pattern.matchEntire(raw?.trim() ?: return null) ?: return null
        return runCatching {
            BedrockVersion(
                major = match.groupValues[1].toInt(),
                minor = match.groupValues[2].toInt(),
                patch = match.groupValues[3].toInt(),
                build = match.groupValues[4].takeIf(String::isNotEmpty)?.toInt()
            )
        }.getOrNull()
    }
}

enum class BedrockBridgeState {
    NOT_DETECTED,
    VERSION_PARSED_NO_ADAPTER,
    ADAPTER_READY,
    ATTACHED
}

data class BedrockBridgeInfo(
    val state: BedrockBridgeState,
    val version: BedrockVersion?,
    val adapterId: String? = null
)

/**
 * Compatibility boundary for future version-matched Bedrock integration.
 *
 * No offsets, hooks, packet mutations, or game automation live here.
 * An adapter must explicitly prove compatibility before the client can
 * expose bridge-dependent modules.
 */
interface BedrockBridgeAdapter {
    val id: String
    fun supports(version: BedrockVersion): Boolean
}

object BedrockBridgeRegistry {
    private val adapters = emptyList<BedrockBridgeAdapter>()

    fun inspect(version: BedrockVersion?): BedrockBridgeInfo {
        if (version == null) return BedrockBridgeInfo(BedrockBridgeState.NOT_DETECTED, null)
        val adapter = adapters.firstOrNull { it.supports(version) }
        return if (adapter == null) {
            BedrockBridgeInfo(BedrockBridgeState.VERSION_PARSED_NO_ADAPTER, version)
        } else {
            BedrockBridgeInfo(BedrockBridgeState.ADAPTER_READY, version, adapter.id)
        }
    }
}