package com.apylon26.fylo

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ScreenManagement(model: ScreenState?, modules: List<InstalledModule>, repository: ScreenRepository,
    change: (() -> ScreenState) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    Text("Screens", color = Orange, fontSize = 19.sp)
    OutlinedTextField(value = name, onValueChange = { if (it.length <= 60) name = it },
        label = { Text("Bezeichnung des neuen Screens") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Button(enabled = model != null && name.isNotBlank(), onClick = {
        val entered = name
        change { repository.add(entered) }
        name = ""
    }, modifier = Modifier.fillMaxWidth()) {
        FyloActionIcon(FyloAction.ADD, enabled = model != null && name.isNotBlank())
        Spacer(Modifier.width(8.dp))
        Text("Screen anlegen")
    }
    model?.screens?.forEach { screen ->
        val count = model.assignments.count { it.value == screen.signature }
        Text("${screen.name} · ${screen.signature}", color = Pale)
        TextButton(enabled = count == 0, onClick = { change { repository.delete(screen.signature) } }) {
            FyloActionIcon(FyloAction.DELETE, enabled = count == 0)
            Spacer(Modifier.width(8.dp))
            Text(if (count == 0) "Screen löschen" else "$count Modul(e) · Löschen gesperrt")
        }
    }
    // Missing/uninstalled modules retain their binding until explicitly removed.
    val known = modules.map { it.component.flattenToString() }.toSet()
    model?.assignments?.filterKeys { it !in known }?.forEach { (id, signature) ->
        Text("Nicht verfügbares Modul auf $signature: $id", color = Pale, fontSize = 12.sp)
        TextButton(onClick = { change { repository.assign(id, null) } }) { FyloActionIcon(FyloAction.REMOVE); Spacer(Modifier.width(8.dp)); Text("Zuordnung entfernen") }
    }
}

@Composable
internal fun ModulePlacement(module: InstalledModule, model: ScreenState, repository: ScreenRepository,
    change: (() -> ScreenState) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    val id = module.component.flattenToString()
    val selected = model.screens.firstOrNull { it.signature == model.assignments[id] }
    Box {
        TextButton(onClick = { menu = true }) { FyloActionIcon(FyloAction.OPEN); Spacer(Modifier.width(8.dp)); Text(selected?.let { "Screen: ${it.name} · ${it.signature}" } ?: "Screen zuordnen") }
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(leadingIcon = { FyloActionIcon(FyloAction.REMOVE) }, text = { Text("Keine Zuordnung") }, onClick = { menu = false; change { repository.assign(id, null) } })
            model.screens.forEach { screen ->
                DropdownMenuItem(leadingIcon = { FyloActionIcon(if (selected?.signature == screen.signature) FyloAction.CONFIRM else FyloAction.SAVE) }, text = { Text("${screen.name} · ${screen.signature}") }, onClick = {
                    menu = false
                    change { repository.assign(id, screen.signature) }
                })
            }
        }
    }
}

@Composable
internal fun AssistantSelection(modules: List<InstalledModule>, model: ScreenState?, repository: ScreenRepository,
    change: (() -> ScreenState) -> Unit) {
    val assistants = modules.filter { it.isAssistant }
    if (assistants.isEmpty()) Text("Noch kein KI-Modul installiert.", color = Pale)
    else {
        TextButton(enabled = model != null, onClick = { change { repository.assistant(null) } }) {
            FyloActionIcon(if (model?.assistant == null) FyloAction.CONFIRM else FyloAction.SAVE, enabled = model != null)
            Spacer(Modifier.width(8.dp))
            Text("Automatische Auswahl")
        }
        assistants.forEach { module ->
            val id = module.component.flattenToString()
            TextButton(enabled = model != null, onClick = { change { repository.assistant(id) } }) {
                FyloActionIcon(if (model?.assistant == id) FyloAction.CONFIRM else FyloAction.SAVE, enabled = model != null)
                Spacer(Modifier.width(8.dp))
                Text(module.title)
            }
        }
    }
}
