package com.apylon26.fylo.feature.notes

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.apylon26.fylo.data.FyloStore
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
fun NotesPanel(store: FyloStore, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val files = remember { NoteFiles(context.applicationContext) }
    var selected by rememberSaveable { mutableStateOf<String?>(null) }
    var title by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    var draftJson by rememberSaveable { mutableStateOf("[]") }
    val attachments = remember(draftJson) { NoteCodec.decode(draftJson, "").firstOrNull()?.attachments ?: emptyList() }
    val original = store.notes.firstOrNull { it.id == selected }
    val dirty = selected != null && (title != (original?.title ?: "") || text != (original?.text ?: "") || attachments != (original?.attachments ?: emptyList<NoteAttachment>()))
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var askDiscard by remember { mutableStateOf(false) }
    var closeModule by remember { mutableStateOf(false) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    fun close(module: Boolean) {
        if (busy) return
        closeModule = module
        if (dirty) askDiscard = true else if (module || selected == null) onDismiss() else selected = null
    }
    fun edit(note: Note) {
        selected = note.id; title = note.title; text = note.text
        draftJson = NoteCodec.encode(listOf(note)); notice = null; error = null
    }
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                try {
                    context.contentResolver.takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                    files.prepare(uri.toString())
                    store.configureFilesTree(uri.toString())
                    notice = "Ablage FYLO/Dateien ist eingerichtet."
                } catch (_: Exception) { error = "Ablage nicht verfügbar. Bitte einen beschreibbaren Ordner auswählen." }
                finally { busy = false }
            }
        }
    }
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val tree = store.filesTree
        val noteId = selected
        if (uris.isNotEmpty() && tree != null && noteId != null) {
            busy = true
            scope.launch {
                val imported = attachments.toMutableList()
                try {
                    for (uri in uris) {
                        imported.add(files.importFile(tree, noteId, uri))
                        // Preserve completed imports even if a later file fails.
                        draftJson = NoteCodec.encode(listOf(Note(noteId, title, text, imported.toList())))
                    }
                    notice = "Dateien hinzugefügt. Zum Übernehmen blauen Haken drücken."
                } catch (_: Exception) { error = "Datei konnte nicht kopiert werden. Ablage und freien Speicher prüfen; bei Bedarf Ablage neu wählen." }
                finally { busy = false }
            }
        }
    }
    Dialog(onDismissRequest = { close(true) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (selected == null) "Notizen" else "Notiz", Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                    if (selected != null) NoteAction(true, !busy) {
                        scope.launch {
                            busy = true
                            try {
                                store.saveNote(Note(checkNotNull(selected), title.trim().ifBlank { "Ohne Titel" }, text, attachments))
                                title = title.trim().ifBlank { "Ohne Titel" }
                                notice = "Gespeichert"
                            } catch (_: Exception) { error = "Speichern fehlgeschlagen. Bitte erneut versuchen." }
                            finally { busy = false }
                        }
                    }
                    NoteAction(false, !busy) { close(false) }
                }
                store.notesError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (selected == null) {
                    Button(enabled = store.notesError == null, onClick = { edit(Note(UUID.randomUUID().toString(), "", "")) }) { Text("Neue Notiz") }
                    if (store.notes.isEmpty() && store.notesError == null) Text("Noch keine Notizen.")
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(store.notes, key = { it.id }) { note ->
                            OutlinedCard(onClick = { edit(note) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(note.title, style = MaterialTheme.typography.titleMedium)
                                    Text(note.text.take(100), maxLines = 2)
                                    Text("${note.attachments.size} Anhänge", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                } else {
                    OutlinedTextField(title, { title = it; notice = null }, enabled = !busy, label = { Text("Titel") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(text, { text = it; notice = null }, enabled = !busy, label = { Text("Notiztext") }, modifier = Modifier.fillMaxWidth().weight(1f))
                    Text("Anhänge · FYLO/Dateien", style = MaterialTheme.typography.titleSmall)
                    if (attachments.isNotEmpty()) LazyColumn(Modifier.heightIn(max = 140.dp)) {
                        items(attachments, key = { it.uri }) { attachment ->
                            TextButton(enabled = !busy, onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(attachment.uri), attachment.mime)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                                } catch (_: Exception) { error = "Datei nicht verfügbar oder keine passende App zum Öffnen installiert." }
                            }) { Text(attachment.name, maxLines = 2) }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = !busy, onClick = { folderPicker.launch(null) }) {
                            Text(if (store.filesTree == null) "Ablage einrichten" else "Ablage ändern")
                        }
                        Button(enabled = !busy && store.filesTree != null, onClick = { filePicker.launch(arrayOf("*/*")) }) { Text("Datei +") }
                    }
                    if (store.filesTree == null) Text("Wähle einen Ordner auf deinem Handy. FYLO/Dateien wird darin angelegt.", style = MaterialTheme.typography.bodySmall)
                    if (dirty) Text("Ungespeicherte Änderungen", style = MaterialTheme.typography.labelMedium)
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (askDiscard) AlertDialog(onDismissRequest = { askDiscard = false }, title = { Text("Änderungen verwerfen?") },
            text = { Text("Notiztext und neue Anhänge wurden noch nicht gespeichert.") },
            confirmButton = { TextButton(onClick = {
                askDiscard = false; busy = true
                scope.launch {
                    files.discard(attachments.filter { a -> original?.attachments?.any { it.uri == a.uri } != true })
                    busy = false
                    if (closeModule) onDismiss() else selected = null
                }
            }) { Text("Verwerfen") } },
            dismissButton = { TextButton(onClick = { askDiscard = false }) { Text("Weiter bearbeiten") } })
    }
}

@Composable
private fun NoteAction(save: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val color = if (save) Color(0xFF269BFF) else Color(0xFFFF4C5B)
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)
        .semantics { contentDescription = if (save) "Notiz speichern" else "Schließen" }) {
        Canvas(Modifier.size(30.dp)) {
            val stroke = 2.5.dp.toPx()
            if (save) {
                drawCircle(color, style = Stroke(stroke))
                drawLine(color, Offset(size.width * .25f, size.height * .51f), Offset(size.width * .43f, size.height * .68f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .43f, size.height * .68f), Offset(size.width * .76f, size.height * .33f), stroke, StrokeCap.Round)
            } else {
                drawLine(color, Offset(size.width * .22f, size.height * .22f), Offset(size.width * .78f, size.height * .78f), stroke, StrokeCap.Round)
                drawLine(color, Offset(size.width * .78f, size.height * .22f), Offset(size.width * .22f, size.height * .78f), stroke, StrokeCap.Round)
            }
        }
    }
}
