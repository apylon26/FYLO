package com.apylon26.fylo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class InstalledModule(val title: String, val component: ComponentName)

@Suppress("DEPRECATION")
fun installedModules(context: Context): List<InstalledModule> {
    val manager = context.packageManager
    return manager.queryIntentActivities(Intent(ModulePolicy.ACTION), PackageManager.GET_META_DATA)
        .filter { result ->
            val info = result.activityInfo
            ModulePolicy.accepts(info.exported, info.enabled && info.applicationInfo.enabled,
                info.metaData?.getInt(ModulePolicy.API_KEY, -1) ?: -1, !info.permission.isNullOrEmpty())
        }.map { InstalledModule(it.loadLabel(manager).toString(), ComponentName(it.activityInfo.packageName, it.activityInfo.name)) }
        .distinctBy { it.component }.sortedBy { it.title }
}

suspend fun stageModule(context: Context, uri: Uri): File = withContext(Dispatchers.IO) {
    val directory = File(context.cacheDir, "modules").apply { mkdirs() }
    val temporary = File(directory, "selected.tmp")
    val target = File(directory, "selected.apk")
    try {
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Die Datei konnte nicht geöffnet werden." }
            temporary.outputStream().use { output ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    require(total <= 512L * 1024 * 1024) { "Die Moduldatei ist größer als 512 MB." }
                    output.write(buffer, 0, read)
                }
            }
        }
        @Suppress("DEPRECATION")
        val archive = context.packageManager.getPackageArchiveInfo(temporary.absolutePath, PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA)
        require(archive != null && archive.packageName != context.packageName &&
            archive.activities?.any { it.exported && it.metaData?.getInt(ModulePolicy.API_KEY, -1) == ModulePolicy.API_VERSION } == true) {
            "Keine passende FYLO-Modul-APK (Schnittstelle 1)."
        }
        check(temporary.renameTo(target)) { "Die Moduldatei konnte nicht vorbereitet werden." }
        target
    } finally { temporary.delete() }
}

fun requestModuleInstall(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
}
