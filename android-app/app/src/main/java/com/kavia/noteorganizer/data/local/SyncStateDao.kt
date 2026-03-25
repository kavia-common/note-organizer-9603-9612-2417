package com.kavia.noteorganizer.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncStateDao {
    @Query("SELECT lastSyncAt FROM sync_state WHERE key = :key LIMIT 1")
    suspend fun getLastSyncAt(key: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: SyncStateEntity)
}
