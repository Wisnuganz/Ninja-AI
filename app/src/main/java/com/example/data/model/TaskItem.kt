package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val subject: String = "Umum",
    val questionText: String,
    val shortAnswer: String,
    val detailedSteps: String,
    val keyFormulas: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "TEXT", // "TEXT" or "LIVE_SCREEN" or "PHOTO"
    val isFavorite: Boolean = false
)
