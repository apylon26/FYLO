package com.apylon26.fylo.modules

enum class ModuleId { TASKS, NOTES }
data class FyloModule(val id: ModuleId, val title: String, val symbol: String, val description: String)

/** Compiled-in feature registry. New modules supply their own UI and use a stable ID. */
object ModuleRegistry {
    val modules = listOf(
        FyloModule(ModuleId.TASKS, "Aufgaben", "✓", "Nächste Schritte festhalten und erledigen."),
        FyloModule(ModuleId.NOTES, "Notizen", "≡", "Gedanken und Informationen lokal speichern.")
    )
    fun find(id: ModuleId): FyloModule = modules.first { it.id == id }
}
