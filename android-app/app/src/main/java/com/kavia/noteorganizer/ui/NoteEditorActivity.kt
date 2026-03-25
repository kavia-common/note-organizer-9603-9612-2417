package com.kavia.noteorganizer.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.kavia.noteorganizer.App
import com.kavia.noteorganizer.databinding.ActivityNoteEditorBinding
import com.kavia.noteorganizer.presentation.NoteEditorViewModel
import com.kavia.noteorganizer.presentation.ViewModelFactories
import kotlinx.coroutines.launch

class NoteEditorActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NOTE_ID = "extra_note_id"
    }

    private lateinit var binding: ActivityNoteEditorBinding

    private val noteId: String? by lazy { intent.getStringExtra(EXTRA_NOTE_ID) }

    private val viewModel: NoteEditorViewModel by viewModels {
        ViewModelFactories.noteEditor(
            repository = (application as App).repository,
            noteId = noteId,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }

        // Render existing note into the editor when editing.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val note = state.note

                    // Only overwrite text fields if they don't currently have focus,
                    // so the UI doesn't "fight" the user while typing.
                    if (note != null) {
                        if (!binding.titleInput.hasFocus()) {
                            val current = binding.titleInput.text?.toString().orEmpty()
                            if (current != note.title) binding.titleInput.setText(note.title)
                        }
                        if (!binding.contentInput.hasFocus()) {
                            val current = binding.contentInput.text?.toString().orEmpty()
                            if (current != note.content) binding.contentInput.setText(note.content)
                        }
                    }

                    // Hide delete button for new notes.
                    binding.deleteButton.visibility = if (noteId == null) View.GONE else View.VISIBLE
                }
            }
        }

        binding.saveButton.setOnClickListener {
            val title = binding.titleInput.text?.toString().orEmpty()
            val content = binding.contentInput.text?.toString().orEmpty()
            viewModel.save(title, content) { finish() }
        }

        binding.deleteButton.setOnClickListener {
            viewModel.delete { finish() }
        }
    }
}
