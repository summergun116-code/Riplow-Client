package com.riplow.client

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.riplow.client.modules.MinecraftCompatibility
import com.riplow.client.network.BedrockRelayService
import com.riplow.client.network.RelayRuntime
import com.riplow.client.modules.ModuleAvailability
import com.riplow.client.modules.ModuleCapabilities
import com.riplow.client.modules.ModuleDefinition
import com.riplow.client.modules.ModuleManager
import com.riplow.client.modules.ModuleRegistry
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private enum class Page { HOME, MODULES, PACKS, PERFORMANCE, NETWORK, SETTINGS }

    private val handler = Handler(Looper.getMainLooper())
    private val prefs by lazy { getSharedPreferences("riplow_settings", MODE_PRIVATE) }
    private lateinit var pageTitle: TextView
    private lateinit var pageSubtitle: TextView
    private lateinit var pageContainer: LinearLayout
    private lateinit var status: TextView
    private lateinit var moduleCount: TextView
    private var minecraftVersion: TextView? = null
    private var minecraftState: TextView? = null
    private var launchRequested = false
    private var launchOverlay: View? = null
    private var loadingDetail: TextView? = null
    private var telemetryView: TextView? = null
    private val telemetryHandler = Handler(Looper.getMainLooper())
    private val telemetryRunnable = object : Runnable {
        override fun run() {
            val view = telemetryView ?: return
            view.text = runtimeTelemetry()
            telemetryHandler.postDelayed(this, 1000L)
        }
    }

    companion object {
        private const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe"
        private const val REQUEST_EXPORT_PROFILE = 4101
        private const val REQUEST_IMPORT_PROFILE = 4102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pageTitle = findViewById(R.id.page_title)
        pageSubtitle = findViewById(R.id.page_subtitle)
        pageContainer = findViewById(R.id.page_container)
        status = findViewById(R.id.status)
        moduleCount = findViewById(R.id.module_count)

        findViewById<TextView>(R.id.version_chip).text = "Riplow " + BuildConfig.VERSION_NAME
        moduleCount.text = ModuleRegistry.all.size.toString() + " Minecraft modules"

        bindNavigation()
        showPage(Page.HOME)
        refreshMinecraftState()
    }

    override fun onResume() {
        super.onResume()
        refreshMinecraftState()
    }

    private fun bindNavigation() {
        val mapping = listOf(
            R.id.nav_home to Page.HOME,
            R.id.nav_modules to Page.MODULES,
            R.id.nav_packs to Page.PACKS,
            R.id.nav_performance to Page.PERFORMANCE,
            R.id.nav_network to Page.NETWORK,
            R.id.nav_settings to Page.SETTINGS
        )
        mapping.forEach { (id, page) ->
            findViewById<Button>(id).setOnClickListener { showPage(page) }
        }
    }

    private fun showPage(page: Page) {
        telemetryHandler.removeCallbacks(telemetryRunnable)
        telemetryView = null
        pageTitle.text = when (page) {
            Page.HOME -> "Home"
            Page.MODULES -> "Modules"
            Page.PACKS -> "Packs"
            Page.PERFORMANCE -> "Performance"
            Page.NETWORK -> "Network"
            Page.SETTINGS -> "Settings"
        }
        pageSubtitle.text = when (page) {
            Page.HOME -> "Launch Minecraft and manage your client tools from one place."
            Page.MODULES -> "Minecraft modules and utilities."
            Page.PACKS -> "Resource packs, add-ons, worlds and profiles."
            Page.PERFORMANCE -> "Device-side tuning for Riplow, with honest Minecraft limits."
            Page.NETWORK -> "Bedrock connection visibility: latency, jitter and session diagnostics."
            Page.SETTINGS -> "Simple local preferences. No background client service."
        }

        listOf(
            Page.HOME to R.id.nav_home,
            Page.MODULES to R.id.nav_modules,
            Page.PACKS to R.id.nav_packs,
            Page.PERFORMANCE to R.id.nav_performance,
            Page.NETWORK to R.id.nav_network,
            Page.SETTINGS to R.id.nav_settings
        ).forEach { (item, id) ->
            findViewById<Button>(id).alpha = if (item == page) 1f else 0.62f
        }

        pageContainer.removeAllViews()
        minecraftVersion = null
        minecraftState = null
        when (page) {
            Page.HOME -> buildHomePage()
            Page.MODULES -> buildModulesPage()
            Page.PACKS -> buildPacksPage()
            Page.PERFORMANCE -> buildPerformancePage()
            Page.NETWORK -> buildNetworkPage()
            Page.SETTINGS -> buildSettingsPage()
        }
        (pageContainer.parent as? ScrollView)?.scrollTo(0, 0)
    }

    private fun buildHomePage() {
        val minecraftCard = card()
        addEyebrow(minecraftCard, "MINECRAFT")
        minecraftVersion = addTitle(minecraftCard, null, "Minecraft")
        minecraftState = addBody(minecraftCard, null, "Checking installation…")
        val play = Button(this).apply {
            text = "PLAY MINECRAFT"
            isAllCaps = false
            textSize = 13f
            setOnClickListener { launchMinecraft() }
        }
        minecraftCard.addView(play, buttonParams())
        pageContainer.addView(minecraftCard)

        val featureRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        featureRow.addView(infoCard(
            "MODULES",
            ModuleRegistry.all.size.toString() + " focused tools",
            "HUD, visual, performance, network and gameplay modules."
        ), weightParams(1f))
        featureRow.addView(infoCard(
            "MODE",
            "Companion",
            "Riplow launches the Minecraft you already own. It does not inject a built-in game menu."
        ), weightParams(1f))
        featureRow.addView(infoCard(
            "OVERHEAD",
            "App-first",
            "No persistent overlay service, notification loop or always-running client process."
        ), weightParams(1f))
        pageContainer.addView(featureRow, LinearLayout.LayoutParams(-1, -2))

        val core = card()
        addEyebrow(core, "NATIVE CORE")
        addTitle(core, null, "Runtime health")
        addBody(core, null, NativeBridge.loadStatus() + "\nRenderer adapter: " + NativeBridge.renderStatus())
        pageContainer.addView(core)

        val note = card()
        addEyebrow(note, "IMPORTANT")
        addBody(note, null, "The renderer adapter is shipped as a guarded integration layer. The normal companion APK keeps process hooks disabled, so it cannot destabilize the Minecraft app or pretend it has renderer access it does not actually have.")
        pageContainer.addView(note)
    }

    private fun buildModulesPage() {
        val summary = card()
        addEyebrow(summary, "MODULE LIBRARY")
        addTitle(summary, null, ModuleRegistry.all.size.toString() + " focused modules")
        pageContainer.addView(summary)

        ModuleRegistry.all.groupBy { it.category }.forEach { (category, modules) ->
            val label = TextView(this).apply {
                text = category.uppercase(Locale.getDefault())
                textSize = 11f
                setTextColor(getColor(R.color.riplow_muted))
                letterSpacing = 0.08f
                setPadding(dp(2), dp(16), dp(2), dp(8))
            }
            pageContainer.addView(label)

            modules.chunked(2).forEach { pair ->
                val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                pair.forEach { module -> row.addView(moduleCard(module), weightParams(1f)) }
                if (pair.size == 1) row.addView(View(this), weightParams(1f))
                pageContainer.addView(row)
            }
        }
    }

    private fun buildPacksPage() {
        addWorkspace(
            "PACK WORKSPACE",
            "Bedrock pack profiles",
            "Riplow keeps resource packs and behavior packs explicitly Minecraft-focused, not as generic app extensions.",
            "Available",
            "Pack integration"
        )
        addWorkspace(
            "ADD-ONS",
            "Behavior-pack workspace",
            "The app does not silently modify the game. A compatible content bridge or Android file-access flow is required before real import/switch actions are enabled.",
            "Bridge required",
            "Integration state"
        )
        addWorkspace(
            "WORLDS",
            "World profiles and backups",
            "Local organization for Minecraft worlds and backups. No automatic background copying.",
            "Planned",
            "Manual workflow"
        )
    }

    private fun buildPerformancePage() {
        val card = card()
        addEyebrow(card, "PERFORMANCE PROFILE")
        addTitle(card, null, "Choose how light Riplow should run")
        addBody(card, null, "These profiles reduce Riplow's own work. They do not claim to replace Minecraft's renderer or secretly alter the game.")

        val module = ModuleRegistry.all.first { it.id == "performance_profile" }
        val setting = module.settings.first()
        setting.options.forEach { option ->
            val button = Button(this).apply {
                text = option
                isAllCaps = false
                textSize = 12f
                alpha = if (ModuleManager.setting(prefs, module.id, setting) == option) 1f else 0.58f
                setOnClickListener {
                    prefs.edit().putString("module_setting_" + module.id + "_" + setting.id, option).apply()
                    showPage(Page.PERFORMANCE)
                }
            }
            card.addView(button, buttonParams())
        }
        pageContainer.addView(card)

        val telemetry = card()
        addEyebrow(telemetry, "LIVE TELEMETRY")
        addTitle(telemetry, null, "Device runtime")
        telemetryView = addBody(telemetry, null, runtimeTelemetry())
        pageContainer.addView(telemetry)
        telemetryHandler.post(telemetryRunnable)

        addWorkspace("WHAT IT DOES", "Local overhead control",
            "Controls refresh cadence, diagnostics sampling and UI animation cost inside Riplow.",
            "Working", "Riplow-side")
        addWorkspace("WHAT IT DOES NOT DO", "No fake FPS claims",
            "Riplow will not advertise renderer hacks that Android's normal app sandbox cannot actually perform.",
            "Working", "Honest boundary")
    }

    private fun buildNetworkPage() {
        val relay = card()
        addEyebrow(relay, "BEDROCK RELAY")
        addTitle(relay, null, "Local RakNet / UDP relay")

        val host = EditText(this).apply {
            hint = "Server host"
            textSize = 13f
            setSingleLine(true)
            setText(prefs.getString("relay_host", ""))
        }
        relay.addView(host, buttonParams())

        val remotePort = EditText(this).apply {
            hint = "Server port"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            textSize = 13f
            setSingleLine(true)
            setText(prefs.getInt("relay_remote_port", 19132).toString())
        }
        relay.addView(remotePort, buttonParams())

        val localPort = EditText(this).apply {
            hint = "Local relay port"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            textSize = 13f
            setSingleLine(true)
            setText(prefs.getInt("relay_local_port", 19132).toString())
        }
        relay.addView(localPort, buttonParams())

        val start = Button(this).apply {
            text = "START RELAY"
            isAllCaps = false
            setOnClickListener {
                val targetHost = host.text.toString().trim()
                val targetPort = remotePort.text.toString().toIntOrNull() ?: 19132
                val bindPort = localPort.text.toString().toIntOrNull() ?: 19132
                if (BedrockRelayService.start(this@MainActivity, targetHost, targetPort, bindPort)) {
                    prefs.edit()
                        .putString("relay_host", targetHost)
                        .putInt("relay_remote_port", targetPort)
                        .putInt("relay_local_port", bindPort)
                        .apply()
                    status.text = "Relay start requested"
                } else {
                    status.text = "Invalid relay settings"
                }
            }
        }
        relay.addView(start, buttonParams())

        val stop = Button(this).apply {
            text = "STOP RELAY"
            isAllCaps = false
            setOnClickListener {
                BedrockRelayService.stop(this@MainActivity)
                status.text = "Relay stop requested"
            }
        }
        relay.addView(stop, buttonParams())

        val copy = Button(this).apply {
            text = "COPY LOCAL ADDRESS"
            isAllCaps = false
            setOnClickListener {
                val port = localPort.text.toString().toIntOrNull() ?: 19132
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(
                    android.content.ClipData.newPlainText("Riplow relay", "127.0.0.1:$port")
                )
                status.text = "127.0.0.1:$port copied"
            }
        }
        relay.addView(copy, buttonParams())
        pageContainer.addView(relay)

        val diagnostics = card()
        addEyebrow(diagnostics, "RELAY STATUS")
        addTitle(diagnostics, null, "Live transport")
        addBody(diagnostics, null, RelayRuntime.summary())
        pageContainer.addView(diagnostics)

        val native = card()
        addEyebrow(native, "NATIVE TELEMETRY")
        addTitle(native, null, "Transport diagnostics")
        addBody(native, null, NativeBridge.nativeDiagnostics().ifBlank {
            "Native core unavailable"
        })
        pageContainer.addView(native)
    }

    private fun buildSettingsPage() {
        val card = card()
        addEyebrow(card, "LOCAL SETTINGS")
        addTitle(card, null, "Riplow preferences")

        val remember = CheckBox(this).apply {
            text = "Remember enabled modules"
            isChecked = prefs.getBoolean("remember_modules", true)
            setOnCheckedChangeListener { _, checked ->
                ModuleManager.setRememberModules(prefs, checked)
            }
        }
        card.addView(remember)

        val reducedMotion = CheckBox(this).apply {
            text = "Reduce UI motion"
            isChecked = prefs.getBoolean("reduced_motion", false)
            setOnCheckedChangeListener { _, checked ->
                prefs.edit().putBoolean("reduced_motion", checked).apply()
            }
        }
        card.addView(reducedMotion)

        card.addView(Button(this).apply {
            text = "Export Profile"
            isAllCaps = false
            setOnClickListener { exportProfile() }
        }, buttonParams())

        card.addView(Button(this).apply {
            text = "Import Profile"
            isAllCaps = false
            setOnClickListener { importProfile() }
        }, buttonParams())

        card.addView(Button(this).apply {
            text = "Reset Riplow State"
            isAllCaps = false
            setOnClickListener {
                ModuleManager.reset(prefs)
                status.text = "Riplow state reset"
                showPage(Page.SETTINGS)
            }
        }, buttonParams())

        pageContainer.addView(card)
        addWorkspace("CLIENT MODEL", "External launcher",
            "Riplow is not a second Minecraft implementation and does not bundle a fake in-game world. It opens the installed Minecraft package directly.",
            "Enabled", "Architecture")
    }

    private fun moduleCard(module: ModuleDefinition): View {
        val card = card()
        addTitle(card, null, module.title)
        val availability = ModuleCapabilities.availability(module.id)
        val stateText = when (availability) {
            ModuleAvailability.READY -> if (ModuleManager.isEnabled(prefs, module.id)) "ON" else "OFF"
            ModuleAvailability.NATIVE_TELEMETRY -> "TELEMETRY"
            ModuleAvailability.GAME_BRIDGE_REQUIRED -> "BRIDGE REQUIRED"
            ModuleAvailability.PLANNED -> "PLANNED"
        }
        card.addView(TextView(this).apply {
            text = stateText
            textSize = 10f
            setTextColor(getColor(R.color.riplow_secondary))
            setPadding(0, dp(8), 0, dp(4))
        })

        if (availability == ModuleAvailability.READY) {
            card.addView(Button(this).apply {
                text = if (ModuleManager.isEnabled(prefs, module.id)) "Disable" else "Enable"
                isAllCaps = false
                setOnClickListener {
                    ModuleManager.toggle(prefs, module.id)
                    showPage(Page.MODULES)
                }
            }, buttonParams())
        }

        module.settings.forEach { setting ->
            val current = ModuleManager.setting(prefs, module.id, setting)
            card.addView(Button(this).apply {
                text = setting.title + ": " + current
                isAllCaps = false
                textSize = 11f
                setOnClickListener {
                    ModuleManager.cycleSetting(prefs, module.id, setting)
                    showPage(Page.MODULES)
                }
            }, buttonParams())
        }

        return card
    }

    private fun runtimeTelemetry(): String {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val runtime = Runtime.getRuntime()
        val usedAppMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)
        val maxAppMb = runtime.maxMemory() / (1024L * 1024L)
        val availableSystemMb = memoryInfo.availMem / (1024L * 1024L)
        val totalSystemMb = memoryInfo.totalMem / (1024L * 1024L)

        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        val battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charging = batteryManager.isCharging

        val uptimeSeconds = android.os.SystemClock.elapsedRealtime() / 1000L
        val hours = uptimeSeconds / 3600L
        val minutes = (uptimeSeconds % 3600L) / 60L
        val seconds = uptimeSeconds % 60L

        return buildString {
            append("Riplow session: %02d:%02d:%02d".format(hours, minutes, seconds))
            append("\nApp memory: " + usedAppMb + " MB / " + maxAppMb + " MB")
            append("\nSystem memory available: " + availableSystemMb + " MB / " + totalSystemMb + " MB")
            append("\nBattery: " + (if (battery >= 0) battery.toString() + "%" else "unknown"))
            if (charging) append(" • charging")
            append("\nMinecraft: ")
            append(if (MinecraftCompatibility.detect(this@MainActivity).installed) "installed" else "not detected")
            append("\nNative core: " + if (NativeBridge.isAvailable()) "available" else "unavailable")
        }
    }

    private fun exportProfile() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
            putExtra(Intent.EXTRA_TITLE, "riplow-profile.json")
        }
        startActivityForResult(intent, REQUEST_EXPORT_PROFILE)
    }

    private fun importProfile() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/json"
        }
        startActivityForResult(intent, REQUEST_IMPORT_PROFILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK) return
        val uri = data?.data ?: return

        try {
            when (requestCode) {
                REQUEST_EXPORT_PROFILE -> {
                    contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(ModuleManager.exportJson(prefs).toByteArray(Charsets.UTF_8))
                    } ?: throw IllegalStateException("Could not open destination")
                    status.text = "Profile exported"
                }
                REQUEST_IMPORT_PROFILE -> {
                    val raw = contentResolver.openInputStream(uri)?.use {
                        it.readBytes().toString(Charsets.UTF_8)
                    } ?: throw IllegalStateException("Could not read profile")
                    status.text = if (ModuleManager.importJson(prefs, raw)) {
                        "Profile imported"
                    } else {
                        "Profile rejected"
                    }
                    showPage(Page.SETTINGS)
                }
            }
        } catch (_: Throwable) {
            status.text = "Profile operation failed"
        }
    }

    private fun addWorkspace(title: String, name: String, body: String, state: String, stateLabel: String) {
        val card = card()
        addEyebrow(card, title)
        addTitle(card, null, name)
        addBody(card, null, body)
        card.addView(TextView(this).apply {
            text = stateLabel + "  •  " + state
            textSize = 10f
            setTextColor(getColor(R.color.riplow_secondary))
            setPadding(0, dp(8), 0, 0)
        })
        pageContainer.addView(card)
    }

    private fun infoCard(eyebrow: String, title: String, body: String): View {
        val card = card()
        addEyebrow(card, eyebrow)
        addTitle(card, null, title)
        addBody(card, null, body)
        return card
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = getDrawable(R.drawable.bg_surface)
    }.also {
        it.layoutParams = LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(dp(5), dp(5), dp(5), dp(5))
        }
    }

    private fun addEyebrow(parent: LinearLayout, text: String) {
        parent.addView(TextView(this).apply {
            this.text = text
            textSize = 9f
            setTextColor(getColor(R.color.riplow_muted))
            letterSpacing = 0.08f
        })
    }

    private fun addTitle(parent: LinearLayout, existing: TextView?, text: String): TextView {
        val view = existing ?: TextView(this)
        view.text = text
        view.textSize = 18f
        view.setTextColor(getColor(R.color.riplow_foreground))
        view.setTypeface(null, android.graphics.Typeface.BOLD)
        view.setPadding(0, dp(6), 0, dp(4))
        if (existing == null) parent.addView(view)
        return view
    }

    private fun addBody(parent: LinearLayout, existing: TextView?, text: String): TextView {
        val view = existing ?: TextView(this)
        view.text = text
        view.textSize = 11f
        view.setTextColor(getColor(R.color.riplow_secondary))
        view.setPadding(0, 0, 0, dp(4))
        if (existing == null) parent.addView(view)
        return view
    }

    private fun buttonParams() = LinearLayout.LayoutParams(-1, dp(44)).apply {
        setMargins(0, dp(8), 0, 0)
    }

    private fun weightParams(weight: Float) = LinearLayout.LayoutParams(0, -2, weight).apply {
        setMargins(dp(4), 0, dp(4), 0)
    }

    private fun refreshMinecraftState() {
        val install = MinecraftCompatibility.detect(this)
        if (install.installed) {
            minecraftVersion?.text = install.versionName
            minecraftState?.text = "Installed • version code " + install.versionCode
            status.text = "Minecraft ready"
        } else {
            minecraftVersion?.text = "Not detected"
            minecraftState?.text = "Install Minecraft Bedrock to use the launcher."
            status.text = "Minecraft not detected"
        }
    }

    override fun onDestroy() {
        telemetryHandler.removeCallbacks(telemetryRunnable)
        telemetryView = null
        super.onDestroy()
    }

    private fun launchMinecraft() {
        if (launchRequested) return
        val install = MinecraftCompatibility.detect(this)
        if (!install.installed) {
            refreshMinecraftState()
            return
        }

        launchRequested = true
        showLaunchOverlay(install.versionName)

        handler.postDelayed({
            if (!launchRequested) return@postDelayed
            try {
                val intent = packageManager.getLaunchIntentForPackage(MINECRAFT_PACKAGE)
                    ?: throw ActivityNotFoundException("Minecraft launcher activity not found")
                startActivity(intent)
                loadingDetail?.text = "Minecraft opened"
                status.text = "Minecraft launch requested"
            } catch (_: Exception) {
                launchRequested = false
                loadingDetail?.text = "Android could not open Minecraft"
                status.text = "Minecraft launch failed"
                handler.postDelayed({ hideLaunchOverlay() }, 1100)
            }
        }, 180)
    }

    private fun showLaunchOverlay(version: String) {
        val overlay = findViewById<View>(R.id.launch_overlay)
        launchOverlay = overlay
        loadingDetail = findViewById(R.id.loading_detail)
        loadingDetail?.text = "Minecraft " + version + " • opening your installed game"
        overlay.visibility = View.VISIBLE
        overlay.alpha = 0f
        overlay.animate().alpha(1f).setDuration(160).start()
    }

    private fun hideLaunchOverlay() {
        launchRequested = false
        launchOverlay?.animate()?.alpha(0f)?.setDuration(140)?.withEndAction {
            launchOverlay?.visibility = View.GONE
        }?.start()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}