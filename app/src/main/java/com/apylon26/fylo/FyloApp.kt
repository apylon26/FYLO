package com.apylon26.fylo

import android.content.Intent
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal val Navy = Color(0xFF06132B)
internal val Blue = Color(0xFF147DE1)
internal val Orange = Color(0xFFFF9800)
internal val Pale = Color(0xFFB9CDE8)

@Composable
fun FyloApp(visible: Boolean, refresh: Int) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ScreenStores.get(context.applicationContext) }
    val mutations = remember { Mutex() }
    var model by remember { mutableStateOf<ScreenState?>(null) }
    var screenError by remember { mutableStateOf<String?>(null) }
    var modules by remember { mutableStateOf(emptyList<InstalledModule>()) }
    var reload by remember { mutableIntStateOf(0) }
    var settings by rememberSaveable { mutableStateOf(false) }
    var assistantDialog by rememberSaveable { mutableStateOf(false) }
    var reading by remember { mutableStateOf(DeviceReading()) }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    val monitor = remember { DeviceMonitor(context.applicationContext) }
    val screens = model?.screens.orEmpty()
    val pager = rememberPagerState(pageCount = { screens.size })
    fun change(operation: () -> ScreenState) {
        scope.launch {
            mutations.withLock {
                try {
                    model = withContext(Dispatchers.IO) { operation() }
                    screenError = null
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    screenError = e.message ?: "Die Screen-Daten konnten nicht gespeichert werden."
                }
            }
        }
    }
    fun launchModule(module: InstalledModule, signature: String?) {
        try {
            context.startActivity(Intent(ModulePolicy.ACTION).setComponent(module.component)
                .putExtra("com.apylon26.fylo.SCREEN_SIGNATURE", signature))
        } catch (_: Exception) { screenError = "Dieses Modul ist nicht mehr verfügbar."; reload++ }
    }
    LaunchedEffect(Unit) {
        try { model = withContext(Dispatchers.IO) { repository.read() } }
        catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            screenError = "Screen-Daten nicht lesbar. Sie werden nicht überschrieben."
        }
    }
    LaunchedEffect(refresh, reload) { modules = withContext(Dispatchers.IO) { installedModules(context) } }
    LaunchedEffect(screens.size) {
        if (screens.isNotEmpty() && pager.currentPage >= screens.size) pager.scrollToPage(screens.lastIndex)
    }
    LaunchedEffect(visible) { if (visible) monitor.observe { reading = it } }
    LaunchedEffect(visible) {
        if (visible) while (true) { now = ZonedDateTime.now(); delay(1000L - System.currentTimeMillis() % 1000L) }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Orange, background = Navy,
        surface = Color(0xFF102747), onSurface = Color.White, onBackground = Color.White)) {
        Surface(color = Navy) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10396C), Navy)))
                .windowInsetsPadding(WindowInsets.safeDrawing)) {
                val clockSize = minOf(maxWidth - 48.dp, maxHeight * 0.44f, 360.dp) / 2 * 0.75f
                Column(Modifier.fillMaxSize().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically) {
                        RoundButton("⚙", "Systemsteuerung öffnen") { settings = true }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        AnalogClock(now, Modifier.size(clockSize).alpha(0.60f))
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(now.format(DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN)), color = Pale, fontSize = 16.sp)
                            Text(now.format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)), fontSize = 20.sp, fontWeight = FontWeight.Medium)
                            Spacer(Modifier.height(12.dp))
                            Text((if (reading.charging) "Akku · lädt " else "Akku ") + (reading.battery?.let { "$it %" } ?: "—"), color = Pale, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        val assistants = modules.filter { it.isAssistant }
                        val chosen = assistants.firstOrNull { it.component.flattenToString() == model?.assistant }
                            ?: assistants.singleOrNull()
                        if (chosen != null) launchModule(chosen, screens.getOrNull(pager.currentPage)?.signature)
                        else assistantDialog = true
                    }) { Text("KI-Assistent öffnen  →") }
                    Spacer(Modifier.height(8.dp))
                    LazyRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(screens, key = { it.signature }) { screen ->
                            val selected = screens.getOrNull(pager.currentPage)?.signature == screen.signature
                            Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).clickable {
                                scope.launch { pager.animateScrollToPage(screens.indexOf(screen)) }
                            }.semantics { contentDescription = "Screen ${screen.name}, Signatur ${screen.signature}" }, contentAlignment = Alignment.Center) {
                                Box(Modifier.size(25.dp).clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) Orange else Blue)
                                    .border(1.dp, Pale, RoundedCornerShape(6.dp)))
                            }
                        }
                    }
                    screenError?.let { Text(it, color = Orange, fontSize = 12.sp) }
                    if (screens.isEmpty()) {
                        Text(if (model == null) "Screens werden geladen …" else "Noch kein Screen. Bitte in der Systemsteuerung anlegen.", color = Pale)
                    } else HorizontalPager(state = pager, key = { screens[it].signature }, modifier = Modifier.weight(1f).fillMaxWidth()) { index ->
                        val screen = screens[index]
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF102A50), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.padding(18.dp).heightIn(min = 64.dp)) {
                                    Text("Status · ${screen.name}", color = Orange, fontSize = 17.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Text("Bereit für spätere Anwendungen und Funktionen.", color = Pale, fontSize = 14.sp)
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            val assigned = model?.assignments.orEmpty().filterValues { it == screen.signature }
                            assigned.keys.forEach { id ->
                                val module = modules.firstOrNull { it.component.flattenToString() == id }
                                OutlinedButton(enabled = module != null, modifier = Modifier.fillMaxWidth(), onClick = {
                                    if (module != null) launchModule(module, screen.signature)
                                }) { Text(module?.title ?: "Modul nicht verfügbar") }
                            }
                            Spacer(Modifier.height(20.dp))
                        }
                    }
                }
                if (settings) SettingsPopup(modules, { reload++ }, model, repository, ::change, screenError, onClose = { settings = false })
                if (assistantDialog) AlertDialog(onDismissRequest = { assistantDialog = false }, title = { Text("KI-Assistent") },
                    text = {
                        Column {
                            val choices = modules.filter { it.isAssistant }
                            if (choices.isEmpty()) Text("Noch kein KI-Modul installiert. Ein kompatibles KI-Modul kann über die Systemsteuerung ergänzt werden.")
                            choices.forEach { module ->
                                TextButton(onClick = {
                                    change { repository.assistant(module.component.flattenToString()) }
                                    assistantDialog = false
                                    launchModule(module, screens.getOrNull(pager.currentPage)?.signature)
                                }) { Text(module.title) }
                            }
                        }
                    }, confirmButton = { TextButton(onClick = { assistantDialog = false; settings = true }) { Text("Systemsteuerung") } },
                    dismissButton = { TextButton(onClick = { assistantDialog = false }) { Text("Schließen") } })
            }
        }
    }
}

@Composable
private fun RoundButton(symbol: String, description: String, action: () -> Unit) {
    Box(Modifier.size(52.dp).shadow(6.dp, CircleShape).clip(CircleShape)
        .background(Brush.verticalGradient(listOf(Color(0xFF5BB6FF), Blue, Color(0xFF073D90))))
        .border(1.dp, Color(0xFF70BEFF), CircleShape)
        .clickable(onClickLabel = description, onClick = action)
        .semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Text(symbol, color = Orange, fontSize = 30.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun AnalogClock(time: ZonedDateTime, modifier: Modifier) {
    val spoken = time.format(DateTimeFormatter.ofPattern("HH:mm:ss", Locale.GERMAN))
    Canvas(modifier.semantics { contentDescription = "Uhrzeit $spoken" }) {
        val radius = size.minDimension / 2
        val center = Offset(size.width / 2, size.height / 2)
        fun point(degrees: Float, length: Float): Offset {
            val angle = Math.toRadians((degrees - 90).toDouble())
            return center + Offset((cos(angle) * length).toFloat(), (sin(angle) * length).toFloat())
        }
        drawCircle(Brush.linearGradient(listOf(Color(0xFF62B8FF), Blue, Color(0xFF073675))), radius, center)
        drawCircle(Color(0xFF071831), radius * 0.90f, center)
        drawCircle(Color(0xFF77BCF7), radius * 0.95f, center, style = Stroke(1.dp.toPx()))
        repeat(60) { tick ->
            val major = tick % 5 == 0
            drawLine(if (major) Pale else Color(0xFF496489), point(tick * 6f, radius * 0.81f),
                point(tick * 6f, radius * if (major) 0.72f else 0.77f),
                if (major) 3.dp.toPx() else 1.dp.toPx(), StrokeCap.Round)
        }
        val seconds = time.second.toFloat()
        val minutes = time.minute + seconds / 60f
        val hours = time.hour % 12 + minutes / 60f
        drawLine(Color.White, center, point(hours * 30f, radius * 0.43f), 7.dp.toPx(), StrokeCap.Round)
        drawLine(Pale, center, point(minutes * 6f, radius * 0.64f), 4.dp.toPx(), StrokeCap.Round)
        drawLine(Orange, point(seconds * 6f + 180, radius * 0.15f), point(seconds * 6f, radius * 0.70f), 2.dp.toPx(), StrokeCap.Round)
        drawCircle(Orange, 5.dp.toPx(), center)
    }
}

@Composable
private fun SettingsPopup(modules: List<InstalledModule>, onReload: () -> Unit, model: ScreenState?, repository: ScreenRepository, change: (() -> ScreenState) -> Unit, screenError: String?, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            try {
                val file = stageModule(context, uri)
                requestModuleInstall(context, file)
                message = "Nach der Android-Installation erscheint das Modul hier."
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                message = e.message ?: "Das Modul konnte nicht installiert werden."
            } finally { busy = false }
        }
    }
    Dialog(onDismissRequest = onClose) {
        Surface(shape = RoundedCornerShape(24.dp), color = Color(0xFF102747)) {
            Column(Modifier.fillMaxWidth().heightIn(max = 650.dp).verticalScroll(rememberScrollState()).padding(22.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Systemsteuerung", fontSize = 19.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    RoundButton("×", "Systemsteuerung schließen", onClose)
                }
                Spacer(Modifier.height(20.dp))
                ScreenManagement(model, modules, repository, change)
                screenError?.let { Text(it, color = Orange) }
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Text("Module", color = Orange, fontSize = 19.sp)
                Text("Erweiterungen werden separat installiert und hier geöffnet.", color = Pale)
                Spacer(Modifier.height(10.dp))
                Button(enabled = !busy, modifier = Modifier.fillMaxWidth(), onClick = {
                    try {
                        if (!context.packageManager.canRequestPackageInstalls()) {
                            message = "Installation für FYLO erlauben, dann erneut auf Modul installieren tippen."
                            context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:${context.packageName}")))
                        } else picker.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream"))
                    } catch (_: Exception) { message = "Die Android-Modulinstallation ist auf diesem Gerät nicht verfügbar." }
                }) { Text(if (busy) "Modul wird geprüft …" else "+ Modul installieren") }
                TextButton(onClick = onReload) { Text("Module aktualisieren") }
                if (modules.isEmpty()) Text("Noch keine Module installiert.", color = Pale)
                modules.forEach { module ->
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        try { context.startActivity(Intent(ModulePolicy.ACTION).setComponent(module.component)
                            .putExtra("com.apylon26.fylo.SCREEN_SIGNATURE", model?.assignments?.get(module.component.flattenToString()))) }
                        catch (_: Exception) { message = "Das Modul ist nicht mehr verfügbar."; onReload() }
                    }) { Text(module.title) }
                    if (model != null) ModulePlacement(module, model, repository, change)
                }
                message?.let { Spacer(Modifier.height(10.dp)); Text(it, color = Orange) }
                HorizontalDivider(Modifier.padding(vertical = 20.dp))
                Text("KI-Assistent", color = Orange, fontSize = 19.sp)
                AssistantSelection(modules, model, repository, change)
                Spacer(Modifier.height(16.dp))
                Text("FYLO 0.2.0 · persönliches Assistenz-Hauptprogramm", color = Pale, fontSize = 12.sp)
            }
        }
    }
}
