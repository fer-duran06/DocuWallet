package com.docuwallet.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.docuwallet.app.data.local.database.AppDatabase
import com.docuwallet.app.data.repository.DocumentRepository
import com.docuwallet.app.notifications.ReminderWorker
import com.docuwallet.app.notifications.WorkerFactory
import java.util.concurrent.TimeUnit

class DocuWalletApplication : Application(), Configuration.Provider {

    private val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { DocumentRepository(database.documentDao(), this) }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        setupRecurringWork()
    }
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(WorkerFactory(repository))
            .build()


    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                "reminder_channel",
                "Recordatorios de Vencimiento",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones para documentos a punto de vencer."
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }
    
    private fun setupRecurringWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresCharging(false)
            .build()

        val repeatingRequest = PeriodicWorkRequestBuilder<ReminderWorker>(
            1, TimeUnit.DAYS
        ).setConstraints(constraints).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "document_reminder_work",
            ExistingPeriodicWorkPolicy.KEEP,
            repeatingRequest
        )
    }
}