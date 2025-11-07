package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.Note
import com.example.myapplication.data.NoteDatabase
import com.example.myapplication.notification.NotificationScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class NoteViewModel(application: Application) : AndroidViewModel(application) {
    private val noteDao = NoteDatabase.getDatabase(application).noteDao()
    private val notificationScheduler = NotificationScheduler(application)

    val allNotes: Flow<List<Note>> = noteDao.getAllNotes()

    fun getNotesForDateRange(startTime: Long, endTime: Long): Flow<List<Note>> {
        return noteDao.getNotesForDateRange(startTime, endTime)
    }

    fun insertNote(note: Note) {
        viewModelScope.launch {
            val noteId = noteDao.insertNote(note)
            // Schedule notification 10 minutes before the scheduled time
            if (note.scheduledDateTime > System.currentTimeMillis()) {
                val notificationTime = note.scheduledDateTime - (10 * 60 * 1000) // 10 minutes in milliseconds
                if (notificationTime > System.currentTimeMillis()) {
                    notificationScheduler.scheduleNotification(
                        note.copy(id = noteId),
                        notificationTime
                    )
                }
            }
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note)
            // Reschedule notification
            if (note.scheduledDateTime > System.currentTimeMillis()) {
                notificationScheduler.cancelNotification(note.id)
                val notificationTime = note.scheduledDateTime - (10 * 60 * 1000)
                if (notificationTime > System.currentTimeMillis()) {
                    notificationScheduler.scheduleNotification(note, notificationTime)
                }
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
            notificationScheduler.cancelNotification(note.id)
        }
    }
}

