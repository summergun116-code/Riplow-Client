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

object ModuleRegistry {
    val all = listOf(
        ModuleDefinition("fps", "FPS Overlay", "Frame-rate telemetry overlay.", "Performance",
            listOf(ModuleSetting("window", "Sample window", "How much telemetry to average.", listOf("1s", "3s", "5s")))),
        ModuleDefinition("frame_pacing", "Frame Pacing", "Frame-timing telemetry and pacing controls.", "Performance",
            listOf(ModuleSetting("target", "Target", "Preferred pacing target.", listOf("30 FPS", "60 FPS", "90 FPS", "120 FPS")))),
        ModuleDefinition("performance_profile", "Performance Profile", "Select a saved performance profile.", "Performance",
            listOf(ModuleSetting("profile", "Profile", "Profile used by Riplow helpers.", listOf("Balanced", "Performance", "Battery")))),
        ModuleDefinition("battery_guard", "Battery Guard", "Battery-aware helper profile.", "Performance",
            listOf(ModuleSetting("threshold", "Threshold", "Switch guard behavior at this battery level.", listOf("20%", "30%", "40%")))),
        ModuleDefinition("memory_monitor", "Memory Monitor", "Monitor app/native memory usage.", "Performance",
            listOf(ModuleSetting("interval", "Refresh", "Telemetry refresh cadence.", listOf("1s", "3s", "5s")))),
        ModuleDefinition("cps", "CPS Counter", "Client-side click-rate telemetry.", "PvP",
            listOf(ModuleSetting("window", "Window", "Click-rate sampling window.", listOf("1s", "3s", "5s")))),
        ModuleDefinition("keystrokes", "Keystrokes", "Keyboard/input telemetry when available.", "PvP",
            listOf(ModuleSetting("layout", "Layout", "Input display style.", listOf("Classic", "Compact")))),
        ModuleDefinition("session_timer", "Session Timer", "Track the current Minecraft session.", "Utility",
            listOf(ModuleSetting("format", "Format", "Timer presentation.", listOf("MM:SS", "HH:MM:SS")))),
        ModuleDefinition("clock", "Clock", "Always-on local time widget.", "Utility",
            listOf(ModuleSetting("format", "Format", "Local clock format.", listOf("12-hour", "24-hour")))),
        ModuleDefinition("coordinates", "Coordinates", "Player position display when game data is available.", "Utility",
            listOf(ModuleSetting("layout", "Layout", "Coordinate presentation.", listOf("Compact", "Detailed")))),
        ModuleDefinition("compass", "Compass", "Direction helper for the HUD.", "Utility",
            listOf(ModuleSetting("mode", "Mode", "Compass readout.", listOf("Cardinal", "Degrees")))),
        ModuleDefinition("item_info", "Item Info", "Selected-item helper information.", "Utility",
            listOf(ModuleSetting("density", "Density", "Information density.", listOf("Compact", "Detailed")))),
        ModuleDefinition("zoom", "Zoom", "Accessibility-oriented zoom helper.", "Visual",
            listOf(ModuleSetting("factor", "Zoom", "Preferred zoom factor.", listOf("2x", "3x", "4x")))),
        ModuleDefinition("crosshair", "Crosshair", "Custom client-side crosshair.", "Visual",
            listOf(
                ModuleSetting("style", "Style", "Crosshair shape.", listOf("Cross", "Dot", "Circle", "Plus")),
                ModuleSetting("size", "Size", "Crosshair scale.", listOf("Small", "Medium", "Large"))
            )),
        ModuleDefinition("hud", "HUD", "Movable HUD layer.", "HUD",
            listOf(ModuleSetting("layout", "Layout", "HUD preset.", listOf("Classic", "Minimal", "Compact")))),
        ModuleDefinition("ping", "Ping", "Network latency display.", "Network",
            listOf(ModuleSetting("interval", "Refresh", "Latency refresh cadence.", listOf("1s", "2s", "5s")))),
        ModuleDefinition("network_diagnostics", "Network Diagnostics", "RakNet/UDP latency, jitter and loss telemetry.", "Network",
            listOf(ModuleSetting("detail", "Detail", "Diagnostic density.", listOf("Basic", "Full")))),
        ModuleDefinition("jitter_monitor", "Jitter Monitor", "Track short-term latency variation.", "Network",
            listOf(ModuleSetting("window", "Window", "Jitter sampling window.", listOf("5 samples", "10 samples", "20 samples")))),
        ModuleDefinition("connection_status", "Connection Status", "Show session/network state.", "Network",
            listOf(ModuleSetting("transport", "Transport", "Show transport details.", listOf("Simple", "Detailed")))),
        ModuleDefinition("pack_switcher", "Pack Switcher", "Quick access to Bedrock pack profiles.", "Client",
            listOf(ModuleSetting("remember", "Remember", "Keep the last selected pack profile.", listOf("On", "Off")))),
        ModuleDefinition("profile_switcher", "Profile Switcher", "Switch Riplow configurations.", "Client",
            listOf(ModuleSetting("profile", "Active profile", "Riplow configuration profile.", listOf("Default", "Performance", "Quiet")))),
        ModuleDefinition("addon_manager", "Add-on Manager", "Manage imported Bedrock content.", "Client",
            listOf(ModuleSetting("scan", "Auto-scan", "Scan known content folders on launch.", listOf("On", "Off")))),
        ModuleDefinition("launch_monitor", "Launch Monitor", "Track launch handoff and failures.", "Client",
            listOf(ModuleSetting("timeout", "Timeout", "How long to wait for launch handoff.", listOf("15s", "30s", "60s")))),
        ModuleDefinition("client_menu", "Client Menu", "Quick access to Riplow controls.", "Client",
            listOf(ModuleSetting("defaultTab", "Default tab", "Tab opened by the shortcut.", listOf("Modules", "HUD", "Performance", "Network")))),

        ModuleDefinition("fps_boost_profile", "FPS Boost Profile", "Reduces Riplow overlay and diagnostic overhead while Minecraft is active.", "Performance",\n            listOf(ModuleSetting("mode", "Mode", "Local Riplow performance policy.", listOf("Balanced", "Performance", "Extreme"), "Performance"))),
        ModuleDefinition("fps_unlocker", "FPS Unlocker", "Frame-rate control when a compatible game bridge exists.", "Performance"),
        ModuleDefinition("shader_loader", "Shader Loader", "Manage compatible Bedrock shader/material profiles.", "Performance"),
        ModuleDefinition("render_budget", "Render Budget", "Track safe visual-complexity targets for a device.", "Performance"),
        ModuleDefinition("gpu_budget", "GPU Budget", "Device-side visual budget helper.", "Performance"),
        ModuleDefinition("thermal_guard", "Thermal Guard", "Thermal-aware helper profile.", "Performance"),

        ModuleDefinition("fps_overlay", "FPS Overlay", "Movable FPS readout for the HUD.", "HUD"),
        ModuleDefinition("ping_counter", "Ping Counter", "Network latency display.", "HUD"),
        ModuleDefinition("speed_display", "Speed Display", "Movement-speed readout when game data is available.", "HUD"),
        ModuleDefinition("potion_hud", "Potion HUD", "Active-effect display.", "HUD"),
        ModuleDefinition("armor_hud", "Armor HUD", "Armor/status display.", "HUD"),
        ModuleDefinition("boss_bar", "Boss Bar", "Movable boss-bar presentation.", "HUD"),
        ModuleDefinition("scoreboard", "Scoreboard", "Movable scoreboard presentation.", "HUD"),
        ModuleDefinition("tablist", "Tablist", "Compact player-list presentation.", "HUD"),
        ModuleDefinition("debug_panel", "Debug Panel", "Compact diagnostics overlay.", "HUD"),
        ModuleDefinition("direction_hud", "Direction HUD", "Heading and navigation display.", "HUD"),
        ModuleDefinition("break_indicator", "Break Indicator", "Visual block-break progress indicator.", "HUD"),
        ModuleDefinition("combo_display", "Combo Display", "Visual combo counter when game events are available.", "HUD"),
        ModuleDefinition("timer", "Timer", "General purpose HUD timer.", "HUD"),
        ModuleDefinition("tnt_timer", "TNT Timer", "TNT timing display when game data is available.", "HUD"),

        ModuleDefinition("fullbright", "Fullbright", "Visibility helper for dark areas.", "Visual"),
        ModuleDefinition("no_fog", "No Fog", "Disable fog when supported by the game bridge.", "Visual"),
        ModuleDefinition("fog_color", "Fog Color", "Customize fog color when supported.", "Visual"),
        ModuleDefinition("view_model", "View Model", "Adjust first-person item presentation.", "Visual"),
        ModuleDefinition("block_outline", "Block Outline", "Enhanced targeted-block outline.", "Visual"),
        ModuleDefinition("light_overlay", "Light Overlay", "Show light-level guidance.", "Visual"),
        ModuleDefinition("hit_particles", "Hit Particles", "Optional visual hit particles.", "Visual"),
        ModuleDefinition("better_nametags", "Better Nametags", "Improved player-name presentation.", "Visual"),
        ModuleDefinition("third_person_nametag", "Third Person Nametag", "Third-person nametags where supported.", "Visual"),
        ModuleDefinition("chunk_borders", "Chunk Borders", "Chunk boundary visualization.", "Visual"),
        ModuleDefinition("motion_blur", "Motion Blur", "Visual motion-blur effect.", "Visual"),
        ModuleDefinition("view_bobbing", "View Bobbing", "Control first-person camera bobbing.", "Visual"),
        ModuleDefinition("fov_changer", "FOV Changer", "Convenient FOV presets.", "Visual"),
        ModuleDefinition("gui_scale", "GUI Scale", "Client-side UI scale preference.", "Visual"),
        ModuleDefinition("low_fire", "Low Fire", "Reduce first-person fire obstruction.", "Visual"),
        ModuleDefinition("shulker_preview", "Shulker Preview", "Preview container contents when supported.", "Visual"),
        ModuleDefinition("connected_glass", "Connected Glass", "Connected-glass styling helper.", "Visual"),
        ModuleDefinition("minimap", "Minimap", "Optional overhead navigation map.", "Visual"),
        ModuleDefinition("waypoints", "Waypoints", "World location markers.", "Visual"),
        ModuleDefinition("breadcrumbs", "Breadcrumbs", "Recent-path navigation markers.", "Visual"),
        ModuleDefinition("loot_beams", "Loot Beams", "Optional item-drop visual markers.", "Visual"),

        ModuleDefinition("auto_reconnect", "Auto Reconnect", "Retry a disconnected session when supported.", "Utility"),
        ModuleDefinition("anti_afk", "Anti-AFK", "Idle-session helper without automated combat.", "Utility"),
        ModuleDefinition("no_disconnect", "No Disconnect", "Keep the client overlay stable during ordinary drops.", "Utility"),
        ModuleDefinition("chat_timestamps", "Chat Timestamps", "Add local timestamps to chat presentation.", "Utility"),
        ModuleDefinition("better_chat", "Better Chat", "Chat presentation preferences.", "Utility"),
        ModuleDefinition("drop_prevention", "Drop Prevention", "Confirmation before risky inventory drops.", "Utility"),
        ModuleDefinition("world_manager", "World Manager", "World organization and launch profiles.", "Utility"),
        ModuleDefinition("backup_manager", "Backup Manager", "Local profile and launcher backup workflow.", "Utility"),
        ModuleDefinition("content_scanner", "Content Scanner", "Scan supported Riplow-managed content locations.", "Utility"),
        ModuleDefinition("quick_launch", "Quick Launch", "Shortcut to the detected Minecraft installation.", "Utility"),
        ModuleDefinition("version_profiles", "Version Profiles", "Associate Riplow settings with Minecraft versions.", "Utility"),

        ModuleDefinition("packet_loss", "Packet Loss", "Track reported packet-loss percentage.", "Network"),
        ModuleDefinition("transport_inspector", "Transport Inspector", "Inspect Bedrock transport diagnostics.", "Network"),
        ModuleDefinition("session_transport", "Session Transport", "Summarize the active Bedrock transport state.", "Network"),

        ModuleDefinition("pack_profiles", "Pack Profiles", "Save and switch Riplow-managed pack profiles.", "Client"),
        ModuleDefinition("version_manager", "Version Manager", "Organize supported Minecraft version profiles.", "Client"),
        ModuleDefinition("module_config", "Module Config", "Export and import module state and settings as JSON.", "Client"),
        ModuleDefinition("theme_manager", "Theme Manager", "Choose Riplow interface presets.", "Client"),
        ModuleDefinition("extension_center", "Extension Center", "Home for trusted Riplow extensions.", "Client"),
        ModuleDefinition("cosmetics_preview", "Cosmetics Preview", "Preview locally stored client cosmetics.", "Client"),
        ModuleDefinition("replay_timeline", "Replay Timeline", "Replay timeline workspace for future capture integration.", "Client"),
        ModuleDefinition("script_center", "Script Center", "Future home for sandboxed Riplow scripts.", "Client"),
        ModuleDefinition("replay_preview", "Replay Preview", "Replay preview workspace for future recorder integration.", "Client")
    )

    val gameBridgeIds = setOf(
        "fps", "cps", "keystrokes", "coordinates", "compass", "item_info",
        "zoom", "crosshair", "hud", "fps_overlay", "ping_counter", "speed_display",
        "potion_hud", "armor_hud", "boss_bar", "scoreboard", "tablist", "debug_panel",
        "direction_hud", "break_indicator", "combo_display", "timer", "tnt_timer",
        "fullbright", "no_fog", "fog_color", "view_model", "block_outline",
        "light_overlay", "hit_particles", "better_nametags", "third_person_nametag",
        "chunk_borders", "motion_blur", "view_bobbing", "fov_changer", "gui_scale",
        "low_fire", "shulker_preview", "connected_glass", "minimap", "waypoints",
        "breadcrumbs", "loot_beams", "replay_preview", "auto_reconnect", "anti_afk",
        "chat_timestamps", "better_chat", "drop_prevention"
    )

    val nativeIds = setOf(
        "ping", "network_diagnostics", "jitter_monitor", "connection_status",
        "packet_loss", "transport_inspector", "session_transport", "ping_counter"
    )

    fun requiresGameBridge(id: String): Boolean = gameBridgeIds.contains(id)
    fun usesNativeTelemetry(id: String): Boolean = nativeIds.contains(id)
}
