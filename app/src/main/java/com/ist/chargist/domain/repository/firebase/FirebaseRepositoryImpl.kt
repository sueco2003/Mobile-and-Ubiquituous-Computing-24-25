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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.google.firebase.firestore.FieldPath
import com.ist.chargist.domain.model.StationRating

class FirebaseRepositoryImpl @Inject constructor(
    private val deviceInfo: DeviceInfoProvider,
    private val dispatcherProvider: DispatcherProvider
) : DatabaseRepository {
    companion object {

        private const val STATIONS_DATABASE = "chargerStations"
        private const val SLOTS_DATABASE = "chargerSlots"
        private const val STATION_RATINGS_DATABASE = "stationRatings"

        private const val ERROR_NO_INTERNET = "ERROR_NO_INTERNET"
        private const val ERROR_GETTING_LOCATION = "ERROR_GETTING_LOCATION"
        private const val ERROR_ASSET_ALREADY_EXITS = "ERROR_ASSET_ALREADY_EXITS"
        private const val ERROR_ASSET_ALREADY_RETURNED = "ERROR_ASSET_ALREADY_RETURNED"
        private const val ERROR_ASSET_NOT_GIVEN = "ERROR_ASSET_NOT_GIVEN"
        private const val ERROR_ASSET_NOT_FOUND = "ERROR_ASSET_NOT_FOUND"
        private const val ERROR_ASSET_INCONSISTENT_STATE = "ERROR_ASSET_INCONSISTENT_STATE"
    }

    // cache data to avoid redundant network calls
    private val _stations = mutableStateMapOf<String, ChargerStation>()
    private val _slots = mutableStateMapOf<String, List<ChargerSlot>>()
    private val _favourites = mutableStateListOf<String>()
    private var _favouritesLastFetched: Long? = null

    private val repositoryScope = CoroutineScope(dispatcherProvider.io())

    override suspend fun getFilteredStations(
        lastStation: ChargerStation,
        searchQuery: String,
        filters: List<String>,
        userLocation: LatLng?
    ): Result<List<ChargerStation>> {

        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        Timber.tag("FirebaseRepositoryImpl")
            .d("getFilteredStations called with lastStationID: $lastStation, searchQuery: $searchQuery, filters: $filters, userLocation: $userLocation")
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

                if (orderByPrice && searchQuery.isBlank()) {
                    query = query.orderBy("lowestPrice", computeDirection(direction))
                    if (lastStation.id != "") {
                        query = query.startAfter(lastStation.lowestPrice)
                    }
                }

                var limit = 7L
                if (orderByDistance && userLocation != null) {
                    limit = 50L
                }

                query.limit(limit)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val stations = documentSnapshot.toObjects(ChargerStationDtoFirebase::class.java)
                        val chargerStations = stations.map { it.toChargerStation() }

                        // add to cache
                        chargerStations.forEach { _stations[it.id] = it }

                        val sortedStations = if (orderByDistance && userLocation != null) {
                            val sorted = chargerStations.sortedBy { calculateDistance(it, userLocation) }
                            if (direction == "descending") sorted.reversed() else sorted
                        } else if (orderByPrice) {
                            val sorted = chargerStations.sortedBy { it.lowestPrice }
                            if (direction == "descending") sorted.reversed() else sorted
                        } else {
                            chargerStations
                        }

                        val startIndex = sortedStations.indexOfFirst { it.id == lastStation.id } + 1
                        val paginated = sortedStations.drop(startIndex).take(7)

                        continuation.resume(Result.success(paginated))

                    }.addOnFailureListener { exception ->
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

    override suspend fun getAllKnownStations(): Result<List<ChargerStation>> {
        return Result.success(_stations.values.toList())
    }

    override suspend fun getNearbyStations(position: LatLng, radius: Double): Result<List<ChargerStation>> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        val radiusDegree = radius / 111 // 1 degree is roughly 111km

        val minLat = position.latitude - radiusDegree
        val maxLat = position.latitude + radiusDegree
        val minLon = position.longitude - radiusDegree
        val maxLon = position.longitude + radiusDegree

        Timber.tag("FirebaseRepositoryImpl").i("fetching nearby charger stations for lat: ${position.latitude}, lon: ${position.longitude}, radius: $radius")
        return try {
            suspendCoroutine { continuation ->
                Firebase.firestore
                    .collection(STATIONS_DATABASE)
                    .whereGreaterThanOrEqualTo("lat", minLat)
                    .whereLessThanOrEqualTo("lat", maxLat)
                    .whereGreaterThanOrEqualTo("lon", minLon)
                    .whereLessThanOrEqualTo("lon", maxLon)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val stations = snapshot.toObjects(ChargerStationDtoFirebase::class.java)
                            .map { it.toChargerStation() }

                        // ids of stations returned
                        val fetchedIds = stations.map { it.id }.toSet()

                        // update cache
                        stations.forEach { _stations[it.id] = it }

                        // remove cached stations within this range that don't exist anymore
                        val keysToRemove = _stations.filter { (_, station) ->
                            station.lat.toDouble() in minLat..maxLat &&
                                    station.lon.toDouble() in minLon..maxLon &&
                                    station.id !in fetchedIds
                        }.keys

                        keysToRemove.forEach {
                            _stations.remove(it)
                            _slots.remove(it)
                            _favourites.remove(it)
                        }

                        continuation.resume(Result.success(stations))
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

    override suspend fun getChargerStation(stationId: String): Result<ChargerStation> {
        if (!deviceInfo.hasInternetConnection()) {
            _stations[stationId]?.let { cachedStation ->
                return Result.success(cachedStation)
            }
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        Timber.tag("FirebaseRepositoryImpl").i("fetching charger station with id: $stationId")
        return try {
            suspendCoroutine { continuation ->
                Firebase.firestore
                    .collection(STATIONS_DATABASE)
                    .document(stationId)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val station = documentSnapshot
                            .toObject(ChargerStationDtoFirebase::class.java)
                            ?.toChargerStation()

                        if (station == null) {
                            _stations.remove(stationId)
                            _slots.remove(stationId)
                            _favourites.remove(stationId)
                            continuation.resume(Result.failure(Throwable("Charger station not found")))
                            return@addOnSuccessListener
                        }

                        _stations[station.id] = station
                        continuation.resume(Result.success(station))
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

    override suspend fun getSlotsForStation(stationId: String, forceRefresh: Boolean): Result<List<ChargerSlot>> {
        if (!forceRefresh && !deviceInfo.hasInternetConnection()) {
            _slots[stationId]?.let { cachedSlots ->
                return Result.success(cachedSlots)
            }
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        Timber.tag("FirebaseRepositoryImpl").i("fetching slots for station: $stationId")
        return try {
            suspendCoroutine { continuation ->
                Firebase.firestore
                    .collection(SLOTS_DATABASE)
                    .whereEqualTo("stationId", stationId)
                    .get()
                    .addOnSuccessListener { documentSnapshot ->
                        val slots = documentSnapshot
                            .toObjects(ChargerSlotDtoFirebase::class.java)
                            .map { it.toChargerSlot() }

                        _slots[stationId] = slots

                        continuation.resume(Result.success(slots))
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

        // extract only successful slot IDs
        val successfulSlotIds: List<String> = newSlotIds.mapNotNull { result ->
            result.getOrNull()
        }

        stationToInsert.slotsId = stationToInsert.slotsId + successfulSlotIds
        stationToInsert.availableSlots = stationToInsert.slotsId.isNotEmpty()

        Timber.tag("FirebaseRepositoryImpl").i("creating/updating charger station with id: ${stationToInsert.id}")
        return try {
            suspendCoroutine { continuation ->
                Firebase.firestore
                    .collection(STATIONS_DATABASE)
                    .document(stationToInsert.id)
                    .set(stationToInsert)
                    .addOnCompleteListener {
                        val updatedStation = stationToInsert.toChargerStation()
                        _stations[updatedStation.id] = updatedStation
                        continuation.resume(Result.success(listOf(updatedStation)))
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

    override suspend fun createOrUpdateChargerSlot(
        stationId: String,
        slot: ChargerSlot
    ): Result<String> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        val slotToInsert = slot.toChargerSlotDtoFirebase(stationId)

        Timber.tag("FirebaseRepositoryImpl").i("creating/updating charger slot with id: ${slotToInsert.slotId}")
        return try {
            val result = suspendCoroutine<Result<String>> { continuation ->
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

            // update cache
            result.getOrNull()?.let { _ ->
                val updatedList = _slots[stationId].orEmpty().toMutableList()

                // replace existing slot if already cached, or add it
                val existingIndex = updatedList.indexOfFirst { it.slotId == slot.slotId }
                if (existingIndex >= 0) {
                    updatedList[existingIndex] = slot
                } else {
                    updatedList += slot
                }

                _slots[stationId] = updatedList
            }

            // update available slots after slot is created/updated
            updateChargerStationAvailableSlots(stationId)

            result
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }

    override suspend fun getFavorites(userId: String): Result<List<String>> {
        val now = System.currentTimeMillis()
        val tenMinutes = 10 * 60 * 1000

        if (_favourites.isNotEmpty() && _favouritesLastFetched != null &&
            now - _favouritesLastFetched!! < tenMinutes
        ) {
            return Result.success(_favourites)
        }

        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        Timber.tag("FirebaseRepositoryImpl").i("fetching favourites for user: $userId")
        return try {
            suspendCoroutine { continuation ->
                Firebase.firestore.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener { document ->
                        val favourites = document.get("favourites") as? List<String> ?: emptyList()

                        // update cache
                        _favourites.clear()
                        _favourites.addAll(favourites)
                        _favouritesLastFetched = now

                        continuation.resume(Result.success(favourites))
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

    override suspend fun toggleFavorite(userId: String, stationId: String): Result<Unit> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        Timber.tag("FirebaseRepositoryImpl").i("toggling favourite for user: $userId, and station: $stationId")
        return try {
            suspendCoroutine { continuation ->

                val userDocRef = Firebase.firestore.collection("users").document(userId)

                userDocRef.get()
                    .addOnSuccessListener { snapshot ->
                        val favorites = snapshot.get("favourites") as? List<String> ?: emptyList()

                        val isRemoving = favorites.contains(stationId)
                        val updateValue = if (isRemoving) {
                            FieldValue.arrayRemove(stationId)
                        } else {
                            FieldValue.arrayUnion(stationId)
                        }

                        userDocRef.update("favourites", updateValue)
                            .addOnSuccessListener {
                                // update cache
                                val updatedFavorites = if (isRemoving) {
                                    favorites.filterNot { it == stationId }
                                } else {
                                    favorites + stationId
                                }
                                _favourites.clear()
                                _favourites.addAll(updatedFavorites)
                                _favouritesLastFetched = System.currentTimeMillis()

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

    override suspend fun reportDamagedSlot(slotId: String): Result<Unit> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        Timber.tag("FirebaseRepositoryImpl").i("reporting damage for slot: $slotId")
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

    override suspend fun getLatestDamageReportTimestamp(slotId: String): Result<Long?> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        Timber.tag("FirebaseRepositoryImpl").i("fetching damage report for slot: $slotId")
        return try {
            suspendCoroutine { continuation ->
                Firebase.firestore
                    .collection("slotReports")
                    .whereEqualTo("slotId", slotId)
                    .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING) // Sort by timestamp
                    .limit(1) // Only fetch the latest
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val timestamp = snapshot.documents
                            .firstOrNull()
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

        Timber.tag("FirebaseRepositoryImpl").i("reporting damage for slot: $slotId")
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

    override suspend fun submitStationRating(userId: String, stationId: String, rating: Int): Result<Unit> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        return try {
            suspendCoroutine { continuation ->
                val db = Firebase.firestore

                val ratingDoc = StationRating(
                    userId = userId,
                    stationId = stationId,
                    rating = rating
                )

                val ratingDocRef = db.collection(STATION_RATINGS_DATABASE)
                    .document("${stationId}_${userId}")

                val stationRef = db.collection(STATIONS_DATABASE).document(stationId)

                db.runTransaction { transaction ->
                    val stationSnapshot = transaction.get(stationRef)
                    if (!stationSnapshot.exists()) {
                        throw Exception("Station not found")
                    }

                    @Suppress("UNCHECKED_CAST")
                    val currentRatingsRaw = stationSnapshot.get("ratings") as? Map<String, Any> ?: emptyMap()
                    val currentRatings = currentRatingsRaw.mapValues { (it.value as? Number)?.toInt() ?: 0 }.toMutableMap()

                    val currentUserRating = transaction.get(ratingDocRef)
                        .toObject(StationRating::class.java)
                        ?.rating

                    currentUserRating?.let { oldRating ->
                        val oldKey = oldRating.toString()
                        val currentCount = currentRatings[oldKey] ?: 0
                        if (currentCount <= 1) {
                            currentRatings.remove(oldKey)
                        } else {
                            currentRatings[oldKey] = currentCount - 1
                        }
                    }

                    val newKey = rating.toString()
                    currentRatings[newKey] = (currentRatings[newKey] ?: 0) + 1

                    val totalRatings = currentRatings.values.sum()
                    val averageRating = if (totalRatings > 0) {
                        currentRatings.entries.sumOf { it.key.toInt() * it.value }.toFloat() / totalRatings
                    } else 0.0f

                    transaction.update(stationRef, mapOf(
                        "ratings" to currentRatings,
                        "averageRating" to averageRating,
                        "totalRatings" to totalRatings
                    ))

                    transaction.set(ratingDocRef, ratingDoc)
                }.addOnSuccessListener {
                    continuation.resume(Result.success(Unit))
                }.addOnFailureListener { exception ->
                    continuation.resume(Result.failure(exception))
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
            Result.failure(e)
        }
    }

    override suspend fun getUserRatingForStation(userId: String, stationId: String): Result<Int?> {
        return try {
            val db = Firebase.firestore
            val ratingDoc = db.collection(STATION_RATINGS_DATABASE)
                .document("${stationId}_${userId}")
                .get()
                .await()

            val rating = if (ratingDoc.exists()) {
                ratingDoc.toObject(StationRating::class.java)?.rating
            } else null

            Result.success(rating)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun updateChargerStationAvailableSlots(
        stationId: String
    ): Result<Unit> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        return getSlotsForStation(stationId, true).fold(
            onSuccess = { slots ->
                Timber.tag("FirebaseRepositoryImpl").i("updating available property for station: $stationId")

                val hasAvailableSlots = slots.any { it.available }
                try {
                    suspendCoroutine { continuation ->
                        Firebase.firestore
                            .collection(STATIONS_DATABASE)
                            .document(stationId)
                            .update("availableSlots", hasAvailableSlots)
                            .addOnSuccessListener {
                                // update cache
                                _stations[stationId]?.let { cachedStation ->
                                    _stations[stationId] = cachedStation.copy(availableSlots = hasAvailableSlots)
                                }

                                continuation.resume(Result.success(Unit))
                            }
                            .addOnFailureListener { exception ->
                                continuation.resume(Result.failure(exception))
                            }
                    }
                } catch (e: Exception) {
                    Timber.e(e)
                    Result.failure(e)
                }
            },
            onFailure = { exception ->
                Timber.e(exception, "Failed to get slots for station $stationId")
                Result.failure(exception)
            }
        )
    }

}

