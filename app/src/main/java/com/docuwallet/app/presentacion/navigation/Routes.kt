package com.docuwallet.app.presentacion.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val DOCUMENTS = "documents"
    const val DOCUMENT_DETAIL = "document_detail/{documentId}"
    const val ADD_DOCUMENT = "add_document"
    const val EDIT_DOCUMENT = "edit_document/{documentId}"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"

    fun documentDetail(documentId: String) = "document_detail/$documentId"
    fun editDocument(documentId: String) = "edit_document/$documentId"
}