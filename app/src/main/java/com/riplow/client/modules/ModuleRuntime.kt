package com.riplow.client.modules

import android.content.Context
import android.os.BatteryManager
import android.os.SystemClock
import com.riplow.client.NativeBridge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ModuleRuntime {
    private var startedAt = SystemClock.elapsedRealtime()

    fun resetSession() {
        startedAt = SystemClock.elapsedRealtime()
    }

    fun status(
        context: Context,
        prefs: android.content.SharedPreferences,
        module: ModuleDefinition
    ): String {
        return when (module.id) {
            "clock" -> {
                val fallback = ModuleSetting(
                    "format",
                    "Format",
                    "",
                    listOf("12-hour", "24-hour")
                )
                val format = ModuleManager.setting(
                    prefs,
                    module.id,
                    module.settings.firstOrNull() ?: fallback
                )
                SimpleDateFormat(
                    if (format.contains("24")) "HH:mm" else "h:mm a",
                    Locale.getDefault()
                ).format(Date())
            }

            "session_timer" -> {
                val total = (SystemClock.elapsedRealtime() - startedAt) / 1000L
                val h = total / 3600L
                val m = (total % 3600L) / 60L
                val s = total % 60L
                val format = prefs.getString(
                    "module_setting_session_timer_format",
                    "MM:SS"
                ) ?: "MM:SS"
                if (format.startsWith("HH")) {
                    "%02d:%02d:%02d".format(Locale.US, h, m, s)
                } else {
                    "%02d:%02d".format(Locale.US, m, s)
                }
            }

            "memory_monitor" -> {
                val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                val info = android.app.ActivityManager.MemoryInfo()
                manager.getMemoryInfo(info)
                "RAM " + (info.availMem / (1024L * 1024L)) + " MB free"
            }

            "battery_guard" -> {
                val battery = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
                val level = battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
                if (level in 0..100) "Battery " + level + "%" else "Battery unavailable"
            }

            "performance_profile", "fps_boost_profile" -> {
                val fallback = ModuleSetting("profile", "Profile", "", listOf("Balanced"))
                "Profile " + ModuleManager.setting(
                    prefs,
                    module.id,
                    module.settings.firstOrNull() ?: fallback
                )
            }

            "network_diagnostics",
            "ping",
            "jitter_monitor",
            "packet_loss",
            "connection_status",
            "transport_inspector",
            "session_transport",
            "ping_counter" -> {
                NativeBridge.nativeDiagnostics()
                    .lineSequence()
                    .lastOrNull { it.isNotBlank() }
                    ?: "Waiting for an active Bedrock session"
            }

            "launch_monitor", "quick_launch" -> {
                val install = MinecraftCompatibility.detect(context)
                if (install.installed) "Minecraft " + install.versionName
                else "Minecraft not detected"
            }

            "module_config" -> "JSON config ready"
            "pack_profiles", "pack_switcher" -> "Pack profiles ready"
            "addon_manager", "content_scanner" -> "Content manager ready"
            "profile_switcher" -> "Profiles ready"
            "version_manager", "version_profiles" -> "Version profiles ready"
            "world_manager", "backup_manager" -> "Local data tools ready"
            "extension_center", "script_center" -> "Extension system ready"
            else -> "Registered • Bedrock bridge required"
        }
    }
}
