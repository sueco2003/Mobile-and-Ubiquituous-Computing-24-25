package com.ist.chargist.presentation.map

import android.util.Log
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.ist.chargist.domain.model.ChargerStation
import com.ist.chargist.presentation.components.ChargerStationPanel
import com.ist.chargist.presentation.components.FilterSortDialog
import com.ist.chargist.utils.UiState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.foundation.layout.size

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    viewModel: MapViewModel = viewModel(),
    onLogoutClick: () -> Unit,
    navigateToAddChargerStation: () -> Unit,
    onChargerStationClick: ((ChargerStation) -> Unit)?
) {
    val context = LocalContext.current

    val mapStationsUiState by viewModel.mapStations

    val searchResultsUiState by viewModel.searchResults

    val cameraPosition by viewModel.cameraPosition.collectAsState()
    val forceUpdate by viewModel.locationUpdates.collectAsState()
    val activity = context as Activity

    val isAnonymous = remember { viewModel.isUserAnonymous() }
    val favoriteIds by viewModel.favouriteStationIds

    var searchQuery by remember { mutableStateOf("") }
    val isLoadingMore by viewModel.isLoadingMoreSearch
    val hasMoreData by viewModel.hasMoreSearchData

    LaunchedEffect(searchQuery) {
        viewModel.handleSearchInput(searchQuery)
    }

    var selectedStationId by remember { mutableStateOf("") }
    var showPanel by remember { mutableStateOf(false) }
    var closestStation by remember { mutableStateOf<ChargerStation?>(null) }
    var location by remember { mutableStateOf<LatLng?>(null) }

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

                viewModel.getAllStationsForMap()

                // 2. Wait for location
                val locationDeferred = CompletableDeferred<LatLng?>()
                viewModel.getCurrentLocation { latLng ->
                    locationDeferred.complete(latLng)
                }
                location = locationDeferred.await()

                // 3. Then fetch favorites
                viewModel.getFavorites()
            }

            else -> {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }
    }

    LaunchedEffect(location) {
        location?.let { userLoc ->
            val nearbyStation = ((mapStationsUiState as? UiState.Success)?.data as? List<ChargerStation>)
                ?.firstOrNull { station ->
                    val stationLoc = LatLng(station.lat.toDouble(), station.lon.toDouble())
                    FloatArray(1).also {
                        Location.distanceBetween(
                            userLoc.latitude, userLoc.longitude,
                            stationLoc.latitude, stationLoc.longitude,
                            it
                        )
                    }[0] <= 1000
                }

            if (nearbyStation != closestStation) {
                closestStation = nearbyStation
                showPanel = true
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
            cameraPositionState = cameraState,
            properties = MapProperties(
                isMyLocationEnabled = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ),
            uiSettings = MapUiSettings(
                myLocationButtonEnabled = false,
                zoomControlsEnabled = false)
        ){

            when (mapStationsUiState) {
                is UiState.Success -> {
                    ((mapStationsUiState as? UiState.Success)?.data as? List<ChargerStation>)?.forEach { station ->
                        val isFavorite = ((favoriteIds as? UiState.Success)?.data as? List<String>?)?.contains(station.id) == true
                        val hue = if (isFavorite) BitmapDescriptorFactory.HUE_YELLOW else BitmapDescriptorFactory.HUE_RED
                        Marker(
                            state = MarkerState(
                                position = LatLng(station.lat.toDouble(), station.lon.toDouble())
                            ),
                            icon = BitmapDescriptorFactory.defaultMarker(hue),
                            onClick = {
                                selectedStationId = station.id
                                showPanel = true
                                false
                            }
                        )
                    }
                }
                is UiState.Fail -> {
                    Toast.makeText(
                        context,
                        (mapStationsUiState as UiState.Fail).message,
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
            var showChargerStationResults by remember { mutableStateOf(false) }

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
                        showChargerStationResults = true
                    },
                    onSearch = { viewModel.searchLocation(it, context) },
                    active = false,
                    onActiveChange = {
                        showChargerStationResults = it
                        if (it) {
                            viewModel.triggerInitialSearch()
                        }
                    },
                    placeholder = { Text("Search stations or location...") },
                    trailingIcon = {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter & Sort Stations"
                            )
                        }
                    }
                ) {}
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
                        showChargerStationResults = true
                    }
                )
            }

            if (showChargerStationResults) {
                val searchStations = when (val state = searchResultsUiState) {
                    is UiState.Success -> state.data as? List<ChargerStation> ?: emptyList()
                    else -> emptyList()
                }

                Popup(alignment = Alignment.Center) {
                    Box(
                        Modifier.clickable(
                            onClick = {
                                showChargerStationResults = false
                            })
                    ) {
                        Box(
                            Modifier
                                .heightIn(600.dp)
                                .padding(vertical = 90.dp)
                        ) {
                            StationSearchResults(
                                stations = searchStations,
                                onStationClick = { station ->
                                    selectedStationId = station.id
                                    showPanel = true
                                    viewModel.moveToStation(station)
                                    searchQuery = ""
                                    showChargerStationResults = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .padding(horizontal = 16.dp),
                                viewModel = viewModel,
                                isLoadingMore = isLoadingMore,
                                hasMoreData = hasMoreData
                            )
                        }
                    }
                }
            }
        }

        if (showPanel && selectedStationId.isNotEmpty()) {
            Popup(alignment = Alignment.Center) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clickable(
                            onClick = {
                                showPanel = false
                                selectedStationId = ""
                            },
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        )
                ) {
                    Box(
                        Modifier
                            .align(Alignment.Center)
                            .clickable(enabled = false) {}
                            .heightIn(max = 400.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        val isFavorite = ((favoriteIds as? UiState.Success)?.data as? List<String>?)?.contains(selectedStationId) == true
                        ChargerStationPanel(
                            stationId = selectedStationId,
                            isAnonymous = isAnonymous,
                            viewModel = viewModel,
                            isFavorite = isFavorite,
                            onToggleFavorite = { viewModel.toggleFavorite(selectedStationId) },
                            onDismiss = {
                                selectedStationId = ""
                                showPanel = false
                            }
                        )
                    }
                }
            }
        }

        if (closestStation != null) {
            val backgroundColor = remember { Animatable(Color.White) }

            LaunchedEffect(closestStation) {
                repeat(5) {
                    backgroundColor.animateTo(
                        targetValue = Color(0xFFFFF59D), // light yellow
                        animationSpec = tween(durationMillis = 250)
                    )
                    backgroundColor.animateTo(
                        targetValue = Color.White,
                        animationSpec = tween(durationMillis = 250)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 100.dp)
                    .widthIn(max = 300.dp)
                    .background(backgroundColor.value, shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Color.Gray, shape = RoundedCornerShape(8.dp))
                    .clickable {
                        viewModel.moveToStation(closestStation!!)
                        closestStation = null
                    }
                    .padding(12.dp)
            ) {
                Column {
                    Text(text = closestStation!!.name, fontWeight = FontWeight.Bold)
                    Text(
                        text = "Lat: %.5f, Lon: %.5f".format(
                            closestStation!!.lat.toDouble(), closestStation!!.lon.toDouble()
                        ),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Payments: ${closestStation!!.payment.joinToString()}",
                        fontSize = 12.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 2.dp, bottom = 50.dp),
            verticalArrangement = Arrangement.Bottom
        ){
            if (!isAnonymous) {
                FloatingActionButton(
                    onClick = navigateToAddChargerStation,
                    modifier = Modifier.padding(8.dp)
                ) {
                    Icon(Icons.Default.Add, "Add Station")
                }
            }

            FloatingActionButton(
                onClick = { viewModel.getCurrentLocation{} },
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(Icons.Default.MyLocation, "Current Location")
            }

            FloatingActionButton(
                onClick = onLogoutClick,
                modifier = Modifier.padding(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, "Logout")
            }
        }

        ZoomControls(cameraState)
    }
}

@Composable
fun ZoomControls(
    cameraState: CameraPositionState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val backgroundColor = MaterialTheme.colorScheme.primaryContainer

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ){
        Surface(
            modifier = modifier
                .padding(end = 10.dp, bottom = 58.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            color = backgroundColor
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(8.dp)
            ) {
                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            cameraState.animate(CameraUpdateFactory.zoomIn())
                        }
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In")
                }

                IconButton(
                    onClick = {
                        coroutineScope.launch {
                            cameraState.animate(CameraUpdateFactory.zoomOut())
                        }
                    }
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out")
                }
            }
        }
    }
}



@Composable
fun StationSearchResults(
    stations: List<ChargerStation>,
    onStationClick: (ChargerStation) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MapViewModel, // Add viewModel parameter
    isLoadingMore: Boolean = false,
    hasMoreData: Boolean = true
) {
    Card(
        modifier = modifier.heightIn(max = 500.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        LazyColumn {
            items(
                count = stations.size,
                key = { index -> stations[index].id }
            ) { index ->
                StationSearchItem(stations[index], onStationClick)

                if (index >= stations.size - 3 && hasMoreData && !isLoadingMore) {
                    LaunchedEffect(stations.size) {
                        viewModel.loadMoreSearchResults()
                    }
                }
            }

            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            if (!hasMoreData && stations.isNotEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No more results",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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
                text = "${station.fastPrice.takeIf { it >= 0 }?.let { "F: €$it " } ?: ""}" +
                        "${station.mediumPrice.takeIf { it >= 0 }?.let { "M: €$it " } ?: ""}" +
                        "${station.slowPrice.takeIf { it >= 0 }?.let { "S: €$it " } ?: ""}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
    Divider()
}

