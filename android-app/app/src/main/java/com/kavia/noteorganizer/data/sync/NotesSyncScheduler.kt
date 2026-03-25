package com.kavia.noteorganizer.data.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Schedules sync work with WorkManager (network constrained).
 *
 * We keep this as a thin wrapper to avoid sprinkling WorkManager calls across UI classes.
 */
object NotesSyncScheduler {

    /**
     * PUBLIC_INTERFACE
     * Schedule periodic sync with a CONNECTED network constraint.
     *
     * Call once from Application.onCreate().
     */
    fun schedulePeriodic(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodic = PeriodicWorkRequestBuilder<NotesSyncWorker>(
            6, // hours; conservative default for demo app
            TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NotesSyncWorker.UNIQUE_WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            periodic,
        )
    }

    /**
     * PUBLIC_INTERFACE
     * Enqueue a one-off sync with CONNECTED network constraint (for manual Sync action).
     */
    fun enqueueOneOff(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = OneTimeWorkRequestBuilder<NotesSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            NotesSyncWorker.UNIQUE_WORK_NAME_ONE_OFF,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }
}
