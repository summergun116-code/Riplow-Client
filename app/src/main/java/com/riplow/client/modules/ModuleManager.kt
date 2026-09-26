package com.riplow.client.modules

import android.content.SharedPreferences
import com.riplow.client.NativeBridge
import org.json.JSONObject

/**
 * Riplow's persistent module layer.
 *
 * Architecture is intentionally simple: identity/category/settings live in
 * ModuleRegistry, mutable state lives here, and the native module core is
 * synchronised only when state changes or the overlay is opened.
 */
object ModuleManager {
    private const val ENABLED_PREFIX = "module_enabled_"
    private const val SETTING_PREFIX = "module_setting_"
    private val transientState = mutableMapOf<String, Boolean>()

    private fun remembers(prefs: SharedPreferences): Boolean =
        prefs.getBoolean("remember_modules", true)

    fun isEnabled(prefs: SharedPreferences, id: String): Boolean {
        if (ModuleRegistry.requiresGameBridge(id)) return false
        return if (remembers(prefs)) {
            prefs.getBoolean(ENABLED_PREFIX + id, false)
        } else {
            transientState[id] ?: false
        }
    }

    fun setEnabled(prefs: SharedPreferences, id: String, enabled: Boolean): Boolean {
        if (ModuleRegistry.requiresGameBridge(id)) {
            transientState[id] = false
            prefs.edit().remove(ENABLED_PREFIX + id).apply()
            try { NativeBridge.nativeSetModule(id, false) } catch (_: Throwable) {}
            return false
        }

        transientState[id] = enabled
        val editor = prefs.edit()
        if (remembers(prefs)) {
            editor.putBoolean(ENABLED_PREFIX + id, enabled)
        } else {
            editor.remove(ENABLED_PREFIX + id)
        }
        editor.apply()

        return try {
            NativeBridge.nativeSetModule(id, enabled)
        } catch (_: Throwable) {
            false
        }
    }

    fun toggle(prefs: SharedPreferences, id: String): Boolean {
        if (ModuleRegistry.requiresGameBridge(id)) return false
        val next = !isEnabled(prefs, id)
        return setEnabled(prefs, id, next)
    }

    fun setting(prefs: SharedPreferences, moduleId: String, setting: ModuleSetting): String =
        prefs.getString(SETTING_PREFIX + moduleId + "_" + setting.id, setting.defaultValue)
            ?: setting.defaultValue

    fun cycleSetting(
        prefs: SharedPreferences,
        moduleId: String,
        setting: ModuleSetting
    ): String {
        val current = setting(prefs, moduleId, setting)
        val currentIndex = setting.options.indexOf(current).coerceAtLeast(0)
        val next = setting.options[(currentIndex + 1) % setting.options.size]
        prefs.edit().putString(SETTING_PREFIX + moduleId + "_" + setting.id, next).apply()
        return next
    }

    fun syncNative(prefs: SharedPreferences) {
        val current = nativeStateMap()
        ModuleRegistry.all.forEach { module ->
            val enabled = isEnabled(prefs, module.id)
            if (current[module.id] == enabled) return@forEach
            try {
                NativeBridge.nativeSetModule(module.id, enabled)
            } catch (_: Throwable) {
                // UI/state persistence remains usable even when native integration is unavailable.
            }
        }
    }

    fun nativeStateMap(): Map<String, Boolean> {
        return try {
            NativeBridge.nativeModuleSummary()
                .lineSequence()
                .mapNotNull { line ->
                    val parts = line.split("|")
                    if (parts.size >= 4) parts[0] to (parts[3] == "ON") else null
                }
                .toMap()
        } catch (_: Throwable) {
            emptyMap()
        }
    }

    fun reset(prefs: SharedPreferences) {
        transientState.clear()
        val editor = prefs.edit()
        ModuleRegistry.all.forEach { module ->
            editor.remove(ENABLED_PREFIX + module.id)
            module.settings.forEach { setting ->
                editor.remove(SETTING_PREFIX + module.id + "_" + setting.id)
            }
        }
        editor.apply()
        ModuleRegistry.all.forEach { module ->
            try {
                NativeBridge.nativeSetModule(module.id, false)
            } catch (_: Throwable) {
            }
        }
    }

    fun exportJson(prefs: SharedPreferences): String {
        val root = JSONObject()
        root.put("version", 1)
        val modules = JSONObject()
        ModuleRegistry.all.forEach { module ->
            val item = JSONObject()
            item.put("enabled", isEnabled(prefs, module.id))
            val values = JSONObject()
            module.settings.forEach { setting ->
                values.put(setting.id, setting(prefs, module.id, setting))
            }
            item.put("settings", values)
            modules.put(module.id, item)
        }
        root.put("modules", modules)
        return root.toString(2)
    }

    fun importJson(prefs: SharedPreferences, raw: String): Boolean {
        return try {
            val root = JSONObject(raw)
            val modules = root.optJSONObject("modules") ?: return false
            val editor = prefs.edit()

            ModuleRegistry.all.forEach { module ->
                val item = modules.optJSONObject(module.id) ?: return@forEach
                if (item.has("enabled")) {
                    if (ModuleRegistry.requiresGameBridge(module.id)) {
                        editor.remove(ENABLED_PREFIX + module.id)
                    } else {
                        editor.putBoolean(ENABLED_PREFIX + module.id, item.optBoolean("enabled"))
                    }
                }
                val settings = item.optJSONObject("settings")
                module.settings.forEach { setting ->
                    if (settings != null && settings.has(setting.id)) {
                        val value = settings.optString(setting.id, setting.defaultValue)
                        if (setting.options.contains(value)) {
                            editor.putString(
                                SETTING_PREFIX + module.id + "_" + setting.id,
                                value
                            )
                        }
                    }
                }
            }
            editor.apply()
            syncNative(prefs)
            true
        } catch (_: Throwable) {
            false
        }
    }
}
