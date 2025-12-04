package com.docuwallet.app.domain.usecase

import android.content.Context
import com.docuwallet.app.data.repository.DocumentRepository

class SyncDocumentsUseCase(private val context: Context) {

    private val repository = DocumentRepository(context)

    suspend operator fun invoke(): Result<Int> {
        return repository.syncPendingDocuments()
    }
}