package com.riplow.client.modules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MasterFeatureCatalogTest {
    @Test
    fun hasExactly100NumberedFeatures() {
        assertEquals(100, MasterFeatureCatalog.all.size)
        assertEquals((1..100).toList(), MasterFeatureCatalog.all.map { it.number })
        assertEquals(100, MasterFeatureCatalog.all.map { it.id }.toSet().size)
    }

    @Test
    fun everyFeatureHasUsefulMetadata() {
        assertTrue(MasterFeatureCatalog.all.all { it.title.isNotBlank() })
        assertTrue(MasterFeatureCatalog.all.all { it.category.isNotBlank() })
        assertTrue(MasterFeatureCatalog.all.all { it.description.isNotBlank() })
    }

    @Test
    fun lifeboatCandidateDoesNotActivateWithoutSessionConfirmation() {
        val state = LifeboatPolicy.evaluate("play.lbsg.net", "Lifeboat Network", sessionConfirmed = false)
        assertFalse(state.active)
        assertTrue(state.reason?.contains("awaiting bridge confirmation") == true)
    }

    @Test
    fun lifeboatActiveModeUsesExplicitAllowlist() {
        val state = LifeboatPolicy.evaluate("play.lbsg.net", "Lifeboat Network", sessionConfirmed = true)
        assertTrue(state.active)
        assertTrue(LifeboatPolicy.isAllowed("zoom", state))
        assertTrue(LifeboatPolicy.isAllowed("cps_counter", state))
        assertFalse(LifeboatPolicy.isAllowed("kill_aura", state))
        assertTrue(state.disabledFeatureIds.contains("kill_aura"))
    }
}
