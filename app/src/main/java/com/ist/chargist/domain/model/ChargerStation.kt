package com.ist.chargist.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChargerStation(
    val id: String = "",
    val name: String,
    val lat: Float,
    val lon: Float,
    val payment: List<String>,
    val imageUri: String?,
    val slowPrice: Float,
    val mediumPrice: Float,
    val fastPrice: Float,
    val slotId: List<String>,
) : Parcelable
