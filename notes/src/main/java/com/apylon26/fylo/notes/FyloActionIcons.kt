package com.apylon26.fylo.notes

import android.content.res.Resources
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

internal enum class FyloAction(@DrawableRes val image: Int, val description: String) {
    CONFIRM(R.drawable.fylo_confirm, "Bestätigt"),
    CANCEL(R.drawable.fylo_cancel, "Abbrechen"),
    NOTICE(R.drawable.fylo_notice, "Wichtige Meldung"),
    ADD(R.drawable.fylo_add, "Hinzufügen"),
    REMOVE(R.drawable.fylo_remove, "Zuordnung entfernen"),
    OPEN(R.drawable.fylo_open, "Öffnen"),
    DELETE(R.drawable.fylo_delete, "Löschen"),
    SAVE(R.drawable.fylo_save, "Speichern"),
    CLOSE(R.drawable.fylo_close, "Schließen")
}

private object FyloIconCache {
    private val images = mutableMapOf<Int, ImageBitmap>()
    @Synchronized fun get(resources: Resources, @DrawableRes id: Int): ImageBitmap = images.getOrPut(id) {
        // Keep the original generated PNG assets; decode sampled copies for toolbar-size rendering.
        BitmapFactory.decodeResource(resources, id, BitmapFactory.Options().apply {
            inSampleSize = 8
            inScaled = false
        }).asImageBitmap()
    }
}

@Composable
internal fun FyloActionIcon(action: FyloAction, modifier: Modifier = Modifier.size(28.dp),
    enabled: Boolean = true, describe: Boolean = false) {
    val resources = LocalContext.current.resources
    val bitmap = remember(action, resources) { FyloIconCache.get(resources, action.image) }
    Image(bitmap, contentDescription = if (describe) action.description else null,
        modifier = modifier, alpha = if (enabled) 1f else 0.35f)
}

@Composable
internal fun FyloIconButton(action: FyloAction, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.size(52.dp)) {
        FyloActionIcon(action, Modifier.fillMaxSize(), describe = true)
    }
}
