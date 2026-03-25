package com.kavia.noteorganizer.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.kavia.noteorganizer.data.repository.NotesRepository

/**
 * Simple ViewModel factories for this sample app.
 *
 * We intentionally avoid adding a DI framework; Activities can use these factories with the
 * built-in ViewModelProvider.
 */
object ViewModelFactories {

    // PUBLIC_INTERFACE
    fun notesList(repository: NotesRepository): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(NotesListViewModel::class.java)) {
                    return NotesListViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }

    // PUBLIC_INTERFACE
    fun noteEditor(repository: NotesRepository, noteId: String?): ViewModelProvider.Factory =
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(NoteEditorViewModel::class.java)) {
                    return NoteEditorViewModel(repository = repository, noteId = noteId) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
        }
}
