package com.ist.chargist.presentation.mapLocationPicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.ist.chargist.presentation.components.CustomAlertDialog

@Composable
fun MapLocationPickerScreen(
    navigateBack: () -> Unit,
    onLocationSelected: (LatLng) -> Unit
) {
    var selectedLatLng by remember { mutableStateOf<LatLng?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            properties = MapProperties(),
            uiSettings = MapUiSettings(),
            onMapClick = { latLng ->
                selectedLatLng = latLng
                showDialog = true
            }
        ) {
            selectedLatLng?.let { latLng ->
                Marker(
                    state = MarkerState(position = latLng),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }
        }

        IconButton(
            onClick = navigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .background(Color.White.copy(alpha = 0.8f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black
            )
        }

        if (showDialog && selectedLatLng != null) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                CustomAlertDialog(
                    onDismissRequest = {
                        showDialog = false
                        selectedLatLng = null
                    },
                    title = {
                        Text(
                            "Confirm Location",
                            fontSize = 22.sp,
                            color = Color.Black
                        )
                    },
                    text = {
                        Text(
                            "Do you want to select: ${"%.4f".format(selectedLatLng!!.latitude)}, ${
                                "%.4f".format(selectedLatLng!!.longitude)
                            }?",
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                    },
                    confirmButton = {
                        IconButton(onClick = {
                            onLocationSelected(selectedLatLng!!)
                            showDialog = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Confirm",
                                tint = Color(0xFF4CAF50) // green check
                            )
                        }
                    },
                    dismissButton = {
                        IconButton(onClick = {
                            showDialog = false
                            selectedLatLng = null
                        }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = Color(0xFFF44336) // red cross
                            )
                        }
                    },
                    backgroundColor = Color.White.copy(alpha = 0.4f),  // lighter background
                    borderColor = Color.Gray,
                    borderWidth = 1.dp,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}
