package com.ist.chargist.domain.repository.firebase.model

import com.ist.chargist.domain.model.ChargerStation
import java.util.UUID
import kotlin.toString

data class ChargerStationDtoFirebase(
    val id: String = "",
    val name: String = "",
    val lat : Float = 0.0.toFloat(),
    val lon : Float = 0.0.toFloat(),
    val payment : String = "",
    val imageUri : String = ""
)

fun ChargerStation.toChargerStationDtoFirebase() = ChargerStationDtoFirebase(
    id = if (id.isEmpty()) UUID.randomUUID().toString() else this.id,
    name = this.name.toString(),
    lat = this.lat,
    lon = this.lon,
    payment = this.payment.toString(),
    imageUri = this.imageUri.toString()
)

fun ChargerStationDtoFirebase.toChargerStation() = ChargerStation(
    id = this.id,
    name = this.name,
    lat = this.lat,
    lon = this.lon,
    payment = this.payment,
    imageUri = this.imageUri
)