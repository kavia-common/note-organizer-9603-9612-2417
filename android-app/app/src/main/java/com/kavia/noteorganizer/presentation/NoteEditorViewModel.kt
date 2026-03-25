package com.kavia.noteorganizer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavia.noteorganizer.data.repository.NotesRepository
import com.kavia.noteorganizer.domain.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val note: Note? = null,
)

class NoteEditorViewModel(
    private val repository: NotesRepository,
    private val noteId: String?,
) : ViewModel() {

    val uiState: StateFlow<NoteEditorUiState> =
        if (noteId == null) {
            kotlinx.coroutines.flow.flowOf(NoteEditorUiState(note = null))
                .stateIn(viewModelScope, SharingStarted.Eagerly, NoteEditorUiState(note = null))
        } else {
            repository.observeNote(noteId)
                .map { note -> NoteEditorUiState(note = note) }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteEditorUiState(note = null))
        }

    fun save(title: String, content: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val normalizedTitle = title.trim()
            val normalizedContent = content.trim()

            if (noteId == null) {
                repository.createNote(
                    title = normalizedTitle,
                    content = normalizedContent,
                )
            } else {
                repository.updateNote(
                    id = noteId,
                    title = normalizedTitle,
                    content = normalizedContent,
                )
            }
            onDone()
        }
    }

    fun delete(onDone: () -> Unit) {
        if (noteId == null) {
            onDone()
            return
        }
        viewModelScope.launch {
            repository.deleteNote(noteId)
            onDone()
        }
    }
}
