package com.ist.chargist.presentation.map


import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.ist.chargist.presentation.components.ChargerStationPanel
import com.ist.chargist.presentation.components.FilterSortDialog
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

    val isAnonymous = remember { viewModel.isUserAnonymous() }
    val favoriteIds by viewModel.favouriteStationIds


    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        viewModel.handleSearchInput(searchQuery)
    }



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
                        val isFavorite = ((favoriteIds as? UiState.Success)?.data as? List<String>?)?.contains(station.id) == true
                        val hue = if (isFavorite) BitmapDescriptorFactory.HUE_YELLOW else BitmapDescriptorFactory.HUE_RED

                        Marker(
                            state = MarkerState(
                                position = LatLng(station.lat.toDouble(), station.lon.toDouble())
                            ),
                            icon = BitmapDescriptorFactory.defaultMarker(hue),
                            onClick = {
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

        Column(
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            var showFilterDialog by remember { mutableStateOf(false) }
            val filterOptions = remember { mutableStateListOf<String>() }
            var selectedSort by remember { mutableStateOf("distance") }
            var sortAscending by remember { mutableStateOf(true) }

            Row(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBar(
                    modifier = Modifier.weight(1f),
                    query = searchQuery,
                    onQueryChange = {
                        searchQuery = it
                    },
                    onSearch = { viewModel.searchLocation(it, context) },
                    active = false,
                    onActiveChange = {},
                    placeholder = { Text("Search stations or location...") }
                ) {}

                IconButton(
                    onClick = { showFilterDialog = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter & Sort Stations"
                    )
                }
            }

            // Filter/Sort Dialog
            if (showFilterDialog) {
                FilterSortDialog(
                    currentFilters = filterOptions,
                    selectedSort = selectedSort,
                    sortAscending = sortAscending,
                    onDismiss = { showFilterDialog = false },
                    onApply = { filters, sort, ascending ->
                        filterOptions.clear()
                        filterOptions.addAll(filters)
                        selectedSort = sort
                        sortAscending = ascending
                        viewModel.applyFiltersAndSort(filters, sort, ascending)
                    }
                )
            }

            // Display search results
            if (viewModel.searchQuery.isNotEmpty()) {
                StationSearchResults(
                    stations = viewModel.filteredStations.value, // Access .value here
                    onStationClick = { station ->
                        selectedStation = station
                        showPanel = false
                        viewModel.moveToStation(station)
                        searchQuery = ""
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .padding(horizontal = 16.dp)
                )
            }
        }



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
                            .clickable(enabled = false) {}

                            .heightIn(max = 400.dp) // adjust max height as needed
                            .verticalScroll(rememberScrollState())
                    ) {
                        val isFavorite = ((favoriteIds as? UiState.Success)?.data as? List<String>?)?.contains(selectedStation!!.id) == true
                        ChargerStationPanel(
                            station = selectedStation!!,
                            isAnonymous = isAnonymous,
                            viewModel = viewModel,
                            isFavorite = isFavorite,
                            onToggleFavorite = { viewModel.toggleFavorite(selectedStation!!.id) },
                            onDismiss = {     selectedStation = null
                                showPanel = false }
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

            // Add Station FAB
            if (!isAnonymous) {
                FloatingActionButton(
                    onClick = navigateToAddChargerStation,
                    modifier = Modifier
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Add, "Add Station")
                }
            }
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
                onClick = onLogoutClick,
                modifier = Modifier
                    .padding(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, "Logout")
            }
        }
    }
}

@Composable
fun StationSearchResults(
    stations: List<ChargerStation>,
    onStationClick: (ChargerStation) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        LazyColumn {
            // Add the count parameter first
            items(
                count = stations.size,  // This is the Int parameter
                key = { index -> stations[index].id }  // Add key for better performance
            ) { index ->
                StationSearchItem(stations[index], onStationClick)
            }
        }
    }
}

@Composable
fun StationSearchItem(
    station: ChargerStation,
    onClick: (ChargerStation) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(station) },
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${station.fastPrice?.let { "F: €$it " }} " +
                        "${station.mediumPrice?.let { "M: €$it " }} " +
                        "${station.slowPrice?.let { "S: €$it " }}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    Divider()
}