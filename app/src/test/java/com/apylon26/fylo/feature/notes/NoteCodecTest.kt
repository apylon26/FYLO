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
}
