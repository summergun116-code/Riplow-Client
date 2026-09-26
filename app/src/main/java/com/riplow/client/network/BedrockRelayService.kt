package com.riplow.client.network

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketException
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object RelayRuntime {
    private const val CHANNEL_ID = "riplow_bedrock_relay"

    @Volatile var active: Boolean = false
    @Volatile var localPort: Int = 19132
    @Volatile var remoteHost: String = ""
    @Volatile var remotePort: Int = 19132
    @Volatile var clientEndpoint: String = "waiting"
    @Volatile var lastError: String = ""

    private val packetsClientToServer = AtomicLong()
    private val packetsServerToClient = AtomicLong()
    private val bytesClientToServer = AtomicLong()
    private val bytesServerToClient = AtomicLong()

    fun reset(host: String, port: Int, local: Int) {
        remoteHost = host
        remotePort = port
        localPort = local
        clientEndpoint = "waiting"
        lastError = ""
        packetsClientToServer.set(0)
        packetsServerToClient.set(0)
        bytesClientToServer.set(0)
        bytesServerToClient.set(0)
    }

    internal fun clientPacket(bytes: Int, endpoint: String) {
        clientEndpoint = endpoint
        packetsClientToServer.incrementAndGet()
        bytesClientToServer.addAndGet(bytes.toLong())
    }

    internal fun serverPacket(bytes: Int) {
        packetsServerToClient.incrementAndGet()
        bytesServerToClient.addAndGet(bytes.toLong())
    }

    internal fun fail(message: String) {
        lastError = message
    }

    fun summary(): String = buildString {
        append(if (active) "Relay active" else "Relay stopped")
        append(" • 127.0.0.1:")
        append(localPort)
        append(" → ")
        append(if (remoteHost.isBlank()) "no server" else "$remoteHost:$remotePort")
        append("\nClient: ")
        append(clientEndpoint)
        append("\nC→S: ")
        append(packetsClientToServer.get())
        append(" packets / ")
        append(bytesClientToServer.get())
        append(" B")
        append("\nS→C: ")
        append(packetsServerToClient.get())
        append(" packets / ")
        append(bytesServerToClient.get())
        append(" B")
        if (lastError.isNotBlank()) {
            append("\nError: ")
            append(lastError)
        }
    }
}

class BedrockRelayService : Service() {
    companion object {
        const val EXTRA_HOST = "remote_host"
        const val EXTRA_PORT = "remote_port"
        const val EXTRA_LOCAL_PORT = "local_port"

        private val stopRequested = AtomicBoolean(false)

        @Volatile private var socket: DatagramSocket? = null
        @Volatile private var worker: Thread? = null

        fun start(context: android.content.Context, host: String, remotePort: Int, localPort: Int): Boolean {
            if (host.isBlank() || remotePort !in 1..65535 || localPort !in 1..65535) return false
            val intent = Intent(context, BedrockRelayService::class.java).apply {
                putExtra(EXTRA_HOST, host.trim())
                putExtra(EXTRA_PORT, remotePort)
                putExtra(EXTRA_LOCAL_PORT, localPort)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            return true
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, BedrockRelayService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val host = intent?.getStringExtra(EXTRA_HOST)?.trim().orEmpty()
        val remotePort = intent?.getIntExtra(EXTRA_PORT, 19132) ?: 19132
        val localPort = intent?.getIntExtra(EXTRA_LOCAL_PORT, 19132) ?: 19132
        if (host.isBlank() || remotePort !in 1..65535 || localPort !in 1..65535) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(42, notification("127.0.0.1:$localPort → $host:$remotePort"))

        stopRequested.set(false)
        worker?.interrupt()
        RelayRuntime.reset(host, remotePort, localPort)
        RelayRuntime.active = true

        worker = Thread({ runRelay(host, remotePort, localPort) }, "Riplow-Bedrock-Relay").also { it.start() }
        return START_NOT_STICKY
    }

    private fun runRelay(host: String, remotePort: Int, localPort: Int) {
        try {
            val remote = InetAddress.getAllByName(host)
                .sortedBy { if (it is Inet4Address) 0 else 1 }
                .firstOrNull()
                ?: error("Server address could not be resolved")

            DatagramSocket(InetSocketAddress(InetAddress.getByName("127.0.0.1"), localPort)).also {
                socket = it
            }.use { udp ->
                udp.soTimeout = 1000
                val buffer = ByteArray(65535)
                var client: InetSocketAddress? = null

                while (!stopRequested.get() && !Thread.currentThread().isInterrupted) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    try {
                        udp.receive(packet)
                    } catch (_: SocketTimeoutException) {
                        continue
                    } catch (_: SocketException) {
                        if (stopRequested.get()) break
                        throw
                    }

                    if (packet.address.isLoopbackAddress) {
                        client = InetSocketAddress(packet.address, packet.port)
                        RelayRuntime.clientPacket(
                            packet.length,
                            packet.address.hostAddress + ":" + packet.port
                        )
                        udp.send(DatagramPacket(packet.data, packet.length, remote, remotePort))
                    } else {
                        val target = client ?: continue
                        RelayRuntime.serverPacket(packet.length)
                        udp.send(DatagramPacket(packet.data, packet.length, target.address, target.port))
                    }
                }
            }
        } catch (e: Throwable) {
            if (!stopRequested.get()) RelayRuntime.fail(e.message ?: e.javaClass.simpleName)
        } finally {
            socket?.close()
            socket = null
            RelayRuntime.active = false
        }
    }

    override fun onDestroy() {
        stopRequested.set(true)
        worker?.interrupt()
        socket?.close()
        worker = null
        socket = null
        RelayRuntime.active = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                "riplow_bedrock_relay",
                "Riplow Bedrock Relay",
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    private fun notification(route: String): Notification =
        NotificationCompat.Builder(this, "riplow_bedrock_relay")
            .setContentTitle("Riplow Bedrock Relay")
            .setContentText(route)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
}
