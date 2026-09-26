package com.riplow.client.modules

enum class ServerProfileKind {
    DEFAULT,
    LIFEBOAT
}

data class LifeboatPolicyState(
    val active: Boolean,
    val detectedBy: String? = null,
    val reason: String? = null,
    val allowedFeatureIds: Set<String>,
    val disabledFeatureIds: Set<String>
)

/**
 * Conservative Lifeboat profile derived from Lifeboat's currently published
 * in-game modification rules. Riplow never attempts to bypass, hide or alter
 * server anti-cheat/kick behavior.
 *
 * Detection is deliberately deterministic and must be confirmed by the real
 * Bedrock bridge/session layer before it is treated as an active connection.
 */
object LifeboatPolicy {
    private val explicitlyAllowed = setOf(
        "fullbright",
        "zoom",
        "fov_changer",
        "cps_counter",
        "reach_display",
        "hitbox_display",
        "armor_hud"
    )

    private val knownLifeboatMarkers = setOf(
        "lifeboat",
        "lbsg"
    )

    fun looksLikeLifeboatServer(host: String?, motd: String? = null): Boolean {
        val haystack = listOf(host.orEmpty(), motd.orEmpty())
            .joinToString(" ")
            .lowercase()
        return knownLifeboatMarkers.any(haystack::contains)
    }

    fun evaluate(
        host: String?,
        motd: String? = null,
        sessionConfirmed: Boolean = false
    ): LifeboatPolicyState {
        val candidate = looksLikeLifeboatServer(host, motd)
        val active = candidate && sessionConfirmed

        val allIds = MasterFeatureCatalog.all.map { it.id }.toSet()
        val disabled = if (active) allIds - explicitlyAllowed else emptySet()

        return LifeboatPolicyState(
            active = active,
            detectedBy = when {
                sessionConfirmed && candidate -> "confirmed session host/MOTD"
                candidate -> "host/MOTD candidate"
                else -> null
            },
            reason = when {
                active -> "Conservative Lifeboat allowlist is active."
                candidate -> "Lifeboat candidate detected; awaiting bridge confirmation."
                else -> null
            },
            allowedFeatureIds = if (active) explicitlyAllowed else allIds,
            disabledFeatureIds = disabled
        )
    }

    fun isAllowed(featureId: String, state: LifeboatPolicyState): Boolean =
        !state.active || state.allowedFeatureIds.contains(featureId)
}
