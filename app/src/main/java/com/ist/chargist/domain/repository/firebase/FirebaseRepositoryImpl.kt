package com.ist.chargist.domain.repository.firebase

import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore
import com.hocel.assetmanager.utils.DispatcherProvider
import com.ist.chargist.domain.DatabaseRepository
import com.ist.chargist.domain.DeviceInfoProvider
import com.ist.chargist.domain.ImageRepository
import com.ist.chargist.domain.model.ChargerStation
import com.ist.chargist.domain.repository.firebase.model.ChargerStationDtoFirebase
import com.ist.chargist.domain.repository.firebase.model.toChargerStation
import com.ist.chargist.domain.repository.firebase.model.toChargerStationDtoFirebase
import kotlinx.coroutines.CoroutineScope
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseRepositoryImpl @Inject constructor(
    private val deviceInfo: DeviceInfoProvider,
    private val imageRepository: ImageRepository,
    dispatcherProvider: DispatcherProvider
) : DatabaseRepository {

    companion object {

        private const val STATIONS_DATABASE = "chargerStations"
        private const val SLOTS_DATABASE = "chargerSlots"




        private const val ERROR_NO_INTERNET = "ERROR_NO_INTERNET"
        private const val ERROR_GETTING_LOCATION = "ERROR_GETTING_LOCATION"
        private const val ERROR_ASSET_ALREADY_EXITS = "ERROR_ASSET_ALREADY_EXITS"
        private const val ERROR_ASSET_ALREADY_RETURNED = "ERROR_ASSET_ALREADY_RETURNED"
        private const val ERROR_ASSET_NOT_GIVEN = "ERROR_ASSET_NOT_GIVEN"
        private const val ERROR_ASSET_NOT_FOUND = "ERROR_ASSET_NOT_FOUND"
        private const val ERROR_ASSET_INCONSISTENT_STATE = "ERROR_ASSET_INCONSISTENT_STATE"
    }

    private val repositoryScope = CoroutineScope(dispatcherProvider.io())



    override suspend fun getChargerStations(): Result<List<ChargerStation>> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        return try {
            suspendCoroutine { continuation ->
                Firebase.firestore
                    .collection(STATIONS_DATABASE)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val stations = documentSnapshot.toObjects(ChargerStationDtoFirebase::class.java)
                        continuation.resume(Result.success(stations.map { it.toChargerStation()}))
                    }.addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }

    override suspend fun createOrUpdateChargerStation(station: ChargerStation): Result<List<ChargerStation>> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        val stationToInsert = station.toChargerStationDtoFirebase()

        return try {

            suspendCoroutine { continuation ->
                Firebase.firestore
                    .collection(STATIONS_DATABASE)
                    .document(stationToInsert.id)
                    .set(stationToInsert)
                    .addOnCompleteListener {
                        continuation.resume(Result.success(listOf(stationToInsert.toChargerStation())))
                    }.addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }
}
