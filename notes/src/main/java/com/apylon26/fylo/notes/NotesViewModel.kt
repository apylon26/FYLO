package com.apylon26.fylo.notes

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    private val files = NoteFiles(application)
    var notes by mutableStateOf(emptyList<Note>())
        private set
    var draft by mutableStateOf<Note?>(null)
        private set
    private var original: Note? = null
    var selectedAttachment by mutableStateOf<String?>(null)
        private set
    var busy by mutableStateOf(true)
        private set
    var loaded by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var status by mutableStateOf<String?>(null)
        private set
    var closePrompt by mutableStateOf(false)
    var deletePrompt by mutableStateOf(false)

    init {
        viewModelScope.launch {
            try {
                notes = withContext(Dispatchers.IO) { files.repository.list().also { files.cleanup() } }
                loaded = true
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                error = "Die Notizen konnten nicht gelesen werden. Vorhandene Daten bleiben erhalten."
            } finally { busy = false }
        }
    }
    private fun operation(block: suspend () -> Unit) {
        if (busy || !loaded) return
        busy = true
        error = null
        viewModelScope.launch {
            try { block() }
            catch (e: Exception) {
                if (e is CancellationException) throw e
                error = e.message ?: "Die Aktion konnte nicht ausgeführt werden."
                closePrompt = false
                deletePrompt = false
            } finally { busy = false }
        }
    }
    fun newNote() {
        if (busy || !loaded) return
        val now = System.currentTimeMillis()
        original = null
        draft = Note(UUID.randomUUID().toString(), "", "", now, now)
        selectedAttachment = null
        status = null
        error = null
    }
    fun edit(note: Note) {
        if (busy || !loaded) return
        original = note
        draft = note
        selectedAttachment = null
        status = null
        error = null
    }
    fun title(value: String) { if (!busy) draft = draft?.copy(title = value.take(200)) }
    fun text(value: String) { if (!busy) draft = draft?.copy(text = value) }
    fun select(id: String) {
        if (!busy && draft?.attachments?.any { it.id == id } == true) selectedAttachment = if (selectedAttachment == id) null else id
    }
    fun attach(uris: List<Uri>) {
        val current = draft ?: return
        if (uris.isEmpty()) return
        operation {
            val imported = mutableListOf<Attachment>()
            var firstError: String? = null
            withContext(Dispatchers.IO) {
                uris.forEach { uri ->
                    try { imported += files.import(uri) }
                    catch (e: Exception) {
                        if (e is CancellationException) throw e
                        if (firstError == null) firstError = e.message ?: "Eine Datei konnte nicht hinzugefügt werden."
                    }
                }
            }
            draft = current.copy(attachments = current.attachments + imported)
            selectedAttachment = imported.lastOrNull()?.id ?: selectedAttachment
            error = firstError
            status = "${imported.size} Datei(en) hinzugefügt. Mit dem Haken speichern."
        }
    }
    fun removeAttachment() {
        if (busy) return
        val selected = selectedAttachment ?: return
        draft = draft?.copy(attachments = draft!!.attachments.filterNot { it.id == selected })
        selectedAttachment = null
        status = "Anhang aus der Notiz entfernt. Mit dem Haken speichern."
    }
    fun save() {
        val current = draft ?: return
        operation {
            notes = withContext(Dispatchers.IO) { files.save(current).also { files.cleanup() } }
            draft = null
            original = null
            selectedAttachment = null
            closePrompt = false
            status = "Notiz gespeichert."
        }
    }
    fun delete() {
        val current = draft ?: return
        operation {
            notes = withContext(Dispatchers.IO) { files.repository.delete(current.id).also { files.cleanup() } }
            draft = null
            original = null
            selectedAttachment = null
            deletePrompt = false
            status = "Notiz gelöscht."
        }
    }
    fun closeEditor() {
        val current = draft ?: return
        if (busy) return
        val changed = if (original == null) current.title.isNotBlank() || current.text.isNotBlank() || current.attachments.isNotEmpty()
            else current != original
        if (changed) closePrompt = true else discard()
    }
    fun discard() = operation {
        withContext(Dispatchers.IO) { files.cleanup() }
        draft = null
        original = null
        selectedAttachment = null
        closePrompt = false
        status = "Notiz geschlossen."
    }
    fun open(attachment: Attachment) {
        try { files.open(attachment) }
        catch (_: Exception) { error = "Die Datei konnte nicht geöffnet werden. Möglicherweise fehlt eine passende App." }
    }
}
