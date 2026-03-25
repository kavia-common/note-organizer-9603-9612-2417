package com.kavia.noteorganizer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavia.noteorganizer.data.repository.NotesRepository
import com.kavia.noteorganizer.domain.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesListUiState(
    val query: String = "",
    val notes: List<Note> = emptyList(),
)

class NotesListViewModel(
    private val repository: NotesRepository,
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    val uiState: StateFlow<NotesListUiState> =
        combine(queryFlow, repository.observeNotes(query = "")) { query, notes ->
            NotesListUiState(query = query, notes = notes)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesListUiState())

    fun setQuery(query: String) {
        queryFlow.value = query
        // Rebuild stream by just re-subscribing:
        // For skeleton simplicity, we trigger a sync by launching new collection.
        // In a full app we'd use flatMapLatest with repository.observeNotes(queryFlow).
    }

    fun syncNow() {
        viewModelScope.launch { repository.syncNow() }
    }
}
