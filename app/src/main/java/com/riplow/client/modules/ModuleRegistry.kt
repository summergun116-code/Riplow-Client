package com.riplow.client.modules

data class ModuleSetting(
    val id: String,
    val title: String,
    val options: List<String>,
    val defaultValue: String = options.firstOrNull() ?: ""
)

data class ModuleDefinition(
    val id: String,
    val title: String,
    val category: String,
    val settings: List<ModuleSetting> = emptyList()
)

object ModuleRegistry {
    val all = listOf(
        // Existing Riplow modules
        ModuleDefinition("fps", "FPS Telemetry", "Performance"),
        ModuleDefinition("frame_pacing", "Frame Pacing", "Performance",
            listOf(ModuleSetting("target", "Target", listOf("30 FPS", "60 FPS", "90 FPS", "120 FPS")))),
        ModuleDefinition("performance_profile", "Performance Profile", "Performance",
            listOf(ModuleSetting("profile", "Profile", listOf("Balanced", "Performance", "Battery"), "Performance"))),
        ModuleDefinition("memory_monitor", "Memory Monitor", "Performance",
            listOf(ModuleSetting("interval", "Refresh", listOf("1s", "3s", "5s"), "3s"))),
        ModuleDefinition("battery_guard", "Battery Monitor", "Performance"),

        ModuleDefinition("session_timer", "Session Timer", "HUD",
            listOf(ModuleSetting("format", "Format", listOf("MM:SS", "HH:MM:SS")))),
        ModuleDefinition("clock", "Clock", "HUD",
            listOf(ModuleSetting("format", "Format", listOf("12-hour", "24-hour")))),
        ModuleDefinition("coordinates", "Coordinates", "HUD"),
        ModuleDefinition("compass", "Compass", "HUD"),
        ModuleDefinition("fps_overlay", "FPS Overlay", "HUD"),
        ModuleDefinition("direction_hud", "Direction HUD", "HUD"),
        ModuleDefinition("potion_hud", "Potion HUD", "HUD"),
        ModuleDefinition("armor_hud", "Armor HUD", "HUD"),
        ModuleDefinition("scoreboard", "Scoreboard", "HUD"),

        ModuleDefinition("zoom", "Zoom", "Visual",
            listOf(ModuleSetting("factor", "Zoom", listOf("2x", "3x", "4x"), "2x"))),
        ModuleDefinition("crosshair", "Crosshair", "Visual",
            listOf(
                ModuleSetting("style", "Style", listOf("Cross", "Dot", "Circle", "Plus")),
                ModuleSetting("size", "Size", listOf("Small", "Medium", "Large"))
            )),
        ModuleDefinition("fullbright", "Fullbright", "Visual"),
        ModuleDefinition("no_fog", "No Fog", "Visual"),
        ModuleDefinition("view_bobbing", "View Bobbing", "Visual"),
        ModuleDefinition("fov_changer", "FOV Presets", "Visual"),
        ModuleDefinition("gui_scale", "GUI Scale", "Visual"),
        ModuleDefinition("low_fire", "Low Fire", "Visual"),

        ModuleDefinition("ping", "Ping", "Network"),
        ModuleDefinition("network_diagnostics", "Network Diagnostics", "Network"),
        ModuleDefinition("jitter_monitor", "Jitter Monitor", "Network"),
        ModuleDefinition("packet_loss", "Packet Loss", "Network"),
        ModuleDefinition("connection_status", "Connection Status", "Network"),

        ModuleDefinition("pack_switcher", "Pack Switcher", "Minecraft"),
        ModuleDefinition("pack_profiles", "Pack Profiles", "Minecraft"),
        ModuleDefinition("addon_manager", "Add-on Manager", "Minecraft"),
        ModuleDefinition("world_manager", "World Manager", "Minecraft"),
        ModuleDefinition("backup_manager", "World Backups", "Minecraft"),
        ModuleDefinition("version_profiles", "Version Profiles", "Minecraft"),
        ModuleDefinition("quick_launch", "Quick Launch", "Minecraft"),
        ModuleDefinition("module_config", "Module Config", "Minecraft"),

        // WClient module surface, excluding WAura
        ModuleDefinition("killaura", "Kill Aura", "Combat"),
        ModuleDefinition("auto_fight", "Auto Fight", "Combat"),
        ModuleDefinition("infinite_aura", "Infinite Aura", "Combat"),
        ModuleDefinition("aca", "ACA", "Combat"),
        ModuleDefinition("auto_totem", "Auto Totem", "Combat"),
        ModuleDefinition("auto_hvh", "Auto HvH", "Combat"),
        ModuleDefinition("enemy_hunter", "Enemy Hunter", "Combat"),
        ModuleDefinition("hotbar_switcher", "Hotbar Switcher", "Combat"),
        ModuleDefinition("anti_knockback", "Anti Knockback", "Combat"),
        ModuleDefinition("anti_crystal", "Anti Crystal", "Combat"),
        ModuleDefinition("hit_and_run", "Hit And Run", "Combat"),
        ModuleDefinition("hitbox", "Hitbox", "Combat"),
        ModuleDefinition("crystal_smash", "Crystal Smash", "Combat"),
        ModuleDefinition("trigger_bot", "Trigger Bot", "Combat"),

        ModuleDefinition("motion_fly", "Motion Fly", "Motion"),
        ModuleDefinition("player_tp", "Player TP", "Motion"),
        ModuleDefinition("fly", "Fly", "Motion"),
        ModuleDefinition("speed", "Speed", "Motion"),
        ModuleDefinition("air_jump", "Air Jump", "Motion"),
        ModuleDefinition("no_clip", "No Clip", "Motion"),
        ModuleDefinition("jet_pack", "Jet Pack", "Motion"),
        ModuleDefinition("high_jump", "High Jump", "Motion"),
        ModuleDefinition("bhop", "Bhop", "Motion"),
        ModuleDefinition("sprint", "Sprint", "Motion"),
        ModuleDefinition("auto_walk", "Auto Walk", "Motion"),
        ModuleDefinition("anti_afk", "Anti AFK", "Motion"),
        ModuleDefinition("spider", "Spider", "Motion"),

        ModuleDefinition("damage_text", "Damage Text", "Visual"),
        ModuleDefinition("esp", "ESP", "Visual"),
        ModuleDefinition("player_join", "Player Join", "Visual"),
        ModuleDefinition("no_hurt_camera", "No Hurt Camera", "Visual"),
        ModuleDefinition("speed_display", "Speed Display", "Visual"),
        ModuleDefinition("network_info", "Network Info", "Visual"),
        ModuleDefinition("world_state", "World State", "Visual"),
        ModuleDefinition("minimap", "Minimap", "Visual"),
        ModuleDefinition("target_hud", "Target HUD", "Visual"),

        ModuleDefinition("free_camera", "Free Camera", "World"),
        ModuleDefinition("time_shift", "Time Shift", "World"),
        ModuleDefinition("weather_controller", "Weather Controller", "World"),
        ModuleDefinition("effects", "Effects", "World"),
        ModuleDefinition("particles", "Particles", "World"),
        ModuleDefinition("anti_debuff", "Anti Debuff", "World"),

        ModuleDefinition("auto_disconnect", "Auto Disconnect", "Misc"),
        ModuleDefinition("array_list", "Array List", "Misc"),
        ModuleDefinition("toggle_sound", "Toggle Sound", "Misc"),
        ModuleDefinition("chest_stealer", "Chest Stealer", "Misc"),
        ModuleDefinition("desync", "Desync", "Misc"),
        ModuleDefinition("spammer", "Spammer", "Misc"),
        ModuleDefinition("watermark", "Watermark", "Misc"),
        ModuleDefinition("position_logger", "Position Logger", "Misc"),
        ModuleDefinition("no_chat", "No Chat", "Misc"),
        ModuleDefinition("command_handler", "Command Handler", "Misc"),
        ModuleDefinition("replay", "Replay", "Misc"),
        ModuleDefinition("pie_chart", "Pie Chart", "Misc"),
        ModuleDefinition("fake_death", "Fake Death", "Misc"),
        ModuleDefinition("fake_xp", "Fake XP", "Misc"),
        ModuleDefinition("miner", "Miner", "Misc"),

        // Additional documented WClient features
        ModuleDefinition("op_fight_bot", "OPFightBot", "Combat"),
        ModuleDefinition("auto_armour", "Auto Armour", "Combat"),
        ModuleDefinition("smooth_zoom", "Smooth Zoom", "Visual"),
        ModuleDefinition("pop_counter", "Pop Counter", "Visual"),
        ModuleDefinition("server_manager", "Server Manager", "Minecraft"),
        ModuleDefinition("game_mode_switcher", "GameMode Switcher", "Minecraft"),
        ModuleDefinition("random_chat_suffix", "Random Chat Suffix", "Misc"),
        ModuleDefinition("random_chat_prefix", "Random Chat Prefix", "Misc"),
        ModuleDefinition("add_server", "Add Server", "Minecraft"),
        ModuleDefinition("connection_manager", "Connection Manager", "Network"),

        // Features added in later WClient releases
        ModuleDefinition("tp_mine", "TPMine", "World"),
        ModuleDefinition("block_esp", "Block ESP", "Visual"),
        ModuleDefinition("stash_finder", "Stash Finder", "World"),
        ModuleDefinition("chest_esp", "Chest ESP", "Visual"),
        ModuleDefinition("fast_drop", "Fast Drop", "Misc"),
        ModuleDefinition("sus_chunk_finder", "Sus Chunk Finder", "World"),
        ModuleDefinition("config_manager", "Config Manager", "Misc"),
        ModuleDefinition("shulker_preview", "Shulker Preview", "Visual"),
        ModuleDefinition("old_motion_fly", "Old Motion Fly", "Motion"),
        ModuleDefinition("lifeboat_mode", "Lifeboat Mode", "Network"),
        ModuleDefinition("lifeboat_disabler", "Lifeboat Disabler", "Network"),
        ModuleDefinition("phase", "Phase", "Motion"),
        ModuleDefinition("ping_spoof", "Ping Spoof", "Network")
    )

    val gameBridgeIds = all
        .filter { it.category != "Performance" && it.category != "Client" }
        .mapTo(mutableSetOf()) { it.id }

    val nativeIds = setOf(
        "ping", "network_diagnostics", "jitter_monitor", "packet_loss",
        "connection_status"
    )

    fun requiresGameBridge(id: String): Boolean = gameBridgeIds.contains(id)
    fun usesNativeTelemetry(id: String): Boolean = nativeIds.contains(id)
}
