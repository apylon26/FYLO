package com.apylon26.fylo.feature.notes

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract as Docs
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** All file access is through the user-granted document tree, never broad storage permission. */
class NoteFiles(private val context: Context) {
    private val resolver get() = context.contentResolver
    private fun document(tree: Uri, id: String) = Docs.buildDocumentUriUsingTree(tree, id)
    private fun folder(tree: Uri, parent: Uri, name: String): Uri {
        val children = Docs.buildChildDocumentsUriUsingTree(tree, Docs.getDocumentId(parent))
        resolver.query(children, arrayOf(Docs.Document.COLUMN_DOCUMENT_ID, Docs.Document.COLUMN_DISPLAY_NAME,
            Docs.Document.COLUMN_MIME_TYPE), null, null, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == name) {
                    check(cursor.getString(2) == Docs.Document.MIME_TYPE_DIR) { "$name ist bereits eine Datei." }
                    return document(tree, cursor.getString(0))
                }
            }
        }
        return checkNotNull(Docs.createDocument(resolver, parent, Docs.Document.MIME_TYPE_DIR, name)) {
            "Ordner $name konnte nicht angelegt werden."
        }
    }
    suspend fun prepare(treeValue: String): String = withContext(Dispatchers.IO) {
        val tree = Uri.parse(treeValue)
        val root = document(tree, Docs.getTreeDocumentId(tree))
        val rootName = resolver.query(root, arrayOf(Docs.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }
        val fylo = if (rootName == "FYLO") root else folder(tree, root, "FYLO")
        folder(tree, fylo, "Dateien").toString()
    }
    suspend fun importFile(treeValue: String, noteId: String, source: Uri): NoteAttachment = withContext(Dispatchers.IO) {
        val tree = Uri.parse(treeValue)
        val files = Uri.parse(prepare(treeValue))
        val target = folder(tree, files, noteId)
        val name = resolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) it.getString(0) else null
        }?.substringAfterLast('/')?.substringAfterLast('\\')?.takeIf { it.isNotBlank() && it != "." && it != ".." } ?: "Datei"
        val mime = resolver.getType(source) ?: "application/octet-stream"
        val destination = checkNotNull(Docs.createDocument(resolver, target, mime, name)) { "Datei konnte nicht angelegt werden." }
        try {
            checkNotNull(resolver.openInputStream(source)) { "Quelldatei nicht lesbar." }.use { input ->
                checkNotNull(resolver.openOutputStream(destination, "w")) { "Zieldatei nicht beschreibbar." }.use { input.copyTo(it) }
            }
            NoteAttachment(name, destination.toString(), mime)
        } catch (error: Exception) {
            runCatching { Docs.deleteDocument(resolver, destination) }
            throw error
        }
    }
    suspend fun delete(attachment: NoteAttachment): Boolean = withContext(Dispatchers.IO) {
        runCatching { Docs.deleteDocument(resolver, Uri.parse(attachment.uri)) }.getOrDefault(false)
    }
    suspend fun discard(attachments: List<NoteAttachment>) = withContext(Dispatchers.IO) {
        attachments.forEach { runCatching { Docs.deleteDocument(resolver, Uri.parse(it.uri)) } }
    }
}
