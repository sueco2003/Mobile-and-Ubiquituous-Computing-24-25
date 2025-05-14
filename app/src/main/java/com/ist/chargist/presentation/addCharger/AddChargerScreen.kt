/*
package com.ist.chargist.presentation.addCharger

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.type.LatLng
import com.ist.chargist.R
import com.ist.chargist.domain.model.ChargerStation
import com.ist.chargist.ui.theme.BackgroundColor
import com.ist.chargist.ui.theme.ISTBlue
import com.ist.chargist.utils.UiState
import kotlin.Boolean
import kotlin.Int
import kotlin.OptIn
import kotlin.String
import kotlin.TODO
import kotlin.Unit
import kotlin.apply
import kotlin.collections.List
import kotlin.collections.forEach
import kotlin.collections.forEachIndexed
import kotlin.collections.get
import kotlin.collections.listOf
import kotlin.text.format
import kotlin.text.set
import kotlin.text.trim


@Composable
fun AddChargerScreen(
    viewModel: AddChargerViewModel,
    navigateBack: () -> Unit,
    onSaveClicked: (ChargerStation) -> Unit
) {
    val context = LocalContext.current
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        cameraPermissionGranted = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                context.getString(R.string.error_camera_unavailable),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val creatingDocumentUiState by viewModel.creatingCharger.collectAsState()

    LaunchedEffect(Unit) {
        if (!locationPermissionState.status.isGranted) {
            locationPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Go Back")
                    }
                },
                title = { Text("Add Charger") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        }
    ) { paddingValues ->
        AddChargerContent(
            modifier = Modifier.padding(paddingValues),
            viewModel = viewModel,
            navigateBack = navigateBack,
            onSaveClicked = onSaveClicked,
            requestCameraPermission = {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            },
            cameraPermissionGranted = cameraPermissionGranted,
            locationPermissionGranted = locationPermissionState.status.isGranted
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddChargerContent(
    modifier: Modifier = Modifier,
    viewModel: AddChargerViewModel,
    navigateBack: () -> Unit,
    onSaveClicked: (ChargerStation) -> Unit,
    requestCameraPermission: () -> Unit,
    cameraPermissionGranted: Boolean,
    locationPermissionGranted: Boolean
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var showMapPicker by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<LatLng?>(null) }
    var address by remember { mutableStateOf("") }
    var chargingPositions by remember { mutableStateOf(listOf<ChargingPosition>()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }


    if (showMapPicker) {
        LocationPickerDialog(
            onLocationSelected = { latLng, addr ->
                currentLocation = latLng
                address = addr
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false }
        )
    }

    Column(modifier = modifier
        .fillMaxSize()
        .background(BackgroundColor)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Image Upload
            DocumentUploadSection(
                imageUri = imageUri,
                onSelectImage = { uri -> imageUri = uri },
                requestCameraPermission = requestCameraPermission,
                cameraPermissionGranted = cameraPermissionGranted,
                showBottomSheet = { showBottomSheet = it }
            )

            Spacer(Modifier.height(24.dp))

            // Name Input
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Charger Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(Modifier.height(24.dp))

            // Location Section
            LocationSelectionSection(
                address = address,
                currentLocation = currentLocation,
                locationPermissionGranted = locationPermissionGranted,
                onMapClick = { showMapPicker = true },
                onCurrentLocationClick = {
                    viewModel.getCurrentLocation { location ->
                        currentLocation = location
                        address = viewModel.getAddressFromLocation(location)
                    }
                },
                onAddressChange = { address = it }
            )

            Spacer(Modifier.height(24.dp))

            // Charging Positions
            ChargingPositionsSection(
                positions = chargingPositions,
                onAddPosition = { chargingPositions = chargingPositions + ChargingPosition() },
                onSpeedChange = { index, speed ->
                    chargingPositions = chargingPositions.toMutableList().apply {
                        this[index] = this[index].copy(speed = speed)
                    }
                }
            )
        }

        // Save Button
        Button(
            onClick = {
                if (validateInputs(name, currentLocation, chargingPositions, imageUri)) {
                    onSaveClicked(
                        ChargerStation(
                            name = name.trim(),
                            lat = currentLocation!!.latitude,
                            lon = currentLocation!!.longitude,
                            address = address,
                            chargingSpeeds = chargingPositions.map { it.speed },
                            imageUri = imageUri?.toString() ?: ""
                        )
                    )
                } else {
                    Toast.makeText(
                        context,
                        "Please fill all required fields and select a location",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ISTBlue)
        ) {
            if (creatingDocumentUiState is UiState.Loading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                Text("Save Charger", color = Color.White)
            }
        }
    }

    if (showBottomSheet) {
        ImagePickerBottomSheet(
            onCameraSelected = {
                if (cameraPermissionGranted) {
                    viewModel.openCamera(context)
                } else {
                    requestCameraPermission()
                }
            },
            onGallerySelected = { viewModel.openGallery(context) },
            onDismiss = { showBottomSheet = false }
        )
    }
}

@Composable
private fun LocationSelectionSection(
    address: String,
    currentLocation: LatLng?,
    locationPermissionGranted: Boolean,
    onMapClick: () -> Unit,
    onCurrentLocationClick: () -> Unit,
    onAddressChange: (String) -> Unit
) {
    Column {
        Text("Location Selection", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onMapClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Pick on Map")
            }

            Button(
                onClick = onCurrentLocationClick,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                enabled = locationPermissionGranted
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Current Location")
            }
        }

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Address") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Icon(Icons.Default.LocationOn, contentDescription = "Address icon")
            }
        )

        if (currentLocation != null) {
            Text(
                text = "Selected location: ${"%.4f".format(currentLocation.latitude)}, " +
                        "${"%.4f".format(currentLocation.longitude)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChargingPositionsSection(
    positions: List<ChargingPosition>,
    onAddPosition: () -> Unit,
    onSpeedChange: (Int, ChargingSpeed) -> Unit
) {
    Column {
        Text("Charging Positions", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        positions.forEachIndexed { index, position ->
            ChargingPositionItem(
                positionNumber = index + 1,
                speed = position.speed,
                onSpeedChange = { onSpeedChange(index, it) }
            )
        }

        Button(
            onClick = onAddPosition,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add position")
            Spacer(Modifier.width(8.dp))
            Text("Add Charging Position")
        }
    }
}

@Composable
private fun ChargingPositionItem(
    positionNumber: Int,
    speed: ChargingSpeed,
    onSpeedChange: (ChargingSpeed) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Position $positionNumber", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ChargingSpeed.values().forEach {
                FilterChip(
                    selected = speed == it,
                    onClick = { onSpeedChange(it) },
                    label = { Text(it.name) }
                )
            }
        }
    }
}*/
