package com.apylon26.fylo.notes

import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

data class Attachment(val id: String, val name: String, val mime: String, val size: Long)
data class Note(val id: String, val title: String, val text: String, val createdAt: Long,
    val updatedAt: Long, val attachments: List<Attachment> = emptyList())

object NoteCodec {
    fun isId(value: String) = runCatching { UUID.fromString(value).toString() == value }.getOrDefault(false)
    fun validate(notes: List<Note>): List<Note> {
        require(notes.map { it.id }.distinct().size == notes.size) { "Doppelte Notizkennung." }
        notes.forEach { note ->
            require(isId(note.id) && note.createdAt > 0 && note.updatedAt >= note.createdAt) { "Ungültige Notizdaten." }
            require(note.title.length <= 200) { "Der Titel darf höchstens 200 Zeichen enthalten." }
            require(note.title.isNotBlank() || note.text.isNotBlank() || note.attachments.isNotEmpty()) { "Bitte Text oder einen Anhang hinzufügen." }
            require(note.attachments.map { it.id }.distinct().size == note.attachments.size) { "Doppelter Anhang." }
            note.attachments.forEach {
                require(isId(it.id) && it.name.isNotBlank() && it.name.length <= 255 && it.size >= 0 &&
                    it.mime.matches(Regex("[A-Za-z0-9.+-]+/[A-Za-z0-9.+-]+"))) { "Ungültiger Anhang." }
            }
        }
        return notes
    }
    fun encode(notes: List<Note>): String {
        validate(notes)
        return JSONObject().put("version", 1).put("notes", JSONArray().apply {
            notes.forEach { note -> put(JSONObject().put("id", note.id).put("title", note.title).put("text", note.text)
                .put("createdAt", note.createdAt).put("updatedAt", note.updatedAt)
                .put("attachments", JSONArray().apply { note.attachments.forEach { file ->
                    put(JSONObject().put("id", file.id).put("name", file.name).put("mime", file.mime).put("size", file.size))
                } })) }
        }).toString()
    }
    fun decode(text: String): List<Note> {
        val root = JSONObject(text)
        require(root.getInt("version") == 1) { "Unbekannte Notiz-Datenversion." }
        val notes = root.getJSONArray("notes")
        return validate((0 until notes.length()).map { index ->
            val note = notes.getJSONObject(index)
            val files = note.getJSONArray("attachments")
            Note(note.getString("id"), note.getString("title"), note.getString("text"), note.getLong("createdAt"), note.getLong("updatedAt"),
                (0 until files.length()).map { fileIndex -> files.getJSONObject(fileIndex).let {
                    Attachment(it.getString("id"), it.getString("name"), it.getString("mime"), it.getLong("size"))
                } })
        })
    }
}

interface NoteDisk { fun read(): String?; fun write(text: String) }

class NoteRepository(private val disk: NoteDisk) {
    private var state: List<Note>? = null
    @Synchronized fun list(): List<Note> {
        state?.let { return it }
        return (disk.read()?.let(NoteCodec::decode) ?: emptyList()).also { state = it }
    }
    @Synchronized fun save(note: Note): List<Note> {
        val previous = list()
        val existing = previous.firstOrNull { it.id == note.id }
        val created = existing?.createdAt ?: note.createdAt
        val stored = note.copy(createdAt = created, updatedAt = maxOf(created, System.currentTimeMillis()))
        val next = (previous.filterNot { it.id == stored.id } + stored).sortedByDescending { it.updatedAt }
        disk.write(NoteCodec.encode(next))
        state = next
        return next
    }
    @Synchronized fun delete(id: String): List<Note> {
        val previous = list()
        val next = previous.filterNot { it.id == id }
        if (next != previous) disk.write(NoteCodec.encode(next))
        state = next
        return next
    }
}
