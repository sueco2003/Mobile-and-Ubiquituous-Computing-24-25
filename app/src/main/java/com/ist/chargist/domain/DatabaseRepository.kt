package com.ist.chargist.domain

import com.google.android.gms.maps.model.LatLng
import com.ist.chargist.domain.model.ChargerSlot
import com.ist.chargist.domain.model.ChargerStation

interface DatabaseRepository {

    suspend fun getFilteredStations(
        lastStation: ChargerStation,
        searchQuery: String = "",
        filters: List<String> = emptyList(),
        userLocation: LatLng? = null,
    ): Result<List<ChargerStation>>
    suspend fun getChargerStations() : Result<List<ChargerStation>>
    suspend fun getNearbyStations(lat: Double, lon: Double): Result<List<ChargerStation>>
    suspend fun getChargerStation(stationId: String) : Result<ChargerStation>
    suspend fun createOrUpdateChargerStation(station: ChargerStation, slots: List<ChargerSlot>): Result<List<ChargerStation>>
    suspend fun createOrUpdateChargerSlot(stationId: String, slot: ChargerSlot) : Result<String>
    suspend fun getSlotsForStation(stationId: String, forceRefresh: Boolean = false) : Result<List<ChargerSlot>>
    suspend fun reportDamagedSlot(slotId: String): Result<Unit>
    suspend fun toggleFavorite(userId: String, stationId: String): Result<Unit>
    suspend fun getFavorites(userId: String): Result<List<String>>
    suspend fun getLatestDamageReportTimestamp(slotId: String): Result<Long?>
    suspend fun fixSlot(slotId: String) : Result<Unit>
    suspend fun submitStationRating(userId: String, stationId: String, rating: Int): Result<Unit>
    suspend fun getUserRatingForStation(userId: String, stationId: String): Result<Int?>

}