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
    private const val KEY_MODE = "riplow_performance_mode"

    fun mode(prefs: SharedPreferences): RiplowPerformanceMode =
        when (prefs.getString(KEY_MODE, RiplowPerformanceMode.PERFORMANCE.name)) {
            RiplowPerformanceMode.EXTREME.name -> RiplowPerformanceMode.EXTREME
            RiplowPerformanceMode.BALANCED.name -> RiplowPerformanceMode.BALANCED
            else -> RiplowPerformanceMode.PERFORMANCE
        }

    fun setMode(prefs: SharedPreferences, mode: RiplowPerformanceMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
    }

    fun overlayUpdateMs(prefs: SharedPreferences): Long = mode(prefs).overlayUpdateMs
    fun diagnosticsUpdateMs(prefs: SharedPreferences): Long = mode(prefs).diagnosticsUpdateMs
    fun animationMs(prefs: SharedPreferences): Long = mode(prefs).animationMs
}
