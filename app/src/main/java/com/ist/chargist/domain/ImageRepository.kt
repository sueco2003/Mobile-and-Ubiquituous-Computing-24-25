package com.ist.chargist.domain

import android.net.Uri

interface ImageRepository {

    suspend fun uploadImage(
        fileName: String,
        fileUri: Uri,
        referenceId: String,
    ): Result<String>

    suspend fun deleteImageFromStorage(
        referenceId: String,
        fileName: String,
    ): Result<Unit>

    fun getDefaultImage(id: String): Int
}