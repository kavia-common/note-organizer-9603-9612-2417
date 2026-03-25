package com.kavia.noteorganizer.domain

/**
 * Domain model observed by UI and used by ViewModels.
 */
data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val deleted: Boolean,
    val dirty: Boolean,
)
