package com.riplow.client.modules

import android.content.SharedPreferences
import com.riplow.client.NativeBridge
import org.json.JSONObject

/**
 * Riplow's persistent module layer.
 *
 * Registry = immutable definitions. Manager = mutable state. Native core is
 * optional and synchronized only when needed, so a JNI problem cannot take
 * down the launcher UI.
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

    fun setRememberModules(prefs: SharedPreferences, remember: Boolean) {
        val editor = prefs.edit().putBoolean("remember_modules", remember)
        if (remember) {
            ModuleRegistry.all.forEach { module ->
                if (!ModuleRegistry.requiresGameBridge(module.id) && transientState[module.id] == true) {
                    editor.putBoolean(ENABLED_PREFIX + module.id, true)
                }
            }
        } else {
            ModuleRegistry.all.forEach { module ->
                editor.remove(ENABLED_PREFIX + module.id)
            }
        }
        editor.apply()
    }

    fun setEnabled(prefs: SharedPreferences, id: String, enabled: Boolean): Boolean {
        if (ModuleRegistry.requiresGameBridge(id)) {
            transientState[id] = false
            prefs.edit().remove(ENABLED_PREFIX + id).apply()
            NativeBridge.nativeSetModule(id, false)
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

        return NativeBridge.nativeSetModule(id, enabled)
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
        if (setting.options.isEmpty()) return setting.defaultValue
        val current = setting(prefs, moduleId, setting)
        val currentIndex = setting.options.indexOf(current).takeIf { it >= 0 } ?: 0
        val next = setting.options[(currentIndex + 1) % setting.options.size]
        prefs.edit().putString(SETTING_PREFIX + moduleId + "_" + setting.id, next).apply()
        return next
    }

    fun syncNative(prefs: SharedPreferences) {
        if (!NativeBridge.isAvailable()) return
        val current = nativeStateMap()
        ModuleRegistry.all.forEach { module ->
            val enabled = isEnabled(prefs, module.id)
            if (current[module.id] == enabled) return@forEach
            NativeBridge.nativeSetModule(module.id, enabled)
        }
    }

    fun nativeStateMap(): Map<String, Boolean> {
        val summary = NativeBridge.nativeModuleSummary()
        if (summary.isBlank()) return emptyMap()
        return summary.lineSequence()
            .mapNotNull { line ->
                val parts = line.split("|", limit = 4)
                if (parts.size == 4) parts[0] to (parts[3] == "ON") else null
            }
            .toMap()
    }

    fun reset(prefs: SharedPreferences) {
        transientState.clear()
        val editor = prefs.edit()
            .remove("compact_menu")
            .remove("remember_modules")
            .remove("reduced_motion")
        ModuleRegistry.all.forEach { module ->
            editor.remove(ENABLED_PREFIX + module.id)
            module.settings.forEach { setting ->
                editor.remove(SETTING_PREFIX + module.id + "_" + setting.id)
            }
        }
        editor.apply()

        if (NativeBridge.isAvailable()) {
            ModuleRegistry.all.forEach { module ->
                NativeBridge.nativeSetModule(module.id, false)
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
                        transientState[module.id] = false
                        editor.remove(ENABLED_PREFIX + module.id)
                    } else {
                        val enabled = item.optBoolean("enabled")
                        transientState[module.id] = enabled
                        if (remembers(prefs)) {
                            editor.putBoolean(ENABLED_PREFIX + module.id, enabled)
                        } else {
                            editor.remove(ENABLED_PREFIX + module.id)
                        }
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
