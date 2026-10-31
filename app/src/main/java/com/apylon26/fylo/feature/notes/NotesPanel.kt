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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
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
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var askDiscard by remember { mutableStateOf(false) }
    var closeModule by remember { mutableStateOf(false) }
    var activeAttachment by rememberSaveable { mutableStateOf<String?>(null) }
    var deleteConfirmation by remember { mutableStateOf(false) }
    var pendingDeleteJson by rememberSaveable { mutableStateOf("[]") }
    val pendingDeletes = remember(pendingDeleteJson) { NoteCodec.decode(pendingDeleteJson, "").firstOrNull()?.attachments ?: emptyList() }
    val dirty = selected != null && (title != (original?.title ?: "") || text != (original?.text ?: "") || attachments != (original?.attachments ?: emptyList<NoteAttachment>())) || pendingDeletes.isNotEmpty()
    fun setAttachments(values: List<NoteAttachment>) {
        draftJson = NoteCodec.encode(listOf(Note(checkNotNull(selected), title, text, values)))
    }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    fun close(module: Boolean) {
        if (busy) return
        closeModule = module
        if (dirty) askDiscard = true else if (module || selected == null) onDismiss() else selected = null
    }
    fun edit(note: Note) {
        selected = note.id; title = note.title; text = note.text
        draftJson = NoteCodec.encode(listOf(note)); notice = null; error = null
        activeAttachment = null; pendingDeleteJson = "[]"
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
                    notice = "Dateien hinzugefügt. Zum Übernehmen Speichern drücken."
                } catch (_: Exception) { error = "Datei konnte nicht kopiert werden. Ablage und freien Speicher prüfen; bei Bedarf Ablage neu wählen." }
                finally { busy = false }
            }
        }
    }
    Dialog(onDismissRequest = { close(true) }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (selected == null) "Notizen" else "Notiz", style = MaterialTheme.typography.headlineSmall)
                Surface(tonalElevation = 4.dp, shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                        NoteAction("✓", "Neue Notiz", !busy && store.notesError == null && !dirty) {
                            edit(Note(UUID.randomUUID().toString(), "", ""))
                        }
                        NoteAction("+", "Datei hinzufügen", !busy && selected != null) {
                            if (store.filesTree == null) folderPicker.launch(null) else filePicker.launch(arrayOf("*/*"))
                        }
                        NoteAction("−", "Zuordnung entfernen", !busy && activeAttachment != null) {
                            setAttachments(attachments.filterNot { it.uri == activeAttachment })
                            activeAttachment = null; notice = "Zuordnung entfernt. Zum Übernehmen speichern."
                        }
                        NoteAction("!", "Datei löschen", !busy && activeAttachment != null) { deleteConfirmation = true }
                        NoteAction("X", "Modul schließen", !busy) { close(true) }
                    }
                }
                if (selected != null) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(enabled = !busy, onClick = { close(false) }) { Text("Zur Notizliste") }
                    Button(enabled = !busy, onClick = {
                        scope.launch {
                            busy = true
                            try {
                                store.saveNote(Note(checkNotNull(selected), title.trim().ifBlank { "Ohne Titel" }, text, attachments))
                                title = title.trim().ifBlank { "Ohne Titel" }
                                val failed = pendingDeletes.filterNot { files.delete(it) }
                                pendingDeleteJson = NoteCodec.encode(listOf(Note(checkNotNull(selected), "", "", failed)))
                                notice = if (failed.isEmpty()) "Gespeichert" else "Notiz gespeichert. ${failed.size} Dateikopie(n) konnten nicht gelöscht werden. Erneut Speichern zum Wiederholen."
                            } catch (_: Exception) { error = "Speichern fehlgeschlagen. Bitte erneut versuchen." }
                            finally { busy = false }
                        }
                    }) { Text("Speichern") }
                }
                store.notesError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (selected == null) {

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
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = activeAttachment == attachment.uri, enabled = !busy,
                                    onClick = { activeAttachment = attachment.uri })
                                TextButton(enabled = !busy, modifier = Modifier.weight(1f), onClick = {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(attachment.uri), attachment.mime)
                                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                                    } catch (_: Exception) { error = "Datei nicht verfügbar oder keine passende App zum Öffnen installiert." }
                                }) { Text(attachment.name, maxLines = 2) }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(enabled = !busy, onClick = { folderPicker.launch(null) }) {
                            Text(if (store.filesTree == null) "Ablage einrichten" else "Ablage ändern")
                        }

                    }
                    if (store.filesTree == null) Text("Wähle einen Ordner auf deinem Handy. FYLO/Dateien wird darin angelegt.", style = MaterialTheme.typography.bodySmall)
                    if (dirty) Text("Ungespeicherte Änderungen", style = MaterialTheme.typography.labelMedium)
                }
                if (selected != null) {
                    if (dirty) Text("Vor einer neuen Notiz bitte speichern oder zur Liste zurückgehen.", style = MaterialTheme.typography.bodySmall)
                    if (pendingDeletes.isNotEmpty()) Text("${pendingDeletes.size} Datei(en) zum Löschen vorgemerkt.", style = MaterialTheme.typography.bodySmall)
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                notice?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        }
        if (deleteConfirmation) AlertDialog(onDismissRequest = { deleteConfirmation = false },
            title = { Text("Dateikopie löschen?") },
            text = { Text("Die ausgewählte Kopie in FYLO/Dateien wird beim Speichern endgültig gelöscht. Die ursprüngliche Quelldatei bleibt erhalten.") },
            confirmButton = { TextButton(onClick = {
                val attachment = attachments.firstOrNull { it.uri == activeAttachment }
                if (attachment != null) {
                    pendingDeleteJson = NoteCodec.encode(listOf(Note(checkNotNull(selected), "", "", pendingDeletes + attachment)))
                    setAttachments(attachments.filterNot { it.uri == attachment.uri })
                }
                activeAttachment = null; deleteConfirmation = false; notice = "Löschen vorgemerkt. Zum Übernehmen speichern."
            }) { Text("Löschen vormerken") } },
            dismissButton = { TextButton(onClick = { deleteConfirmation = false }) { Text("Abbrechen") } })
        if (askDiscard) AlertDialog(onDismissRequest = { askDiscard = false }, title = { Text("Änderungen verwerfen?") },
            text = { Text("Notiztext und neue Anhänge wurden noch nicht gespeichert.") },
            confirmButton = { TextButton(onClick = {
                askDiscard = false; busy = true
                scope.launch {
                    files.discard((attachments + pendingDeletes).filter { a -> original?.attachments?.any { it.uri == a.uri } != true })
                    pendingDeleteJson = "[]"
                    busy = false
                    if (closeModule) onDismiss() else selected = null
                }
            }) { Text("Verwerfen") } },
            dismissButton = { TextButton(onClick = { askDiscard = false }) { Text("Weiter bearbeiten") } })
    }
}

@Composable
private fun NoteAction(symbol: String, label: String, enabled: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
        IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)
            .shadow(if (enabled) 5.dp else 1.dp, CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF70CFFF), Color(0xFF167AD0), Color(0xFF06356E))), CircleShape)
            .semantics { contentDescription = label }) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(Color(0xFF90DFFF), radius = size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.dp.toPx()))
                drawArc(Color.White.copy(alpha = .35f), 205f, 95f, false,
                    topLeft = Offset(5.dp.toPx(), 5.dp.toPx()),
                    size = androidx.compose.ui.geometry.Size(size.width - 10.dp.toPx(), size.height - 10.dp.toPx()),
                    style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
            Text(symbol, color = Color(0xFFFF951D).copy(alpha = if (enabled) 1f else .4f), fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        Text(label, style = MaterialTheme.typography.labelSmall, fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else .45f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 5.dp))
    }
}
