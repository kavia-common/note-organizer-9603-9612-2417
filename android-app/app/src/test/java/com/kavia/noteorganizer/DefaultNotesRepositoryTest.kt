package com.kavia.noteorganizer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.kavia.noteorganizer.data.local.AppDatabase
import com.kavia.noteorganizer.data.remote.InMemoryNotesApi
import com.kavia.noteorganizer.data.repository.DefaultNotesRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultNotesRepositoryTest {

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
    fun create_updates_flow_and_persists() = runTest {
        repo.observeNotes(query = "").test {
            val initial = awaitItem()
            assertTrue(initial.isEmpty())

            val id = repo.createNote("t1", "c1")
            val after = awaitItem()
            assertEquals(1, after.size)
            assertEquals(id, after[0].id)
            assertEquals("t1", after[0].title)
            cancelAndIgnoreRemainingEvents()
        }

        // Verify persistence in DB by creating a new repo over same db.
        val repo2 = DefaultNotesRepository(db.noteDao(), db.syncStateDao(), api)
        repo2.observeNotes(query = "").test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("t1", list[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun delete_tombstone_removes_from_observed_list() = runTest {
        val id = repo.createNote("t", "c")

        repo.observeNotes(query = "").test {
            val list = awaitItem()
            assertEquals(1, list.size)

            repo.deleteNote(id)

            val afterDelete = awaitItem()
            assertTrue(afterDelete.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sync_marks_dirty_clean_and_uploads_to_api() = runTest {
        repo.createNote("t", "c")
        // First sync uploads.
        repo.syncNow()

        // Create a new repo/db still same; local should still have note.
        repo.observeNotes(query = "").test {
            val list = awaitItem()
            assertEquals(1, list.size)
            cancelAndIgnoreRemainingEvents()
        }

        // Remote should now return it when asked since 0.
        val remote = api.fetchNotesUpdatedSince(0L)
        assertEquals(1, remote.size)
        assertEquals("t", remote[0].title)
    }

    @Test
    fun conflict_last_write_wins_prefers_newer_updatedAt() = runTest {
        val id = repo.createNote("local", "v1")
        repo.syncNow()

        // Simulate remote update (newer).
        val remoteNow = System.currentTimeMillis() + 10_000
        api.pushNotes(
            listOf(
                com.kavia.noteorganizer.domain.Note(
                    id = id,
                    title = "remote",
                    content = "v2",
                    createdAt = remoteNow - 1,
                    updatedAt = remoteNow,
                    deleted = false,
                    dirty = false,
                ),
            ),
        )

        repo.syncNow()

        repo.observeNotes(query = "").test {
            val list = awaitItem()
            assertEquals(1, list.size)
            assertEquals("remote", list[0].title)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
