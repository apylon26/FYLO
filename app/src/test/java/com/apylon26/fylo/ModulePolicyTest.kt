package com.apylon26.fylo

import org.junit.Assert.*
import org.junit.Test

class ModulePolicyTest {
    @Test fun onlyLaunchableCompatibleModulesAreAccepted() {
        assertTrue(ModulePolicy.accepts(true, true, 1, false))
        assertFalse(ModulePolicy.accepts(false, true, 1, false))
        assertFalse(ModulePolicy.accepts(true, false, 1, false))
        assertFalse(ModulePolicy.accepts(true, true, 2, false))
        assertFalse(ModulePolicy.accepts(true, true, 1, true))
    }
}
