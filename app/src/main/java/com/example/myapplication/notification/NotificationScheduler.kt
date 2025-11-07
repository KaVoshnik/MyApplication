package com.example.myapplication.notification

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.example.myapplication.R
import com.example.myapplication.data.Note
import java.util.concurrent.TimeUnit

class NotificationScheduler(private val application: Application) {
    private val notificationManager =
        application.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Заметки",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Уведомления о предстоящих задачах"
        }
        notificationManager.createNotificationChannel(channel)
    }

    fun scheduleNotification(note: Note, notificationTime: Long) {
        val delay = notificationTime - System.currentTimeMillis()
        if (delay <= 0) return

        val data = Data.Builder()
            .putLong("note_id", note.id)
            .putString("note_title", note.title)
            .putString("note_description", note.description)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("note_${note.id}")
            .build()

        WorkManager.getInstance(application).enqueue(workRequest)
    }

    fun cancelNotification(noteId: Long) {
        WorkManager.getInstance(application).cancelAllWorkByTag("note_$noteId")
    }

    companion object {
        const val CHANNEL_ID = "notes_channel"
    }
}

