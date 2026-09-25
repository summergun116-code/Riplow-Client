package com.riplow.client.modules

data class ModuleDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: String
)

object ModuleRegistry {
    val all = listOf(
        ModuleDefinition("fps", "FPS Overlay", "Frame-rate telemetry overlay.", "Performance"),
        ModuleDefinition("frame_pacing", "Frame Pacing", "Frame-timing telemetry and pacing controls.", "Performance"),
        ModuleDefinition("performance_profile", "Performance Profile", "Select a saved performance profile.", "Performance"),
        ModuleDefinition("battery_guard", "Battery Guard", "Battery-aware helper profile.", "Performance"),
        ModuleDefinition("memory_monitor", "Memory Monitor", "Monitor app/native memory usage.", "Performance"),
        ModuleDefinition("cps", "CPS Counter", "Client-side click-rate telemetry.", "PvP"),
        ModuleDefinition("keystrokes", "Keystrokes", "Keyboard/input telemetry when available.", "PvP"),
        ModuleDefinition("session_timer", "Session Timer", "Track the current Minecraft session.", "Utility"),
        ModuleDefinition("clock", "Clock", "Always-on local time widget.", "Utility"),
        ModuleDefinition("coordinates", "Coordinates", "Player position display when game data is available.", "Utility"),
        ModuleDefinition("compass", "Compass", "Direction helper for the HUD.", "Utility"),
        ModuleDefinition("item_info", "Item Info", "Selected-item helper information.", "Utility"),
        ModuleDefinition("zoom", "Zoom", "Accessibility-oriented zoom helper.", "Visual"),
        ModuleDefinition("crosshair", "Crosshair", "Custom client-side crosshair.", "Visual"),
        ModuleDefinition("hud", "HUD", "Movable HUD layer.", "HUD"),
        ModuleDefinition("ping", "Ping", "Network latency display.", "Network"),
        ModuleDefinition("network_diagnostics", "Network Diagnostics", "RakNet/UDP latency, jitter and loss telemetry.", "Network"),
        ModuleDefinition("jitter_monitor", "Jitter Monitor", "Track short-term latency variation.", "Network"),
        ModuleDefinition("connection_status", "Connection Status", "Show session/network state.", "Network"),
        ModuleDefinition("pack_switcher", "Pack Switcher", "Quick access to Bedrock pack profiles.", "Client"),
        ModuleDefinition("profile_switcher", "Profile Switcher", "Switch Riplow configurations.", "Client"),
        ModuleDefinition("addon_manager", "Add-on Manager", "Manage imported Bedrock content.", "Client"),
        ModuleDefinition("launch_monitor", "Launch Monitor", "Track launch handoff and failures.", "Client"),
        ModuleDefinition("client_menu", "Client Menu", "Quick access to Riplow controls.", "Client")
    )
}
