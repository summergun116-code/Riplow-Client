package com.riplow.client

import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import com.riplow.client.modules.ModuleRuntime
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private enum class Page { HOME, MODULES, PACKS, PERFORMANCE, NETWORK, SETTINGS, REPLAY, ACCOUNTS }

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
    private var relayStatusView: TextView? = null
    private val telemetryHandler = Handler(Looper.getMainLooper())
    private val relayHandler = Handler(Looper.getMainLooper())
    private val telemetryRunnable = object : Runnable {
        override fun run() {
            val view = telemetryView ?: return
            view.text = runtimeTelemetry()
            telemetryHandler.postDelayed(this, 1000L)
        }
    }

    private val relayRunnable = object : Runnable {
        override fun run() {
            val view = relayStatusView ?: return
            view.text = RelayRuntime.summary()
            relayHandler.postDelayed(this, 1000L)
        }
    }

    companion object {
        private const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe"
        private const val REQUEST_EXPORT_PROFILE = 4101
        private const val REQUEST_IMPORT_PROFILE = 4102
        private const val REQUEST_IMPORT_MINECRAFT_CONTENT = 4103
        private const val REQUEST_OPEN_CLIP = 4104
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
            R.id.nav_performance to Page.PERFORMANCE,
            R.id.nav_replay to Page.REPLAY,
            R.id.nav_accounts to Page.ACCOUNTS,
            R.id.nav_packs to Page.PACKS,
            R.id.nav_modules to Page.MODULES,
            R.id.nav_network to Page.NETWORK,
            R.id.nav_settings to Page.SETTINGS
        )
        mapping.forEach { (id, page) ->
            findViewById<Button>(id).setOnClickListener { showPage(page) }
        }
    }

    private fun showPage(page: Page) {
        telemetryHandler.removeCallbacks(telemetryRunnable)
        relayHandler.removeCallbacks(relayRunnable)
        telemetryView = null
        relayStatusView = null
        pageTitle.text = when (page) {
            Page.HOME -> "Home"
            Page.MODULES -> "Modules"
            Page.PACKS -> "Packs"
            Page.PERFORMANCE -> "Performance"
            Page.NETWORK -> "Network"
            Page.SETTINGS -> "Settings"
            Page.REPLAY -> "Replay"
            Page.ACCOUNTS -> "Accounts"
        }
        pageSubtitle.text = when (page) {
            Page.HOME -> "Launch Minecraft and manage your client tools from one place."
            Page.MODULES -> "Minecraft modules and utilities."
            Page.PACKS -> "Resource packs, add-ons, worlds and profiles."
            Page.PERFORMANCE -> "Device-side tuning for Riplow, with honest Minecraft limits."
            Page.NETWORK -> "Ping, jitter and connection diagnostics."
            Page.SETTINGS -> "Appearance, profiles and local preferences."
            Page.REPLAY -> "Record, trim and organize your Minecraft moments."
            Page.ACCOUNTS -> "Account switching with a clear view of what is supported."
        }

        listOf(
            Page.HOME to R.id.nav_home,
            Page.PERFORMANCE to R.id.nav_performance,
            Page.REPLAY to R.id.nav_replay,
            Page.ACCOUNTS to R.id.nav_accounts,
            Page.PACKS to R.id.nav_packs,
            Page.MODULES to R.id.nav_modules,
            Page.NETWORK to R.id.nav_network,
            Page.SETTINGS to R.id.nav_settings
        ).forEach { (item, id) ->
            findViewById<Button>(id).apply {
                val selected = item == page
                alpha = if (selected) 1f else 0.82f
                setBackgroundResource(if (selected) R.drawable.bg_nav_active else R.drawable.bg_nav_idle)
                setTextColor(getColor(if (selected) R.color.riplow_foreground else R.color.riplow_secondary))
            }
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
            Page.REPLAY -> buildReplayPage()
            Page.ACCOUNTS -> buildAccountsPage()
        }
        (pageContainer.parent as? ScrollView)?.scrollTo(0, 0)
    }

    private fun buildHomePage() {
        val hero = card()
        addEyebrow(hero, "MINECRAFT BEDROCK")
        addTitle(hero, null, "Your Minecraft, refined.")
        minecraftVersion = addBody(hero, null, "Checking Minecraft…")
        minecraftState = addBody(hero, null, "Launch your game and keep your tools in one place.")
        val launchRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        launchRow.addView(Button(this).apply {
            text = "Launch Minecraft"
            isAllCaps = false
            setOnClickListener { launchMinecraft() }
        }, weightParams(1f))
        launchRow.addView(Button(this).apply {
            text = "Open modules"
            isAllCaps = false
            setOnClickListener { showPage(Page.MODULES) }
        }, weightParams(1f))
        hero.addView(launchRow, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, dp(12), 0, 0)
        })
        pageContainer.addView(hero)

        val performanceRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        performanceRow.addView(quickCard(
            "FPS",
            "Performance",
            "Device stats and lightweight profiles.",
            Page.PERFORMANCE
        ), weightParams(1f))
        performanceRow.addView(quickCard(
            "REPLAY",
            "Replay Studio",
            "Recording and clip tools.",
            Page.REPLAY
        ), weightParams(1f))
        pageContainer.addView(performanceRow, LinearLayout.LayoutParams(-1, -2))

        val toolsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        toolsRow.addView(quickCard(
            "ACCOUNTS",
            "Quick switch",
            "Account linking is not active yet.",
            Page.ACCOUNTS
        ), weightParams(1f))
        toolsRow.addView(quickCard(
            "PACKS",
            "Pack manager",
            "Import .mcpack, .mcaddon and .mcworld files.",
            Page.PACKS
        ), weightParams(1f))
        pageContainer.addView(toolsRow, LinearLayout.LayoutParams(-1, -2))

        val runtime = card()
        addEyebrow(runtime, "DEVICE STATUS")
        addTitle(runtime, null, "Live overview")
        telemetryView = addBody(runtime, null, runtimeTelemetry())
        pageContainer.addView(runtime)
        telemetryHandler.post(telemetryRunnable)

        val note = card()
        addEyebrow(note, "BUILT FOR BEDROCK")
        addTitle(note, null, "Real features, honest status")
        addBody(note, null, "Riplow can launch installed Minecraft, manage local profiles, import content and inspect device/network data. Features such as renderer-level entity culling and 3D replay need a compatible Bedrock integration; they will not be shown as working before they are.")
        pageContainer.addView(note)
    }

    private fun buildReplayPage() {
        val hero = card()
        addEyebrow(hero, "REPLAY STUDIO")
        addTitle(hero, null, "Keep the moments worth saving.")
        addBody(hero, null, "The first version is designed around screen recording, clip trimming, bookmarks and export. Riplow will request Android's screen-capture permission when recording is implemented.")
        pageContainer.addView(hero)

        val record = card()
        addEyebrow(record, "RECORDING")
        addTitle(record, null, "Screen capture")
        addBody(record, null, "Next up: start and stop a recording, choose quality, and save MP4 clips locally. No capture is running in this build.")
        record.addView(TextView(this).apply {
            text = "RECORDING  •  PLANNED"
            textSize = 10f
            setTextColor(getColor(R.color.riplow_secondary))
            setPadding(0, dp(8), 0, 0)
        })
        record.addView(Button(this).apply {
            text = "Open saved clip"
            isAllCaps = false
            setOnClickListener { chooseSavedClip() }
        }, buttonParams())
        pageContainer.addView(record)
        val timeline = card()
        addEyebrow(timeline, "INSTANT REPLAY")
        addTitle(timeline, null, "Save the last 30–120 seconds")
        addBody(timeline, null, "A rolling clip buffer, bookmarks and fast export are planned. True 3D camera playback needs recorded Bedrock game state and is a separate integration.")
        pageContainer.addView(timeline)
    }

    private fun buildAccountsPage() {
        val hero = card()
        addEyebrow(hero, "ACCOUNT MANAGER")
        addTitle(hero, null, "Switch without the long detour.")
        addBody(hero, null, "Riplow's account manager will use the official Microsoft sign-in flow. This build cannot yet change the account active inside Minecraft, so it will not fake a successful account swap.")
        hero.addView(Button(this).apply {
            text = "Open Minecraft"
            isAllCaps = false
            setOnClickListener { launchMinecraft() }
        }, buttonParams())
        pageContainer.addView(hero)

        val account = card()
        addEyebrow(account, "CURRENT STATUS")
        addTitle(account, null, "Minecraft account")
        addBody(account, null, "Managed by Minecraft • account linking not connected")
        pageContainer.addView(account)

        val next = card()
        addEyebrow(next, "PLANNED")
        addTitle(next, null, "Saved accounts")
        addBody(next, null, "The target flow is simple: add an account through Microsoft, see the signed-in profiles, tap one, then launch Minecraft if the supported game sign-in flow allows it. Tokens will not be copied between apps.")
        pageContainer.addView(next)
    }

    private var moduleSearchQuery = ""
    private var moduleCategory = "All"
    private var moduleDisplayLimit = 24

    private fun buildModulesPage() {
        val summary = card()
        addEyebrow(summary, "RIPLOW MODULES")
        addTitle(summary, null, "Make it yours.")
        addBody(summary, null, "Find tools fast. Only modules that really work can be switched on.")
        val search = EditText(this).apply {
            hint = "Search modules..."
            singleLine = true
            textSize = 13f
            setTextColor(getColor(R.color.riplow_foreground))
            setHintTextColor(getColor(R.color.riplow_muted))
            setPadding(dp(13), 0, dp(13), 0)
            background = getDrawable(R.drawable.bg_chip)
            setText(moduleSearchQuery)
        }
        summary.addView(search, LinearLayout.LayoutParams(-1, dp(44)).apply {
            setMargins(0, dp(10), 0, dp(5))
        })
        pageContainer.addView(summary)

        val categories = listOf(
            "All", "Performance", "Optimization", "HUD", "Visual",
            "Network", "Minecraft", "Client", "World", "Motion", "Combat", "Misc"
        )
        val categoryScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            setPadding(dp(2), dp(2), dp(2), dp(6))
        }
        val categoryRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        categoryScroll.addView(categoryRow)
        pageContainer.addView(categoryScroll)

        val count = TextView(this).apply {
            textSize = 10f
            setTextColor(getColor(R.color.riplow_muted))
            setPadding(dp(4), dp(4), dp(4), dp(8))
        }
        pageContainer.addView(count)

        val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        pageContainer.addView(listContainer)

        fun renderCategories() {
            categoryRow.removeAllViews()
            categories.forEach { category ->
                val selected = category == moduleCategory
                categoryRow.addView(Button(this).apply {
                    text = category
                    isAllCaps = false
                    textSize = 10f
                    minHeight = 0
                    minWidth = 0
                    setPadding(dp(13), 0, dp(13), 0)
                    setTextColor(getColor(if (selected) R.color.riplow_foreground else R.color.riplow_secondary))
                    setBackgroundResource(if (selected) R.drawable.bg_nav_active else R.drawable.bg_nav_idle)
                    setOnClickListener {
                        moduleCategory = category
                        moduleDisplayLimit = 24
                        renderCategories()
                        renderModules()
                    }
                }, LinearLayout.LayoutParams(-2, dp(34)).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                })
            }
        }

        fun renderModules() {
            listContainer.removeAllViews()
            val query = moduleSearchQuery.trim().lowercase(Locale.getDefault())
            val filtered = ModuleRegistry.all.filter { module ->
                (moduleCategory == "All" || module.category.equals(moduleCategory, true)) &&
                    (query.isEmpty() || module.title.lowercase(Locale.getDefault()).contains(query) ||
                        module.category.lowercase(Locale.getDefault()).contains(query) ||
                        module.id.replace('_', ' ').lowercase(Locale.getDefault()).contains(query))
            }
            count.text = filtered.size.toString() + " modules  •  " +
                filtered.count { ModuleCapabilities.availability(it.id) == ModuleAvailability.READY }.toString() +
                " ready"

            if (filtered.isEmpty()) {
                val empty = card()
                addTitle(empty, null, "No modules found")
                addBody(empty, null, "Try another name or category.")
                listContainer.addView(empty)
                return
            }

            val visibleModules = filtered.take(moduleDisplayLimit)
            visibleModules.groupBy { it.category }.forEach { (category, modules) ->
                if (moduleCategory == "All") {
                    val label = TextView(this).apply {
                        text = category.uppercase(Locale.getDefault())
                        textSize = 10f
                        setTextColor(getColor(R.color.riplow_muted))
                        letterSpacing = 0.1f
                        setPadding(dp(5), dp(12), dp(5), dp(5))
                    }
                    listContainer.addView(label)
                }
                modules.chunked(2).forEach { pair ->
                    val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
                    pair.forEach { module -> row.addView(moduleCard(module), weightParams(1f)) }
                    if (pair.size == 1) row.addView(View(this), weightParams(1f))
                    listContainer.addView(row)
                }
            }

            if (filtered.size > visibleModules.size) {
                listContainer.addView(Button(this).apply {
                    text = "Show more  •  " + (filtered.size - visibleModules.size).toString() + " left"
                    isAllCaps = false
                    setOnClickListener {
                        moduleDisplayLimit += 24
                        renderModules()
                    }
                }, buttonParams())
            }
        }

        search.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                moduleSearchQuery = s?.toString().orEmpty()
                moduleDisplayLimit = 24
                renderModules()
            }
            override fun afterTextChanged(s: android.text.Editable?) = Unit
        })

        renderCategories()
        renderModules()
    }

    private fun buildPacksPage() {
        val card = card()
        addEyebrow(card, "BEDROCK CONTENT")
        addTitle(card, null, "Import Minecraft content")
        addBody(card, null, "Pick a .mcpack, .mcaddon or .mcworld file and hand it to your installed Minecraft app.")

        card.addView(Button(this).apply {
            text = "IMPORT PACK / ADD-ON"
            isAllCaps = false
            setOnClickListener { pickMinecraftContent() }
        }, buttonParams())

        card.addView(Button(this).apply {
            text = "IMPORT WORLD"
            isAllCaps = false
            setOnClickListener { pickMinecraftContent() }
        }, buttonParams())

        pageContainer.addView(card)

        val profiles = card()
        addEyebrow(profiles, "CONTENT PROFILES")
        addTitle(profiles, null, "Local organization")
        addBody(profiles, null, "Use the profile system in Settings to save Riplow module configuration. Minecraft content is imported through Android's document flow so the game remains the owner of its data.")
        pageContainer.addView(profiles)
    }

    private fun pickMinecraftContent() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        startActivityForResult(intent, REQUEST_IMPORT_MINECRAFT_CONTENT)
    }

    private fun chooseSavedClip() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
        }
        startActivityForResult(intent, REQUEST_OPEN_CLIP)
    }

    private fun openSavedClip(uri: Uri) {
        val mime = contentResolver.getType(uri) ?: "video/mp4"
        val viewer = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(Intent.createChooser(viewer, "Open replay clip"))
            status.text = "Opening saved clip"
        } catch (_: ActivityNotFoundException) {
            status.text = "No video player can open this clip"
        }
    }

    private fun openMinecraftContent(uri: Uri) {
        val mime = contentResolver.getType(uri) ?: "application/octet-stream"
        val direct = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            setPackage(MINECRAFT_PACKAGE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        try {
            startActivity(direct)
            status.text = "Minecraft content sent to the installed game"
        } catch (_: ActivityNotFoundException) {
            val fallback = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            runCatching {
                startActivity(Intent.createChooser(fallback, "Open with Minecraft"))
                status.text = "Choose Minecraft to import the content"
            }.onFailure {
                status.text = "No app can open that Minecraft content file"
            }
        }
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
        relayStatusView = addBody(diagnostics, null, RelayRuntime.summary())
        pageContainer.addView(diagnostics)
        relayHandler.post(relayRunnable)

        val probe = card()
        addEyebrow(probe, "SERVER PROBE")
        addTitle(probe, null, "Bedrock server status")

        val probeHost = EditText(this).apply {
            hint = "Server host"
            isSingleLine = true
            textSize = 13f
            setText(prefs.getString("probe_host", ""))
        }
        probe.addView(probeHost, buttonParams())

        val probePort = EditText(this).apply {
            hint = "Port"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            isSingleLine = true
            textSize = 13f
            setText(prefs.getInt("probe_port", 19132).toString())
        }
        probe.addView(probePort, buttonParams())

        val probeResult = addBody(probe, null, "Ready")
        val probeButton = Button(this).apply {
            text = "PROBE SERVER"
            isAllCaps = false
            setOnClickListener {
                val targetHost = probeHost.text.toString().trim()
                val targetPort = probePort.text.toString().toIntOrNull() ?: 19132
                if (targetHost.isBlank() || targetPort !in 1..65535) {
                    probeResult.text = "Enter a valid server host and port"
                    return@setOnClickListener
                }

                probeResult.text = "Probing…"
                isEnabled = false
                Thread({
                    val result = com.riplow.client.modules.BedrockServerProbe.probe(targetHost, targetPort)
                    runOnUiThread {
                        isEnabled = true
                        prefs.edit()
                            .putString("probe_host", targetHost)
                            .putInt("probe_port", targetPort)
                            .apply()

                        if (result.reachable && result.info != null) {
                            NativeBridge.samplePing(result.elapsedMs.toDouble())
                            NativeBridge.recordPacketLoss(0.0)
                            val info = result.info
                            probeResult.text = buildString {
                                append("Online • ")
                                append(result.elapsedMs)
                                append(" ms")
                                append("\n")
                                append(info.motd)
                                append("\n")
                                append(info.players ?: "?")
                                append("/")
                                append(info.maxPlayers ?: "?")
                                append(" players")
                                append("\nProtocol ")
                                append(info.protocol ?: "?")
                                append(" • ")
                                append(info.versionName ?: "unknown")
                            }
                        } else {
                            NativeBridge.recordPacketLoss(100.0)
                            probeResult.text = result.error ?: "Server unreachable"
                        }
                    }
                }, "Riplow-Server-Probe").start()
            }
        }
        probe.addView(probeButton, buttonParams())
        pageContainer.addView(probe)

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
        val card = card().apply {
            setPadding(dp(13), dp(12), dp(13), dp(12))
        }
        val availability = ModuleCapabilities.availability(module.id)
        val enabled = availability == ModuleAvailability.READY && ModuleManager.isEnabled(prefs, module.id)
        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val title = TextView(this).apply {
            text = module.title
            textSize = 13f
            maxLines = 2
            setTextColor(getColor(R.color.riplow_foreground))
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        heading.addView(title, LinearLayout.LayoutParams(0, -2, 1f))
        val stateText = when (availability) {
            ModuleAvailability.READY -> if (enabled) "ON" else "OFF"
            ModuleAvailability.NATIVE_TELEMETRY -> "DATA"
            ModuleAvailability.GAME_BRIDGE_REQUIRED -> "BRIDGE"
            ModuleAvailability.PLANNED -> "SOON"
        }
        heading.addView(TextView(this).apply {
            text = stateText
            textSize = 8f
            letterSpacing = 0.05f
            setTextColor(getColor(if (enabled) R.color.riplow_foreground else R.color.riplow_secondary))
            setPadding(dp(7), dp(5), dp(7), dp(5))
            background = getDrawable(R.drawable.bg_chip)
        }, LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(5), 0, 0, 0) })
        card.addView(heading)

        if (availability == ModuleAvailability.READY) {
            card.addView(TextView(this).apply {
                text = ModuleRuntime.status(this@MainActivity, prefs, module)
                textSize = 10f
                maxLines = 2
                setTextColor(getColor(R.color.riplow_secondary))
                setPadding(0, dp(5), 0, 0)
            })
            card.addView(Button(this).apply {
                text = if (enabled) "Turn off" else "Turn on"
                isAllCaps = false
                textSize = 10f
                setOnClickListener {
                    ModuleManager.toggle(prefs, module.id)
                    showPage(Page.MODULES)
                }
            }, LinearLayout.LayoutParams(-1, dp(36)).apply { setMargins(0, dp(7), 0, 0) })
        } else {
            card.setOnClickListener {
                val message = when (availability) {
                    ModuleAvailability.NATIVE_TELEMETRY -> "This module reads telemetry available to Riplow. It is not a game-renderer toggle."
                    ModuleAvailability.GAME_BRIDGE_REQUIRED -> "This feature needs a compatible Minecraft Bedrock integration before it can work in-game."
                    ModuleAvailability.PLANNED -> "This feature is planned but is not implemented yet."
                    ModuleAvailability.READY -> ""
                }
                android.app.AlertDialog.Builder(this)
                    .setTitle(module.title)
                    .setMessage(message)
                    .setPositiveButton("Got it", null)
                    .show()
            }
            card.isClickable = true
            card.isFocusable = true
        }

        module.settings.forEach { setting ->
            val current = ModuleManager.setting(prefs, module.id, setting)
            card.addView(Button(this).apply {
                text = setting.title + ": " + current
                isAllCaps = false
                textSize = 10f
                setOnClickListener {
                    ModuleManager.cycleSetting(prefs, module.id, setting)
                    showPage(Page.MODULES)
                }
            }, LinearLayout.LayoutParams(-1, dp(34)).apply { setMargins(0, dp(6), 0, 0) })
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
                REQUEST_IMPORT_MINECRAFT_CONTENT -> openMinecraftContent(uri)
                REQUEST_OPEN_CLIP -> openSavedClip(uri)
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

    private fun quickCard(eyebrow: String, title: String, body: String, page: Page): View {
        return infoCard(eyebrow, title, body).apply {
            isClickable = true
            isFocusable = true
            setOnClickListener { showPage(page) }
        }
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
        relayHandler.removeCallbacks(relayRunnable)
        telemetryView = null
        relayStatusView = null
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