package com.riplow.client

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.core_version).text =
            getString(R.string.version_format, NativeBridge.version())
        refreshDiagnostics()

        findViewById<Button>(R.id.launch_button).setOnClickListener { launchMinecraft() }
        findViewById<Button>(R.id.client_button).setOnClickListener { enableClientMenu() }
        findViewById<Button>(R.id.diagnostics_button).setOnClickListener { refreshDiagnostics() }
    }

    override fun onResume() {
        super.onResume()
        refreshDiagnostics()
    }

    private fun refreshDiagnostics() {
        findViewById<TextView>(R.id.status).text = "Native core online"
        findViewById<TextView>(R.id.diagnostics).text = NativeBridge.nativeDiagnostics()
    }

    private fun enableClientMenu() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
            findViewById<TextView>(R.id.status).text =
                "Grant overlay permission, then enable the menu again"
            return
        }

        ContextCompat.startForegroundService(
            this,
            Intent(this, ClientOverlayService::class.java)
        )
        findViewById<TextView>(R.id.status).text = "Riplow menu enabled"
    }

    private fun launchMinecraft() {
        val intent = packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
        if (intent != null) {
            startActivity(intent)
        } else {
            findViewById<TextView>(R.id.status).text = "Minecraft installation not detected"
        }
    }
}
