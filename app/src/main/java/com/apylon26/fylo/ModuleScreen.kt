package com.apylon26.fylo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun StatusReportsWindow(report: String, isError: Boolean) {
    Surface(shape = RoundedCornerShape(18.dp), color = Color(0xFF102A50), modifier = Modifier.fillMaxWidth().height(108.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text("Statusberichte", color = Orange, fontSize = 17.sp)
            Spacer(Modifier.height(6.dp))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                Text(report, color = if (isError) Orange else Pale, fontSize = 14.sp)
            }
        }
    }
}

@Composable
internal fun ModuleScreen(screen: FyloScreen, assigned: Set<String>, modules: List<InstalledModule>, refresh: Int,
    onOpen: (InstalledModule) -> Unit) {
    val available = modules.associateBy { it.component.flattenToString() }
    Surface(modifier = Modifier.fillMaxSize(), color = Navy.copy(alpha = 0.4f),
        shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Blue.copy(alpha = 0.4f))) {
        if (assigned.isEmpty()) {
            Column(Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Text(screen.name, color = Pale, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                Text("Keine Module zugeordnet.", color = Pale.copy(alpha = 0.65f), fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        } else LazyVerticalGrid(columns = GridCells.Adaptive(96.dp), modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            gridItems(assigned.sortedBy { available[it]?.title ?: it }, key = { it }) { id ->
                val module = available[id]
                ModuleTile(module, refresh) { if (module != null) onOpen(module) }
            }
        }
    }
}

@Composable
private fun ModuleTile(module: InstalledModule?, refresh: Int, onOpen: () -> Unit) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(null, module?.component, refresh) {
        value = if (module == null) null else withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getActivityIcon(module.component).toBitmap(144, 144).asImageBitmap() }.getOrNull()
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(76.dp).shadow(5.dp, CircleShape).clip(CircleShape)
            .background(Brush.verticalGradient(listOf(Color(0xFF5BB6FF), Blue, Color(0xFF073D90))))
            .border(1.dp, Pale, CircleShape)
            .clickable(enabled = module != null, onClickLabel = "Modul öffnen", onClick = onOpen)
            .semantics { contentDescription = module?.title ?: "Modul nicht verfügbar" }, contentAlignment = Alignment.Center) {
            val image = icon
            if (image != null) Image(image, contentDescription = null, modifier = Modifier.size(48.dp))
            else Text(module?.title?.take(1)?.uppercase() ?: "!", color = Orange, fontSize = 28.sp)
        }
        Spacer(Modifier.height(8.dp))
        Text(module?.title ?: "Nicht verfügbar", color = Pale, fontSize = 13.sp, maxLines = 2,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}
