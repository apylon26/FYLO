package com.apylon26.fylo.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.apylon26.fylo.modules.ModuleId
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import com.apylon26.fylo.feature.notes.Note
import com.apylon26.fylo.feature.notes.NoteCodec

data class Task(val id: String, val title: String, val done: Boolean)

class FyloStore(context: Context) {
    private val prefs = context.getSharedPreferences("fylo", Context.MODE_PRIVATE)
    val tasks = mutableStateListOf<Task>()
    val notes = mutableStateListOf<Note>()
    var notesError by mutableStateOf<String?>(null)
        private set
    var filesTree by mutableStateOf(prefs.getString("files_tree", null))
        private set
    var notesEnabled by mutableStateOf(prefs.getBoolean("module_NOTES", true))
        private set
    var tasksEnabled by mutableStateOf(prefs.getBoolean("module_TASKS", true))
        private set
    init {
        try {
            notes.addAll(NoteCodec.decode(prefs.getString("notes_v1", null), prefs.getString("note", "") ?: ""))
        } catch (_: Exception) {
            notesError = "Notizen konnten nicht gelesen werden. Die gespeicherten Daten bleiben erhalten."
        }
        // Existing keys and JSON schema are retained from the start project.
        val raw = runCatching { JSONArray(prefs.getString("tasks", "[]") ?: "[]") }.getOrDefault(JSONArray())
        for (i in 0 until raw.length()) {
            val item = runCatching {
                val t = raw.getJSONObject(i)
                Task(t.getString("id"), t.getString("title"), t.getBoolean("done"))
            }.getOrNull()
            if (item != null) tasks.add(item)
        }
    }
    fun enabled(id: ModuleId) = when (id) {
        ModuleId.TASKS -> tasksEnabled
        ModuleId.NOTES -> notesEnabled
    }
    fun enable(id: ModuleId, enabled: Boolean) {
        when (id) {
            ModuleId.TASKS -> tasksEnabled = enabled
            ModuleId.NOTES -> notesEnabled = enabled
        }
        prefs.edit().putBoolean("module_${id.name}", enabled).apply()
    }
    fun addTask(title: String) {
        if (title.isBlank()) return
        tasks.add(Task(UUID.randomUUID().toString(), title.trim(), false))
        saveTasks()
    }
    fun toggle(task: Task) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index >= 0) { tasks[index] = task.copy(done = !task.done); saveTasks() }
    }
    fun delete(task: Task) { tasks.remove(task); saveTasks() }
    fun saveNote(value: Note) {
        check(notesError == null) { "Die gespeicherten Notizen konnten nicht gelesen werden." }
        val updated = notes.toMutableList()
        val index = updated.indexOfFirst { it.id == value.id }
        if (index < 0) updated.add(0, value) else updated[index] = value
        check(prefs.edit().putString("notes_v1", NoteCodec.encode(updated)).commit()) { "Speichern fehlgeschlagen." }
        notes.clear()
        notes.addAll(updated)
    }
    fun configureFilesTree(uri: String) {
        check(prefs.edit().putString("files_tree", uri).commit()) { "Ablage konnte nicht gespeichert werden." }
        filesTree = uri
    }
    private fun saveTasks() {
        val raw = JSONArray()
        tasks.forEach { raw.put(JSONObject().put("id", it.id).put("title", it.title).put("done", it.done)) }
        prefs.edit().putString("tasks", raw.toString()).apply()
    }
}
