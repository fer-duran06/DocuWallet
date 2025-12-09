package com.docuwallet.app.domain.usecase

import android.content.Context
import com.docuwallet.app.data.local.database.AppDatabase
import com.docuwallet.app.data.repository.DocumentRepository

class SyncDocumentsUseCase(private val context: Context) {

    private val repository: DocumentRepository

    init {
        // Se obtiene la instancia de la base de datos, el DAO y se inyecta en el repositorio.
        val db = AppDatabase.getDatabase(context.applicationContext)
        val dao = db.documentDao()
        repository = DocumentRepository(dao, context.applicationContext)
    }

    suspend operator fun invoke(): Result<Int> {
        return repository.syncPendingDocuments()
    }
}
