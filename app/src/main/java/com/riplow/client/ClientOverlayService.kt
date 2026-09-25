package com.riplow.client

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.core.app.NotificationCompat

class ClientOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var bubble: TextView
    private var panel: View? = null

    private val moduleIds = listOf(
        "fps", "frame_pacing", "performance_profile", "cps", "coordinates",
        "zoom", "crosshair", "hud", "ping", "network_diagnostics", "client_menu"
    )

    override fun onCreate() {
        super.onCreate()
        startOverlayForegroundService()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        createBubble()
    }

    private fun startOverlayForegroundService() {
        val channelId = "riplow_overlay"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "Riplow overlay",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps the user-enabled Riplow overlay available while Minecraft is in use."
                    setShowBadge(false)
                }
            )
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Riplow overlay active")
            .setContentText("Client menu and diagnostics are running.")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                1001,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(1001, notification)
        }
    }

    private fun params(width: Int, height: Int) =
        WindowManager.LayoutParams(
            width, height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 18
            y = 180
        }

    private fun createBubble() {
        bubble = TextView(this).apply {
            text = "R"
            textSize = 18f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = backgroundShape(Color.rgb(20,20,24), 20)
            setOnClickListener { togglePanel() }
            setOnTouchListener(DragTouchListener())
        }
        windowManager.addView(bubble, params(52, 52))
    }

    private fun togglePanel() {
        panel?.let { windowManager.removeView(it); panel = null; return }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18,18,18,18)
            background = backgroundShape(Color.rgb(16,16,20), 28)
        }
        root.addView(TextView(this).apply {
            text = "RIPLOW"
            textSize = 20f
            setTextColor(Color.WHITE)
        })
        root.addView(TextView(this).apply {
            text = "Client Menu"
            textSize = 12f
            setTextColor(Color.LTGRAY)
        })

        val scroll = ScrollView(this)
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0,12,0,4)
        }

        for (id in moduleIds) {
            val button = android.widget.Button(this).apply {
                text = label(id)
                isAllCaps = false
                setTextColor(Color.WHITE)
                background = backgroundShape(Color.rgb(32,32,38),18)
                setOnClickListener {
                    val enabled = NativeBridge.nativeToggleModule(id)
                    text = label(id) + if (enabled) " • ON" else " • OFF"
                }
            }
            list.addView(button, LinearLayout.LayoutParams(-1,52).apply { bottomMargin = 8 })
        }

        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1,0,1f))
        root.addView(TextView(this).apply {
            text = NativeBridge.nativeDiagnostics()
            textSize = 11f
            setTextColor(Color.LTGRAY)
            setPadding(0,8,0,0)
        })
        panel = root
        windowManager.addView(root, params(310,520))
    }

    private fun label(id:String)=when(id) {
        "fps"->"FPS Overlay"; "frame_pacing"->"Frame Pacing"; "performance_profile"->"Performance Profile"
        "cps"->"CPS Counter"; "coordinates"->"Coordinates"; "zoom"->"Zoom"; "crosshair"->"Crosshair"
        "hud"->"HUD"; "ping"->"Ping"; "network_diagnostics"->"Network Diagnostics"
        else->"Client Menu"
    }

    private fun backgroundShape(color:Int,radius:Int)=GradientDrawable().apply {
        setColor(color)
        cornerRadius=radius.toFloat()
        setStroke(1,Color.rgb(44,44,52))
    }

    override fun onDestroy() {
        panel?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        if (::bubble.isInitialized && bubble.isAttachedToWindow) windowManager.removeView(bubble)
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent:Intent?):IBinder?=null

    private inner class DragTouchListener:View.OnTouchListener {
        private var downX=0f
        private var downY=0f
        private var startX=0
        private var startY=0

        override fun onTouch(view:View,event:MotionEvent):Boolean {
            when(event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX=event.rawX
                    downY=event.rawY
                    val p=view.layoutParams as WindowManager.LayoutParams
                    startX=p.x
                    startY=p.y
                    return true
                }
                MotionEvent.ACTION_MOVE -> {
                    val p=view.layoutParams as WindowManager.LayoutParams
                    p.x=startX+(event.rawX-downX).toInt()
                    p.y=startY+(event.rawY-downY).toInt()
                    windowManager.updateViewLayout(view,p)
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (kotlin.math.abs(event.rawX-downX)<12 && kotlin.math.abs(event.rawY-downY)<12) view.performClick()
                    return true
                }
            }
            return false
        }
    }
}
