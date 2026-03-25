package com.kavia.noteorganizer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavia.noteorganizer.data.repository.NotesRepository
import com.kavia.noteorganizer.domain.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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
                .stateIn(viewModelScope, SharingStarted.Eagerly, NoteEditorUiState())
        } else {
            repository.observeNote(noteId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
                .let { noteFlow ->
                    kotlinx.coroutines.flow.map(noteFlow) { note -> NoteEditorUiState(note = note) }
                        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NoteEditorUiState())
                }

    fun save(title: String, content: String, onDone: () -> Unit) {
        viewModelScope.launch {
            if (noteId == null) {
                repository.createNote(title, content)
            } else {
                repository.updateNote(noteId, title, content)
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
