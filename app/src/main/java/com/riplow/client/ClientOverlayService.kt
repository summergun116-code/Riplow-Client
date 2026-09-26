package com.riplow.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
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
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.text.InputType
import android.widget.Toast
import com.riplow.client.modules.BedrockServerProbe
import com.riplow.client.modules.BedrockVersionParser
import com.riplow.client.modules.MinecraftCompatibility
import com.riplow.client.modules.ModuleAvailability
import com.riplow.client.modules.ModuleCapabilities
import com.riplow.client.modules.ModuleDefinition
import com.riplow.client.modules.ModuleManager
import com.riplow.client.modules.ModuleRegistry
import com.riplow.client.modules.ModuleRuntime

class ClientOverlayService : Service() {
    companion object {
        const val ACTION_OPEN = "com.riplow.client.OPEN"
        const val EXTRA_TAB = "com.riplow.client.TAB"
    }

    private lateinit var windowManager: WindowManager
    private lateinit var bubble: TextView
    private var panel: View? = null
    private var menuWidth = 0
    private var menuHeight = 0
    private var activeTab = "Modules"
    private var expandedModuleId: String? = null
    private val prefs by lazy { getSharedPreferences("riplow_settings", MODE_PRIVATE) }
    private lateinit var runtimeHost: ModuleOverlayHost

    override fun onCreate() {
        super.onCreate()
        try {
            startOverlayForegroundService()
        } catch (_: Throwable) {
            stopSelf()
            return
        }
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        runtimeHost = ModuleOverlayHost(this, windowManager)
        runtimeHost.sync(prefs)
        createBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_OPEN) {
            intent.getStringExtra(EXTRA_TAB)?.let { requestedTab ->
                if (requestedTab in listOf("Modules", "HUD", "Performance", "Network", "Settings")) {
                    activeTab = requestedTab
                }
            }
            showPanel()
        }
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

    private fun overlayParams(
        width: Int,
        height: Int,
        focusable: Boolean = false
    ): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            if (focusable) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
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
        menuWidth = menuWidth
            .coerceAtMost(dp(980))
            .coerceAtMost((metrics.widthPixels - dp(24)).coerceAtLeast(dp(280)))
        menuHeight = menuHeight
            .coerceAtMost(dp(620))
            .coerceAtMost((metrics.heightPixels - dp(24)).coerceAtLeast(dp(240)))
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
        try {
            windowManager.addView(bubble, overlayParams(dp(54), dp(54)).apply {
                gravity = Gravity.TOP or Gravity.START
                x = dp(18)
                y = dp(76)
            })
        } catch (_: Throwable) {
            stopSelf()
        }
    }

    private fun showPanel(animateOpen: Boolean = true) {
        if (panel != null) return
        ModuleManager.syncNative(prefs)
        runtimeHost.sync(prefs)
        calculateMenuSize()
        panel = buildPanel()
        try {
            windowManager.addView(panel, overlayParams(menuWidth, menuHeight, focusable = true))
        } catch (_: Throwable) {
            panel = null
            Toast.makeText(this, "Riplow overlay could not be opened", Toast.LENGTH_SHORT).show()
            return
        }
        if (animateOpen) {
            panel?.alpha = 0f
            panel?.scaleX = 0.97f
            panel?.scaleY = 0.97f
            panel?.animate()
                ?.alpha(1f)
                ?.scaleX(1f)
                ?.scaleY(1f)
                ?.setDuration(if (prefs.getBoolean("reduced_motion", false)) 90 else 190)
                ?.start()
        } else {
            panel?.alpha = 1f
        }
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
                addSection(list, "ALL MODULES", "Registered helpers with persistent state and per-module settings.")
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
                addSection(list, "NETWORK", "Bedrock-aware RakNet/UDP diagnostics with a read-only server probe.")
                addModuleGrid(list, ModuleRegistry.all.filter { it.category == "Network" })
                addServerProbe(list)
                addActionRow(list, "Current diagnostics", NativeBridge.nativeDiagnostics())
            }
            "Settings" -> {
                addSection(list, "CLIENT SETTINGS", "Preferences persist locally on this device.")
                addSettingRow(list, "Compact menu", "Reduce the ClickGUI footprint", "compact_menu")
                addSettingRow(list, "Remember modules", "Keep module state between sessions", "remember_modules")
                addSettingRow(list, "Reduced motion", "Shorten UI transitions", "reduced_motion")
                addActionRow(list, "Copy module config", "Copy enabled modules, settings and client preferences as JSON.") {
                    val json = ModuleManager.exportJson(prefs)
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Riplow module config", json))
                    Toast.makeText(this, "Module config copied", Toast.LENGTH_SHORT).show()
                }
                addActionRow(list, "Import module config", "Import a Riplow JSON configuration from the clipboard.") {
                    val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    val raw = clipboard.primaryClip
                        ?.takeIf { it.itemCount > 0 }
                        ?.getItemAt(0)
                        ?.coerceToText(this)
                        ?.toString()
                        ?: ""
                    if (raw.isBlank()) {
                        Toast.makeText(this, "Clipboard has no configuration", Toast.LENGTH_SHORT).show()
                    } else if (ModuleManager.importJson(prefs, raw)) {
                        runtimeHost.sync(prefs)
                        expandedModuleId = null
                        Toast.makeText(this, "Module config imported", Toast.LENGTH_SHORT).show()
                        rebuildPanel()
                    } else {
                        Toast.makeText(this, "Invalid or unsupported Riplow config", Toast.LENGTH_SHORT).show()
                    }
                }
                addActionRow(list, "Reset local settings", "Clear Riplow preferences") {
                    ModuleManager.reset(prefs)
                    runtimeHost.sync(prefs)
                    expandedModuleId = null
                    Toast.makeText(this, "Riplow settings and modules reset", Toast.LENGTH_SHORT).show()
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
        val availability = ModuleCapabilities.availability(module.id)
        val toggleable = ModuleCapabilities.canToggle(module.id)
        val enabled = ModuleManager.isEnabled(prefs, module.id)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = backgroundShape(Color.rgb(24, 25, 29), 16)
        }

        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(10), dp(8))

            if (toggleable) {
                setOnClickListener {
                    ModuleManager.toggle(prefs, module.id)
                    runtimeHost.sync(prefs)
                    rebuildPanel()
                }
            } else {
                setOnClickListener {
                    Toast.makeText(
                        this@ClientOverlayService,
                        ModuleCapabilities.detail(module.id),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        val textBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

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

        val runtimeStatus = ModuleRuntime.status(this@ClientOverlayService, prefs, module)
        val capabilityDetail = ModuleCapabilities.detail(module.id)
        textBox.addView(TextView(this).apply {
            text = if (availability == ModuleAvailability.READY) {
                runtimeStatus
            } else {
                capabilityDetail
            }
            textSize = 8f
            setTextColor(
                when (availability) {
                    ModuleAvailability.READY -> Color.rgb(112, 187, 164)
                    ModuleAvailability.NATIVE_TELEMETRY -> Color.rgb(175, 178, 186)
                    ModuleAvailability.GAME_BRIDGE_REQUIRED -> Color.rgb(214, 166, 92)
                    ModuleAvailability.PLANNED -> Color.rgb(142, 145, 154)
                }
            )
            setPadding(0, dp(3), 0, 0)
        })

        row.addView(textBox, LinearLayout.LayoutParams(0, dp(60), 1f))

        if (module.settings.isNotEmpty()) {
            row.addView(TextView(this).apply {
                text = if (expandedModuleId == module.id) "▲" else "CFG"
                textSize = 8f
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(185, 188, 196))
                background = backgroundShape(Color.rgb(37, 39, 45), 10)
                setPadding(dp(8), 0, dp(8), 0)
                contentDescription = "Configure " + module.title
                setOnClickListener {
                    expandedModuleId =
                        if (expandedModuleId == module.id) null else module.id
                    rebuildPanel()
                }
            }, LinearLayout.LayoutParams(-2, dp(30)).apply {
                marginEnd = dp(7)
            })
        }

        row.addView(TextView(this).apply {
            text = ModuleCapabilities.label(module.id, enabled)
            textSize = 8f
            gravity = Gravity.CENTER
            setTextColor(
                when (availability) {
                    ModuleAvailability.READY -> {
                        if (enabled) Color.BLACK else Color.LTGRAY
                    }
                    ModuleAvailability.NATIVE_TELEMETRY -> Color.rgb(230, 230, 234)
                    ModuleAvailability.GAME_BRIDGE_REQUIRED -> Color.rgb(14, 14, 16)
                    ModuleAvailability.PLANNED -> Color.rgb(190, 192, 199)
                }
            )
            background = backgroundShape(
                when (availability) {
                    ModuleAvailability.READY ->
                        if (enabled) Color.rgb(220, 223, 226) else Color.rgb(42, 44, 49)
                    ModuleAvailability.NATIVE_TELEMETRY -> Color.rgb(57, 59, 66)
                    ModuleAvailability.GAME_BRIDGE_REQUIRED -> Color.rgb(214, 166, 92)
                    ModuleAvailability.PLANNED -> Color.rgb(37, 39, 45)
                },
                11
            )
            setPadding(dp(9), 0, dp(9), 0)
        }, LinearLayout.LayoutParams(-2, dp(30)))

        card.addView(row, LinearLayout.LayoutParams(-1, dp(80)))

        if (expandedModuleId == module.id && module.settings.isNotEmpty()) {
            module.settings.forEach { setting ->
                val current = ModuleManager.setting(prefs, module.id, setting)
                card.addView(LinearLayout(this).apply {
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(12), dp(6), dp(12), dp(6))
                    setOnClickListener {
                        ModuleManager.cycleSetting(prefs, module.id, setting)
                        rebuildPanel()
                    }

                    addView(TextView(this@ClientOverlayService).apply {
                        text = setting.title + "\n" + setting.description
                        textSize = 9f
                        setTextColor(Color.rgb(150, 153, 161))
                    }, LinearLayout.LayoutParams(0, dp(42), 1f))

                    addView(TextView(this@ClientOverlayService).apply {
                        text = current
                        textSize = 9f
                        gravity = Gravity.CENTER
                        setTextColor(Color.WHITE)
                        background = backgroundShape(Color.rgb(37, 39, 45), 9)
                        setPadding(dp(8), 0, dp(8), 0)
                    }, LinearLayout.LayoutParams(-2, dp(28)))
                }, LinearLayout.LayoutParams(-1, dp(52)))
            }
        }

        parent.addView(card, LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT))
    }

    private fun addSettingRow(parent: LinearLayout, title: String, detail: String, key: String) {
        val value = prefs.getBoolean(key, key == "remember_modules")
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(8), dp(10), dp(8))
            background = backgroundShape(Color.rgb(24, 25, 29), 16)
            setOnClickListener {
                val next = !value
                if (key == "remember_modules") {
                    ModuleManager.setRememberModules(prefs, next)
                } else {
                    prefs.edit().putBoolean(key, next).apply()
                }
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

    private fun addServerProbe(parent: LinearLayout) {
        val host = EditText(this).apply {
            hint = "Server address"
            setHintTextColor(Color.rgb(110, 113, 120))
            setTextColor(Color.WHITE)
            textSize = 11f
            singleLine = true
            inputType = InputType.TYPE_CLASS_TEXT
            background = backgroundShape(Color.rgb(24, 25, 29), 12)
            setPadding(dp(10), 0, dp(10), 0)
            setText(prefs.getString("server_probe_host", "") ?: "")
        }
        val port = EditText(this).apply {
            hint = BedrockServerProbe.DEFAULT_PORT.toString()
            setHintTextColor(Color.rgb(110, 113, 120))
            setTextColor(Color.WHITE)
            textSize = 11f
            singleLine = true
            inputType = InputType.TYPE_CLASS_NUMBER
            background = backgroundShape(Color.rgb(24, 25, 29), 12)
            setPadding(dp(10), 0, dp(10), 0)
            setText(prefs.getString("server_probe_port", BedrockServerProbe.DEFAULT_PORT.toString()))
        }
        val result = TextView(this).apply {
            text = "No probe yet"
            textSize = 9f
            setTextColor(Color.rgb(150, 153, 161))
            setPadding(dp(2), dp(7), dp(2), 0)
        }
        val probeButton = Button(this).apply {
            text = "Probe"
            isAllCaps = false
            textSize = 10f
            setTextColor(Color.LTGRAY)
            background = backgroundShape(Color.rgb(37, 39, 45), 12)
        }
        val inputs = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        inputs.addView(host, LinearLayout.LayoutParams(0, dp(44), 1f))
        inputs.addView(port, LinearLayout.LayoutParams(dp(96), dp(44)).apply { marginStart = dp(7) })
        inputs.addView(probeButton, LinearLayout.LayoutParams(dp(84), dp(44)).apply { marginStart = dp(7) })
        parent.addView(inputs, LinearLayout.LayoutParams(-1, dp(44)).apply { bottomMargin = dp(2) })
        parent.addView(result, LinearLayout.LayoutParams(-1, dp(46)))

        probeButton.setOnClickListener {
            val hostValue = host.text?.toString()?.trim().orEmpty()
            val portValue = port.text?.toString()?.toIntOrNull() ?: BedrockServerProbe.DEFAULT_PORT
            prefs.edit()
                .putString("server_probe_host", hostValue)
                .putString("server_probe_port", portValue.toString())
                .apply()
            probeButton.isEnabled = false
            result.text = "Probing RakNet UDP…"
            Thread {
                val probe = BedrockServerProbe.probe(hostValue, portValue)
                runOnUiThread {
                    probeButton.isEnabled = true
                    result.text = if (!probe.reachable || probe.info == null) {
                        "Unavailable • " + (probe.error ?: "No response")
                    } else {
                        val info = probe.info
                        val clientVersion = BedrockVersionParser.parse(
                            MinecraftCompatibility.detect(this).versionName
                        )
                        val serverVersion = BedrockVersionParser.parse(info.versionName)
                        val versionNote = when {
                            clientVersion == null || serverVersion == null -> "version unknown"
                            clientVersion == serverVersion -> "version matches installed client"
                            clientVersion.major == serverVersion.major && clientVersion.minor == serverVersion.minor ->
                                "patch/build differs"
                            else -> "version mismatch"
                        }
                        info.motd + " • " + (info.players ?: 0) + "/" + (info.maxPlayers ?: 0) +
                            " • protocol " + (info.protocol?.toString() ?: "?") +
                            " • " + versionNote + " • " + probe.elapsedMs + " ms"
                    }
                }
            }.start()
        }
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
            try {
                if (it.isAttachedToWindow) windowManager.removeView(it)
            } catch (_: Throwable) {
            }
        }
        panel = null
        if (android.provider.Settings.canDrawOverlays(this)) {
            showPanel(animateOpen = false)
        }
    }

    private fun backgroundShape(color: Int, radius: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius.toFloat()
        setStroke(dp(1), Color.rgb(43, 45, 51))
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        runtimeHost.destroy()
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
