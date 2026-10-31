package com.apylon26.fylo.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.apylon26.fylo.feature.orb.FyloOrb
import com.apylon26.fylo.feature.status.StatusPanel
import com.apylon26.fylo.data.FyloStore
import com.apylon26.fylo.modules.ModuleId
import com.apylon26.fylo.modules.ModuleRegistry
import kotlinx.coroutines.launch

private val pages = listOf("Start", "Assistent", "Module", "System")

@Composable
fun FyloShell(store: FyloStore) {
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var opened by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = opened == null && pager.currentPage != 0) {
        scope.launch { pager.animateScrollToPage(0) }
    }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().background(
            Brush.verticalGradient(listOf(Color(0xFF14233E), Color(0xFF09111E)))
        )) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("FYLO", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("●  LOKAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text(pages[index], style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    when (index) {
                        0 -> HomeScreen(store, orbActive = pager.currentPage == 0 && !pager.isScrollInProgress && opened == null, onOpen = { opened = it.name }, onAssistant = { scope.launch { pager.animateScrollToPage(1) } })
                        1 -> AssistantScreen(store)
                        2 -> ModulesScreen(store)
                        else -> SystemScreen()
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.Center) {
                pages.indices.forEach { index ->
                    Box(Modifier.padding(4.dp).size(if (index == pager.currentPage) 8.dp else 6.dp)
                        .background(if (index == pager.currentPage) MaterialTheme.colorScheme.primary else Color(0xFF53617A), RoundedCornerShape(50)))
                }
            }
            NavigationBar(containerColor = Color.Transparent) {
                val symbols = listOf("⌂", "◎", "⊞", "⚙")
                pages.forEachIndexed { index, label ->
                    NavigationBarItem(selected = pager.currentPage == index,
                        onClick = { scope.launch { pager.animateScrollToPage(index) } },
                        icon = { Text(symbols[index], style = MaterialTheme.typography.titleLarge) },
                        label = { Text(label) })
                }
            }
        }
    }
    opened?.let { name ->
        val id = ModuleId.valueOf(name)
        ModuleDialog(id, store, onDismiss = { opened = null })
    }
}

@Composable
private fun InfoCard(title: String, detail: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(detail, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun HomeScreen(store: FyloStore, orbActive: Boolean, onOpen: (ModuleId) -> Unit, onAssistant: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(210.dp), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically) {
        FyloOrb(active = orbActive, modifier = Modifier.weight(1.05f).fillMaxHeight())
        StatusPanel(active = orbActive, modifier = Modifier.weight(1f).fillMaxHeight())
    }
    Text("Hallo Andreas.", style = MaterialTheme.typography.headlineSmall)
    Text("Dein Raum für Gedanken, Aufgaben und nächste Schritte.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    InfoCard("Alles im Blick", "${store.tasks.count { !it.done }} offene Aufgaben · ${ModuleRegistry.modules.count { store.enabled(it.id) }} aktive Module")
    Text("Deine Module", style = MaterialTheme.typography.titleMedium)
    val active = ModuleRegistry.modules.filter { store.enabled(it.id) }
    active.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            row.forEach { module ->
                Card(onClick = { onOpen(module.id) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp)) {
                    Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(module.symbol, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                        Text(module.title, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
    if (active.isEmpty()) Text("Aktiviere deine Werkzeuge auf der Seite Module.")
    FilledTonalButton(onClick = onAssistant, modifier = Modifier.fillMaxWidth()) { Text("Assistent öffnen") }
    Text("Nach links oder rechts wischen, um die Seite zu wechseln.", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun AssistantScreen(store: FyloStore) {
    var input by rememberSaveable { mutableStateOf("") }
    var reply by rememberSaveable { mutableStateOf("Ich kann Aufgaben lokal erfassen. Schreibe zum Beispiel: Aufgabe: Projektplan prüfen") }
    InfoCard("Der Kern von FYLO", "Lokale Befehle sind verfügbar. Eine KI für freie Gespräche wird später als eigenes Modul angebunden.")
    Text(reply, style = MaterialTheme.typography.bodyLarge)
    OutlinedTextField(value = input, onValueChange = { input = it }, label = { Text("Dein Auftrag") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
    Button(enabled = input.isNotBlank(), modifier = Modifier.fillMaxWidth(), onClick = {
        val value = input.trim()
        val task = value.substringAfter(':', "").trim()
        reply = when {
            value.startsWith("Aufgabe:", ignoreCase = true) && task.isNotBlank() && store.enabled(ModuleId.TASKS) -> {
                store.addTask(task); "Aufgabe gespeichert: $task"
            }
            value.startsWith("Aufgabe:", ignoreCase = true) && !store.enabled(ModuleId.TASKS) -> "Aktiviere zuerst das Aufgabenmodul auf der Seite Module."
            else -> "Verfügbarer Befehl: Aufgabe: dein Text. Freie KI-Gespräche sind noch nicht eingerichtet."
        }
        input = ""
    }) { Text("Auftrag ausführen") }
}

@Composable
private fun ModulesScreen(store: FyloStore) {
    Text("Erweitere FYLO Schritt für Schritt. Du kannst integrierte Module hier aktivieren oder deaktivieren.")
    ModuleRegistry.modules.forEach { module ->
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${module.symbol}  ${module.title}", style = MaterialTheme.typography.titleMedium)
                    Text(module.description, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = store.enabled(module.id), onCheckedChange = { store.enable(module.id, it) },
                    modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    InfoCard("Als Nächstes", "Kalender · Dateien · Sprache · KI\nDiese Module sind noch nicht enthalten. Neue Module werden zunächst mit einem App-Update ergänzt.")
    Text("Beim Deaktivieren bleiben die gespeicherten Daten erhalten.", style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun SystemScreen() {
    InfoCard("FYLO 0.4.0", "Eigenständige Android-App mit vier wischbaren Seiten und einem gemeinsamen Modulkern.")
    InfoCard("Deine Daten", "Aufgaben, Notizen und Moduleinstellungen bleiben lokal im privaten App-Bereich. Bei einer Deinstallation können sie verloren gehen.")
    InfoCard("Verbindungen", "Kein KI-Anbieter verbunden. Keine Anmeldung und keine API-Schlüssel erforderlich.")
    Text("FYLO ist eine eigene Haupt-App. Der Android-Startbildschirm und andere Handy-Apps bleiben separat verfügbar.", style = MaterialTheme.typography.bodyMedium)
}
