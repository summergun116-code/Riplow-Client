package com.riplow.client.modules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Test

class BedrockBridgeContractTest {
    @Test
    fun parsesStandardBedrockVersion() {
        val parsed = BedrockVersionParser.parse("1.21.100")
        assertNotNull(parsed)
        assertEquals(BedrockVersion(1, 21, 100), parsed)
    }

    @Test
    fun parsesBuildComponent() {
        val parsed = BedrockVersionParser.parse("1.21.100.25")
        assertEquals(BedrockVersion(1, 21, 100, 25), parsed)
    }

    @Test
    fun parses2026ReleaseNumbering() {
        assertEquals(BedrockVersion(1, 26, 51), BedrockVersionParser.parse("26.51"))
        assertEquals(BedrockVersion(1, 26, 51), BedrockVersionParser.parse("1.26.51"))
        assertEquals(
            BedrockVersion(1, 26, 60, 28),
            BedrockVersionParser.parse("1.26.60-preview.28")
        )
    }

    @Test
    fun rejectsMalformedVersion() {
        assertNull(BedrockVersionParser.parse("bedrock-latest"))
        assertNull(BedrockVersionParser.parse("1.21"))
    }

    @Test
    fun currentStableVersionGetsCompatibilityProfile() {
        val info = BedrockBridgeRegistry.inspect(BedrockVersion(1, 26, 51))
        assertEquals(BedrockBridgeState.VERSION_RECOGNIZED_NO_BRIDGE, info.state)
        assertEquals(BedrockSupportLevel.CURRENT_STABLE, info.profile?.support)
        assertEquals(BedrockReleaseChannel.STABLE, info.profile?.channel)
        assertEquals(2193, info.profile?.networkProtocol)
        assertNull(info.adapterId)
    }
}
