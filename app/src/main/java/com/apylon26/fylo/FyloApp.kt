package com.apylon26.fylo

import android.content.Intent
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

private val Navy = Color(0xFF06132B)
private val Blue = Color(0xFF147DE1)
private val Orange = Color(0xFFFF9800)
private val Pale = Color(0xFFB9CDE8)

@Composable
fun FyloApp(visible: Boolean, refresh: Int) {
    val context = LocalContext.current
    var settings by rememberSaveable { mutableStateOf(false) }
    var reading by remember { mutableStateOf(DeviceReading()) }
    var now by remember { mutableStateOf(ZonedDateTime.now()) }
    val monitor = remember { DeviceMonitor(context.applicationContext) }
    LaunchedEffect(visible) {
        if (visible) monitor.observe { reading = it }
    }
    LaunchedEffect(visible) {
        if (visible) while (true) { now = ZonedDateTime.now(); delay(1000L - System.currentTimeMillis() % 1000L) }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Orange, background = Navy,
        surface = Color(0xFF102747), onSurface = Color.White, onBackground = Color.White)) {
        Surface(color = Navy) {
            BoxWithConstraints(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10396C), Navy)))
                .windowInsetsPadding(WindowInsets.safeDrawing)) {
                val clockSize = minOf(maxWidth - 48.dp, maxHeight * 0.44f, 360.dp)
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically) {
                        RoundButton("⚙", "Einstellungen öffnen") { settings = true }
                    }
                    Spacer(Modifier.height(20.dp))
                    AnalogClock(now, Modifier.size(clockSize))
                    Spacer(Modifier.height(28.dp))
                    BatteryCard(reading)
                    Spacer(Modifier.height(18.dp))
                    CpuCard(reading)
                    Spacer(Modifier.height(28.dp))
                }
                if (settings) SettingsPopup(refresh, onClose = { settings = false })
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
private fun BatteryCard(reading: DeviceReading) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF102A50), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (reading.charging) "Akku · lädt" else "Akku", color = Pale, fontSize = 17.sp)
            Text(reading.battery?.let { "$it %" } ?: "—", color = Orange, fontSize = 28.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun percent(value: Float?) = value?.let { String.format(Locale.GERMAN, "%.1f %%", it) } ?: "—"

@Composable
private fun CpuCard(reading: DeviceReading) {
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF102A50), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(reading.cpuSource, color = Pale, fontSize = 17.sp)
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Minimum" to reading.cpu.minimum, "Aktuell" to reading.cpu.current,
                    "Maximum" to reading.cpu.maximum).forEach { (label, value) ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(percent(value), color = if (label == "Aktuell") Orange else Color.White, fontSize = 20.sp)
                        Text(label, color = Pale, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Canvas(Modifier.fillMaxWidth().height(12.dp).semantics {
                contentDescription = "CPU aktuell ${percent(reading.cpu.current)}"
            }) {
                drawRoundRect(Color(0xFF06152B), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                reading.cpu.current?.let { value ->
                    drawRoundRect(Orange, size = size.copy(width = size.width * value / 100f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                }
                listOfNotNull(reading.cpu.minimum, reading.cpu.maximum).forEach { value ->
                    val x = (size.width * value / 100f).coerceIn(1.dp.toPx(), size.width - 1.dp.toPx())
                    drawLine(Pale, Offset(x, 0f), Offset(x, size.height), 2.dp.toPx())
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(if (reading.cpuAvailable) "Live · jede Sekunde" else "Warte auf CPU-Messung …", color = Pale, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SettingsPopup(refresh: Int, onClose: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var modules by remember { mutableStateOf(emptyList<InstalledModule>()) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var reload by remember { mutableStateOf(0) }
    LaunchedEffect(refresh, reload) {
        modules = withContext(Dispatchers.IO) { installedModules(context) }
    }
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
                    Text("Einstellungen", fontSize = 21.sp, fontWeight = FontWeight.Medium)
                    RoundButton("×", "Einstellungen schließen", onClose)
                }
                Spacer(Modifier.height(20.dp))
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
                TextButton(onClick = { reload++ }) { Text("Module aktualisieren") }
                if (modules.isEmpty()) Text("Noch keine Module installiert.", color = Pale)
                modules.forEach { module ->
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                        try { context.startActivity(Intent(ModulePolicy.ACTION).setComponent(module.component)) }
                        catch (_: Exception) { message = "Das Modul ist nicht mehr verfügbar."; reload++ }
                    }) { Text(module.title) }
                }
                message?.let { Spacer(Modifier.height(10.dp)); Text(it, color = Orange) }
                HorizontalDivider(Modifier.padding(vertical = 20.dp))
                Text("KI-Assistent", color = Orange, fontSize = 19.sp)
                Text("Noch kein KI-Modul installiert. Ein lokales Sprachmodell oder ein KI-Dienst kann als eigenes Modul ergänzt werden.", color = Pale)
                HorizontalDivider(Modifier.padding(vertical = 20.dp))
                Text("CPU-Anzeige", color = Orange, fontSize = 19.sp)
                Text("Die Messung aktualisiert sich jede Sekunde, solange FYLO sichtbar ist. Minimum und Maximum gelten seit dem App-Start. Wenn Android die Geräte-CPU sperrt, wird die CPU-Nutzung von FYLO angezeigt, bezogen auf alle verfügbaren Kerne. Die Quelle steht über den Werten.", color = Pale)
                Spacer(Modifier.height(16.dp))
                Text("FYLO 0.1.0 · persönliches Assistenz-Hauptprogramm", color = Pale, fontSize = 12.sp)
            }
        }
    }
}
