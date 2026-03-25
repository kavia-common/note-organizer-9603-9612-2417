package com.kavia.noteorganizer.data.repository

import com.kavia.noteorganizer.data.local.NoteDao
import com.kavia.noteorganizer.data.local.NoteEntity
import com.kavia.noteorganizer.data.local.SyncStateDao
import com.kavia.noteorganizer.data.local.SyncStateEntity
import com.kavia.noteorganizer.data.local.toDomain
import com.kavia.noteorganizer.data.local.toEntity
import com.kavia.noteorganizer.data.remote.NotesApi
import com.kavia.noteorganizer.data.sync.SyncStatus
import com.kavia.noteorganizer.domain.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID

class DefaultNotesRepository(
    private val noteDao: NoteDao,
    private val syncStateDao: SyncStateDao,
    private val api: NotesApi,
) : NotesRepository {

    private val syncKey = "notes"

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)

    // PUBLIC_INTERFACE
    override fun observeSyncStatus(): StateFlow<SyncStatus> = _syncStatus

    override fun observeNotes(query: String): Flow<List<Note>> {
        val q = query.trim()
        return if (q.isEmpty()) {
            noteDao.observeAll().map { list -> list.map { it.toDomain() } }
        } else {
            // Escape LIKE wildcards for safe substring search.
            // DAO uses: LIKE :q ESCAPE '\', so we escape: \, %, _
            val escaped = q
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_")
            val like = "%$escaped%"
            noteDao.observeSearch(like).map { list -> list.map { it.toDomain() } }
        }
    }

    override fun observeNote(id: String): Flow<Note?> =
        noteDao.observeById(id).map { it?.toDomain() }

    override suspend fun createNote(title: String, content: String): String {
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val note = Note(
            id = id,
            title = title,
            content = content,
            createdAt = now,
            updatedAt = now,
            deleted = false,
            dirty = true,
        )
        noteDao.upsert(note.toEntity())
        return id
    }

    override suspend fun updateNote(id: String, title: String, content: String) {
        val now = System.currentTimeMillis()

        // Preserve the original createdAt when editing.
        val existing = noteDao.getById(id)
        val createdAt = existing?.createdAt ?: now

        // Updating a deleted note acts as a restore (undelete) locally.
        val note = Note(
            id = id,
            title = title,
            content = content,
            createdAt = createdAt,
            updatedAt = now,
            deleted = false,
            dirty = true,
        )
        noteDao.upsert(note.toEntity())
    }

    override suspend fun deleteNote(id: String) {
        // Tombstone delete: required for sync propagation.
        val now = System.currentTimeMillis()
        noteDao.markDeleted(id, updatedAt = now)
    }

    // PUBLIC_INTERFACE
    override suspend fun triggerManualSync() {
        /**
         * Manual trigger is used by UI. It runs sync immediately and reports status.
         * Background sync is handled by WorkManager (worker calls syncNow()).
         */
        try {
            _syncStatus.value = SyncStatus.Running
            syncNow()
            _syncStatus.value = SyncStatus.Success(System.currentTimeMillis())
        } catch (t: Throwable) {
            _syncStatus.value = SyncStatus.Error(t.message ?: "Sync failed")
            throw t
        }
    }

    override suspend fun syncNow() {
        /**
         * Client-only sync algorithm:
         * 1) Upload local dirty notes (including tombstones)
         * 2) Download remote changes since lastSyncAt
         * 3) Merge remote into local with Last-Write-Wins (updatedAt)
         * 4) Persist lastSyncAt (monotonic based on max observed updatedAt)
         * 5) Purge deleted notes locally
         */
        val lastSyncAt = syncStateDao.getLastSyncAt(syncKey) ?: 0L

        // Track the maximum updatedAt we observe from BOTH local and remote, so lastSyncAt moves forward
        // even when changes were only local (and then uploaded).
        var maxObservedUpdatedAt = lastSyncAt

        // 1) Upload local dirty notes (including tombstones).
        // Mark clean only after the remote accepted the push.
        val dirtyEntities = noteDao.getDirty()
        val dirtyLocal = dirtyEntities.map { it.toDomain() }
        if (dirtyLocal.isNotEmpty()) {
            maxObservedUpdatedAt = maxOf(maxObservedUpdatedAt, dirtyLocal.maxOf { it.updatedAt })
            api.pushNotes(dirtyLocal)

            // We assume pushNotes is atomic/accepted for all notes if it returns successfully.
            // (A richer API could return per-item ack; out of scope for this mock backend.)
            noteDao.markClean(dirtyLocal.map { it.id })
        }

        // 2) Download remote changes since last sync marker.
        val remote = api.fetchNotesUpdatedSince(lastSyncAt)

        // 3) Merge remote into local with LWW (updatedAt).
        // Only overwrite local when remote is strictly newer; otherwise keep local (and its dirty flag).
        val toUpsert: MutableList<NoteEntity> = mutableListOf()

        for (remoteNote in remote) {
            maxObservedUpdatedAt = maxOf(maxObservedUpdatedAt, remoteNote.updatedAt)

            val local = noteDao.getById(remoteNote.id)
            val shouldApplyRemote = local == null || remoteNote.updatedAt > local.updatedAt

            if (shouldApplyRemote) {
                // Important: remote copy always becomes clean locally.
                toUpsert += remoteNote.copy(dirty = false).toEntity()
            }
        }

        if (toUpsert.isNotEmpty()) {
            noteDao.upsertInTransaction(toUpsert)
        }

        // 4) Persist sync marker (monotonic).
        syncStateDao.upsert(SyncStateEntity(key = syncKey, lastSyncAt = maxObservedUpdatedAt))

        // 5) Optional local cleanup (safe because tombstones have been uploaded).
        noteDao.purgeDeleted()
    }
}
