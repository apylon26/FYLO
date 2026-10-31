package com.apylon26.fylo.feature.notes

import org.junit.Assert.*
import org.junit.Test

class NoteCodecTest {
    @Test fun migrationPreservesLegacyTextExactly() {
        val text = "  Bestehende Notiz\nGrüße ⚓\n"
        val migrated = NoteCodec.decode(null, text)
        assertEquals(text, migrated.single().text)
        assertEquals(migrated, NoteCodec.decode(NoteCodec.encode(migrated), "alter Inhalt"))
    }
    @Test fun emptyNewFormatDoesNotResurrectLegacyNote() {
        assertTrue(NoteCodec.decode("[]", "alte Notiz").isEmpty())
        assertTrue(NoteCodec.decode(null, "").isEmpty())
    }
    @Test fun attachmentsRemainAssociatedWithTheirOwnNote() {
        val notes = listOf(Note("a", "Titel", "Text", listOf(NoteAttachment("Plan.pdf", "content://tree/a", "application/pdf"))),
            Note("b", "Andere Notiz", "zweiter Text", listOf(NoteAttachment("Plan.pdf", "content://tree/b", "application/pdf"))))
        assertEquals(notes, NoteCodec.decode(NoteCodec.encode(notes), ""))
    }
    @Test(expected = Exception::class) fun corruptDataIsNotSilentlyReplacedByLegacy() {
        NoteCodec.decode("kaputtes JSON", "vorher")
    }
    @Test fun creationTimeSurvivesEditingAndReloading() {
        val note = Note("id", "Titel", "Text", createdAt = 1793442000000L)
        val edited = NoteCodec.decode(NoteCodec.encode(listOf(note)), "").single().copy(text = "Neu")
        assertEquals(note.createdAt, NoteCodec.decode(NoteCodec.encode(listOf(edited)), "").single().createdAt)
    }
    @Test fun oldNotesHaveUnknownCreationTime() {
        val old = """[{"id":"old","title":"Alt","text":"Bestehend","attachments":[]}]"""
        val note = NoteCodec.decode(old, "").single()
        assertNull(note.createdAt)
        assertEquals(note, NoteCodec.decode(NoteCodec.encode(listOf(note)), "" ).single())
    }
    @Test fun deletingOneNotePreservesOthersAndDoesNotResurrectLegacy() {
        val attachment = NoteAttachment("Plan.pdf", "content://files/plan", "application/pdf")
        val first = Note("a", "Gleich", "A", listOf(attachment), 1000L)
        val second = Note("b", "Gleich", "B", listOf(attachment), 2000L)
        val result = notesAfterDeleting(listOf(first, second), "a")
        assertEquals(listOf(second), NoteCodec.decode(NoteCodec.encode(result), "alter Text"))
        val empty = notesAfterDeleting(result, "b")
        assertTrue(NoteCodec.decode(NoteCodec.encode(empty), "alter Text").isEmpty())
        assertEquals(result, notesAfterDeleting(result, "unbekannt"))
    }
}
