package com.riplow.client

/**
 * Safe JNI boundary.
 *
 * The Android UI must remain usable when the native library is unavailable
 * because of an ABI mismatch, packaging failure, or a native initialization
 * problem. Native calls therefore have explicit fallbacks.
 */
object NativeBridge {
    private var loadError: String? = null

    private var loaded: Boolean = try {
        System.loadLibrary("riplow_core")
        true
    } catch (error: Throwable) {
        loadError = error.javaClass.simpleName + (error.message?.let { ": " + it } ?: "")
        false
    }

    fun isAvailable(): Boolean = loaded

    fun loadStatus(): String =
        if (loaded) "Native core loaded"
        else "Native core unavailable: " + (loadError ?: "unknown error")

    fun version(): String =
        if (!loaded) "native-unavailable"
        else runCatching { nativeVersion() }.getOrDefault("native-error")

    fun nativeModuleSummary(): String =
        if (!loaded) "" else runCatching { nativeModuleSummaryNative() }.getOrDefault("")

    fun nativeDiagnostics(): String =
        if (!loaded) "Native core unavailable\n" + loadStatus()
        else runCatching { nativeDiagnosticsNative() }.getOrElse {
            "Native diagnostics failed\n" + it.javaClass.simpleName
        }

    fun nativeToggleModule(id: String): Boolean =
        if (!loaded) false else runCatching { nativeToggleModuleNative(id) }.getOrDefault(false)

    fun nativeSetModule(id: String, enabled: Boolean): Boolean =
        if (!loaded) false else runCatching { nativeSetModuleNative(id, enabled) }.getOrDefault(false)

    private external fun nativeVersion(): String
    private external fun nativeModuleSummaryNative(): String
    private external fun nativeDiagnosticsNative(): String
    private external fun nativeToggleModuleNative(id: String): Boolean
    private external fun nativeSetModuleNative(id: String, enabled: Boolean): Boolean
}
