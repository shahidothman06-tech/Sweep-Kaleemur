package com.hidden.devices.detector.bugs.finder.update

/** Which in-app update flow to use. Pure logic, so it can be unit-tested without Play. */
enum class UpdateFlow { IMMEDIATE, FLEXIBLE, NONE }

object UpdatePolicy {
    /** Play Console / Publishing API priority (0–5) at or above which the update is required. */
    const val IMMEDIATE_PRIORITY = 4

    /** Days an update has been available on this phone before it becomes required. */
    const val IMMEDIATE_AFTER_DAYS = 7

    /**
     * Important updates (high priority, or ignored for [IMMEDIATE_AFTER_DAYS]+ days) are IMMEDIATE.
     * Everything else is FLEXIBLE. Falls back to whichever type Play allows.
     */
    fun decide(
        updateAvailable: Boolean,
        priority: Int,
        stalenessDays: Int?,
        immediateAllowed: Boolean,
        flexibleAllowed: Boolean,
    ): UpdateFlow {
        if (!updateAvailable) return UpdateFlow.NONE
        val important = priority >= IMMEDIATE_PRIORITY || (stalenessDays ?: 0) >= IMMEDIATE_AFTER_DAYS
        return when {
            important && immediateAllowed -> UpdateFlow.IMMEDIATE
            flexibleAllowed -> UpdateFlow.FLEXIBLE
            immediateAllowed -> UpdateFlow.IMMEDIATE
            else -> UpdateFlow.NONE
        }
    }
}
