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
    fun rejectsMalformedVersion() {
        assertNull(BedrockVersionParser.parse("bedrock-latest"))
        assertNull(BedrockVersionParser.parse("1.21"))
    }

    @Test
    fun noAdapterNeverClaimsReady() {
        val info = BedrockBridgeRegistry.inspect(BedrockVersion(1, 99, 999))
        assertEquals(BedrockBridgeState.VERSION_PARSED_NO_ADAPTER, info.state)
        assertNull(info.adapterId)
    }
}