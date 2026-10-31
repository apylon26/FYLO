package com.apylon26.fylo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.apylon26.fylo.data.FyloStore
import com.apylon26.fylo.modules.ModuleId
import com.apylon26.fylo.modules.ModuleRegistry

@Composable
fun ModuleDialog(id: ModuleId, store: FyloStore, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().safeDrawingPadding().imePadding(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(ModuleRegistry.find(id).title, style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = onDismiss) { Text("Schließen") }
                }
                when (id) {
                    ModuleId.TASKS -> {
                        var input by rememberSaveable { mutableStateOf("") }
                        OutlinedTextField(input, { input = it }, label = { Text("Neue Aufgabe") }, modifier = Modifier.fillMaxWidth())
                        Button(enabled = input.isNotBlank(), onClick = { store.addTask(input); input = "" }) { Text("Hinzufügen") }
                        if (store.tasks.isEmpty()) Text("Noch keine Aufgaben.")
                        LazyColumn(Modifier.weight(1f)) {
                            items(store.tasks, key = { it.id }) { task ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(task.done, { store.toggle(task) })
                                    Text(task.title, Modifier.weight(1f))
                                    TextButton(onClick = { store.delete(task) }) { Text("Löschen") }
                                }
                            }
                        }
                    }
                    ModuleId.NOTES -> {
                        var text by rememberSaveable { mutableStateOf(store.note) }
                        var saved by rememberSaveable { mutableStateOf(false) }
                        OutlinedTextField(text, { text = it; saved = false }, label = { Text("Deine Notizen") }, modifier = Modifier.fillMaxWidth().weight(1f))
                        Button(onClick = { store.saveNote(text); saved = true }) { Text("Speichern") }
                        if (saved) Text("Gespeichert")
                        // The explicit unsaved indicator keeps the editor's persistence visible.
                        if (text != store.note) Text("Ungespeicherte Änderungen", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
