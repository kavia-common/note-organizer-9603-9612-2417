package com.kavia.noteorganizer

import android.app.Application
import android.util.Log
import com.kavia.noteorganizer.data.local.AppDatabase
import com.kavia.noteorganizer.data.remote.InMemoryNotesApi
import com.kavia.noteorganizer.data.repository.DefaultNotesRepository
import com.kavia.noteorganizer.data.sync.NotesSyncScheduler

class App : Application() {
    lateinit var repository: DefaultNotesRepository
        private set

    override fun onCreate() {
        super.onCreate()

        val db = AppDatabase.create(this)
        val api = InMemoryNotesApi()

        repository = DefaultNotesRepository(
            noteDao = db.noteDao(),
            syncStateDao = db.syncStateDao(),
            api = api,
        )

        /**
         * WorkManager should be safe to call here in typical setups, but some emulator/device
         * environments can throw during WorkManager initialization/scheduling (e.g. missing
         * initializer, bad provider state, corrupted app state after restore).
         *
         * App must still be able to launch even if background scheduling is unavailable.
         */
        runCatching {
            // Schedule periodic background sync (network constrained).
            NotesSyncScheduler.schedulePeriodic(this)
        }.onFailure { t ->
            Log.e("App", "Failed to schedule periodic sync; continuing without it.", t)
        }
    }
}
