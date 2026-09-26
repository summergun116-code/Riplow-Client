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
            listOf(ModuleSetting("defaultTab", "Default tab", "Tab opened by the shortcut.", listOf("Modules", "HUD", "Performance", "Network"))))
    )
}
