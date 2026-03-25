package com.kavia.noteorganizer.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kavia.noteorganizer.App
import com.kavia.noteorganizer.databinding.ActivityNotesListBinding
import kotlinx.coroutines.launch

class NotesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotesListBinding
    private lateinit var adapter: NotesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        adapter = NotesAdapter { note ->
            startActivity(
                Intent(this, NoteEditorActivity::class.java).putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.id),
            )
        }

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.fab.setOnClickListener {
            startActivity(Intent(this, NoteEditorActivity::class.java))
        }

        // Simple collection directly from repository (skeleton). In a full app, use proper ViewModelProviders.
        val repo = (application as App).repository

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repo.observeNotes(query = "").collect { notes ->
                    adapter.submitList(notes)
                }
            }
        }
    }
}
