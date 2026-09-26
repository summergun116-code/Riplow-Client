package com.riplow.client

import android.animation.ObjectAnimator
import android.app.ActivityManager
import android.content.ActivityNotFoundException
import android.content.pm.PackageInfo
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.riplow.client.modules.ModuleRegistry

class MainActivity : AppCompatActivity() {
    companion object {
        private const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe"
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var launchOverlay: FrameLayout
    private lateinit var loadingLogo: TextView
    private lateinit var loadingDetail: TextView
    private var launchRequested = false
    private val prefs by lazy { getSharedPreferences("riplow_settings", MODE_PRIVATE) }
    private var pulseAnimator: ObjectAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        launchOverlay = findViewById(R.id.launch_overlay)
        loadingLogo = findViewById(R.id.loading_logo)
        loadingDetail = findViewById(R.id.loading_detail)

        findViewById<TextView>(R.id.core_version).text =
            getString(R.string.version_format, NativeBridge.version())

        findViewById<TextView>(R.id.module_count).text =
            "${ModuleRegistry.all.size} registered modules"

        refreshDiagnostics()

        findViewById<Button>(R.id.launch_button).setOnClickListener { launchMinecraft() }
        findViewById<Button>(R.id.client_button).setOnClickListener { enableClientMenu() }
        findViewById<Button>(R.id.diagnostics_button).setOnClickListener { refreshDiagnostics() }

    }

    override fun onResume() {
        super.onResume()
        if (launchRequested) {
            launchRequested = false
            hideLaunchScreen()
        }
        if (prefs.getBoolean("open_after_overlay_permission", false) && Settings.canDrawOverlays(this)) {
            prefs.edit().remove("open_after_overlay_permission").apply()
            startClientOverlay()
        }
        refreshDiagnostics()
    }

    private fun minecraftInfo(): PackageInfo? = try {
        packageManager.getPackageInfo(MINECRAFT_PACKAGE, 0)
    } catch (_: Exception) {
        null
    }

    private fun minecraftInstalled(): Boolean = minecraftInfo() != null

    private fun refreshDiagnostics() {
        val info = minecraftInfo()
        val installed = info != null
        val memory = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        memory.getMemoryInfo(memoryInfo)
        val version = info?.versionName ?: "not detected"
        val overlay = if (Settings.canDrawOverlays(this)) "granted" else "required"

        findViewById<TextView>(R.id.status).text =
            if (installed) "Minecraft detected  •  $version" else "Minecraft not detected"
        findViewById<TextView>(R.id.diagnostics).text =
            "Minecraft: ${if (installed) "installed" else "not visible / installed"}" +
                "\nVersion: $version" +
                "\nOverlay permission: $overlay" +
                "\nAvailable RAM: ${memoryInfo.availMem / (1024L * 1024L)} MB" +
                "\n\n" + NativeBridge.nativeDiagnostics()
    }

    private fun enableClientMenu() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            prefs.edit().putBoolean("open_after_overlay_permission", true).apply()
            findViewById<TextView>(R.id.status).text = "Overlay permission required"
            return
        }

        startClientOverlay()
    }

    private fun startClientOverlay() {
        ContextCompat.startForegroundService(
            this,
            Intent(this, ClientOverlayService::class.java)
                .setAction(ClientOverlayService.ACTION_OPEN)
        )
        findViewById<TextView>(R.id.status).text = "Riplow menu opening…"
    }

    private fun launchMinecraft() {
        if (!minecraftInstalled()) {
            loadingDetail.text = "Minecraft was not detected on this device"
            findViewById<TextView>(R.id.status).text = "Minecraft not installed or not visible"
            return
        }

        launchRequested = true
        showLaunchScreen()
        loadingDetail.text = "Minecraft installation found"

        handler.postDelayed({
            if (!launchRequested) return@postDelayed
            loadingDetail.text = "Checking Android launch handoff…"
        }, 220)

        handler.postDelayed({
            if (!launchRequested) return@postDelayed
            loadingDetail.text = "Starting Minecraft…"
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager
                        .getLaunchIntentSenderForPackage(MINECRAFT_PACKAGE)
                        .sendIntent(this, 0, null, null, null)
                } else {
                    val intent = packageManager.getLaunchIntentForPackage(MINECRAFT_PACKAGE)
                        ?: throw ActivityNotFoundException("Minecraft launcher activity not found")
                    startActivity(intent)
                }

                loadingDetail.text = "Handoff complete"
                findViewById<TextView>(R.id.status).text = "Minecraft launch requested"
            } catch (e: Exception) {
                launchRequested = false
                loadingDetail.text = "Android could not start Minecraft"
                findViewById<TextView>(R.id.status).text =
                    "Minecraft launch failed: " + e.javaClass.simpleName
                handler.postDelayed({
                    hideLaunchScreen()
                }, 1400)
            }
        }, 560)
    }

    private fun showLaunchScreen() {
        startLoadingPulse()
        launchOverlay.visibility = View.VISIBLE
        launchOverlay.alpha = 0f
        launchOverlay.animate()
            .alpha(1f)
            .setDuration(220)
            .setInterpolator(DecelerateInterpolator())
            .start()

        loadingLogo.scaleX = 0.86f
        loadingLogo.scaleY = 0.86f
        loadingLogo.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(520)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun hideLaunchScreen() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        launchOverlay.animate()
            .alpha(0f)
            .setDuration(220)
            .withEndAction { launchOverlay.visibility = View.GONE }
            .start()
    }

    private fun startLoadingPulse() {
        pulseAnimator?.cancel()
        pulseAnimator = ObjectAnimator.ofFloat(loadingLogo, View.ALPHA, 0.74f, 1f).apply {
            duration = 950
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    override fun onDestroy() {
        launchRequested = false
        pulseAnimator?.cancel()
        pulseAnimator = null
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
