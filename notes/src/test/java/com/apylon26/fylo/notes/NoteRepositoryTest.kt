package com.apylon26.fylo.notes

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.util.UUID

class NoteRepositoryTest {
    private class Disk : NoteDisk {
        var data: String? = null
        var fail = false
        override fun read() = data
        override fun write(text: String) { check(!fail); data = text }
    }
    private fun note(text: String = "Text") = Note(UUID.randomUUID().toString(), "Titel", text, 1000, 1000)
    private fun file() = Attachment(UUID.randomUUID().toString(), "Prüfung.pdf", "application/pdf", 1234)
    @Test fun savedTextAndAttachmentsSurviveRestart() {
        val disk = Disk()
        val note = note("Änderung\nZeile 2").copy(attachments = listOf(file()))
        val saved = NoteRepository(disk).save(note)
        assertEquals(saved, NoteRepository(disk).list())
        assertEquals("Prüfung.pdf", saved.single().attachments.single().name)
    }
    @Test fun editsReplaceOneNoteAndKeepCreationTime() {
        val repo = NoteRepository(Disk())
        val first = note()
        repo.save(first)
        val edited = repo.save(first.copy(title = "Neu", createdAt = 2000))
        assertEquals(1, edited.size)
        assertEquals(1000L, edited.single().createdAt)
        assertEquals("Neu", edited.single().title)
    }
    @Test fun deleteDoesNotAffectOtherNotes() {
        val repo = NoteRepository(Disk())
        val first = note()
        val second = note("Anderer Inhalt")
        repo.save(first); repo.save(second)
        assertEquals(listOf(second.id), repo.delete(first.id).map { it.id })
    }
    @Test fun detachingFileChangesSavedMetadataOnlyAfterSave() {
        val repo = NoteRepository(Disk())
        val original = repo.save(note().copy(attachments = listOf(file()))).single()
        val draft = original.copy(attachments = emptyList())
        assertEquals(1, repo.list().single().attachments.size)
        assertTrue(repo.save(draft).single().attachments.isEmpty())
    }
    @Test fun failedSavePreservesPreviousData() {
        val disk = Disk()
        val repo = NoteRepository(disk)
        val original = repo.save(note())
        val stored = disk.data
        disk.fail = true
        assertThrows(IllegalStateException::class.java) { repo.save(original.single().copy(text = "Neu")) }
        assertEquals(original, repo.list())
        assertEquals(stored, disk.data)
    }
    @Test fun corruptDataIsNotReset() {
        val disk = Disk().apply { data = "broken" }
        assertThrows(Exception::class.java) { NoteRepository(disk).list() }
        assertEquals("broken", disk.data)
    }
    @Test fun emptyNotesAreRejectedButAttachmentOnlyNotesAreAllowed() {
        val repo = NoteRepository(Disk())
        val empty = note("").copy(title = "")
        assertThrows(IllegalArgumentException::class.java) { repo.save(empty) }
        assertEquals(1, repo.save(empty.copy(attachments = listOf(file()))).size)
    }
    @Test fun unsafeFileIdsAndDuplicateNotesAreRejected() {
        val note = note()
        assertThrows(IllegalArgumentException::class.java) { NoteCodec.encode(listOf(note, note)) }
        assertThrows(IllegalArgumentException::class.java) { NoteCodec.encode(listOf(note.copy(attachments = listOf(file().copy(id = "../original.pdf"))))) }
    }
    @Test fun cleanupPreservesOriginalsAndAttachmentsReferencedByAnyNote() {
        val root = Files.createTempDirectory("fylo-notes-test").toFile()
        try {
            val directory = root.resolve("attachments").apply { mkdir() }
            val original = root.resolve("original.pdf").apply { writeText("Original") }
            val attachment = file()
            val kept = directory.resolve(attachment.id).apply { writeText("Kopie") }
            val orphan = directory.resolve(UUID.randomUUID().toString()).apply { writeText("Unbenutzt") }
            AttachmentCleanup.unreferenced(directory, listOf(note().copy(attachments = listOf(attachment)))).forEach { it.delete() }
            assertTrue(original.exists())
            assertTrue(kept.exists())
            assertFalse(orphan.exists())
        } finally { root.deleteRecursively() }
    }
}
