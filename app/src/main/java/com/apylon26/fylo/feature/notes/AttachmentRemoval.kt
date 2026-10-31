package com.apylon26.fylo.feature.notes

/** A removal only changes the draft. File deletion is deferred until the note is saved. */
data class AttachmentRemoval(val attachments: List<NoteAttachment>, val pendingDeletes: List<NoteAttachment>)

fun planAttachmentRemoval(
    attachments: List<NoteAttachment>,
    pendingDeletes: List<NoteAttachment>,
    uri: String?,
    deleteFile: Boolean
): AttachmentRemoval {
    val selected = attachments.firstOrNull { it.uri == uri }
        ?: return AttachmentRemoval(attachments, pendingDeletes)
    return AttachmentRemoval(
        attachments.filterNot { it.uri == uri },
        if (deleteFile) (pendingDeletes + selected).distinctBy { it.uri } else pendingDeletes
    )
}
