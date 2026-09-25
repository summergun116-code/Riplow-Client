package com.riplow.client.modules

data class ModuleDefinition(
    val id: String,
    val title: String,
    val description: String,
    val category: String
)

object ModuleRegistry {
    val all = listOf(
        ModuleDefinition("fps", "FPS Overlay", "Live frame-rate readout.", "HUD"),
        ModuleDefinition("frame_pacing", "Frame Pacing", "Smooth-frame timing telemetry.", "Performance"),
        ModuleDefinition("performance_profile", "Performance Profile", "Native performance profile control.", "Performance"),
        ModuleDefinition("cps", "CPS Counter", "Client-side click-rate counter.", "HUD"),
        ModuleDefinition("coordinates", "Coordinates", "Optional position readout.", "HUD"),
        ModuleDefinition("zoom", "Zoom", "Accessibility zoom control.", "Visual"),
        ModuleDefinition("crosshair", "Crosshair", "Custom client-side crosshair.", "Visual"),
        ModuleDefinition("hud", "HUD", "Movable HUD layer.", "HUD"),
        ModuleDefinition("ping", "Ping", "Network latency display.", "Network"),
        ModuleDefinition("network_diagnostics", "Network Diagnostics", "RakNet/UDP latency, jitter and loss telemetry.", "Network"),
        ModuleDefinition("client_menu", "Client Menu", "Quick access to Riplow controls.", "System")
    )
}
