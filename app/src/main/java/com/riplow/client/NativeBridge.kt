package com.riplow.client

/**
 * Safe JNI boundary.
 *
 * Riplow remains usable when the native library is unavailable or when the
 * optional in-process render integration is intentionally disabled.
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
        if (!loaded) "Native core unavailable
" + loadStatus()
        else runCatching { nativeDiagnosticsNative() }.getOrElse {
            "Native diagnostics failed
" + it.javaClass.simpleName
        }

    fun renderStatus(): String =
        if (!loaded) "native-unavailable"
        else runCatching { nativeRenderStatusNative() }.getOrDefault("native-error")

    fun startRenderPipeline(): Boolean =
        if (!loaded) false else runCatching { nativeStartRenderPipelineNative() }.getOrDefault(false)

    fun stopRenderPipeline() {
        if (loaded) runCatching { nativeStopRenderPipelineNative() }
    }

    fun setRenderHudEnabled(enabled: Boolean) {
        if (loaded) runCatching { nativeSetRenderHudEnabledNative(enabled) }
    }

    fun pushTouch(action: Int, x: Float, y: Float, pressure: Float): Boolean =
        if (!loaded) false else runCatching {
            nativePushTouchNative(action, x, y, pressure)
        }.getOrDefault(false)

    fun nativeToggleModule(id: String): Boolean =
        if (!loaded) false else runCatching { nativeToggleModuleNative(id) }.getOrDefault(false)

    fun nativeSetModule(id: String, enabled: Boolean): Boolean =
        if (!loaded) false else runCatching { nativeSetModuleNative(id, enabled) }.getOrDefault(false)

    private external fun nativeVersion(): String
    private external fun nativeModuleSummaryNative(): String
    private external fun nativeDiagnosticsNative(): String
    private external fun nativeRenderStatusNative(): String
    private external fun nativeStartRenderPipelineNative(): Boolean
    private external fun nativeStopRenderPipelineNative()
    private external fun nativeSetRenderHudEnabledNative(enabled: Boolean)
    private external fun nativePushTouchNative(
        action: Int,
        x: Float,
        y: Float,
        pressure: Float
    ): Boolean
    private external fun nativeToggleModuleNative(id: String): Boolean
    private external fun nativeSetModuleNative(id: String, enabled: Boolean): Boolean
}
