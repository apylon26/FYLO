package com.apylon26.fylo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private fun percent(value: Float?) = value?.let { String.format(Locale.GERMAN, "%.1f %%", it) } ?: "—"

@Composable
internal fun CpuRadarCard(reading: DeviceReading, visible: Boolean) {
    var phase by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(visible, reading.cpuAvailable) {
        if (visible && reading.cpuAvailable) {
            var previous = 0L
            while (true) withFrameNanos { frame ->
                if (previous != 0L) phase = (phase + (frame - previous) / 1_000_000_000f * 45f) % 360f
                previous = frame
            }
        }
    }
    val stats = reading.cpu
    val ceiling = maxOf(1f, stats.maximum ?: 1f)
    Surface(shape = RoundedCornerShape(20.dp), color = Color(0xFF102A50), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(reading.cpuSource, color = Pale, fontSize = 17.sp, modifier = Modifier.align(Alignment.Start))
            Spacer(Modifier.height(8.dp))
            Text("Aktuelle Leistung", color = Orange, fontSize = 14.sp)
            Text(percent(stats.current), color = Orange, fontSize = 23.sp, fontWeight = FontWeight.Medium)
            Canvas(Modifier.fillMaxWidth().height(130.dp).semantics {
                contentDescription = "CPU-Spinnennetz: Minimum ${percent(stats.minimum)}, aktuell ${percent(stats.current)}, Maximum ${percent(stats.maximum)}"
            }) {
                val radius = minOf(size.width * 0.45f, size.height * 0.57f)
                val center = Offset(size.width / 2, size.height * 0.64f)
                fun point(p: RadarPoint) = center + Offset(p.x * radius, p.y * radius)
                fun polygon(points: List<Offset>) = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                    close()
                }
                for (ring in 1..5) {
                    drawPath(polygon((0..2).map { point(RadarGeometry.axis(it, ring / 5f)) }),
                        Color(0xFF3D6493), style = Stroke(1.dp.toPx()))
                }
                for (axis in 0..2) {
                    val vertex = point(RadarGeometry.axis(axis))
                    drawLine(Pale.copy(alpha = 0.5f), center, vertex, 1.dp.toPx())
                }
                // Top = current, lower right = maximum, lower left = minimum.
                val values = listOf(stats.current, stats.maximum, stats.minimum)
                if (values.all { it != null }) {
                    val points = values.mapIndexed { index, value -> point(RadarGeometry.axis(index, value!! / ceiling)) }
                    val shape = polygon(points)
                    drawPath(shape, Blue.copy(alpha = 0.3f))
                    drawPath(shape, Color(0xFF6DBAFF), style = Stroke(2.dp.toPx()))
                    // Exactly three value markers: fixed minimum/maximum and the moving current marker.
                    points.drop(1).forEach { position -> drawCircle(Pale, 4.dp.toPx(), position) }
                    val marker = point(RadarGeometry.orbit(phase, (stats.current!! / ceiling) * 0.9f))
                    drawLine(Orange.copy(alpha = 0.30f), center, marker, 1.dp.toPx())
                    drawCircle(Orange.copy(alpha = 0.15f), 12.dp.toPx(), marker)
                    drawCircle(Orange, 5.dp.toPx(), marker)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text("Minimale Leistung", color = Pale, fontSize = 12.sp)
                    Text("Min-Peak: ${percent(stats.minimum)}", fontSize = 14.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Maximale Leistung", color = Pale, fontSize = 12.sp)
                    Text("Max-Peak: ${percent(stats.maximum)}", fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(if (reading.cpuAvailable) "Live · 1 s · Skala: 0–${percent(ceiling)}" else "Warte auf CPU-Messung …", color = Pale, fontSize = 12.sp)
        }
    }
}
