package com.kavia.noteorganizer.ui

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.kavia.noteorganizer.App
import com.kavia.noteorganizer.R
import com.kavia.noteorganizer.data.sync.SyncStatus
import com.kavia.noteorganizer.databinding.ActivityNotesListBinding
import com.kavia.noteorganizer.presentation.NotesListViewModel
import com.kavia.noteorganizer.presentation.ViewModelFactories
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class NotesListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotesListBinding
    private lateinit var adapter: NotesAdapter

    private val viewModel: NotesListViewModel by viewModels {
        ViewModelFactories.notesList((application as App).repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotesListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        adapter = NotesAdapter { note ->
            startActivity(
                Intent(this, NoteEditorActivity::class.java)
                    .putExtra(NoteEditorActivity.EXTRA_NOTE_ID, note.id),
            )
        }

        binding.recycler.layoutManager = LinearLayoutManager(this)
        binding.recycler.adapter = adapter

        binding.fab.setOnClickListener {
            startActivity(Intent(this, NoteEditorActivity::class.java))
        }

        // Search -> ViewModel query.
        binding.searchInput.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
                override fun afterTextChanged(s: Editable?) {
                    viewModel.setQuery(s?.toString().orEmpty())
                }
            },
        )

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.notes)

                    val isEmpty = state.notes.isEmpty()
                    binding.emptyState.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
                    binding.recycler.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE

                    // Show sync status as toolbar subtitle.
                    binding.toolbar.subtitle = when (val s = state.syncStatus) {
                        is SyncStatus.Idle -> ""
                        is SyncStatus.Running -> getString(R.string.sync_status_running)
                        is SyncStatus.Success -> getString(
                            R.string.sync_status_success,
                            DateFormat.getDateTimeInstance().format(Date(s.finishedAtEpochMillis)),
                        )
                        is SyncStatus.Error -> getString(R.string.sync_status_error, s.message)
                    }
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_notes_list, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sync -> {
                viewModel.syncNow(this)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
