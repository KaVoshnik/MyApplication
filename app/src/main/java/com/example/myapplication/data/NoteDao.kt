package com.example.myapplication.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isCompleted = 0 ORDER BY priority DESC, scheduledDateTime ASC")
    fun getAllNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isCompleted = 1 ORDER BY scheduledDateTime DESC")
    fun getCompletedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE scheduledDateTime >= :startTime AND scheduledDateTime < :endTime AND isCompleted = 0 ORDER BY priority DESC, scheduledDateTime ASC")
    fun getNotesForDateRange(startTime: Long, endTime: Long): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' ORDER BY priority DESC, scheduledDateTime ASC")
    fun searchNotes(query: String): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE category = :category AND isCompleted = 0 ORDER BY priority DESC, scheduledDateTime ASC")
    fun getNotesByCategory(category: String): Flow<List<Note>>

    @Query("SELECT DISTINCT category FROM notes")
    suspend fun getAllCategories(): List<String>

    @Query("SELECT COUNT(*) FROM notes WHERE isCompleted = 0")
    suspend fun getActiveNotesCount(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE isCompleted = 1")
    suspend fun getCompletedNotesCount(): Int

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)
}

