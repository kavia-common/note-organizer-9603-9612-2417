package com.kavia.noteorganizer.data.remote

import com.kavia.noteorganizer.domain.Note

/**
 * Minimal remote API contract for sync.
 *
 * This project intentionally ships with an in-memory implementation to satisfy the
 * "client-only sync against a mock backend" requirement.
 */
interface NotesApi {
    suspend fun pushNotes(notes: List<Note>)
    suspend fun fetchNotesUpdatedSince(sinceEpochMillis: Long): List<Note>
}
