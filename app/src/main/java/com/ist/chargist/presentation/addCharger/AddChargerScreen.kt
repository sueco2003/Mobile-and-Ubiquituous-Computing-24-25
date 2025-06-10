package com.ist.chargist.presentation.addCharger

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.model.LatLng
import com.ist.chargist.R
import com.ist.chargist.domain.model.ChargeSpeed
import com.ist.chargist.domain.model.ChargerSlot
import com.ist.chargist.domain.model.ChargerStation
import com.ist.chargist.domain.model.Connector
import com.ist.chargist.presentation.components.ChargerAddedDialog
import com.ist.chargist.presentation.components.PaymentMethodButton
import com.ist.chargist.presentation.components.imageUpload.DocumentUploadItem
import com.ist.chargist.presentation.components.imageUpload.ImageSelectorSheet
import com.ist.chargist.ui.theme.BackgroundColor
import com.ist.chargist.ui.theme.ISTBlue
import com.ist.chargist.ui.theme.MutedTextColor
import com.ist.chargist.utils.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddChargerScreen(
    viewModel: AddChargerViewModel,
    navigateToMapLocationPicker: () -> Unit,
    navigateBack: () -> Unit,
    navController: NavController
) {
    val context = LocalContext.current
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    val requestCameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        cameraPermissionGranted = isGranted
        if (!isGranted) {
            Toast.makeText(
                context,
                context.getString(R.string.error_camera_unavailable),
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    val creatingDocumentUiState by viewModel.creatingCharger
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back_arrow_content_description)
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(R.string.charger_add_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        }
    ) { paddingValues ->
        // Let AddChargerContent manage its own scrolling
        AddChargerContent(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(BackgroundColor),
            viewModel = viewModel,
            navController = navController,
            creatingDocumentUiState = creatingDocumentUiState,
            onSelectCamera = {
                if (!cameraPermissionGranted) {
                    requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                } else {
                    viewModel.onSelectCamera?.invoke()
                }
            },
            onSelectGallery = {
                viewModel.onSelectGallery?.invoke()
            },
            navigateBack = navigateBack,
            navigateToMapLocationPicker = navigateToMapLocationPicker
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddChargerContent(
    modifier: Modifier = Modifier,
    viewModel: AddChargerViewModel,
    creatingDocumentUiState: UiState,
    onSelectCamera: () -> Unit,
    onSelectGallery: () -> Unit,
    navigateBack: () -> Unit,
    navController: NavController,
    navigateToMapLocationPicker: () -> Unit,
) {
    val context = LocalContext.current

    var locationQuery by remember { mutableStateOf("") }
    var showImageSelectionSheet by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val chargerLocationState = viewModel.chargerLocation
    val myLocationState = viewModel.myLocation

    val selectedLocation = navController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<LatLng?>("selected_location", null)
        ?.collectAsState()

    LaunchedEffect(chargerLocationState.value) {
        when (val state = chargerLocationState.value) {
            is UiState.Success -> {
                val location = state.data as? Pair<Double, Double>
                val lat = location?.first
                val lon = location?.second

                if (lat != null && lon != null) {
                    viewModel.createCharger(
                        ChargerStation(
                            name = viewModel.name.trim(),
                            imageUri = viewModel.imageUri?.toString(),
                            lat = lat.toFloat(),
                            lon = lon.toFloat(),
                            payment = viewModel.selectedMethods,
                            slowPrice = viewModel.slowPrice.toFloatOrNull() ?: -1.0f,
                            mediumPrice = viewModel.mediumPrice.toFloatOrNull() ?: -1.0f,
                            fastPrice = viewModel.fastPrice.toFloatOrNull() ?: -1.0f,
                            slotId = emptyList()
                        ),
                        viewModel.chargingSlots
                    )
                }
                else {
                    Toast.makeText(context, "Invalid location data", Toast.LENGTH_SHORT).show()
                }
            }
            is UiState.Error -> {
                Toast.makeText(context, state.message ?: "Invalid address", Toast.LENGTH_SHORT).show()
            }
            else -> { }
        }
    }

    LaunchedEffect(myLocationState.value) {
        when (val state = myLocationState.value) {
            is UiState.Success -> {
                val locationStrings = state.data as? Pair<Double, Double>
                val lat = locationStrings?.first
                val lon = locationStrings?.second
                if (lat != null && lon != null) {
                    locationQuery = "%.4f, %.4f".format(lat, lon)
                }
            }
            is UiState.Error -> {
                Toast.makeText(context, state.message ?: "Invalid address", Toast.LENGTH_SHORT).show()
            }
            else -> { }
        }
    }

    LaunchedEffect(selectedLocation?.value) {
        selectedLocation?.value?.let { latLng ->
            locationQuery = "%.4f, %.4f".format(latLng.latitude, latLng.longitude)
        }
    }

    fun handleFormSubmit() {
        when {
            viewModel.name.isBlank() -> Toast.makeText(
                context,
                context.getString(R.string.error_input_cannot_be_empty),
                Toast.LENGTH_SHORT
            ).show()

            locationQuery.isBlank() -> Toast.makeText(
                context,
                "Station location cannot be empty",
                Toast.LENGTH_SHORT
            ).show()

            !isValidPriceString(viewModel.slowPrice) ||
                    !isValidPriceString(viewModel.mediumPrice) ||
                    !isValidPriceString(viewModel.fastPrice) -> Toast.makeText(
                context,
                "Prices need to be positive numbers",
                Toast.LENGTH_SHORT
            ).show()

            viewModel.chargingSlots.isEmpty() -> Toast.makeText(
                context,
                "Station must contain at least one slot",
                Toast.LENGTH_SHORT
            ).show()

            else -> {
                viewModel.searchLocation(
                    query = locationQuery,
                    context = context
                )
            }
        }
    }

    if (showImageSelectionSheet) {
        ModalBottomSheet(onDismissRequest = { showImageSelectionSheet = false }) {
            ImageSelectorSheet(
                onSelectCamera = onSelectCamera,
                onSelectGallery = onSelectGallery
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            DocumentUploadItem(
                onClick = { selectGallery, selectCamera ->
                    showImageSelectionSheet = true
                    viewModel.onSelectGallery = selectGallery
                    viewModel.onSelectCamera = selectCamera
                },
                onImageChosen = {
                    showImageSelectionSheet = false
                    viewModel.updateImageUri(it)
                },
                imageUriSaved = viewModel.imageUri
            )
        }

        item {
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.updateName(it) },
                label = {
                    Text(
                        text = stringResource(R.string.charger_name_label_hint),
                        color = MutedTextColor,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
        }

        item {
            Column (
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = locationQuery,
                    onValueChange = { locationQuery = it },
                    label = {
                        Text(
                            text = stringResource(R.string.address_label_hint),
                            color = MutedTextColor
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { viewModel.getCurrentLocation() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Use Current Location"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Use My Location")
                    }

                    Button(
                        onClick = navigateToMapLocationPicker,
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Pick on Map"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick on Map")
                    }
                }
            }
        }

        item {
            Column (
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Payment Methods",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    PaymentMethodButton(
                        label = "Cash",
                        isSelected = "cash" in viewModel.selectedMethods,
                        onClick = {
                            viewModel.updateSelectedMethods(viewModel.selectedMethods.toggle("cash"))
                        }
                    )
                    PaymentMethodButton(
                        label = "Credit Card",
                        isSelected = "credit" in viewModel.selectedMethods,
                        onClick = {
                            viewModel.updateSelectedMethods(viewModel.selectedMethods.toggle("credit"))
                        }
                    )
                    PaymentMethodButton(
                        label = "PayPal",
                        isSelected = "paypal" in viewModel.selectedMethods,
                        onClick = {
                            viewModel.updateSelectedMethods(viewModel.selectedMethods.toggle("paypal"))
                        }
                    )
                }
            }
        }

        item {
            Column (
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Prices",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.slowPrice,
                        onValueChange = { viewModel.updateSlowPrice(it) },
                        label = {
                            Text("Slow", color = MutedTextColor)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = viewModel.mediumPrice,
                        onValueChange = { viewModel.updateMediumPrice(it) },
                        label = {
                            Text("Medium", color = MutedTextColor)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    OutlinedTextField(
                        value = viewModel.fastPrice,
                        onValueChange = { viewModel.updateFastPrice(it) },
                        label = {
                            Text("Fast", color = MutedTextColor)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }

        item {
            Column (
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Nearby Services",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = viewModel.nearbyServiceText,
                        onValueChange = { viewModel.updateNearbyServiceText(it) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Service Name", color = MutedTextColor) },
                        shape = RoundedCornerShape(16.dp)
                    )
                    Button(
                        onClick = {
                            if (viewModel.nearbyServiceText.isNotBlank()) {
                                viewModel.updateNearbyServices(viewModel.nearbyServices + viewModel.nearbyServiceText)
                                viewModel.updateNearbyServiceText("")
                            }
                        },
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Add")
                    }
                }
                if (viewModel.nearbyServices.isNotEmpty()) {
                    FlowRow (
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        viewModel.nearbyServices.forEach { service ->
                            Box(
                                modifier = Modifier
                                    .background(color = ISTBlue.copy(alpha = 0.9f), shape = RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .padding(12.dp, 8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = service, color = Color.White)
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier
                                            .height(20.dp)
                                            .clickable{ viewModel.updateNearbyServices(viewModel.nearbyServices - service) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Charging Slots",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    IconButton(onClick = {
                        viewModel.updateChargingSlots(
                            viewModel.chargingSlots + ChargerSlot(
                                speed = ChargeSpeed.F,
                                connector = Connector.CCS2,
                                available = true
                            )
                        )
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Charger")
                    }
                }

                viewModel.chargingSlots.forEachIndexed { index, charger ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(color = Color(0xFFF0F0F0), shape = RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .padding(16.dp, 4.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Slot ${index + 1}",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            IconButton(
                                onClick = {
                                    viewModel.updateChargingSlots(
                                        viewModel.chargingSlots.toMutableList().also { it.removeAt(index) }
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove Charger",
                                    tint = Color.Red
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Speed:", style = MaterialTheme.typography.titleSmall, modifier= Modifier.weight(1f))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChargeSpeed.entries.forEach { speed ->
                                    val selected = speed == charger.speed
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.updateChargingSlots(
                                                viewModel.chargingSlots.mapIndexed { i, c ->
                                                    if (i == index) c.copy(speed = speed) else c
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (selected)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                    ) {
                                        Text(speed.name)
                                    }
                                }
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Connector:", style = MaterialTheme.typography.titleSmall, modifier= Modifier.weight(1f))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Connector.entries.forEach { conn ->
                                    val selected = conn == charger.connector
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.updateChargingSlots(
                                                viewModel.chargingSlots.mapIndexed { i, c ->
                                                    if (i == index) c.copy(connector = conn) else c
                                                }
                                            )
                                        },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (selected)
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else Color.Transparent
                                        )
                                    ) {
                                        Text(conn.name)
                                    }
                                }
                            }
                        }
                    }

                }
            }
        }

        item {
            Button(
                onClick = { handleFormSubmit() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ISTBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                when (creatingDocumentUiState) {
                    UiState.Loading -> CircularProgressIndicator()
                    is UiState.Error -> {
                        Toast.makeText(context, creatingDocumentUiState.message, Toast.LENGTH_SHORT).show()
                        Text(stringResource(R.string.btn_add_station_button_label), fontSize = 16.sp, color = Color.White)
                    }
                    is UiState.Fail,
                    UiState.Idle -> {
                        Text(stringResource(R.string.btn_add_station_button_label), fontSize = 16.sp, color = Color.White)
                    }
                    is UiState.Success -> {
                        showSuccessDialog = true
                        Text(stringResource(R.string.btn_add_station_button_label), fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }

    if (showSuccessDialog) {
        ChargerAddedDialog (
            navigateBack = {
                navigateBack()
            }
        )
    }
}

private fun isValidPriceString(priceString: String): Boolean {
    if (priceString.isBlank()) return true // no price provided is valid
    val price = priceString.toFloatOrNull()
    return price != null && price >= 0
}

// Extension for toggling strings in a set
private fun List<String>.toggle(item: String): List<String> =
    if (item in this) this - item else this + item
