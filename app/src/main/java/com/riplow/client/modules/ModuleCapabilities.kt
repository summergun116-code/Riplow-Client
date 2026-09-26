package com.riplow.client.modules

enum class ModuleAvailability {
    READY,
    NATIVE_TELEMETRY,
    GAME_BRIDGE_REQUIRED,
    PLANNED
}

object ModuleCapabilities {
    val runtimeReadyIds = setOf(
        "clock",
        "session_timer",
        "memory_monitor",
        "battery_guard",
        "performance_profile",
        "quick_launch",
        "module_config"
    )

    fun availability(id: String): ModuleAvailability = when {
        runtimeReadyIds.contains(id) -> ModuleAvailability.READY
        ModuleRegistry.usesNativeTelemetry(id) -> ModuleAvailability.NATIVE_TELEMETRY
        ModuleRegistry.requiresGameBridge(id) -> ModuleAvailability.GAME_BRIDGE_REQUIRED
        else -> ModuleAvailability.PLANNED
    }

    fun canToggle(id: String): Boolean = availability(id) == ModuleAvailability.READY

    fun label(id: String, enabled: Boolean): String = when (availability(id)) {
        ModuleAvailability.READY -> if (enabled) "ON" else "OFF"
        ModuleAvailability.NATIVE_TELEMETRY -> "TELEMETRY"
        ModuleAvailability.GAME_BRIDGE_REQUIRED -> "BRIDGE"
        ModuleAvailability.PLANNED -> "PLANNED"
    }
}
