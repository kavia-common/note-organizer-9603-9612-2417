package com.kavia.noteorganizer.data.remote

import com.kavia.noteorganizer.domain.Note
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory mock backend.
 *
 * Behavior:
 * - Stores notes by id.
 * - Applies last-write-wins based on updatedAt.
 */
class InMemoryNotesApi : NotesApi {
    private val store: MutableMap<String, Note> = ConcurrentHashMap()

    override suspend fun pushNotes(notes: List<Note>) {
        for (incoming in notes) {
            val existing = store[incoming.id]
            if (existing == null || incoming.updatedAt >= existing.updatedAt) {
                store[incoming.id] = incoming.copy(dirty = false)
            }
        }
    }

    override suspend fun fetchNotesUpdatedSince(sinceEpochMillis: Long): List<Note> {
        return store.values
            .filter { it.updatedAt > sinceEpochMillis }
            .sortedByDescending { it.updatedAt }
    }
}
