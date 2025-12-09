package com.docuwallet.app.notifications

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.docuwallet.app.data.repository.DocumentRepository

/**
 * Factory para crear instancias de Workers con dependencias.
 */
class WorkerFactory(private val repository: DocumentRepository) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            ReminderWorker::class.java.name -> {
                ReminderWorker(appContext, workerParameters, repository)
            }
            else -> null
        }
    }
}