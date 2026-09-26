package com.riplow.client.modules

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

data class BedrockServerInfo(
    val host: String,
    val port: Int,
    val edition: String,
    val motd: String,
    val protocol: Int?,
    val versionName: String?,
    val players: Int?,
    val maxPlayers: Int?,
    val rawFields: List<String>
)

data class BedrockServerProbeResult(
    val reachable: Boolean,
    val elapsedMs: Long = -1L,
    val info: BedrockServerInfo? = null,
    val error: String? = null
)

object BedrockServerProbe {
    const val DEFAULT_PORT = 19132
    private const val DEFAULT_TIMEOUT_MS = 1800
    private val magic = byteArrayOf(
        0x00, 0xff.toByte(), 0xff.toByte(), 0x00,
        0xfe.toByte(), 0xfe.toByte(), 0xfe.toByte(), 0xfe.toByte(),
        0xfd.toByte(), 0xfd.toByte(), 0xfd.toByte(), 0xfd.toByte(),
        0x12, 0x34, 0x56, 0x78
    )

    fun probe(host: String, port: Int = DEFAULT_PORT, timeoutMs: Int = DEFAULT_TIMEOUT_MS): BedrockServerProbeResult {
        val cleanHost = host.trim()
        if (cleanHost.isEmpty()) return BedrockServerProbeResult(false, error = "Server address is empty")
        if (cleanHost.length > 253) return BedrockServerProbeResult(false, error = "Server address is too long")
        if (port !in 1..65535) return BedrockServerProbeResult(false, error = "Invalid server port")

        return try {
            DatagramSocket().use { socket ->
                socket.soTimeout = timeoutMs.coerceIn(250, 5000)
                val guid = java.util.UUID.randomUUID().mostSignificantBits
                val started = System.nanoTime()
                val request = buildPing(System.currentTimeMillis(), guid)
                val target = InetSocketAddress(cleanHost, port)
                socket.send(DatagramPacket(request, request.size, target))

                val receiveBuffer = ByteArray(2048)
                val response = DatagramPacket(receiveBuffer, receiveBuffer.size)
                socket.receive(response)
                val elapsed = (System.nanoTime() - started) / 1_000_000L
                val info = parsePong(response.data.copyOf(response.length), cleanHost, response.port)
                    ?: return BedrockServerProbeResult(false, elapsed, error = "Invalid RakNet server response")
                BedrockServerProbeResult(true, elapsed, info = info)
            }
        } catch (error: java.net.SocketTimeoutException) {
            BedrockServerProbeResult(false, error = "Server did not answer the RakNet probe")
        } catch (error: Exception) {
            BedrockServerProbeResult(false, error = error.javaClass.simpleName + (error.message?.let { ": " + it } ?: ""))
        }
    }

    private fun buildPing(clientTime: Long, clientGuid: Long): ByteArray {
        val buffer = ByteBuffer.allocate(1 + 8 + 16 + 8).order(ByteOrder.BIG_ENDIAN)
        buffer.put(0x01)
        buffer.putLong(clientTime)
        buffer.put(magic)
        buffer.putLong(clientGuid)
        return buffer.array()
    }

    internal fun parsePong(payload: ByteArray, host: String, port: Int): BedrockServerInfo? {
        if (payload.size < 35 || payload[0].toInt() and 0xff != 0x1c) return null
        val magicOffset = 17
        if (!payload.copyOfRange(magicOffset, magicOffset + magic.size).contentEquals(magic)) return null
        val lengthOffset = magicOffset + magic.size
        val textLength = ((payload[lengthOffset].toInt() and 0xff) shl 8) or
            (payload[lengthOffset + 1].toInt() and 0xff)
        val textStart = lengthOffset + 2
        if (textLength < 0 || textStart + textLength > payload.size) return null

        val text = String(payload, textStart, textLength, StandardCharsets.UTF_8)
        val fields = text.split(";")
        if (fields.size < 4) return null

        fun intAt(index: Int): Int? = fields.getOrNull(index)?.toIntOrNull()
        return BedrockServerInfo(
            host = host,
            port = port,
            edition = fields.getOrNull(0).orEmpty(),
            motd = fields.getOrNull(1).orEmpty(),
            protocol = intAt(2),
            versionName = fields.getOrNull(3)?.takeIf { it.isNotBlank() },
            players = intAt(4),
            maxPlayers = intAt(5),
            rawFields = fields
        )
    }
}