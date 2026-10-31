package com.apylon26.fylo.notes

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.util.AtomicFile
import android.webkit.MimeTypeMap
import java.util.Locale
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

class AtomicNoteDisk(file: File) : NoteDisk {
    private val atomic = AtomicFile(file)
    override fun read(): String? {
        if (!atomic.baseFile.exists() && !File(atomic.baseFile.path + ".bak").exists()) return null
        return atomic.readFully().toString(Charsets.UTF_8)
    }
    override fun write(text: String) {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val stream = atomic.startWrite()
        try {
            stream.write(bytes)
            atomic.finishWrite(stream)
            check(atomic.readFully().contentEquals(bytes)) { "Die Notizen konnten nicht gespeichert werden." }
        } catch (error: Exception) { atomic.failWrite(stream); throw error }
    }
}

class NoteFiles(private val context: Context) {
    val repository = NoteRepository(AtomicNoteDisk(File(context.filesDir, "notes-v1.json")))
    private val directory = File(context.filesDir, "attachments")
    fun file(attachment: Attachment): File {
        require(NoteCodec.isId(attachment.id)) { "Ungültige Dateikennung." }
        return File(directory, attachment.id)
    }
    fun import(uri: Uri): Attachment {
        check(directory.isDirectory || directory.mkdirs()) { "Der Anhangordner konnte nicht angelegt werden." }
        val resolver = context.contentResolver
        var name = "Datei"
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) name = cursor.getString(0)?.takeIf { it.isNotBlank() }?.take(255) ?: "Datei"
        }
        val mime = (resolver.getType(uri) ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase(Locale.ROOT)))?.takeIf { it.matches(Regex("[A-Za-z0-9.+-]+/[A-Za-z0-9.+-]+")) } ?: "application/octet-stream"
        val id = UUID.randomUUID().toString()
        val target = File(directory, id)
        val temporary = File(directory, "$id.part")
        var size = 0L
        try {
            resolver.openInputStream(uri).use { input ->
                requireNotNull(input) { "Die Datei konnte nicht geöffnet werden." }
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        size += count
                        require(size <= 512L * 1024 * 1024) { "Anhänge dürfen höchstens 512 MB groß sein." }
                        output.write(buffer, 0, count)
                    }
                    output.fd.sync()
                }
            }
            check(temporary.renameTo(target)) { "Der Anhang konnte nicht übernommen werden." }
            return Attachment(id, name, mime, size)
        } finally { temporary.delete() }
    }
    fun save(note: Note): List<Note> {
        require(note.attachments.all { file(it).isFile }) { "Ein Anhang fehlt. Bitte entfernen oder erneut hinzufügen." }
        return repository.save(note)
    }
    fun cleanup() {
        // Never clean up if the saved note index cannot be decoded.
        AttachmentCleanup.unreferenced(directory, repository.list()).forEach { it.delete() }
    }
    fun open(attachment: Attachment) {
        val source = file(attachment)
        require(source.isFile) { "Diese Anhangdatei ist nicht mehr vorhanden." }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", source, attachment.name)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, attachment.mime)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
