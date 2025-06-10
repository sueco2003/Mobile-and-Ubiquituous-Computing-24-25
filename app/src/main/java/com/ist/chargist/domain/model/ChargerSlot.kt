package com.ist.chargist.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChargerSlot(
    val slotId : String = "",
    val speed : ChargeSpeed,
    val connector : Connector,
    val stationId : String = "",
    var available : Boolean
) : Parcelable

enum class ChargeSpeed { F, M, S }
enum class Connector { CCS2, Type2 }