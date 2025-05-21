package com.ist.chargist.domain

import com.ist.chargist.domain.model.ChargerSlot
import com.ist.chargist.domain.model.ChargerStation

interface DatabaseRepository {

    suspend fun getChargerStations() : Result<List<ChargerStation>>
    suspend fun createOrUpdateChargerStation(station: ChargerStation, slots: List<ChargerSlot>): Result<List<ChargerStation>>
    suspend fun createOrUpdateChargerSlot(stationId: String, slot: ChargerSlot) : Result<String>
    suspend fun getSlotsForStation(stationId: String) : Result<List<ChargerSlot>>
    suspend fun reportDamagedSlot(slotId: String): Result<Unit>
    suspend fun toggleFavorite(userId: String, stationId: String): Result<Unit>
    suspend fun getFavorites(userId: String): Result<List<String>>
    suspend fun getUserId(): Result<String>



}