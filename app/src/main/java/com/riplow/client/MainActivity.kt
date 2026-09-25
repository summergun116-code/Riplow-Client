package com.riplow.client

import android.animation.ObjectAnimator
import android.content.ActivityNotFoundException
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

class MainActivity : AppCompatActivity() {
    companion object {
        private const val MINECRAFT_PACKAGE = "com.mojang.minecraftpe"
    }

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var launchOverlay: FrameLayout
    private lateinit var loadingLogo: TextView
    private lateinit var loadingDetail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        launchOverlay = findViewById(R.id.launch_overlay)
        loadingLogo = findViewById(R.id.loading_logo)
        loadingDetail = findViewById(R.id.loading_detail)

        findViewById<TextView>(R.id.core_version).text =
            getString(R.string.version_format, NativeBridge.version())

        refreshDiagnostics()

        findViewById<Button>(R.id.launch_button).setOnClickListener { launchMinecraft() }
        findViewById<Button>(R.id.client_button).setOnClickListener { enableClientMenu() }
        findViewById<Button>(R.id.diagnostics_button).setOnClickListener { refreshDiagnostics() }

        startLoadingPulse()
    }

    override fun onResume() {
        super.onResume()
        refreshDiagnostics()
    }

    private fun minecraftInstalled(): Boolean = try {
        packageManager.getPackageInfo(MINECRAFT_PACKAGE, 0)
        true
    } catch (_: Exception) {
        false
    }

    private fun refreshDiagnostics() {
        val installed = minecraftInstalled()
        findViewById<TextView>(R.id.status).text =
            if (installed) "Minecraft detected" else "Minecraft not detected"
        findViewById<TextView>(R.id.diagnostics).text =
            "Minecraft: " + if (installed) "installed" else "not visible/installed" +
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
            findViewById<TextView>(R.id.status).text = "Overlay permission required"
            return
        }

        ContextCompat.startForegroundService(
            this,
            Intent(this, ClientOverlayService::class.java)
                .setAction(ClientOverlayService.ACTION_OPEN)
        )
        findViewById<TextView>(R.id.status).text = "Riplow overlay ready"
    }

    private fun launchMinecraft() {
        if (!minecraftInstalled()) {
            loadingDetail.text = "Minecraft was not detected on this device"
            findViewById<TextView>(R.id.status).text = "Minecraft not installed or not visible"
            return
        }

        showLaunchScreen()
        loadingDetail.text = "Minecraft installation found"

        handler.postDelayed({
            loadingDetail.text = "Preparing Android launch handoff…"
        }, 260)

        handler.postDelayed({
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

                loadingDetail.text = "Launch request handed to Minecraft"
                findViewById<TextView>(R.id.status).text = "Minecraft launch requested"

                handler.postDelayed({
                    hideLaunchScreen()
                }, 900)
            } catch (e: Exception) {
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
        launchOverlay.animate()
            .alpha(0f)
            .setDuration(220)
            .withEndAction { launchOverlay.visibility = View.GONE }
            .start()
    }

    private fun startLoadingPulse() {
        ObjectAnimator.ofFloat(loadingLogo, View.ALPHA, 0.72f, 1f).apply {
            duration = 950
            repeatMode = ObjectAnimator.REVERSE
            repeatCount = ObjectAnimator.INFINITE
            start()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
