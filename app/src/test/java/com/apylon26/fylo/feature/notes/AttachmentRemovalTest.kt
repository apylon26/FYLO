package com.apylon26.fylo.feature.notes

import org.junit.Assert.*
import org.junit.Test

class AttachmentRemovalTest {
    private val first = NoteAttachment("Plan.pdf", "content://fylo/one", "application/pdf")
    private val second = NoteAttachment("Plan.pdf", "content://fylo/two", "application/pdf")

    @Test fun unlinkKeepsFileAndOtherAttachmentsWithSameName() {
        val original = listOf(first, second)
        val result = planAttachmentRemoval(original, emptyList(), first.uri, false)
        assertEquals(listOf(second), result.attachments)
        assertTrue(result.pendingDeletes.isEmpty())
        assertEquals(listOf(first, second), original)
    }

    @Test fun deleteQueuesOnlySelectedCopyAndPreservesPreviousRequests() {
        val result = planAttachmentRemoval(listOf(first, second), listOf(second), first.uri, true)
        assertEquals(listOf(second), result.attachments)
        assertEquals(listOf(second, first), result.pendingDeletes)
        val repeated = planAttachmentRemoval(result.attachments, result.pendingDeletes, first.uri, true)
        assertEquals(result, repeated)
    }

    @Test fun missingSelectionCannotRemoveOrQueueAnything() {
        val original = listOf(first, second)
        assertEquals(AttachmentRemoval(original, emptyList()), planAttachmentRemoval(original, emptyList(), null, true))
        assertEquals(AttachmentRemoval(original, emptyList()), planAttachmentRemoval(original, emptyList(), "missing", true))
    }
}
