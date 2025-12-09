package com.docuwallet.app.presentacion.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.docuwallet.app.data.local.database.AppDatabase
import com.docuwallet.app.data.repository.DocumentRepository

// ✨ CORREGIDO: La Factory ahora construye y pasa TODAS las dependencias.
class FileManagerViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileManagerViewModel::class.java)) {
            // 1. Se obtiene la base de datos y el DAO.
            val db = AppDatabase.getDatabase(context.applicationContext)
            val dao = db.documentDao()

            // 2. Se crea el Repositorio con el DAO y el Context.
            val repository = DocumentRepository(dao, context.applicationContext)

            // 3. Se crea el ViewModel con el Repositorio.
            @Suppress("UNCHECKED_CAST")
            return FileManagerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}