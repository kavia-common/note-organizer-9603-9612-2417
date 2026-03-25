package com.kavia.noteorganizer

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.kavia.noteorganizer.data.local.AppDatabase
import com.kavia.noteorganizer.data.remote.InMemoryNotesApi
import com.kavia.noteorganizer.data.repository.DefaultNotesRepository
import com.kavia.noteorganizer.domain.Note
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultNotesRepositorySyncAndCrudTest {

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
    fun crud_create_update_delete_are_reflected_in_observeNote_and_observeNotes() = runTest {
        val id = repo.createNote("  Title  ", "  Content  ")

        // Observe single note
        repo.observeNote(id).test {
            val note = awaitItem()
            assertNotNull(note)
            assertEquals(id, note!!.id)
            assertEquals("  Title  ", note.title) // repository doesn't trim here; ViewModel does
            assertEquals("  Content  ", note.content)
            assertTrue(note.dirty)
            assertFalse(note.deleted)
            cancelAndIgnoreRemainingEvents()
        }

        // Update
        repo.updateNote(id, "New", "Body")
        repo.observeNote(id).test {
            val updated = awaitItem()
            assertNotNull(updated)
            assertEquals("New", updated!!.title)
            assertEquals("Body", updated.content)
            assertTrue(updated.dirty)
            assertFalse(updated.deleted)
            cancelAndIgnoreRemainingEvents()
        }

        // Delete -> should disappear from list queries (DAO filters deleted=0)
        repo.deleteNote(id)
        repo.observeNotes(query = "").test {
            val list = awaitItem()
            assertTrue(list.isEmpty())
            cancelAndIgnoreRemainingEvents()
        }

        // But observeNote is not filtered by deleted; it may still exist as tombstone until purgeDeleted().
        repo.observeNote(id).test {
            val tombstone = awaitItem()
            assertNotNull(tombstone)
            assertTrue(tombstone!!.deleted)
            assertTrue(tombstone.dirty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun offline_persistence_room_keeps_data_across_repository_instances() = runTest {
        val id = repo.createNote("t1", "c1")

        val repo2 = DefaultNotesRepository(db.noteDao(), db.syncStateDao(), api)
        repo2.observeNote(id).test {
            val note = awaitItem()
            assertNotNull(note)
            assertEquals("t1", note!!.title)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun sync_uploads_dirty_items_and_marks_clean() = runTest {
        val id = repo.createNote("t", "c")

        // Before sync, it is dirty in DB.
        val before = db.noteDao().getById(id)
        assertNotNull(before)
        assertTrue(before!!.dirty)

        repo.syncNow()

        // After sync, should be clean.
        val after = db.noteDao().getById(id)
        assertNotNull(after)
        assertFalse(after!!.dirty)

        // Remote should have it.
        val remote = api.fetchNotesUpdatedSince(0L)
        assertEquals(1, remote.size)
        assertEquals(id, remote[0].id)
    }

    @Test
    fun sync_last_write_wins_does_not_overwrite_local_when_local_is_newer_than_remote() = runTest {
        val id = repo.createNote("local", "v1")
        repo.syncNow()

        // Make a newer local update but do NOT sync yet.
        repo.updateNote(id, "local_newer", "v2")

        // Push an older remote update (older updatedAt than local) directly into api.
        val localEntity = db.noteDao().getById(id)
        requireNotNull(localEntity)
        val olderRemote = Note(
            id = id,
            title = "remote_older",
            content = "should_not_apply",
            createdAt = localEntity.createdAt,
            updatedAt = localEntity.updatedAt - 1, // older than local
            deleted = false,
            dirty = false,
        )
        api.pushNotes(listOf(olderRemote))

        repo.syncNow()

        // Local should still reflect the newer local update (remote older must not overwrite).
        val final = db.noteDao().getById(id)
        requireNotNull(final)
        assertEquals("local_newer", final.title)
        assertEquals("v2", final.content)
    }

    @Test
    fun sync_tombstone_deletion_is_uploaded_and_purged_locally_and_visible_to_remote() = runTest {
        val id = repo.createNote("t", "c")

        // Upload initial creation.
        repo.syncNow()

        // Delete locally: becomes tombstone (deleted=1, dirty=1).
        repo.deleteNote(id)
        val tombstone = db.noteDao().getById(id)
        requireNotNull(tombstone)
        assertTrue(tombstone.deleted)
        assertTrue(tombstone.dirty)

        // Sync should upload tombstone and then purge deleted locally.
        repo.syncNow()

        // Purged locally.
        val afterPurge = db.noteDao().getById(id)
        assertNull(afterPurge)

        // Remote should now contain a deleted record (tombstone) with same id.
        val remoteAll = api.fetchNotesUpdatedSince(0L)
        val remote = remoteAll.firstOrNull { it.id == id }
        assertNotNull(remote)
        assertTrue(remote!!.deleted)
    }

    @Test
    fun search_escapes_like_wildcards_percent_and_underscore() = runTest {
        val id1 = repo.createNote("100% legit", "x")
        val id2 = repo.createNote("under_score", "y")
        repo.createNote("other", "z")

        // Query contains '%' should match literal '%' in title.
        repo.observeNotes(query = "%").test {
            val list = awaitItem()
            assertEquals(listOf(id1), list.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }

        // Query contains '_' should match literal '_' in title.
        repo.observeNotes(query = "_").test {
            val list = awaitItem()
            assertEquals(listOf(id2), list.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun observeNotes_is_reactive_emits_when_item_changes_to_match_filter() = runTest {
        val id1 = repo.createNote("A", "nope")
        val id2 = repo.createNote("B", "target")

        repo.observeNotes(query = "target").test {
            val initial = awaitItem()
            assertEquals(listOf(id2), initial.map { it.id })

            // Update id1 so it matches; should emit again.
            repo.updateNote(id1, "A", "now has target")
            val next = awaitItem()
            assertEquals(setOf(id1, id2), next.map { it.id }.toSet())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
