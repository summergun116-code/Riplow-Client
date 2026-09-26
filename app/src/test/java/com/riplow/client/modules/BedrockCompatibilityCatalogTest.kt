package com.riplow.client.modules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BedrockCompatibilityCatalogTest {
    @Test
    fun currentStableUsesKnownProtocol() {
        val profile = BedrockCompatibilityCatalog.inspect(BedrockVersion(1, 26, 51))
        assertEquals(BedrockSupportLevel.CURRENT_STABLE, profile.support)
        assertEquals(BedrockReleaseChannel.STABLE, profile.channel)
        assertEquals(2193, profile.networkProtocol)
    }

    @Test
    fun newerVersionFallsBackToGenericSafeMode() {
        val profile = BedrockCompatibilityCatalog.inspect(BedrockVersion(1, 26, 60))
        assertEquals(BedrockSupportLevel.FUTURE_RELEASE, profile.support)
        assertEquals(BedrockReleaseChannel.UNKNOWN, profile.channel)
        assertNull(profile.networkProtocol)
    }

    @Test
    fun knownStableProtocolLookupIsExact() {
        assertEquals(
            2193,
            BedrockCompatibilityCatalog.expectedNetworkProtocol(BedrockVersion(1, 26, 50))
        )
        assertNull(
            BedrockCompatibilityCatalog.expectedNetworkProtocol(BedrockVersion(1, 26, 52))
        )
    }
}
