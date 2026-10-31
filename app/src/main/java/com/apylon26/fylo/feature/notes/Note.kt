package com.apylon26.fylo.feature.notes

import org.json.JSONArray
import org.json.JSONObject

data class NoteAttachment(val name: String, val uri: String, val mime: String)
data class Note(val id: String, val title: String, val text: String, val attachments: List<NoteAttachment> = emptyList())

object NoteCodec {
    fun encode(notes: List<Note>): String = JSONArray().apply {
        notes.forEach { note -> put(JSONObject().put("id", note.id).put("title", note.title).put("text", note.text)
            .put("attachments", JSONArray().apply { note.attachments.forEach {
                put(JSONObject().put("name", it.name).put("uri", it.uri).put("mime", it.mime))
            } })) }
    }.toString()

    fun decode(raw: String?, legacy: String): List<Note> {
        if (raw == null) return if (legacy.isEmpty()) emptyList() else listOf(Note("legacy", "Bisherige Notiz", legacy))
        val array = JSONArray(raw)
        return (0 until array.length()).map { index ->
            val item = array.getJSONObject(index)
            val attachments = item.optJSONArray("attachments") ?: JSONArray()
            Note(item.getString("id"), item.getString("title"), item.getString("text"),
                (0 until attachments.length()).map {
                    val a = attachments.getJSONObject(it)
                    NoteAttachment(a.getString("name"), a.getString("uri"), a.getString("mime"))
                })
        }
    }
}
