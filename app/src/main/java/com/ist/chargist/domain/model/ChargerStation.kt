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
    val lowestPrice: Float = listOf(fastPrice, mediumPrice, slowPrice).filter { it >= 0 }.minOrNull() ?: 0.0f,
    val availableSlots: Boolean = false,
    val nearbyServices: List<String> = emptyList(),
    val slotId: List<String> = emptyList(),
    val ratings: Map<String, Int> = emptyMap(),
    val averageRating: Float = 0.0f,
    val totalRatings: Int = 0,
) : Parcelable {

    fun calculateAverageRating(): Float {
        if (ratings.isEmpty()) return 0.0f
        val totalScore = ratings.entries.sumOf { (key, value) ->
            key.toIntOrNull()?.times(value) ?: 0
        }
        val totalCount = ratings.values.sum()
        return if (totalCount > 0) totalScore.toFloat() / totalCount else 0.0f
    }
}
