package com.ist.chargist.domain.repository.firebase.model

import com.ist.chargist.domain.model.ChargerStation
import java.util.UUID

data class ChargerStationDtoFirebase(
    val id: String = "",
    val name: String = "",
    val lat : Float = 0.0f,
    val lon : Float = 0.0f,
    val payment : List<String> = emptyList(),
    val imageUri : String = "",
    val availableSpeeds: List<String> = emptyList(),
    val fastPrice: Float = 0.0f,
    val mediumPrice: Float = 0.0f,
    val slowPrice: Float = 0.0f,
    var availableSlots: Boolean = false,
    var lowestPrice: Float = 0.0f,
    val nearbyServices: List<String> = emptyList(),
    var slotsId : List<String> = emptyList()
)

fun ChargerStation.toChargerStationDtoFirebase() = ChargerStationDtoFirebase(
    id = if (id.isEmpty()) UUID.randomUUID().toString() else this.id,
    name = this.name.toString(),
    lat = this.lat,
    lon = this.lon,
    payment = this.payment,
    imageUri = this.imageUri?.toString() ?: "",
    availableSpeeds = this.availableSpeeds,
    fastPrice = this.fastPrice,
    mediumPrice = this.mediumPrice,
    slowPrice = this.slowPrice,
    availableSlots = this.availableSlots,
    lowestPrice = this.lowestPrice,
    nearbyServices = this.nearbyServices
)

fun ChargerStationDtoFirebase.toChargerStation() = ChargerStation(
    id = this.id,
    name = this.name,
    lat = this.lat,
    lon = this.lon,
    payment = this.payment,
    imageUri = this.imageUri,
    availableSpeeds = this.availableSpeeds,
    fastPrice = this.fastPrice,
    mediumPrice = this.mediumPrice,
    slowPrice = this.slowPrice,
    availableSlots = this.availableSlots,
    lowestPrice = this.lowestPrice,
    nearbyServices = this.nearbyServices,
    slotId = this.slotsId
)