package com.example.myapplication.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String,
    val scheduledDateTime: Long, // Unix timestamp
    val createdAt: Long = System.currentTimeMillis(),
    val category: String = "Общее", // Категория заметки
    val priority: Int = 1, // 1 - низкий, 2 - средний, 3 - высокий
    val isCompleted: Boolean = false, // Выполнена ли задача
    val color: Int = 0 // Цвет заметки (ARGB)
)

