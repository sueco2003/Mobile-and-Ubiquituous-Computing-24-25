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
    val slowPrice : Float = 0.0f,
    val mediumPrice : Float = 0.0f,
    val fastPrice : Float = 0.0f,
    var slotsId : List<String> = emptyList()
)

fun ChargerStation.toChargerStationDtoFirebase() = ChargerStationDtoFirebase(
    id = if (id.isEmpty()) UUID.randomUUID().toString() else this.id,
    name = this.name.toString(),
    lat = this.lat,
    lon = this.lon,
    payment = this.payment,
    imageUri = this.imageUri.toString(),
    slowPrice = this.slowPrice,
    mediumPrice = this.mediumPrice,
    fastPrice = this.fastPrice
)

fun ChargerStationDtoFirebase.toChargerStation() = ChargerStation(
    id = this.id,
    name = this.name,
    lat = this.lat,
    lon = this.lon,
    payment = this.payment,
    imageUri = this.imageUri,
    slowPrice = this.slowPrice,
    mediumPrice = this.mediumPrice,
    fastPrice = this.fastPrice,
    slotId = this.slotsId
)