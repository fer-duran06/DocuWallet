package com.docuwallet.app.presentacion.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val HOME = "home"
    const val DOCUMENTS = "documents"
    const val STATS = "stats"
    const val PROFILE = "profile"

    const val DOCUMENT_DETAIL = "document_detail/{documentId}"
    const val NEW_DOCUMENT = "new_document"
    const val EDIT_DOCUMENT = "edit_document/{documentId}"
    const val EDIT_PROFILE = "edit_profile"
    const val CHANGE_PASSWORD = "change_password"
    const val SETTINGS = "settings"

    const val CAMERA_SCAN = "camera_scan"
    const val PAGE_PREVIEW = "page_preview"

    fun documentDetail(documentId: String) = "document_detail/$documentId"
    fun editDocument(documentId: String) = "edit_document/$documentId"
}