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
    fun registryContainsExpandedWClientSurfaceAndExcludesWAura() {
        val ids = ModuleRegistry.all.map { it.id }.toSet()
        assertTrue("killaura" in ids)
        assertTrue("motion_fly" in ids)
        assertTrue("block_esp" in ids)
        assertTrue("tp_mine" in ids)
        assertTrue("config_manager" in ids)
        assertTrue("waura" !in ids)
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
    fun noNativeAndGameBridgeOverlap() {
        assertEquals(emptySet<String>(), ModuleRegistry.nativeIds intersect ModuleRegistry.gameBridgeIds)
    }
}
