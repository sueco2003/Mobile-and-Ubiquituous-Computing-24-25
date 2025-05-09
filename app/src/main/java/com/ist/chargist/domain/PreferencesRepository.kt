package com.ist.chargist.domain

interface PreferencesRepository {

    suspend fun setIsLocationGiving(locationId: String, isGiving: Boolean)

    suspend fun isLocationGiving(locationId: String, defaultValue: Boolean): Boolean
}