package com.riplow.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.riplow.client.modules.ModuleDefinition
import com.riplow.client.modules.ModuleRegistry

class ClientOverlayService : Service() {
    companion object {
        const val ACTION_OPEN = "com.riplow.client.OPEN"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var bubble: TextView
    private var panel: View? = null
    private var menuWidth = 0
    private var menuHeight = 0
    private var activeTab = "Modules"
    private val prefs by lazy { getSharedPreferences("riplow_settings", MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        startOverlayForegroundService()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_OPEN || panel == null) showPanel()
        return START_STICKY
    }

    private fun startOverlayForegroundService() {
        val channelId = "riplow_overlay"
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(channelId, "Riplow overlay", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Keeps the user-enabled Riplow overlay available while Minecraft is in use."
                    setShowBadge(false)
                }
            )
        }

        val notification: Notification =
            androidx.core.app.NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_stat_riplow)
                .setContentTitle("Riplow overlay active")
                .setContentText("Client menu and diagnostics are running.")
                .setOngoing(true)
                .setCategory(androidx.core.app.NotificationCompat.CATEGORY_SERVICE)
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
                .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(1001, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1001, notification)
        }
    }

    private fun overlayParams(width: Int, height: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }

    private fun calculateMenuSize() {
        val metrics = resources.displayMetrics
        val compact = prefs.getBoolean("compact_menu", false)
        val widthRatio = if (compact) 0.78f else 0.86f
        val heightRatio = if (compact) 0.74f else 0.82f
        menuWidth = (metrics.widthPixels * widthRatio).toInt()
            .coerceAtLeast(dp(if (compact) 420 else 520))
        menuHeight = (metrics.heightPixels * heightRatio).toInt()
            .coerceAtLeast(dp(if (compact) 290 else 310))
        menuWidth = menuWidth.coerceAtMost(dp(980))
        menuHeight = menuHeight.coerceAtMost(dp(620))
    }

    private fun createBubble() {
        bubble = TextView(this).apply {
            text = "R"
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = backgroundShape(Color.rgb(24, 25, 29), 20)
            elevation = 10f
            setOnClickListener { togglePanel() }
            setOnTouchListener(DragTouchListener())
        }
        windowManager.addView(bubble, overlayParams(dp(54), dp(54)).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(18)
            y = dp(76)
        })
    }

    private fun showPanel() {
        if (panel != null) return
        activeTab = "Modules"
        calculateMenuSize()
        panel = buildPanel()
        panel?.alpha = 0f
        panel?.scaleX = 0.97f
        panel?.scaleY = 0.97f
        windowManager.addView(panel, overlayParams(menuWidth, menuHeight))
        panel?.animate()
            ?.alpha(1f)
            ?.scaleX(1f)
            ?.scaleY(1f)
            ?.setDuration(if (prefs.getBoolean("reduced_motion", false)) 90 else 190)
            ?.start()
    }

    private fun togglePanel() {
        if (panel != null) {
            panel?.animate()?.alpha(0f)?.setDuration(if (prefs.getBoolean("reduced_motion", false)) 50 else 120)?.withEndAction {
                panel?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
                panel = null
            }?.start()
        } else {
            showPanel()
        }
    }

    private fun buildPanel(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = backgroundShape(Color.rgb(12, 13, 16), 26)
            elevation = 18f
        }

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }

        header.addView(TextView(this).apply {
            text = "RIPLOW"
            textSize = 20f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, dp(38), 1f))

        header.addView(TextView(this).apply {
            text = "${ModuleRegistry.all.size} modules"
            textSize = 10f
            setTextColor(Color.rgb(185, 188, 196))
            gravity = Gravity.CENTER
            background = backgroundShape(Color.rgb(27, 29, 34), 50)
            setPadding(dp(12), 0, dp(12), 0)
        }, LinearLayout.LayoutParams(-2, dp(32)))

        header.addView(TextView(this).apply {
            text = "×"
            textSize = 20f
            setTextColor(Color.rgb(170, 173, 181))
            gravity = Gravity.CENTER
            setOnClickListener { togglePanel() }
        }, LinearLayout.LayoutParams(dp(40), dp(38)))

        root.addView(header)

        root.addView(TextView(this).apply {
            text = "Landscape ClickGUI  •  helper / QoL tools"
            textSize = 10f
            setTextColor(Color.rgb(138, 141, 150))
        })

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(10), 0, dp(9))
        }

        listOf("Modules", "HUD", "Performance", "Network", "Settings").forEach { tab ->
            tabs.addView(TextView(this).apply {
                text = tab
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (tab == activeTab) Color.WHITE else Color.rgb(130, 133, 141))
                background = backgroundShape(
                    if (tab == activeTab) Color.rgb(36, 38, 44) else Color.rgb(18, 19, 22),
                    13
                )
                setPadding(dp(10), 0, dp(10), 0)
                setOnClickListener {
                    activeTab = tab
                    rebuildPanel()
                }
            }, LinearLayout.LayoutParams(0, dp(36), 1f).apply {
                marginEnd = dp(5)
            })
        }

        root.addView(tabs)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }
        scroll.addView(buildTabContent())
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(Button(this).apply {
            text = "Close"
            isAllCaps = false
            textSize = 11f
            setTextColor(Color.LTGRAY)
            background = backgroundShape(Color.rgb(27, 28, 32), 14)
            setOnClickListener { togglePanel() }
        }, LinearLayout.LayoutParams(-1, dp(42)).apply {
            topMargin = dp(9)
        })

        return root
    }

    private fun buildTabContent(): View {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(2), 0, dp(4))
        }

        when (activeTab) {
            "Modules" -> {
                addSection(list, "ALL MODULES", "Core-registered helpers. Game integration is wired progressively.")
                addModuleGrid(list, ModuleRegistry.all)
            }
            "HUD" -> {
                addSection(list, "HUD & VISUALS", "Information and presentation helpers.")
                addModuleGrid(
                    list,
                    ModuleRegistry.all.filter {
                        it.category == "HUD" ||
                            it.category == "Visual" ||
                            it.id in listOf("fps", "cps", "coordinates", "keystrokes", "clock", "compass", "item_info")
                    }
                )
            }
            "Performance" -> {
                addSection(list, "PERFORMANCE", "Telemetry and profile controls. Riplow does not fake renderer optimizations.")
                addModuleGrid(list, ModuleRegistry.all.filter { it.category == "Performance" })
                addActionRow(list, "Native telemetry", NativeBridge.nativeDiagnostics())
            }
            "Network" -> {
                addSection(list, "NETWORK", "Bedrock-aware RakNet/UDP diagnostics.")
                addModuleGrid(list, ModuleRegistry.all.filter { it.category == "Network" })
                addActionRow(list, "Current diagnostics", NativeBridge.nativeDiagnostics())
            }
            "Settings" -> {
                addSection(list, "CLIENT SETTINGS", "Preferences persist locally on this device.")
                addSettingRow(list, "Compact menu", "Reduce the ClickGUI footprint", "compact_menu")
                addSettingRow(list, "Remember modules", "Keep module state between sessions", "remember_modules")
                addSettingRow(list, "Reduced motion", "Shorten UI transitions", "reduced_motion")
                addActionRow(list, "Reset local settings", "Clear Riplow preferences") {
                    prefs.edit().clear().apply()
                    ModuleRegistry.all.forEach { NativeBridge.nativeSetModule(it.id, false) }
                    Toast.makeText(this, "Riplow settings reset", Toast.LENGTH_SHORT).show()
                    rebuildPanel()
                }
            }
        }

        return list
    }

    private fun addModuleGrid(parent: LinearLayout, modules: List<ModuleDefinition>) {
        modules.chunked(2).forEach { pair ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }

            pair.forEach { module ->
                val holder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                addModuleRow(holder, module)
                row.addView(holder, LinearLayout.LayoutParams(0, -2, 1f).apply {
                    if (row.childCount > 0) marginStart = dp(7)
                })
            }

            if (pair.size == 1) {
                row.addView(LinearLayout(this), LinearLayout.LayoutParams(0, dp(72), 1f))
            }

            parent.addView(row, LinearLayout.LayoutParams(-1, -2).apply {
                bottomMargin = dp(7)
            })
        }
    }

    private fun addSection(parent: LinearLayout, title: String, detail: String) {
        parent.addView(TextView(this).apply {
            text = title
            textSize = 10f
            setTextColor(Color.rgb(130, 133, 141))
        }, LinearLayout.LayoutParams(-1, dp(20)))

        parent.addView(TextView(this).apply {
            text = detail
            textSize = 11f
            setTextColor(Color.rgb(175, 178, 186))
            setPadding(0, 0, 0, dp(8))
        })
    }

    private fun addModuleRow(parent: LinearLayout, module: ModuleDefinition) {
        val key = "module_" + module.id
        val remember = prefs.getBoolean("remember_modules", true)
        val enabled = if (remember) prefs.getBoolean(key, false) else false
        NativeBridge.nativeSetModule(module.id, enabled)

        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(10), dp(8))
            background = backgroundShape(Color.rgb(24, 25, 29), 16)
            setOnClickListener {
                val next = !enabled
                if (remember) prefs.edit().putBoolean(key, next).apply()
                NativeBridge.nativeSetModule(module.id, next)
                rebuildPanel()
            }
        }

        val textBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        textBox.addView(TextView(this).apply {
            text = module.title
            textSize = 13f
            setTextColor(Color.WHITE)
        })

        textBox.addView(TextView(this).apply {
            text = module.description
            textSize = 9f
            setTextColor(Color.rgb(130, 133, 141))
            setPadding(0, dp(3), 0, 0)
        })

        row.addView(textBox, LinearLayout.LayoutParams(0, dp(52), 1f))

        row.addView(TextView(this).apply {
            text = if (enabled) "ON" else "OFF"
            textSize = 9f
            gravity = Gravity.CENTER
            setTextColor(if (enabled) Color.BLACK else Color.LTGRAY)
            background = backgroundShape(
                if (enabled) Color.rgb(220, 223, 226) else Color.rgb(42, 44, 49),
                11
            )
            setPadding(dp(10), 0, dp(10), 0)
        }, LinearLayout.LayoutParams(-2, dp(30)))

        parent.addView(row, LinearLayout.LayoutParams(-1, dp(72)))
    }

    private fun addSettingRow(parent: LinearLayout, title: String, detail: String, key: String) {
        val value = prefs.getBoolean(key, false)
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(10), dp(8))
            background = backgroundShape(Color.rgb(24, 25, 29), 16)
            setOnClickListener {
                prefs.edit().putBoolean(key, !value).apply()
                rebuildPanel()
            }
        }

        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        texts.addView(TextView(this).apply {
            text = title
            textSize = 13f
            setTextColor(Color.WHITE)
        })

        texts.addView(TextView(this).apply {
            text = detail
            textSize = 9f
            setTextColor(Color.rgb(130, 133, 141))
            setPadding(0, dp(3), 0, 0)
        })

        row.addView(texts, LinearLayout.LayoutParams(0, dp(52), 1f))

        row.addView(TextView(this).apply {
            text = if (value) "ON" else "OFF"
            gravity = Gravity.CENTER
            textSize = 9f
            setTextColor(if (value) Color.BLACK else Color.LTGRAY)
            background = backgroundShape(
                if (value) Color.rgb(220, 223, 226) else Color.rgb(42, 44, 49),
                11
            )
            setPadding(dp(10), 0, dp(10), 0)
        }, LinearLayout.LayoutParams(-2, dp(30)))

        parent.addView(row, LinearLayout.LayoutParams(-1, dp(72)).apply {
            bottomMargin = dp(7)
        })
    }

    private fun addActionRow(
        parent: LinearLayout,
        title: String,
        detail: String,
        action: (() -> Unit)? = null
    ) {
        parent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), dp(11), dp(12), dp(11))
            background = backgroundShape(Color.rgb(24, 25, 29), 16)
            if (action != null) setOnClickListener { action() }

            addView(TextView(this@ClientOverlayService).apply {
                text = title
                textSize = 12f
                setTextColor(Color.WHITE)
            })

            addView(TextView(this@ClientOverlayService).apply {
                text = detail
                textSize = 9f
                setTextColor(Color.rgb(130, 133, 141))
                setPadding(0, dp(4), 0, 0)
            })
        }, LinearLayout.LayoutParams(-1, -2).apply {
            bottomMargin = dp(7)
        })
    }

    private fun rebuildPanel() {
        panel?.let {
            if (it.isAttachedToWindow) windowManager.removeView(it)
        }
        panel = null
        showPanel()
    }

    private fun backgroundShape(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
        setStroke(dp(1), Color.rgb(43, 45, 51))
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        panel?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        if (::bubble.isInitialized && bubble.isAttachedToWindow) {
            windowManager.removeView(bubble)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private inner class DragTouchListener : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0

        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                    val p = view.layoutParams as WindowManager.LayoutParams
                    startX = p.x
                    startY = p.y
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val p = view.layoutParams as WindowManager.LayoutParams
                    p.x = startX + (event.rawX - downX).toInt()
                    p.y = startY + (event.rawY - downY).toInt()
                    windowManager.updateViewLayout(view, p)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (kotlin.math.abs(event.rawX - downX) < 12 &&
                        kotlin.math.abs(event.rawY - downY) < 12) {
                        view.performClick()
                    }
                    return true
                }
            }
            return false
        }
    }
}
