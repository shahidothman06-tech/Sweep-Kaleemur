package com.hidden.devices.detector.bugs.finder.update

import org.junit.Assert.assertEquals
import org.junit.Test

class UpdatePolicyTest {
    private fun decide(
        available: Boolean = true,
        priority: Int = 0,
        days: Int? = null,
        immediate: Boolean = true,
        flexible: Boolean = true,
    ) = UpdatePolicy.decide(available, priority, days, immediate, flexible)

    @Test fun noUpdate_isNone() = assertEquals(UpdateFlow.NONE, decide(available = false, priority = 5, days = 30))

    @Test fun normalUpdate_isFlexible() = assertEquals(UpdateFlow.FLEXIBLE, decide(priority = 0, days = 2))

    @Test fun highPriority_isImmediate() = assertEquals(UpdateFlow.IMMEDIATE, decide(priority = 4))

    @Test fun priorityBelowThreshold_isFlexible() = assertEquals(UpdateFlow.FLEXIBLE, decide(priority = 3))

    @Test fun sevenDaysOld_isImmediate() = assertEquals(UpdateFlow.IMMEDIATE, decide(days = 7))

    @Test fun sixDaysOld_isFlexible() = assertEquals(UpdateFlow.FLEXIBLE, decide(days = 6))

    @Test fun nullStaleness_isFlexible() = assertEquals(UpdateFlow.FLEXIBLE, decide(days = null))

    @Test fun important_butImmediateNotAllowed_fallsBackToFlexible() =
        assertEquals(UpdateFlow.FLEXIBLE, decide(priority = 5, immediate = false))

    @Test fun normal_butFlexibleNotAllowed_fallsBackToImmediate() =
        assertEquals(UpdateFlow.IMMEDIATE, decide(flexible = false))

    @Test fun nothingAllowed_isNone() = assertEquals(UpdateFlow.NONE, decide(immediate = false, flexible = false))
}
