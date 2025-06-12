package com.ist.chargist.domain.repository.firebase

import android.net.Uri
import com.google.firebase.Firebase
import com.google.firebase.storage.storage
import com.ist.chargist.R
import com.ist.chargist.domain.ImageRepository
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseStorageImpl @Inject constructor() : ImageRepository {
    private val defaultImageMapper = mutableMapOf<String, Int>()

    override suspend fun uploadImage(
        fileName: String,
        fileUri: Uri,
        referenceId: String,
    ): Result<String> {
        return suspendCoroutine { continuation ->
            val storageRef = Firebase.storage.reference
            val imageRef = storageRef.child("$referenceId/$fileName")

            imageRef.putFile(fileUri)
                .addOnSuccessListener {
                    imageRef.downloadUrl
                        .addOnSuccessListener { downloadUrl ->
                            continuation.resume(Result.success(downloadUrl.toString()))
                        }
                        .addOnFailureListener { exception ->
                            continuation.resume(Result.failure((exception)))
                        }
                }
                .addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
        }
    }

    override suspend fun deleteImageFromStorage(
        referenceId: String,
        fileName: String,
    ): Result<Unit> {
        return try {
            suspendCoroutine { continuation ->
                // Create a storage reference from our app
                val storageRef = Firebase.storage.reference
                val profileRef = storageRef.child("$referenceId/$fileName")

                // Delete file
                profileRef.delete()
                    .addOnSuccessListener {
                        continuation.resume(Result.success(Unit))

                    }.addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getDefaultImage(id: String): Int {
        val defaultImage = defaultImageMapper[id]
        if (defaultImage != null) {
            return defaultImage
        }

        val newDefaultImage = R.drawable.img_default_bg_charger
        defaultImageMapper[id] = newDefaultImage
        return newDefaultImage
    }
}
