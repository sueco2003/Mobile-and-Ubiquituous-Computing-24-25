package com.ist.chargist.presentation.map


import ChargerStationPanel
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ist.chargist.domain.model.ChargerStation
import com.ist.chargist.utils.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    onLogoutClick: () -> Unit,
    navigateToAddChargerStation: () -> Unit,
    onChargerStationClick: ((ChargerStation) -> Unit)?
) {
    val context = LocalContext.current
    val chargerStationsUiState by viewModel.chargerStationList
    val cameraPosition by viewModel.cameraPosition.collectAsState()
    val forceUpdate by viewModel.locationUpdates.collectAsState()
    val activity = context as Activity





    var selectedStation by remember { mutableStateOf<ChargerStation?>(null) }
    var showPanel by remember { mutableStateOf(false) }



    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(
                context,
                "Location permission required for map features",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    // Check permission when screen loads
    LaunchedEffect(Unit) {
        when {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted - proceed
            }

            else -> {
                // Request permission directly
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    // Map camera control
    val cameraState = rememberCameraPositionState()

    LaunchedEffect(cameraPosition, forceUpdate) {
        cameraPosition?.let { latLng ->
            cameraState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .target(latLng)
                        .zoom(15f)
                        .build()
                ),
                1000
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState
        ) {
            // Existing marker code...
            when (chargerStationsUiState) {
                is UiState.Success -> {
                    ((chargerStationsUiState as? UiState.Success)?.data as? List<ChargerStation>)?.forEach { station ->
                        Marker(
                            state = MarkerState(
                                position = LatLng(station.lat.toDouble(), station.lon.toDouble())
                            ),
                            title = station.name,
                            icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                            onClick = {
                                Log.d("MapScreen", "Marker clicked: ${station.name}")
                                selectedStation = station
                                showPanel = true
                                false
                            }
                        )
                    }
                }
                is UiState.Fail -> {
                    Toast.makeText(
                        context,
                        (chargerStationsUiState as UiState.Fail).message,
                        Toast.LENGTH_SHORT
                    ).show()
                    null
                }

                is UiState.Error,
                UiState.Idle,
                UiState.Loading -> {
                    null
                }
            }
        }

        // Search Bar
        var searchQuery by remember { mutableStateOf("") }
        SearchBar(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { viewModel.searchLocation(it, context) },
            active = false,
            onActiveChange = {},
            placeholder = { Text("Search location...") }
        ) {}


        if (showPanel && selectedStation != null) {
            Popup(alignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxSize()
                        // Transparent clickable background to detect outside clicks
                        .clickable(
                            onClick = {
                                showPanel = false
                                selectedStation = null
                            },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                ) {
                    // The panel itself - stop click events from propagating outside
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .clickable(enabled = false) {} // consume clicks so they don't dismiss the popup
                    ) {
                        ChargerStationPanel(
                            station = selectedStation!!,
                            viewModel = viewModel,
                            onDismiss = { selectedStation = null }
                        )
                    }
                }
            }
        }


        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(2.dp),
            verticalArrangement = Arrangement.Bottom
        ){

            // Current Location FAB
            FloatingActionButton(
                onClick = { viewModel.getCurrentLocation() },
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.MyLocation, "Current Location")
            }

            // Add Station FAB
            FloatingActionButton(
                onClick = navigateToAddChargerStation,
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Add, "Add Station")
            }
            // Add Station FAB
            FloatingActionButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, "Logout")
            }
        }
    }
}
