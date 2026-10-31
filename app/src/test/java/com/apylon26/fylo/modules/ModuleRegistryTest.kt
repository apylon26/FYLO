package com.apylon26.fylo.modules

import org.junit.Assert.*
import org.junit.Test

class ModuleRegistryTest {
    @Test fun everyModuleHasAUniqueStableIdAndCanBeResolved() {
        val modules = ModuleRegistry.modules
        assertEquals(ModuleId.entries.toSet(), modules.map { it.id }.toSet())
        assertEquals(modules.size, modules.map { it.id }.distinct().size)
        modules.forEach { assertEquals(it, ModuleRegistry.find(it.id)) }
    }
}
