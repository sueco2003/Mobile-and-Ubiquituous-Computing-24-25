package com.ist.chargist.domain.repository.firebase.model

import com.ist.chargist.domain.model.ChargeSpeed
import com.ist.chargist.domain.model.ChargerSlot
import com.ist.chargist.domain.model.Connector
import java.util.UUID

data class ChargerSlotDtoFirebase(
    val slotId: String = "",
    val speed : String = "",
    val connector : String = "",
    val stationId : String = "",
    var available : Boolean = true

)

fun ChargerSlot.toChargerSlotDtoFirebase(stationId: String) = ChargerSlotDtoFirebase(
    slotId = if (slotId.isEmpty()) UUID.randomUUID().toString() else this.slotId,
    speed = this.speed.toString(),
    connector = this.connector.toString(),
    stationId = stationId,
    available = this.available
)

fun ChargerSlotDtoFirebase.toChargerSlot() = ChargerSlot(
    slotId = this.slotId,
    speed = ChargeSpeed.valueOf(this.speed),     // Convertendo de String para enum
    connector = Connector.valueOf(this.connector), // Convertendo de String para enum
    stationId = this.stationId,
    available = this.available,
)
