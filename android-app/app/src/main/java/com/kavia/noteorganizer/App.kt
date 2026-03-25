package com.kavia.noteorganizer

import android.app.Application
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

        // Schedule periodic background sync (network constrained).
        NotesSyncScheduler.schedulePeriodic(this)
    }
}
