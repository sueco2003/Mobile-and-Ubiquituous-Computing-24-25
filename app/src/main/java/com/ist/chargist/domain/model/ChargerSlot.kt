package com.ist.chargist.domain.model

data class ChargerSlot(
    val slotId : String = "",
    val speed : ChargeSpeed,
    val connector : Connector,
    val stationId : String = "",
    var available : Boolean
)

enum class ChargeSpeed { F, M, S }
enum class Connector { CCS2, Type2 }