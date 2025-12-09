package com.docuwallet.app.notifications

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.docuwallet.app.MainActivity
import com.docuwallet.app.R
import com.docuwallet.app.data.repository.DocumentRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters,
    private val repository: DocumentRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext Result.success()

                // 1. Obtener los documentos que vencen en los próximos 7 días
                val upcomingExpiryDocs = repository.getDocumentsExpiringSoon(userId, 7)

                if (upcomingExpiryDocs.isNotEmpty()) {
                    // 2. Si hay documentos, lanzar una notificación
                    sendNotification(upcomingExpiryDocs.size)
                }

                Result.success()
            } catch (e: Exception) {
                // Si algo falla, lo reintentamos más tarde
                Result.retry()
            }
        }
    }

    private fun sendNotification(documentCount: Int) {
        // Crear un intent para abrir la app al pulsar la notificación
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notificationTitle = if (documentCount == 1) {
            "1 documento está a punto de vencer"
        } else {
            "$documentCount documentos están a punto de vencer"
        }

        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setSmallIcon(R.drawable.ic_notification) // Asegúrate de tener este icono
            .setContentTitle(notificationTitle)
            .setContentText("Revisa tus documentos para no tener problemas.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(1, notification)
        }
    }
}