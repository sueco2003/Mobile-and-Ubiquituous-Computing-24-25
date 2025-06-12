package com.ist.chargist.presentation.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ist.chargist.domain.model.ChargerStation

@Composable
fun RatingSection(
    station: ChargerStation,
    userRating: Int?,
    isAnonymous: Boolean,
    onRatingSubmit: (Int) -> Unit
) {
    Log.d("RatingSection", "RatingSection - isAnonymous: $isAnonymous, userRating: $userRating")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ratings", style = MaterialTheme.typography.titleSmall)

        // Average rating display
        if (station.totalRatings > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "%.1f".format(station.averageRating),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                StarRatingDisplay(rating = station.averageRating)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "(${station.totalRatings} reviews)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Rating histogram
            RatingHistogram(ratings = station.ratings, totalRatings = station.totalRatings)
        } else {
            Text(
                "No ratings yet",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }

        // User rating input (only for non-anonymous users)
        if (!isAnonymous) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (userRating != null) "Your rating:" else "Rate this station:",
                style = MaterialTheme.typography.bodySmall
            )
            Log.d("RatingSection", "About to show InteractiveStarRating")
            InteractiveStarRating(
                currentRating = userRating ?: 0,
                onRatingChange = { rating ->
                    Log.d("RatingSection", "Rating changed to: $rating")
                    onRatingSubmit(rating)
                }
            )
        } else {
            Log.d("RatingSection", "User is anonymous, not showing rating input")
        }
    }
}

@Composable
fun StarRatingDisplay(rating: Float) {
    Row {
        repeat(5) { index ->
            val starValue = index + 1
            Icon(
                imageVector = when {
                    rating >= starValue -> Icons.Default.Star
                    rating >= starValue - 0.5f -> Icons.Default.Star // You might want a half-star icon
                    else -> Icons.Default.StarBorder
                },
                contentDescription = null,
                tint = if (rating >= starValue - 0.5f) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
fun InteractiveStarRating(
    currentRating: Int,
    onRatingChange: (Int) -> Unit
) {
    Log.d("StarRating", "InteractiveStarRating - currentRating: $currentRating")

    Row {
        repeat(5) { index ->
            val starValue = index + 1
            Icon(
                imageVector = if (starValue <= currentRating) Icons.Default.Star else Icons.Default.StarBorder,
                contentDescription = "Rate $starValue stars",
                tint = if (starValue <= currentRating) Color(0xFFFFD700) else Color.Gray,
                modifier = Modifier
                    .size(24.dp)
                    .clickable {
                        Log.d("StarRating", "Star clicked: $starValue")
                        onRatingChange(starValue)
                    }
                    .padding(2.dp)
            )
        }
    }
}

@Composable
fun RatingHistogram(ratings: Map<String, Int>, totalRatings: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        (5 downTo 1).forEach { stars ->
            val count = ratings[stars.toString()] ?: 0
            val percentage = if (totalRatings > 0) count.toFloat() / totalRatings else 0f

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "$stars",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(12.dp)
                )
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(
                            Color.Gray.copy(alpha = 0.3f),
                            RoundedCornerShape(4.dp)
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(percentage)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(4.dp)
                            )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(20.dp)
                )
            }
        }
    }
}
