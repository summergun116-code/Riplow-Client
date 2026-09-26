package com.riplow.client.modules

import android.content.SharedPreferences

enum class RiplowPerformanceMode(
    val label: String,
    val overlayUpdateMs: Long,
    val diagnosticsUpdateMs: Long,
    val animationMs: Long
) {
    BALANCED("Balanced", 1000L, 1000L, 190L),
    PERFORMANCE("Performance", 2500L, 2000L, 110L),
    EXTREME("Extreme", 5000L, 4000L, 0L)
}

object PerformancePolicy {
    fun mode(prefs: SharedPreferences): RiplowPerformanceMode =
        when (prefs.getString(
            "module_setting_fps_boost_profile_mode",
            RiplowPerformanceMode.PERFORMANCE.label
        )) {
            RiplowPerformanceMode.EXTREME.label -> RiplowPerformanceMode.EXTREME
            RiplowPerformanceMode.BALANCED.label -> RiplowPerformanceMode.BALANCED
            else -> RiplowPerformanceMode.PERFORMANCE
        }

    fun overlayUpdateMs(prefs: SharedPreferences): Long = mode(prefs).overlayUpdateMs
    fun diagnosticsUpdateMs(prefs: SharedPreferences): Long = mode(prefs).diagnosticsUpdateMs
    fun animationMs(prefs: SharedPreferences): Long = mode(prefs).animationMs
}
