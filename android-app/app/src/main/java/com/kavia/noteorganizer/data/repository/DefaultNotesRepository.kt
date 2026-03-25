package com.kavia.noteorganizer.data.repository

import com.kavia.noteorganizer.data.local.NoteDao
import com.kavia.noteorganizer.data.local.NoteEntity
import com.kavia.noteorganizer.data.local.SyncStateDao
import com.kavia.noteorganizer.data.local.SyncStateEntity
import com.kavia.noteorganizer.data.local.toDomain
import com.kavia.noteorganizer.data.local.toEntity
import com.kavia.noteorganizer.data.remote.NotesApi
import com.kavia.noteorganizer.domain.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class DefaultNotesRepository(
    private val noteDao: NoteDao,
    private val syncStateDao: SyncStateDao,
    private val api: NotesApi,
) : NotesRepository {

    private val syncKey = "notes"

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

    override suspend fun syncNow() {
        // 1) Upload local dirty notes (including tombstones).
        // Mark clean only after the remote accepted the push.
        val dirtyLocal = noteDao.getDirty().map { it.toDomain() }
        if (dirtyLocal.isNotEmpty()) {
            api.pushNotes(dirtyLocal)
            noteDao.markClean(dirtyLocal.map { it.id })
        }

        // 2) Download remote changes since last sync marker.
        val lastSyncAt = syncStateDao.getLastSyncAt(syncKey) ?: 0L
        val remote = api.fetchNotesUpdatedSince(lastSyncAt)

        // 3) Merge remote into local with LWW (updatedAt).
        // Only overwrite local when remote is strictly newer; otherwise keep local (and its dirty flag).
        val toUpsert: MutableList<NoteEntity> = mutableListOf()
        var maxRemoteUpdatedAt = lastSyncAt

        for (remoteNote in remote) {
            maxRemoteUpdatedAt = maxOf(maxRemoteUpdatedAt, remoteNote.updatedAt)

            val local = noteDao.getById(remoteNote.id)
            val shouldApplyRemote = local == null || remoteNote.updatedAt > local.updatedAt

            if (shouldApplyRemote) {
                toUpsert += remoteNote.copy(dirty = false).toEntity()
            }
        }

        if (toUpsert.isNotEmpty()) {
            noteDao.upsertInTransaction(toUpsert)
        }

        // 4) Advance sync marker. Even if remote returned nothing, this keeps lastSyncAt stable.
        syncStateDao.upsert(SyncStateEntity(key = syncKey, lastSyncAt = maxRemoteUpdatedAt))

        // 5) Optional local cleanup.
        noteDao.purgeDeleted()
    }
}
