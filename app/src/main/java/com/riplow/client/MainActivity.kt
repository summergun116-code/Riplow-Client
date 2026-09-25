package com.riplow.client

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    external fun nativeVersion(): String
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.status).text = "Riplow \${nativeVersion()}\nCore initialized"
    }
    companion object { init { System.loadLibrary("riplow_core") } }
}
