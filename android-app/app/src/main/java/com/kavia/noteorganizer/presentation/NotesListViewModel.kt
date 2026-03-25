package com.kavia.noteorganizer.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kavia.noteorganizer.data.repository.NotesRepository
import com.kavia.noteorganizer.data.sync.NotesSyncScheduler
import com.kavia.noteorganizer.data.sync.SyncStatus
import com.kavia.noteorganizer.domain.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesListUiState(
    val query: String = "",
    val notes: List<Note> = emptyList(),
    val syncStatus: SyncStatus = SyncStatus.Idle,
)

class NotesListViewModel(
    private val repository: NotesRepository,
) : ViewModel() {

    private val queryFlow = MutableStateFlow("")

    private val notesFlow: StateFlow<List<Note>> =
        queryFlow
            .distinctUntilChanged()
            .flatMapLatest { q -> repository.observeNotes(query = q) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val syncStatusFlow: StateFlow<SyncStatus> =
        repository.observeSyncStatus()

    val uiState: StateFlow<NotesListUiState> =
        combine(queryFlow, notesFlow, syncStatusFlow) { query, notes, syncStatus ->
            NotesListUiState(query = query, notes = notes, syncStatus = syncStatus)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesListUiState())

    fun setQuery(query: String) {
        queryFlow.value = query
    }

    /**
     * PUBLIC_INTERFACE
     * Manual sync action from UI:
     * - enqueue a network-constrained WorkManager one-off sync
     * - also attempt immediate sync call so user sees result quickly (if already connected)
     */
    fun syncNow(context: Context) {
        // Ensure we have a network-constrained attempt even if user triggers while offline.
        NotesSyncScheduler.enqueueOneOff(context)

        // Also try immediate sync (will fail if offline, but status will show error).
        viewModelScope.launch {
            runCatching { repository.triggerManualSync() }
        }
    }
}
