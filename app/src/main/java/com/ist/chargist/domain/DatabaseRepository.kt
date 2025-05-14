package com.ist.chargist.domain

import com.ist.chargist.domain.model.ChargerStation

interface DatabaseRepository {

    suspend fun getChargerStations() : Result<List<ChargerStation>>
    suspend fun createOrUpdateChargerStation(station: ChargerStation) : Result<List<ChargerStation>>


}