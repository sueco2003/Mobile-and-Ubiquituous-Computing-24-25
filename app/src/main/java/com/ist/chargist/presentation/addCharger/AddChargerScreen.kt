package com.ist.chargist.presentation.addCharger

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.ist.chargist.presentation.components.AddChargerDialog
import com.ist.chargist.presentation.components.PaymentMethodButton
import com.ist.chargist.presentation.components.imageUpload.DocumentUploadItem
import com.ist.chargist.presentation.components.imageUpload.ImageSelectorSheet
import com.ist.chargist.ui.theme.BackgroundColor
import com.ist.chargist.ui.theme.ISTBlue
import com.ist.chargist.ui.theme.TextColor
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
                        text = stringResource(R.string.location_add_title),
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

@OptIn(ExperimentalMaterial3Api::class)
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


    var searchQuery by remember { mutableStateOf("") }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    val chargerLocationState = viewModel.chargerLocation
    val myLocationState = viewModel.myLocation

    var name by remember { mutableStateOf("") }
    var selectedMethods by remember { mutableStateOf<List<String>>(emptyList()) }
    var chargers by remember { mutableStateOf(listOf<ChargerSlot>()) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var fastPrice by remember { mutableStateOf("") }
    var mediumPrice by remember { mutableStateOf("") }
    var slowPrice by remember { mutableStateOf("") }

    val selectedLocation = navController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.getStateFlow<LatLng?>("selected_location", null)
        ?.collectAsState()

    LaunchedEffect(selectedLocation?.value) {
        selectedLocation?.value?.let { latLng ->
            searchQuery = "${latLng.latitude}, ${latLng.longitude}"
        }
    }

    val context = LocalContext.current
    if (showBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showBottomSheet = false }) {
            ImageSelectorSheet(
                onSelectCamera = onSelectCamera,
                onSelectGallery = onSelectGallery,
                onClose = {
                    showBottomSheet = false
                }
            )
        }
    }

    LaunchedEffect(chargerLocationState.value) {
        when (val state = chargerLocationState.value) {
            is UiState.Success -> {
                val locationStrings = state.data as? List<String>
                val lat = locationStrings?.getOrNull(0)?.toFloatOrNull()
                val lon = locationStrings?.getOrNull(1)?.toFloatOrNull()

                if (lat != null && lon != null) {
                    viewModel.createCharger(
                        ChargerStation(
                            name = name.trim(),
                            imageUri = imageUri?.toString(),
                            lat = lat,
                            lon = lon,
                            payment = selectedMethods,
                            slowPrice = slowPrice.toFloatOrNull(),
                            mediumPrice = mediumPrice.toFloatOrNull(),
                            fastPrice = fastPrice.toFloatOrNull(),
                            slotId = emptyList()
                        ),
                        chargers
                    )
                } else if (slowPrice.toFloatOrNull()!! < 0 || mediumPrice.toFloatOrNull()!! < 0 || fastPrice.toFloatOrNull()!! < 0) {
                    Toast.makeText(context, "Prices can´t be negative", Toast.LENGTH_SHORT).show()
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
                val locationStrings = state.data as? List<String>
                val lat = locationStrings?.getOrNull(0)
                val lon = locationStrings?.getOrNull(1)
                if (lat != null && lon != null) {
                    searchQuery = "$lat, $lon"
                }
            }
            is UiState.Error -> {
                Toast.makeText(context, state.message ?: "Invalid address", Toast.LENGTH_SHORT).show()
            }
            else -> { }
        }
    }


    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            DocumentUploadItem(
                onClick = { selectGallery, selectCamera ->
                    showBottomSheet = true
                    viewModel.onSelectGallery = selectGallery
                    viewModel.onSelectCamera = selectCamera
                },
                onImageChosen = {
                    showBottomSheet = false
                    imageUri = it
                },
                imageUriSaved = imageUri
            )
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = {
                    Text(
                        text = stringResource(R.string.charger_name_label_hint),
                        color = TextColor
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = {
                    Text(
                        text = stringResource(R.string.address_label_hint),
                        color = TextColor
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.getCurrentLocation() },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location"
                    )
                }
                Button(
                    onClick = navigateToMapLocationPicker,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Map"
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Payment System",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    PaymentMethodButton(
                        label = "Credit Card",
                        isSelected = "credit" in selectedMethods,
                        onClick = {
                            selectedMethods = selectedMethods.toggle("credit")
                        }
                    )
                    PaymentMethodButton(
                        label = "PayPal",
                        isSelected = "paypal" in selectedMethods,
                        onClick = {
                            selectedMethods = selectedMethods.toggle("paypal")
                        }
                    )
                    PaymentMethodButton(
                        label = "Cash",
                        isSelected = "cash" in selectedMethods,
                        onClick = {
                            selectedMethods = selectedMethods.toggle("cash")
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Prices",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = fastPrice,
                        onValueChange = { fastPrice = it },
                        label = { Text("Fast") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = mediumPrice,
                        onValueChange = { mediumPrice = it },
                        label = { Text("Medium") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = slowPrice,
                        onValueChange = { slowPrice = it },
                        label = { Text("Slow") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Slots", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        chargers = chargers + ChargerSlot(
                            speed = ChargeSpeed.F,
                            connector = Connector.CCS2,
                            available = true
                        )
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Charger")
                    }
                }

                chargers.forEachIndexed { index, charger ->
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Charger ${index + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                chargers = chargers.toMutableList().also { it.removeAt(index) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove Charger"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text("Charge Speed", style = MaterialTheme.typography.labelMedium)
                    Row {
                        ChargeSpeed.entries.forEach { speed ->
                            val selected = speed == charger.speed
                            OutlinedButton(
                                onClick = {
                                    chargers = chargers.mapIndexed { i, c ->
                                        if (i == index) c.copy(speed = speed) else c
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else Color.Transparent
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(speed.name)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Connector", style = MaterialTheme.typography.labelMedium)
                    Row {
                        Connector.entries.forEach { conn ->
                            val selected = conn == charger.connector
                            OutlinedButton(
                                onClick = {
                                    chargers = chargers.mapIndexed { i, c ->
                                        if (i == index) c.copy(connector = conn) else c
                                    }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (selected)
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                    else Color.Transparent
                                ),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(conn.name)
                            }
                        }
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    when {
                        name.isBlank() -> Toast.makeText(
                            context,
                            context.getString(R.string.error_input_cannot_be_empty),
                            Toast.LENGTH_SHORT
                        ).show()

                        searchQuery.isBlank() -> Toast.makeText(
                            context,
                            "Search query cannot be empty",
                            Toast.LENGTH_SHORT
                        ).show()

                        else -> {
                            viewModel.searchLocation(
                                query = searchQuery,
                                context = context
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ISTBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                when (creatingDocumentUiState) {
                    UiState.Loading -> CircularProgressIndicator()
                    is UiState.Error -> {
                        Toast.makeText(context, creatingDocumentUiState.message, Toast.LENGTH_SHORT).show()
                        Text(stringResource(R.string.btn_add_location_button_label), fontSize = 16.sp, color = Color.White)
                    }
                    is UiState.Fail,
                    UiState.Idle -> {
                        Text(stringResource(R.string.btn_add_location_button_label), fontSize = 16.sp, color = Color.White)
                    }
                    is UiState.Success -> {
                        showDialog = true
                        Text(stringResource(R.string.btn_add_location_button_label), fontSize = 16.sp, color = Color.White)
                    }
                }
            }
        }
    }

    if (showDialog) {
        AddChargerDialog(
            navigateBack = {
                navigateBack()
            }
        )
    }
}

// Extension for toggling strings in a set
private fun List<String>.toggle(item: String): List<String> =
    if (item in this) this - item else this + item

