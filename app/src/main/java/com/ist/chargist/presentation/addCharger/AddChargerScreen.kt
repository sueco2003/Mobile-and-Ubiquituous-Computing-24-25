package com.ist.chargist.presentation.addCharger

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng

import com.ist.chargist.R
import com.ist.chargist.domain.model.ChargerStation
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
    navController: NavController,
    onSaveClicked: (ChargerStation) -> Unit
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
            TopAppBar(navigationIcon = {
                IconButton(onClick = navigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.btn_back_arrow_content_description)
                    )
                }
            }, title = {
                Text(
                    text = stringResource(R.string.location_add_title),
                    fontWeight = FontWeight.Bold
                )

            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = BackgroundColor)
            )
        }) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(top = paddingValues.calculateTopPadding())
                .background(BackgroundColor)
                .fillMaxSize()
        ) {
            AddChargerContent(
                modifier = Modifier.fillMaxSize(),
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
                navigateToMapLocationPicker = navigateToMapLocationPicker,
                onSaveClicked = {
                    onSaveClicked(it)
                }
            )
        }
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
    onSaveClicked: (ChargerStation) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf<String?>("credit") }
    var searchQuery by remember { mutableStateOf("") }
    var showBottomSheet by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val chargerLocationState = viewModel.chargerLocation
    val myLocationState = viewModel.myLocation

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
                    onSaveClicked(
                        ChargerStation(
                            name = name.trim(),
                            imageUri = imageUri?.toString(),
                            lat = lat,
                            lon = lon,
                            payment = selectedMethod.toString()
                        )
                    )
                } else {
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


    Column(
        modifier = modifier
            .padding(16.dp)
    ) {
        Column(Modifier.weight(9f)) {
            Spacer(modifier = Modifier.height(16.dp))
            DocumentUploadItem(
                onClick = { onSelectGallery, onSelectCamera ->
                    showBottomSheet = true
                    viewModel.onSelectCamera = onSelectCamera
                    viewModel.onSelectGallery = onSelectGallery
                },
                onImageChosen = {
                    showBottomSheet = false
                    imageUri = it
                },
                imageUriSaved = imageUri
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                },
                label = {
                    Text(
                        text = stringResource(R.string.charger_name_label_hint),
                        color = TextColor
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                },
                label = {
                    Text(
                        text = stringResource(R.string.address_label_hint),
                        color = TextColor
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { viewModel.getCurrentLocation() },
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location"
                    )
                }

                Button(
                    onClick = { navigateToMapLocationPicker() },
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Map"
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(
                        color = Color(0xFFF0F0F0), // Light gray background
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp) // Inner padding
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
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
                            isSelected = selectedMethod == "credit",
                            onClick = { selectedMethod = "credit" }
                        )

                        PaymentMethodButton(
                            label = "PayPal",
                            isSelected = selectedMethod == "paypal",
                            onClick = { selectedMethod = "paypal" }
                        )
                    }
                }
            }
        }

        Column(Modifier.weight(1f)) {
            Button(
                onClick = {
                    when {
                        name.isEmpty() -> Toast.makeText(
                            context,
                            context.getString(R.string.error_input_cannot_be_empty),
                            Toast.LENGTH_SHORT
                        ).show()

                        searchQuery.isEmpty() -> Toast.makeText(
                            context,
                            "Search query cannot be empty",
                            Toast.LENGTH_SHORT
                        ).show()

                        else -> {
                            viewModel.searchLocation(searchQuery, context)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors().copy(containerColor = ISTBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                when (creatingDocumentUiState) {
                    UiState.Loading -> {
                        CircularProgressIndicator()
                    }

                    is UiState.Error -> {
                        Toast.makeText(
                            context,
                            creatingDocumentUiState.message,
                            Toast.LENGTH_SHORT
                        ).show()

                        Text(
                            text = stringResource(R.string.btn_add_location_button_label),
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                    is UiState.Fail,
                    UiState.Idle ->{
                        Text(
                            text = stringResource(R.string.btn_add_location_button_label),
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                    is UiState.Success -> {
                        showDialog = true
                        Text(
                            text = stringResource(R.string.btn_add_location_button_label),
                            fontSize = 16.sp,
                            color = Color.White
                        )
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

