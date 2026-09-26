package com.riplow.client.modules

import org.junit.Assert.assertEquals
import org.junit.Test

class PerformancePolicyTest {
    @Test
    fun normalizesPerformanceModes() {
        assertEquals(RiplowPerformanceMode.PERFORMANCE, PerformancePolicy.modeValue(null))
        assertEquals(RiplowPerformanceMode.PERFORMANCE, PerformancePolicy.modeValue("Performance"))
        assertEquals(RiplowPerformanceMode.BALANCED, PerformancePolicy.modeValue("Balanced"))
        assertEquals(RiplowPerformanceMode.EXTREME, PerformancePolicy.modeValue("Extreme"))
        assertEquals(RiplowPerformanceMode.PERFORMANCE, PerformancePolicy.modeValue("unknown"))
    }
}
