package com.ist.chargist.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class StationRating(
    val userId: String = "",
    val stationId: String = "",
    val rating: Int = 0, // 1-5
    val timestamp: Long = System.currentTimeMillis()
) : Parcelable