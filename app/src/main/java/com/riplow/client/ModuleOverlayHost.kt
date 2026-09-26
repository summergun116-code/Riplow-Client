package com.riplow.client

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.riplow.client.modules.ModuleManager
import com.riplow.client.modules.ModuleRegistry
import com.riplow.client.modules.ModuleRuntime
import com.riplow.client.modules.PerformancePolicy

/**
 * Executes the first real Android-side modules as a lightweight overlay HUD.
 * This host is deliberately separate from the ClickGUI so modules continue
 * updating while the menu is closed.
 */
class ModuleOverlayHost(
    private val context: Context,
    private val windowManager: WindowManager
) {
    private val handler = Handler(Looper.getMainLooper())
    private var root: LinearLayout? = null
    private var activeIds: List<String> = emptyList()
    private val labels = linkedMapOf<String, TextView>()

    private val updater = object : Runnable {
        override fun run() {
            updateLabels()
            if (root != null) handler.postDelayed(this, PerformancePolicy.overlayUpdateMs(context.getSharedPreferences("riplow_settings", Context.MODE_PRIVATE)))
        }
    }

    fun sync(prefs: SharedPreferences) {
        val ids = ModuleRegistry.all
            .filter { it.id in com.riplow.client.modules.ModuleCapabilities.runtimeReadyIds }
            .filter { ModuleManager.isEnabled(prefs, it.id) }
            .map { it.id }

        if (ids.isEmpty()) {
            remove()
            return
        }

        if (root == null || ids != activeIds) {
            remove()
            activeIds = ids
            build()
        }

        updateLabels()
        handler.removeCallbacks(updater)
        handler.postDelayed(updater, PerformancePolicy.overlayUpdateMs(prefs))
    }

    private fun build() {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(9), dp(12), dp(9))
            background = surfaceBackground()
            alpha = 0.96f
            elevation = 8f
        }

        activeIds.forEach { id ->
            val module = ModuleRegistry.all.firstOrNull { it.id == id } ?: return@forEach
            val row = TextView(context).apply {
                textSize = 11f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(6), dp(4), dp(6), dp(4))
                contentDescription = module.title
            }
            labels[id] = row
            container.addView(
                row,
                LinearLayout.LayoutParams(-1, dp(30))
            )
        }

        val params = WindowManager.LayoutParams(
            dp(250),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(16)
            y = dp(16)
        }

        try {
            windowManager.addView(container, params)
            root = container
        } catch (_: Throwable) {
            labels.clear()
            activeIds = emptyList()
            root = null
        }
    }

    private fun updateLabels() {
        val prefs = context.getSharedPreferences("riplow_settings", Context.MODE_PRIVATE)
        activeIds.forEach { id ->
            val module = ModuleRegistry.all.firstOrNull { it.id == id } ?: return@forEach
            labels[id]?.text = module.title + "  •  " +
                ModuleRuntime.status(context, prefs, module)
        }
    }

    private fun remove() {
        handler.removeCallbacks(updater)
        root?.let {
            try {
                if (it.isAttachedToWindow) windowManager.removeView(it)
            } catch (_: Throwable) {
            }
        }
        root = null
        labels.clear()
        activeIds = emptyList()
    }

    fun destroy() {
        remove()
    }

    private fun surfaceBackground() = GradientDrawable().apply {
        setColor(Color.rgb(18, 19, 22))
        cornerRadius = dp(15).toFloat()
        setStroke(dp(1), Color.rgb(46, 48, 54))
    }

    private fun dp(value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
