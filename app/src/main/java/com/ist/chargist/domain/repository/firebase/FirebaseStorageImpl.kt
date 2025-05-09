package com.ist.chargist.domain.repository.firebase

import android.net.Uri
import com.ist.chargist.domain.ImageRepository
import javax.inject.Inject

class FirebaseStorageImpl @Inject constructor() : ImageRepository {
    override suspend fun uploadImage(
        collectionName: String,
        fileName: String,
        fileUri: Uri,
        referenceId: String
    ): Result<String> {
        TODO("Not yet implemented")
    }

    override suspend fun deleteImageFromStorage(
        referenceId: String,
        fileName: String
    ): Result<Unit> {
        TODO("Not yet implemented")
    }

    override fun getDefaultImage(id: String): Int {
        TODO("Not yet implemented")
    }


}