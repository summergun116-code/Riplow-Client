package com.riplow.client.modules

data class ModuleSetting(
    val id: String,
    val title: String,
    val description: String,
    val options: List<String>,
    val defaultValue: String = options.firstOrNull() ?: ""
)

data class ModuleDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val settings: List<ModuleSetting> = emptyList()
)

/**
 * Focused Minecraft-only module surface.
 *
 * Riplow intentionally contains no combat automation, mining automation,
 * inventory stealing, anti-AFK, auto-clicking or other gameplay automation.
 */
object ModuleRegistry {
    val all = listOf(
        ModuleDefinition("fps", "FPS Telemetry", "Frame-rate telemetry when a compatible integration is available.", "Performance"),
        ModuleDefinition("frame_pacing", "Frame Pacing", "Frame-time visibility and pacing diagnostics.", "Performance",
            listOf(ModuleSetting("target", "Target", "Preferred observation target.", listOf("30 FPS", "60 FPS", "90 FPS", "120 FPS")))),
        ModuleDefinition("performance_profile", "Performance Profile", "Controls how aggressively Riplow reduces its own polling, animation and diagnostic overhead.", "Performance",
            listOf(ModuleSetting("profile", "Profile", "Riplow-side performance profile.", listOf("Balanced", "Performance", "Battery"), "Performance"))),
        ModuleDefinition("memory_monitor", "Memory Monitor", "Available Android memory readout for the device running Minecraft.", "Performance",
            listOf(ModuleSetting("interval", "Refresh", "Telemetry refresh cadence.", listOf("1s", "3s", "5s"), "3s"))),
        ModuleDefinition("battery_guard", "Battery Monitor", "Battery state visibility for long Minecraft sessions.", "Performance"),

        ModuleDefinition("session_timer", "Session Timer", "Track the current Riplow/Minecraft launch session.", "HUD",
            listOf(ModuleSetting("format", "Format", "Timer presentation.", listOf("MM:SS", "HH:MM:SS")))),
        ModuleDefinition("clock", "Clock", "Local time widget for a Minecraft session.", "HUD",
            listOf(ModuleSetting("format", "Format", "Local clock format.", listOf("12-hour", "24-hour")))),
        ModuleDefinition("coordinates", "Coordinates", "Coordinate readout when a real Bedrock bridge is available.", "HUD"),
        ModuleDefinition("compass", "Compass", "Heading helper when game data is available.", "HUD"),
        ModuleDefinition("fps_overlay", "FPS Overlay", "Minecraft FPS HUD when game frame data is available.", "HUD"),
        ModuleDefinition("direction_hud", "Direction HUD", "Compact Minecraft heading display.", "HUD"),
        ModuleDefinition("potion_hud", "Potion HUD", "Active-effect presentation from Minecraft game data.", "HUD"),
        ModuleDefinition("armor_hud", "Armor HUD", "Armor/status presentation from Minecraft game data.", "HUD"),
        ModuleDefinition("scoreboard", "Scoreboard", "Compact scoreboard presentation from Minecraft game data.", "HUD"),

        ModuleDefinition("zoom", "Zoom", "Visual accessibility zoom for Minecraft viewing.", "Visual",
            listOf(ModuleSetting("factor", "Zoom", "Preferred zoom factor.", listOf("2x", "3x", "4x"), "2x"))),
        ModuleDefinition("crosshair", "Crosshair", "Minecraft visual crosshair preferences.", "Visual",
            listOf(
                ModuleSetting("style", "Style", "Crosshair shape.", listOf("Cross", "Dot", "Circle", "Plus")),
                ModuleSetting("size", "Size", "Crosshair scale.", listOf("Small", "Medium", "Large"))
            )),
        ModuleDefinition("fullbright", "Fullbright", "Visibility helper for dark Minecraft areas.", "Visual"),
        ModuleDefinition("no_fog", "No Fog", "Reduce or disable distance fog when the Minecraft integration supports it.", "Visual"),
        ModuleDefinition("view_bobbing", "View Bobbing", "Minecraft camera-bobbing preference.", "Visual"),
        ModuleDefinition("fov_changer", "FOV Presets", "Convenient Minecraft FOV presets.", "Visual"),
        ModuleDefinition("gui_scale", "GUI Scale", "Minecraft interface scale preference.", "Visual"),
        ModuleDefinition("low_fire", "Low Fire", "Reduce first-person fire obstruction.", "Visual"),

        ModuleDefinition("ping", "Ping", "Bedrock server latency telemetry.", "Network"),
        ModuleDefinition("network_diagnostics", "Network Diagnostics", "Read-only Bedrock latency, jitter and loss diagnostics.", "Network"),
        ModuleDefinition("jitter_monitor", "Jitter Monitor", "Track short-term latency variation.", "Network"),
        ModuleDefinition("packet_loss", "Packet Loss", "Display measured or reported packet-loss information.", "Network"),
        ModuleDefinition("connection_status", "Connection Status", "Summarize the current Minecraft connection state.", "Network"),

        ModuleDefinition("pack_switcher", "Pack Switcher", "Minecraft resource/behavior pack workspace.", "Minecraft"),
        ModuleDefinition("pack_profiles", "Pack Profiles", "Save Minecraft pack profile selections.", "Minecraft"),
        ModuleDefinition("addon_manager", "Add-on Manager", "Minecraft behavior/resource add-on workspace.", "Minecraft"),
        ModuleDefinition("world_manager", "World Manager", "Organize Minecraft world profiles.", "Minecraft"),
        ModuleDefinition("backup_manager", "World Backups", "Manual Minecraft world backup workspace.", "Minecraft"),
        ModuleDefinition("version_profiles", "Version Profiles", "Associate settings with Minecraft versions.", "Minecraft"),
        ModuleDefinition("quick_launch", "Quick Launch", "Open the detected Minecraft installation.", "Minecraft"),
        ModuleDefinition("module_config", "Module Config", "Export/import Riplow's Minecraft module preferences.", "Minecraft")
    )

    val gameBridgeIds = setOf(
        "fps", "coordinates", "compass", "fps_overlay", "direction_hud", "potion_hud",
        "armor_hud", "scoreboard", "zoom", "crosshair", "fullbright", "no_fog",
        "view_bobbing", "fov_changer", "gui_scale", "low_fire"
    )

    val nativeIds = setOf(
        "ping", "network_diagnostics", "jitter_monitor", "packet_loss", "connection_status"
    )

    fun requiresGameBridge(id: String): Boolean = gameBridgeIds.contains(id)
    fun usesNativeTelemetry(id: String): Boolean = nativeIds.contains(id)
}
