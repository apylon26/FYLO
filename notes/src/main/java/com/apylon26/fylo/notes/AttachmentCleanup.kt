package com.apylon26.fylo.notes

import java.io.File

object AttachmentCleanup {
    fun unreferenced(directory: File, notes: List<Note>): List<File> {
        val keep = NoteCodec.validate(notes).flatMap { it.attachments }.map { it.id }.toSet()
        return directory.listFiles()?.filter { it.isFile && it.name !in keep }.orEmpty()
    }
}
