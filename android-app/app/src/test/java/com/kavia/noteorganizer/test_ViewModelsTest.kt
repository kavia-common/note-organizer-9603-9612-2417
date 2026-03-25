package com.kavia.noteorganizer

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.kavia.noteorganizer.data.local.AppDatabase
import com.kavia.noteorganizer.data.remote.InMemoryNotesApi
import com.kavia.noteorganizer.data.repository.DefaultNotesRepository
import com.kavia.noteorganizer.presentation.NoteEditorViewModel
import com.kavia.noteorganizer.presentation.NotesListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelsTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var db: AppDatabase
    private lateinit var repo: DefaultNotesRepository
    private lateinit var api: InMemoryNotesApi

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        api = InMemoryNotesApi()
        repo = DefaultNotesRepository(db.noteDao(), db.syncStateDao(), api)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun notesListViewModel_setQuery_filters_results_reactively() = runTest {
        val id1 = repo.createNote("Shopping", "milk")
        val id2 = repo.createNote("Work", "plan")

        val vm = NotesListViewModel(repo)

        vm.uiState.test {
            // Initial state
            val initial = awaitItem()
            assertEquals("", initial.query)

            // Set query to 'milk' should emit state with only id1
            vm.setQuery("milk")
            val afterQuery = awaitItem()
            assertEquals("milk", afterQuery.query)
            assertEquals(listOf(id1), afterQuery.notes.map { it.id })

            // Update second note so it matches query -> should update
            repo.updateNote(id2, "Work", "milk and plan")
            val afterUpdate = awaitItem()
            assertEquals(setOf(id1, id2), afterUpdate.notes.map { it.id }.toSet())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noteEditorViewModel_create_trims_input_and_persists_note() = runTest {
        val vm = NoteEditorViewModel(repository = repo, noteId = null)

        var doneCalls = 0
        vm.save("  T  ", "  C  ") { doneCalls++ }

        // Observe repo list; created note should exist with trimmed fields (ViewModel responsibility).
        repo.observeNotes(query = "").test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("T", list[0].title)
            assertEquals("C", list[0].content)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, doneCalls)
    }

    @Test
    fun noteEditorViewModel_edit_updates_existing_note_and_uiState_emits_note() = runTest {
        val id = repo.createNote("Old", "Body")
        val vm = NoteEditorViewModel(repository = repo, noteId = id)

        vm.uiState.test {
            val first = awaitItem()
            assertNotNull(first.note)
            assertEquals("Old", first.note!!.title)

            var doneCalls = 0
            vm.save("  New  ", "  Body2  ") { doneCalls++ }

            val second = awaitItem()
            assertNotNull(second.note)
            assertEquals("New", second.note!!.title)
            assertEquals("Body2", second.note!!.content)

            assertEquals(1, doneCalls)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noteEditorViewModel_delete_removes_note_and_calls_onDone() = runTest {
        val id = repo.createNote("T", "C")
        val vm = NoteEditorViewModel(repository = repo, noteId = id)

        var doneCalls = 0
        vm.delete { doneCalls++ }

        repo.observeNotes(query = "").test {
            val list = awaitItem()
            assertEquals(0, list.size)
            cancelAndIgnoreRemainingEvents()
        }

        // Still exists as tombstone until purgeDeleted; editor screen doesn't care, list is filtered.
        repo.observeNote(id).test {
            val note = awaitItem()
            assertNotNull(note)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, doneCalls)
    }

    @Test
    fun noteEditorViewModel_delete_when_creating_is_noop_and_onDone_called() = runTest {
        val vm = NoteEditorViewModel(repository = repo, noteId = null)
        var doneCalls = 0
        vm.delete { doneCalls++ }

        // No note should be created/deleted
        repo.observeNotes(query = "").test {
            val list = awaitItem()
            assertEquals(0, list.size)
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals(1, doneCalls)
    }

    @Test
    fun noteEditorViewModel_with_missing_noteId_emits_null_note() = runTest {
        val vm = NoteEditorViewModel(repository = repo, noteId = "missing")
        vm.uiState.test {
            val state = awaitItem()
            assertNull(state.note)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
