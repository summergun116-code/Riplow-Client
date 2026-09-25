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
        showPanel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_OPEN) showPanel()
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
        val notification: Notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
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
            width, height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 18
            y = 110
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
        windowManager.addView(bubble, overlayParams(54, 54))
    }

    private fun showPanel() {
        if (panel != null) return
        activeTab = "Modules"
        panel = buildPanel()
        windowManager.addView(panel, overlayParams(menuWidth, menuHeight))
    }

    private fun togglePanel() {
        if (panel != null) {
            windowManager.removeView(panel)
            panel = null
        } else {
            showPanel()
        }
    }

    private fun buildPanel(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            background = backgroundShape(Color.rgb(12, 13, 16), 26)
            elevation = 18f
        }

        val header = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        header.addView(TextView(this).apply {
            text = "RIPLOW"
            textSize = 19f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
        }, LinearLayout.LayoutParams(0, 44, 1f))
        header.addView(TextView(this).apply {
            text = "0.1.1"
            textSize = 11f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            background = backgroundShape(Color.rgb(26, 27, 31), 50)
            setPadding(12, 0, 12, 0)
        }, LinearLayout.LayoutParams(-2, 34))
        root.addView(header)
        root.addView(TextView(this).apply {
            text = "Client overlay  •  server-safe utilities"
            textSize = 11f
            setTextColor(Color.rgb(145, 148, 156))
        })

        val tabs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 12, 0, 10)
        }
        listOf("Modules", "HUD", "Performance", "Network", "Settings").forEach { tab ->
            tabs.addView(TextView(this).apply {
                text = tab
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(if (tab == activeTab) Color.WHITE else Color.rgb(130, 133, 141))
                background = backgroundShape(
                    if (tab == activeTab) Color.rgb(35, 37, 42) else Color.rgb(18, 19, 22), 14
                )
                setPadding(9, 0, 9, 0)
                setOnClickListener { activeTab = tab; rebuildPanel() }
            }, LinearLayout.LayoutParams(0, 38, 1f).apply { marginEnd = 5 })
        }
        root.addView(tabs)

        val scroll = ScrollView(this)
        scroll.addView(buildTabContent())
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        root.addView(Button(this).apply {
            text = "Hide menu"
            isAllCaps = false
            textSize = 12f
            setTextColor(Color.LTGRAY)
            background = backgroundShape(Color.rgb(27, 28, 32), 16)
            setOnClickListener { togglePanel() }
        }, LinearLayout.LayoutParams(-1, 46).apply { topMargin = 10 })

        menuWidth = dp(350)
        menuHeight = dp(590)
        return root
    }

    private fun buildTabContent(): View {
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, 2, 0, 4)
        }
        when (activeTab) {
            "Modules" -> {
                addSection(list, "MODULES", "Tap a module to enable or disable it.")
                ModuleRegistry.all.forEach { addModuleRow(list, it) }
            }
            "HUD" -> {
                addSection(list, "HUD & VISUALS", "Clean, movable client-side information.")
                ModuleRegistry.all.filter { it.category == "HUD" || it.category == "Visual" }.forEach { addModuleRow(list, it) }
            }
            "Performance" -> {
                addSection(list, "PERFORMANCE", "Telemetry and smooth-frame tools.")
                ModuleRegistry.all.filter { it.category == "Performance" }.forEach { addModuleRow(list, it) }
                addActionRow(list, "Native telemetry", NativeBridge.nativeDiagnostics())
            }
            "Network" -> {
                addSection(list, "NETWORK", "Bedrock-aware RakNet/UDP diagnostics.")
                ModuleRegistry.all.filter { it.category == "Network" }.forEach { addModuleRow(list, it) }
                addActionRow(list, "Current diagnostics", NativeBridge.nativeDiagnostics())
            }
            "Settings" -> {
                addSection(list, "SETTINGS", "Preferences are stored locally on this device.")
                addSettingRow(list, "Compact menu", "Use a smaller overlay layout", "compact_menu")
                addSettingRow(list, "Remember modules", "Keep module state between sessions", "remember_modules")
                addSettingRow(list, "Reduced motion", "Use shorter UI transitions", "reduced_motion")
                addActionRow(list, "Reset local settings", "Clear Riplow preferences") {
                    prefs.edit().clear().apply()
                    Toast.makeText(this, "Riplow settings reset", Toast.LENGTH_SHORT).show()
                    rebuildPanel()
                }
            }
        }
        return list
    }

    private fun addSection(parent: LinearLayout, title: String, detail: String) {
        parent.addView(TextView(this).apply {
            text = title
            textSize = 10f
            setTextColor(Color.rgb(130, 133, 141))
        }, LinearLayout.LayoutParams(-1, 24))
        parent.addView(TextView(this).apply {
            text = detail
            textSize = 12f
            setTextColor(Color.rgb(175, 178, 186))
            setPadding(0, 0, 0, 10)
        })
    }

    private fun addModuleRow(parent: LinearLayout, module: ModuleDefinition) {
        val enabled = prefs.getBoolean("module_${module.id}", false)
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 8, 10, 8)
            background = backgroundShape(Color.rgb(24, 25, 29), 17)
        }
        val textBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        textBox.addView(TextView(this).apply {
            text = module.title
            textSize = 14f
            setTextColor(Color.WHITE)
        })
        textBox.addView(TextView(this).apply {
            text = module.description
            textSize = 10f
            setTextColor(Color.rgb(130, 133, 141))
        })
        row.addView(textBox, LinearLayout.LayoutParams(0, 58, 1f))
        row.addView(TextView(this).apply {
            text = if (enabled) "ON" else "OFF"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(if (enabled) Color.BLACK else Color.LTGRAY)
            background = backgroundShape(if (enabled) Color.rgb(220, 223, 226) else Color.rgb(42, 44, 49), 12)
            setPadding(13, 0, 13, 0)
        }, LinearLayout.LayoutParams(-2, 34))
        row.setOnClickListener {
            val next = NativeBridge.nativeToggleModule(module.id)
            prefs.edit().putBoolean("module_${module.id}", next).apply()
            rebuildPanel()
        }
        parent.addView(row, LinearLayout.LayoutParams(-1, 68).apply { bottomMargin = 7 })
    }

    private fun addSettingRow(parent: LinearLayout, title: String, detail: String, key: String) {
        val value = prefs.getBoolean(key, false)
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 8, 10, 8)
            background = backgroundShape(Color.rgb(24, 25, 29), 17)
            setOnClickListener {
                prefs.edit().putBoolean(key, !value).apply()
                rebuildPanel()
            }
        }
        val texts = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        texts.addView(TextView(this).apply {
            text = title
            textSize = 14f
            setTextColor(Color.WHITE)
        })
        texts.addView(TextView(this).apply {
            text = detail
            textSize = 10f
            setTextColor(Color.rgb(130, 133, 141))
        })
        row.addView(texts, LinearLayout.LayoutParams(0, 58, 1f))
        row.addView(TextView(this).apply {
            text = if (value) "ON" else "OFF"
            gravity = Gravity.CENTER
            textSize = 10f
            setTextColor(if (value) Color.BLACK else Color.LTGRAY)
            background = backgroundShape(if (value) Color.rgb(220, 223, 226) else Color.rgb(42, 44, 49), 12)
            setPadding(13, 0, 13, 0)
        }, LinearLayout.LayoutParams(-2, 34))
        parent.addView(row, LinearLayout.LayoutParams(-1, 68).apply { bottomMargin = 7 })
    }

    private fun addActionRow(parent: LinearLayout, title: String, detail: String, action: (() -> Unit)? = null) {
        parent.addView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            background = backgroundShape(Color.rgb(24, 25, 29), 17)
            if (action != null) setOnClickListener { action() }
            addView(TextView(this@ClientOverlayService).apply {
                text = title
                textSize = 13f
                setTextColor(Color.WHITE)
            })
            addView(TextView(this@ClientOverlayService).apply {
                text = detail
                textSize = 10f
                setTextColor(Color.rgb(130, 133, 141))
                setPadding(0, 4, 0, 0)
            })
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 7 })
    }

    private fun rebuildPanel() {
        panel?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        panel = buildPanel()
        windowManager.addView(panel, overlayParams(menuWidth, menuHeight))
    }

    private fun backgroundShape(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
        setStroke(1, Color.rgb(43, 45, 51))
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        panel?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        if (::bubble.isInitialized && bubble.isAttachedToWindow) windowManager.removeView(bubble)
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
                    if (kotlin.math.abs(event.rawX - downX) < 12 && kotlin.math.abs(event.rawY - downY) < 12) view.performClick()
                    return true
                }
            }
            return false
        }
    }
}
