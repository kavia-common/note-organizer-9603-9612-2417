package com.kavia.noteorganizer.data.repository

import com.kavia.noteorganizer.data.sync.SyncStatus
import com.kavia.noteorganizer.domain.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface NotesRepository {
    fun observeNotes(query: String): Flow<List<Note>>
    fun observeNote(id: String): Flow<Note?>

    suspend fun createNote(title: String, content: String): String
    suspend fun updateNote(id: String, title: String, content: String)
    suspend fun deleteNote(id: String)

    /**
     * Low-level sync entrypoint used by background workers.
     */
    suspend fun syncNow()

    /**
     * PUBLIC_INTERFACE
     * Manual sync entrypoint intended for UI.
     * Updates observeSyncStatus() while running.
     */
    suspend fun triggerManualSync()

    /**
     * PUBLIC_INTERFACE
     * Observable sync status for UI.
     */
    fun observeSyncStatus(): StateFlow<SyncStatus>
}
