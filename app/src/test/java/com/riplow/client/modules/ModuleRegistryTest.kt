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
    fun registryIsFocusedAndAutomationFree() {
        assertTrue(ModuleRegistry.all.size <= 40)
        val ids = ModuleRegistry.all.map { it.id }.toSet()
        assertTrue("kill_aura" !in ids)
        assertTrue("auto_mine" !in ids)
        assertTrue("chest_stealer" !in ids)
        assertTrue("anti_afk" !in ids)
        assertTrue("auto_reconnect" !in ids)
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
