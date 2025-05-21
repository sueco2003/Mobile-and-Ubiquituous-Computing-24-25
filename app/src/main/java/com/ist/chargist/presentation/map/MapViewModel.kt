package com.ist.chargist.presentation.map


import android.content.Context
import android.location.Geocoder
import android.location.Location
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.ist.chargist.domain.AuthenticationRepository
import com.ist.chargist.domain.DatabaseRepository
import com.ist.chargist.domain.model.ChargeSpeed
import com.ist.chargist.domain.model.ChargerSlot
import com.ist.chargist.domain.model.ChargerStation
import com.ist.chargist.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val authRepo: AuthenticationRepository,
    private val dbRepo: DatabaseRepository,
    private val locationClient: FusedLocationProviderClient
) : ViewModel() {

    // State Management
    private val _chargerStationList = mutableStateOf<UiState>(UiState.Idle)
    val chargerStationList: State<UiState> get() = _chargerStationList

    private val _favouriteStationIds = mutableStateOf<UiState>(UiState.Idle)
    val favouriteStationIds: State<UiState> get() = _favouriteStationIds

    private val _cameraPosition = MutableStateFlow<LatLng?>(null)
    val cameraPosition = _cameraPosition.asStateFlow()

    private val _slotLocation = mutableStateOf<UiState>(UiState.Idle)
    val slotLocation: State<UiState> get() = _slotLocation

    private val _errors = MutableSharedFlow<String>()

    private val _forceLocationUpdate = MutableStateFlow(0)
    val locationUpdates = _forceLocationUpdate.asStateFlow()

    // Search & Filter State
    private val _allStations = mutableStateListOf<ChargerStation>()
    private val _filteredStations = mutableStateOf<List<ChargerStation>>(emptyList())
    private val _searchQuery = mutableStateOf("")
    private val _activeFilters = mutableStateListOf<String>()
    private var _selectedSort by mutableStateOf("distance")
    private var _sortAscending by mutableStateOf(true)
    private val _stationSlotsCache = mutableStateMapOf<String, List<ChargerSlot>>()
    private val _userLocation = mutableStateOf<LatLng?>(null)

    val filteredStations: State<List<ChargerStation>> get() = _filteredStations
    val searchQuery: String get() = _searchQuery.value

    init {
        getChargerStations()
        getFavorites()
        getCurrentLocation()
    }


    // Station Data Management
    private fun getChargerStations() {
        viewModelScope.launch {
            _chargerStationList.value = UiState.Loading
            try {
                dbRepo.getChargerStations()
                    .onSuccess { stations ->
                        _allStations.clear()
                        _allStations.addAll(stations)
                        updateFilteredStations()
                        _chargerStationList.value = UiState.Success(stations)
                    }
                    .onFailure {
                        _chargerStationList.value = UiState.Fail(it.message ?: "Unknown error")
                    }
            } catch (e: Exception) {
                _chargerStationList.value = UiState.Fail(e.message ?: "Loading failed")
            }
        }
    }

    // Search & Filter Implementation
    fun handleSearchInput(query: String) {
        _searchQuery.value = query
        viewModelScope.launch {
            updateFilteredStations()
        }
    }

    fun applyFiltersAndSort(filters: List<String>, sort: String, ascending: Boolean) {
        _activeFilters.clear()
        _activeFilters.addAll(filters)
        _selectedSort = sort
        _sortAscending = ascending
        updateFilteredStations()
    }

    private fun updateFilteredStations() {
        viewModelScope.launch {
            // Base name filter
            val nameFiltered = if (_searchQuery.value.isNotBlank()) {
                _allStations.filter { it.name.contains(_searchQuery.value, true) }
            } else {
                _allStations
            }

            // Pre-fetch required slots
            val stationsToProcess = nameFiltered.take(100) // Limit for performance
            prefetchSlots(stationsToProcess)

            // Apply filters
            val filtered = stationsToProcess.filter { station ->
                val slots = _stationSlotsCache[station.id] ?: emptyList()
                _activeFilters.all { filter ->
                    when (filter) {
                        "available" -> slots.any { it.available }
                        "fast" -> slots.any { it.speed == ChargeSpeed.F && it.available }
                        "medium" -> slots.any { it.speed == ChargeSpeed.M && it.available }
                        "slow" -> slots.any { it.speed == ChargeSpeed.S && it.available }
                        else -> true
                    }
                }
            }

            // Apply sorting
            val sorted = when (_selectedSort) {
                "price" -> filtered.sortedBy { getMinPrice(it) }
                "distance" -> filtered.sortedBy { calculateDistance(it) }
                "speed" -> filtered.sortedByDescending { getMaxSpeed(it) }
                else -> filtered
            }

            _filteredStations.value = if (_sortAscending) sorted else sorted.reversed()
        }
    }

    private suspend fun prefetchSlots(stations: List<ChargerStation>) {
        coroutineScope {
            stations.map { station ->
                async<Unit> {
                    if (!_stationSlotsCache.containsKey(station.id)) {
                        dbRepo.getSlotsForStation(station.id)
                            .onSuccess { slots ->
                                _stationSlotsCache[station.id] = slots
                            }
                            .onFailure {
                                Timber.e("Error fetching slots for ${station.id}")
                            }
                    }
                }
            }.awaitAll()
        }
    }
    // Sorting Helpers
    private fun getMinPrice(station: ChargerStation): Double {
        val slots = _stationSlotsCache[station.id] ?: return Double.MAX_VALUE
        return slots.filter { it.available }
            .minOfOrNull { getPriceForSpeed(station, it.speed) }
            ?: Double.MAX_VALUE
    }

    private fun getPriceForSpeed(station: ChargerStation, speed: ChargeSpeed): Double {
        return when (speed) {
            ChargeSpeed.F -> station.fastPrice?.toDouble() ?: Double.MAX_VALUE
            ChargeSpeed.M -> station.mediumPrice?.toDouble() ?: Double.MAX_VALUE
            ChargeSpeed.S -> station.slowPrice?.toDouble() ?: Double.MAX_VALUE
        }
    }

    private fun getMaxSpeed(station: ChargerStation): Int {
        return (_stationSlotsCache[station.id] ?: emptyList())
            .filter { it.available }
            .maxOfOrNull { it.speed.ordinal } ?: -1
    }

    private fun calculateDistance(station: ChargerStation): Double {
        return _userLocation.value?.let { userLoc ->
            val results = FloatArray(1)
            Location.distanceBetween(
                userLoc.latitude,
                userLoc.longitude,
                station.lat.toDouble(),
                station.lon.toDouble(),
                results
            )
            results[0].toDouble()
        } ?: Double.MAX_VALUE
    }

    // Favorite Handling
    fun toggleFavorite(stationId: String) {
        viewModelScope.launch {
            authRepo.getCurrentUser().uid.let { uid ->
                dbRepo.toggleFavorite(uid, stationId)
                    .onSuccess { getFavorites() }
                    .onFailure { _errors.emit("Favorite update failed: ${it.message}") }
            }
        }
    }

    private fun getFavorites() {
        viewModelScope.launch {
            authRepo.getCurrentUser().uid.let { uid ->
                _favouriteStationIds.value = UiState.Loading
                dbRepo.getFavorites(uid)
                    .onSuccess {
                        _favouriteStationIds.value = UiState.Success(it)
                    }
                    .onFailure {
                        _favouriteStationIds.value = UiState.Fail(it.message ?: "Unknown error")
                    }
            }
        }
    }

    // Location & UI Actions
    fun moveToStation(station: ChargerStation) {
        _cameraPosition.value = LatLng(station.lat.toDouble(), station.lon.toDouble())
    }

    fun getCurrentLocation() {
        viewModelScope.launch {
            try {
                locationClient.lastLocation.await()?.let { location ->
                    _cameraPosition.value = LatLng(location.latitude, location.longitude)
                    _forceLocationUpdate.value++  // Force recomposition
                } ?: _errors.emit("Location unavailable")
            } catch (e: SecurityException) {
                _errors.emit("Permission required")
            }
        }
    }

    // Slot Management
    fun updateSlots(slots: List<ChargerSlot>) {
        viewModelScope.launch {
            slots.forEach { slot ->
                dbRepo.createOrUpdateChargerSlot(slot.stationId, slot)
                    .onFailure { Timber.e("Slot update failed: ${it.message}") }
            }
            // Refresh affected stations
            slots.map { it.stationId }.toSet().forEach { stationId ->
                _stationSlotsCache.remove(stationId)
            }
        }
    }

    fun reportProblem(slotId: String) {
        viewModelScope.launch {
            dbRepo.reportDamagedSlot(slotId)
                .onFailure { _errors.emit("Report failed: ${it.message}") }
        }
    }

    fun searchLocation(query: String, context: Context) {
        viewModelScope.launch {
            try {
                val geocoder = Geocoder(context)
                val addresses = geocoder.getFromLocationName(query, 1)
                addresses?.firstOrNull()?.let {
                    _cameraPosition.value = LatLng(it.latitude, it.longitude)
                } ?: _errors.emit("Location not found")
            } catch (e: Exception) {
                _errors.emit("Search failed: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun signOutUser() {
        authRepo.signOutUser()
    }

    fun getSlotsForStation(stationId: String) {
        viewModelScope.launch {
            _slotLocation.value = UiState.Loading
            try {
                dbRepo.getSlotsForStation(stationId)
                    .onSuccess {
                        _slotLocation.value = UiState.Success(it)
                    }
                    .onFailure {
                        _slotLocation.value = UiState.Fail(it.message ?: "Unknown error")
                    }
            } catch (e: Exception) {
                _slotLocation.value = UiState.Fail(e.message ?: "Loading failed")
            }
        }
    }
}




