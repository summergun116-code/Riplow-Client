package com.riplow.client

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    external fun nativeVersion(): String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.core_version).text =
            getString(R.string.version_format, nativeVersion())
        findViewById<Button>(R.id.launch_button).setOnClickListener { launchMinecraft() }
        findViewById<Button>(R.id.client_button).setOnClickListener {
            findViewById<TextView>(R.id.status).text = "Client UI boundary ready"
        }
    }

    private fun launchMinecraft() {
        val intent = packageManager.getLaunchIntentForPackage("com.mojang.minecraftpe")
        if (intent != null) startActivity(intent)
        else findViewById<TextView>(R.id.status).text = "Minecraft installation not detected"
    }

    companion object { init { System.loadLibrary("riplow_core") } }
}