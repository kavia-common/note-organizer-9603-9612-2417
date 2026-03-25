package com.kavia.noteorganizer.data.sync

/**
 * Represents the current sync state for UI and diagnostics.
 */
sealed class SyncStatus {
    data object Idle : SyncStatus()
    data object Running : SyncStatus()
    data class Success(val finishedAtEpochMillis: Long) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}
