package com.ist.chargist.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlin.math.min

@Parcelize
data class ChargerStation(
    val id: String = "",
    val name: String,
    val lat: Float,
    val lon: Float,
    val payment: List<String>,
    val imageUri: String?,
    val fastPrice: Float = 0.0f,
    val mediumPrice: Float = 0.0f,
    val slowPrice: Float = 0.0f,
    val availableSpeeds: List<String> = listOf<String>().let {
        val speeds = mutableListOf<String>()
        if (fastPrice >= 0) speeds.add("fast")
        if (mediumPrice >= 0) speeds.add("medium")
        if (slowPrice >= 0) speeds.add("slow")
        speeds
    },
    val lowestPrice: Float = listOf(fastPrice, mediumPrice, slowPrice).filter { it >= 0 }.min(),
    val availableSlots: Boolean = false,
    val slotId: List<String> = emptyList(),
) : Parcelable


