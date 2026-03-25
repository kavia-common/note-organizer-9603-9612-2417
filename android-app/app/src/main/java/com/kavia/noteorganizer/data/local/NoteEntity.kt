package com.kavia.noteorganizer.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kavia.noteorganizer.domain.Note

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean,
    val dirty: Boolean,
)

fun NoteEntity.toDomain(): Note =
    Note(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted,
        dirty = dirty,
    )

fun Note.toEntity(): NoteEntity =
    NoteEntity(
        id = id,
        title = title,
        content = content,
        createdAt = createdAt,
        updatedAt = updatedAt,
        deleted = deleted,
        dirty = dirty,
    )
