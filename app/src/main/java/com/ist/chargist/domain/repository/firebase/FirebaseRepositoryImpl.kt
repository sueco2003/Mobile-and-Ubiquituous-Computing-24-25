package com.ist.chargist.domain.repository.firebase

import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import com.hocel.assetmanager.utils.DispatcherProvider
import com.ist.chargist.domain.DatabaseRepository
import com.ist.chargist.domain.DeviceInfoProvider
import com.ist.chargist.domain.ImageRepository
import com.ist.chargist.domain.model.ChargerSlot
import com.ist.chargist.domain.model.ChargerStation
import com.ist.chargist.domain.repository.firebase.model.ChargerSlotDtoFirebase
import com.ist.chargist.domain.repository.firebase.model.ChargerStationDtoFirebase
import com.ist.chargist.domain.repository.firebase.model.toChargerSlot
import com.ist.chargist.domain.repository.firebase.model.toChargerSlotDtoFirebase
import com.ist.chargist.domain.repository.firebase.model.toChargerStation
import com.ist.chargist.domain.repository.firebase.model.toChargerStationDtoFirebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import android.location.Location
import com.google.firebase.firestore.FieldPath

class FirebaseRepositoryImpl @Inject constructor(
    private val deviceInfo: DeviceInfoProvider,
    private val imageRepository: ImageRepository,
    private val dispatcherProvider: DispatcherProvider
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

    override suspend fun getFilteredStations(
        lastStationID: String,
        searchQuery: String,
        filters: List<String>,
        userLocation: LatLng?
    ): Result<List<ChargerStation>> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        Timber.tag("FirebaseRepository")
            .d("getFilteredStations called with lastStationID: $lastStationID, searchQuery: $searchQuery, filters: $filters, userLocation: $userLocation")
        val availableSlotsFilter = filters.contains("available")
        val paymentFilters = filters.filter { it in listOf("credit", "paypal", "cash") }
        val speedFilters = filters.filter { it in listOf("fast", "medium", "slow") }
        val orderByPrice = filters.contains("price")
        val orderByDistance = filters.contains("distance")
        val direction = filters.filter { it in listOf("ascending", "descending") }.firstOrNull() ?: "descending"

        return try {
            suspendCoroutine { continuation ->
                var query: Query = Firebase.firestore.collection(STATIONS_DATABASE)

                if(speedFilters.isNotEmpty())
                    query = query.whereArrayContainsAny("availableSpeeds", speedFilters)

                if (paymentFilters.isNotEmpty())
                    query = query.whereArrayContainsAny("payment", paymentFilters)

                if (searchQuery.isNotBlank()) {
                    query = query
                        .whereGreaterThanOrEqualTo("name", searchQuery)
                        .whereLessThanOrEqualTo("name", searchQuery + '\uf8ff')
                }

                if (availableSlotsFilter) {
                    query = query.whereEqualTo("availableSlots", true)
                }

                if (orderByPrice && searchQuery.isBlank())
                    query = query.orderBy("lowestPrice", computeDirection(direction))

                if(searchQuery.isBlank())
                    query = query.orderBy(FieldPath.documentId(), Query.Direction.ASCENDING)

                if (lastStationID.isNotEmpty()) {
                    query = query.startAfter(lastStationID)
                }

                query.limit(5)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val stations = documentSnapshot.toObjects(ChargerStationDtoFirebase::class.java)
                        val chargerStations = stations.map { it.toChargerStation() }
                        Timber.tag("FirebaseRepository")
                            .d("FetchedChargerStations: $chargerStations")
                        var sortedStations = chargerStations
                        if(!orderByPrice) {
                            sortedStations = if (orderByDistance && userLocation != null) {
                                val stationsWithDistance = chargerStations.sortedWith(
                                    compareBy { calculateDistance(it, userLocation) }
                                )
                                if (direction == "descending") stationsWithDistance.reversed() else stationsWithDistance
                            } else {
                                chargerStations
                            }
                        }


                        continuation.resume(Result.success(sortedStations))
                    }.addOnFailureListener { exception ->
                        Timber.tag("FirebaseRepository")
                            .d("Error no fetched stations: $exception")
                        continuation.resume(Result.failure(exception))
                    }
            }

        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }

    private fun calculateDistance(station: ChargerStation, userLocation: LatLng): Double {
        val results = FloatArray(1)
        Location.distanceBetween(
            userLocation.latitude,
            userLocation.longitude,
            station.lat.toDouble(),
            station.lon.toDouble(),
            results
        )
        return results[0].toDouble()
    }

    private fun computeDirection(direction: String): Query.Direction {
        return when (direction.lowercase()) {
            "ascending" -> Query.Direction.ASCENDING
            "descending" -> Query.Direction.DESCENDING
            else -> Query.Direction.DESCENDING
        }
    }


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
                        val stations =
                            documentSnapshot.toObjects(ChargerStationDtoFirebase::class.java)
                        continuation.resume(Result.success(stations.map { it.toChargerStation() }))
                    }.addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }


    override suspend fun getSlotsForStation(stationId: String): Result<List<ChargerSlot>> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        return try {
            suspendCoroutine { continuation ->
                Firebase.firestore
                    .collection(SLOTS_DATABASE)
                    .whereEqualTo("stationId", stationId) // <<-- filter at database level
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val slots = documentSnapshot.toObjects(ChargerSlotDtoFirebase::class.java)
                        continuation.resume(Result.success(slots.map { it.toChargerSlot() }))
                    }.addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }

    override suspend fun createOrUpdateChargerStation(
        station: ChargerStation,
        slots: List<ChargerSlot>
    ): Result<List<ChargerStation>> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        val stationToInsert = station.toChargerStationDtoFirebase()

        val newSlotIds: List<Result<String>> = slots.map { slot ->
            createOrUpdateChargerSlot(stationToInsert.id, slot)
        }

        // Extract only successful slot IDs
        val successfulSlotIds: List<String> = newSlotIds.mapNotNull { result ->
            result.getOrNull()  // returns the String on success, or null on failure
        }

        // Now create a new list combining old slotsId + successful ones
        stationToInsert.slotsId = stationToInsert.slotsId + successfulSlotIds

        stationToInsert.availableSlots = stationToInsert.slotsId.isNotEmpty()

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

    override suspend fun createOrUpdateChargerSlot(
        stationId: String,
        slot: ChargerSlot
    ): Result<String> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        val slotToInsert = slot.toChargerSlotDtoFirebase(stationId)

        return try {
            suspendCoroutine { continuation ->
                Firebase.firestore
                    .collection(SLOTS_DATABASE)
                    .document(slotToInsert.slotId)
                    .set(slotToInsert)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(Result.success(slotToInsert.slotId))
                        } else {
                            continuation.resume(
                                Result.failure(
                                    task.exception ?: Exception("Unknown Firestore error")
                                )
                            )
                        }
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }


    override suspend fun reportDamagedSlot(slotId: String): Result<Unit> {
        return try {
            val report = mapOf(
                "slotId" to slotId,
                "message" to "Slot is damaged",
                "timestamp" to System.currentTimeMillis()
            )
            Firebase.firestore
                .collection("slotReports")
                .add(report)
                .await()  // from kotlinx-coroutines-play-services
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }

    override suspend fun toggleFavorite(userId: String, stationId: String): Result<Unit> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        return try {
            suspendCoroutine { continuation ->

                val userDocRef = Firebase.firestore.collection("users").document(userId)

                userDocRef.get()
                    .addOnSuccessListener { snapshot ->
                        val favorites = snapshot.get("favourites") as? List<String> ?: emptyList()
                        val updateValue = if (favorites.contains(stationId)) {
                            FieldValue.arrayRemove(stationId)
                        } else {
                            FieldValue.arrayUnion(stationId)
                        }

                        userDocRef.update("favourites", updateValue)
                            .addOnSuccessListener {
                                continuation.resume(Result.success(Unit))
                            }
                            .addOnFailureListener {
                                continuation.resume(Result.failure(it))
                            }
                    }
                    .addOnFailureListener {
                        continuation.resume(Result.failure(it))
                    }
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }


    override suspend fun getFavorites(userId: String): Result<List<String>> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        return try {
            suspendCoroutine { continuation ->
                Firebase.firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        val favs = document.get("favourites") as? List<String> ?: emptyList()
                        continuation.resume(Result.success(favs))
                    }
                    .addOnFailureListener {
                        continuation.resume(Result.failure(it))
                    }
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }

    override suspend fun getLatestDamageReportTimestamp(slotId: String): Result<Long?> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        return try {
            suspendCoroutine { continuation ->
                Firebase.firestore
                    .collection("slotReports")
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val timestamp = snapshot.documents
                            .firstOrNull { it.getString("slotId") == slotId }
                            ?.getLong("timestamp")
                        continuation.resume(Result.success(timestamp))
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }

    override suspend fun fixSlot(slotId: String): Result<Unit> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        return try {
            suspendCoroutine { continuation ->
                Firebase.firestore
                    .collection("slotReports")
                    .whereEqualTo("slotId", slotId)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val batch = Firebase.firestore.batch()
                        for (document in snapshot.documents) {
                            batch.delete(document.reference)
                        }

                        batch.commit()
                            .addOnSuccessListener {
                                continuation.resume(Result.success(Unit))
                            }
                            .addOnFailureListener { exception ->
                                continuation.resume(Result.failure(exception))
                            }
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }



}

