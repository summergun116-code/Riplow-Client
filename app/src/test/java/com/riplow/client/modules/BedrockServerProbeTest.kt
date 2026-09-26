package com.riplow.client.modules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BedrockServerProbeTest {
    @Test
    fun parsesRakNetPong() {
        val text = "MCPE;Test Bedrock;2193;1.26.51;2;20;123;Survival;1;19132;19133"
        val magic = byteArrayOf(
            0x00, 0xff.toByte(), 0xff.toByte(), 0x00,
            0xfe.toByte(), 0xfe.toByte(), 0xfe.toByte(), 0xfe.toByte(),
            0xfd.toByte(), 0xfd.toByte(), 0xfd.toByte(), 0xfd.toByte(),
            0x12, 0x34, 0x56, 0x78
        )
        val textBytes = text.toByteArray(Charsets.UTF_8)
        val payload = ByteBuffer.allocate(1 + 8 + 8 + 16 + 2 + textBytes.size)
            .order(ByteOrder.BIG_ENDIAN)
            .apply {
                put(0x1c)
                putLong(123L)
                putLong(456L)
                put(magic)
                putShort(textBytes.size.toShort())
                put(textBytes)
            }.array()

        val parsed = BedrockServerProbe.parsePong(payload, "127.0.0.1", 19132)
        assertNotNull(parsed)
        assertEquals("Test Bedrock", parsed?.motd)
        assertEquals(2193, parsed?.protocol)
        assertEquals("1.26.51", parsed?.versionName)
        assertEquals(2, parsed?.players)
        assertEquals(20, parsed?.maxPlayers)
    }

    @Test
    fun rejectsInvalidRakNetMagic() {
        val payload = ByteArray(40)
        payload[0] = 0x1c
        val parsed = BedrockServerProbe.parsePong(payload, "127.0.0.1", 19132)
        assertEquals(null, parsed)
    }
}