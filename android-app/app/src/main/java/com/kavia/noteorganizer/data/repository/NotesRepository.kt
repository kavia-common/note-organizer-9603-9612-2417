package com.kavia.noteorganizer.data.repository

import com.kavia.noteorganizer.domain.Note
import kotlinx.coroutines.flow.Flow

interface NotesRepository {
    fun observeNotes(query: String): Flow<List<Note>>
    fun observeNote(id: String): Flow<Note?>

    suspend fun createNote(title: String, content: String): String
    suspend fun updateNote(id: String, title: String, content: String)
    suspend fun deleteNote(id: String)

    suspend fun syncNow()
}
