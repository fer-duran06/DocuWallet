package com.docuwallet.app.presentacion.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val DOCUMENTS = "documents"
    const val STATS = "stats"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "edit_profile"
    const val CHANGE_PASSWORD = "change_password"
    const val NEW_DOCUMENT = "new_document"
    const val CAMERA_SCAN = "camera_scan"
    const val PAGE_PREVIEW = "page_preview"
    const val FILE_MANAGER = "file_manager"
    const val DOCUMENT_DETAIL = "document_detail/{documentId}"

    fun documentDetail(documentId: String) = "document_detail/$documentId"
}