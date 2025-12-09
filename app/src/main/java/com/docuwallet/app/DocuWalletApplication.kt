package com.docuwallet.app

import android.app.Application
import com.docuwallet.app.data.local.database.AppDatabase
import com.docuwallet.app.data.repository.DocumentRepository

/**
 * Clase Application personalizada para gestionar dependencias como singletons.
 */
class DocuWalletApplication : Application() {

    // Inicialización perezosa de la base de datos. Se crea una sola vez.
    private val database by lazy { AppDatabase.getDatabase(this) }

    // Inicialización perezosa del repositorio. Se crea una sola vez, usando el DAO de la base de datos.
    val repository by lazy { DocumentRepository(database.documentDao(), this) }
}
