package com.kavia.noteorganizer.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kavia.noteorganizer.App

/**
 * WorkManager worker that runs notes sync when network is available.
 *
 * This is "client-only" in the sense that the app syncs against an in-memory mock backend (NotesApi).
 */
class NotesSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as App
        return try {
            app.repository.syncNow()
            Result.success()
        } catch (t: Throwable) {
            // WorkManager will honor backoff & retry policies when we return retry().
            Result.retry()
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME_PERIODIC = "notes_sync_periodic"
        const val UNIQUE_WORK_NAME_ONE_OFF = "notes_sync_one_off"
    }
}
