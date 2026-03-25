package com.kavia.noteorganizer.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kavia.noteorganizer.App
import com.kavia.noteorganizer.databinding.ActivityNoteEditorBinding
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NoteEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
    }

    private lateinit var binding: ActivityNoteEditorBinding
    private val scope = MainScope()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val repo = (application as App).repository
        val noteId = intent.getStringExtra(EXTRA_NOTE_ID)

        // Skeleton: no reactive load into fields; can be added later.
        binding.saveButton.setOnClickListener {
            val title = binding.titleInput.text?.toString().orEmpty()
            val content = binding.contentInput.text?.toString().orEmpty()
            if (noteId == null) {
                scope.launch { repo.createNote(title, content); finish() }
            } else {
                scope.launch { repo.updateNote(noteId, title, content); finish() }
            }
        }

        binding.deleteButton.setOnClickListener {
            if (noteId == null) {
                finish()
            } else {
                scope.launch { repo.deleteNote(noteId); finish() }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
