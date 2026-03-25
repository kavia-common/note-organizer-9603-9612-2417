package com.kavia.noteorganizer.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.kavia.noteorganizer.R
import com.kavia.noteorganizer.domain.Note
import java.text.DateFormat
import java.util.Date

class NotesAdapter(
    private val onClick: (Note) -> Unit,
) : ListAdapter<Note, NotesAdapter.VH>(Diff) {

    object Diff : DiffUtil.ItemCallback<Note>() {
        override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean = oldItem == newItem
    }

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val contentPreview: TextView = itemView.findViewById(R.id.contentPreview)
        val updatedAt: TextView = itemView.findViewById(R.id.updatedAt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_note, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val note = getItem(position)
        holder.title.text = note.title.ifBlank { "(untitled)" }
        holder.contentPreview.text = note.content
        holder.updatedAt.text = "Updated: " + DateFormat.getDateTimeInstance().format(Date(note.updatedAt))
        holder.itemView.setOnClickListener { onClick(note) }
    }
}
