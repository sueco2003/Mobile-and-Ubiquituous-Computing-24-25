package com.ist.chargist.domain.model

data class ChargerSlot(
    val slotId : String,
    val speed : String,
    val price : Float,
    val connector : String,
    val stationId : String,
    var available : Boolean
)
