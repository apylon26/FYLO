package com.apylon26.fylo.notes

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Navy = Color(0xFF06132B)
private val Orange = Color(0xFFFF9800)
private val Pale = Color(0xFFB9CDE8)

@Composable
private fun Action(action: FyloAction, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(52.dp).semantics { contentDescription = label }) {
        FyloActionIcon(action, Modifier.fillMaxSize(), enabled = enabled)
    }
}

@Composable
fun NotesApp(model: NotesViewModel, onClose: () -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments(), model::attach)
    val note = model.draft
    BackHandler {
        if (!model.busy) { if (note == null) onClose() else model.closeEditor() }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Orange, background = Navy,
        surface = Color(0xFF102747), onSurface = Color.White, onBackground = Color.White)) {
        Surface(color = Navy) {
            Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10396C), Navy)))
                .windowInsetsPadding(WindowInsets.safeDrawing).imePadding().padding(horizontal = 16.dp)) {
                Text(if (note == null) "Notizen" else "Notiz bearbeiten", fontSize = 22.sp,
                    fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                if (note == null) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Action(FyloAction.ADD, "Neue Notiz erstellen", model.loaded && !model.busy, model::newNote)
                        Text("Neue Notiz", color = Pale, modifier = Modifier.weight(1f))
                        Action(FyloAction.CANCEL, "Notizmodul schließen", !model.busy, onClose)
                    }
                } else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Action(FyloAction.CONFIRM, "Notiz speichern", !model.busy, model::save)
                    Action(FyloAction.DELETE, "Notiz löschen", !model.busy) { model.deletePrompt = true }
                    Action(FyloAction.ADD, "Datei hinzufügen", !model.busy) { picker.launch(arrayOf("*/*")) }
                    Action(FyloAction.REMOVE, "Ausgewählten Anhang entfernen", !model.busy && model.selectedAttachment != null, model::removeAttachment)
                    Action(FyloAction.CANCEL, "Notiz schließen", !model.busy, model::closeEditor)
                }
                if (model.busy) { LinearProgressIndicator(Modifier.fillMaxWidth()); Spacer(Modifier.height(8.dp)) }
                model.error?.let {
                    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        FyloActionIcon(FyloAction.NOTICE, Modifier.size(28.dp))
                        Spacer(Modifier.width(8.dp)); Text(it, color = Orange, fontSize = 14.sp)
                    }
                }
                model.status?.let { Text(it, color = Pale, fontSize = 13.sp, modifier = Modifier.padding(vertical = 6.dp)) }
                if (note == null) {
                    if (model.notes.isEmpty() && model.loaded) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Noch keine Notizen. Mit + eine neue Notiz anlegen.", color = Pale)
                    } else LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
                        items(model.notes, key = { it.id }) { item ->
                            Surface(onClick = { model.edit(item) }, enabled = !model.busy, shape = RoundedCornerShape(16.dp), color = Color(0xFF102A50)) {
                                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                    Text(item.title.ifBlank { item.text.lineSequence().firstOrNull { it.isNotBlank() } ?: "Notiz mit Anhang" },
                                        maxLines = 1, overflow = TextOverflow.Ellipsis, color = Orange, fontSize = 18.sp)
                                    if (item.text.isNotBlank()) Text(item.text, color = Pale, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Spacer(Modifier.height(8.dp))
                                    Text(SimpleDateFormat("dd.MM.yyyy · HH:mm", Locale.GERMAN).format(Date(item.createdAt)) +
                                        " · ${item.attachments.size} Anhänge", color = Pale, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
                    OutlinedTextField(value = note.title, onValueChange = model::title, enabled = !model.busy,
                        modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text("Titel (optional)") })
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = note.text, onValueChange = model::text, enabled = !model.busy,
                        modifier = Modifier.fillMaxWidth(), minLines = 6, maxLines = 12, label = { Text("Notiz") })
                    Spacer(Modifier.height(18.dp))
                    Text("Anhänge", color = Orange, fontSize = 18.sp)
                    Text("Zum Entfernen einen Anhang auswählen. Der Dateiname öffnet die Datei.", color = Pale, fontSize = 12.sp)
                    note.attachments.forEach { file ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = model.selectedAttachment == file.id, onClick = { model.select(file.id) }, enabled = !model.busy)
                            Column(Modifier.weight(1f)) {
                                TextButton(enabled = !model.busy, onClick = { model.open(file) }) { Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                                Text(String.format(Locale.GERMAN, "%.1f KB", file.size / 1024.0), color = Pale, fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        if (model.closePrompt) AlertDialog(onDismissRequest = { model.closePrompt = false }, title = { Text("Änderungen speichern?") },
            text = { Text("Die Notiz enthält ungespeicherte Änderungen.") },
            confirmButton = { TextButton(enabled = !model.busy, onClick = model::save) { FyloActionIcon(FyloAction.CONFIRM); Spacer(Modifier.width(6.dp)); Text("Speichern") } },
            dismissButton = {
                Row {
                    TextButton(enabled = !model.busy, onClick = model::discard) { Text("Verwerfen") }
                    TextButton(enabled = !model.busy, onClick = { model.closePrompt = false }) { Text("Abbrechen") }
                }
            })
        if (model.deletePrompt) AlertDialog(onDismissRequest = { model.deletePrompt = false }, title = { Text("Notiz löschen?") },
            text = { Text("Die Notiz und ihre gespeicherten Anhangkopien werden gelöscht. Originaldateien bleiben erhalten.") },
            confirmButton = { TextButton(enabled = !model.busy, onClick = model::delete) { FyloActionIcon(FyloAction.DELETE); Spacer(Modifier.width(6.dp)); Text("Löschen") } },
            dismissButton = { TextButton(enabled = !model.busy, onClick = { model.deletePrompt = false }) { Text("Abbrechen") } })
    }
}
