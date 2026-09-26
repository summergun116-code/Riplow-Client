package com.riplow.client.modules

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModuleRegistryTest {
    @Test
    fun registryIdsAreUnique() {
        val ids = ModuleRegistry.all.map { it.id }
        assertEquals("duplicate module ids", ids.size, ids.toSet().size)
    }

    @Test
    fun everySettingHasValidDefault() {
        ModuleRegistry.all.forEach { module ->
            module.settings.forEach { setting ->
                val key = module.id + ":" + setting.id
                assertTrue("$key has no options", setting.options.isNotEmpty())
                assertTrue("$key default is not an option", setting.options.contains(setting.defaultValue))
            }
        }
    }

    @Test
    fun capabilitySetsOnlyReferenceRegisteredModules() {
        val ids = ModuleRegistry.all.map { it.id }.toSet()
        assertTrue(ModuleRegistry.gameBridgeIds.all(ids::contains))
        assertTrue(ModuleRegistry.nativeIds.all(ids::contains))
        assertTrue(ModuleCapabilities.runtimeReadyIds.all(ids::contains))
    }

    @Test
    fun nativeAndBridgeOverlapIsIntentionalAndKnown() {
        val overlap = ModuleRegistry.nativeIds intersect ModuleRegistry.gameBridgeIds
        assertEquals(setOf("ping_counter"), overlap)
    }
}