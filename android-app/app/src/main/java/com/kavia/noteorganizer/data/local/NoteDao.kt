package com.kavia.noteorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deleted = 0 ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    /**
     * Simple LIKE-based search across title and content.
     *
     * IMPORTANT: Repository must escape wildcard characters in the user query and pass it
     * surrounded with %...%. We also specify ESCAPE '\\' so the repository can escape %/_ safely.
     */
    @Query(
        "SELECT * FROM notes " +
            "WHERE deleted = 0 AND (title LIKE :q ESCAPE '\\' OR content LIKE :q ESCAPE '\\') " +
            "ORDER BY updatedAt DESC",
    )
    fun observeSearch(q: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<NoteEntity?>

    /**
     * Non-reactive lookup used by repository for correct update semantics (preserving createdAt,
     * and merging based on updatedAt).
     */
    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE dirty = 1")
    suspend fun getDirty(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("UPDATE notes SET deleted = 1, dirty = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun markDeleted(id: String, updatedAt: Long)

    @Query("UPDATE notes SET dirty = 0 WHERE id IN (:ids)")
    suspend fun markClean(ids: List<String>)

    @Query("DELETE FROM notes WHERE deleted = 1")
    suspend fun purgeDeleted()

    @Transaction
    suspend fun upsertInTransaction(notes: List<NoteEntity>) {
        upsertAll(notes)
    }
}
