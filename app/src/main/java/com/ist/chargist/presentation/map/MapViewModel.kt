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

    suspend fun getChargerStations() {
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
            val availabilityFilterActive = _activeFilters.contains("available")
            val speedFilters = setOf("fast", "medium", "slow")

            // Base name filter
            val nameFiltered = if (_searchQuery.value.isNotBlank()) {
                _allStations.filter { it.name.contains(_searchQuery.value, true) }
            } else {
                _allStations
            }

            // Pre-fetch slots for first 100 stations
            val stationsToProcess = nameFiltered.take(100)
            prefetchSlots(stationsToProcess)

            // Apply filters
            val filtered = stationsToProcess.filter { station ->
                val slots = _stationSlotsCache[station.id] ?: emptyList()
                val activeSpeedFilters = _activeFilters intersect speedFilters
                val hasSlot = slots.isNotEmpty()

                // Apply each filter
                _activeFilters.all { filter ->
                    when (filter) {
                        "available" -> {
                            // Only require availability if speed filters aren't active
                            if (activeSpeedFilters.isEmpty()) {
                                slots.any { it.available }
                            } else {
                                true // let speed filters handle availability check
                            }
                        }
                        "fast", "medium", "slow" -> {
                            val speed = when (filter) {
                                "fast" -> ChargeSpeed.F
                                "medium" -> ChargeSpeed.M
                                else -> ChargeSpeed.S
                            }
                            slots.any {
                                it.speed == speed && (!availabilityFilterActive || it.available)
                            }
                        }
                        "card" -> station.payment.any { it.equals("credit", true) }
                        "paypal" -> station.payment.any { it.equals("paypal", true) }
                        "cash" -> station.payment.any { it.equals("cash", true) }
                        else -> true
                    }
                }
            }

            // Apply sorting with availability awareness
            val sorted = when (_selectedSort) {
                "price" -> {
                    if (_sortAscending) {
                        filtered.sortedBy { getMinPrice(it, availabilityFilterActive) }
                    } else {
                        filtered.sortedByDescending { getMaxPrice(it, availabilityFilterActive) }
                    }
                }
                "distance" -> {
                    filtered.sortedWith(compareBy { calculateDistance(it) })
                        .let { if (_sortAscending) it else it.reversed() }
                }
                else -> filtered
            }

            _filteredStations.value = sorted
        }
    }

    private fun getMinPrice(station: ChargerStation, availabilityFilterActive: Boolean): Double {
        val slots = _stationSlotsCache[station.id]

        return (if (!availabilityFilterActive) {
            // No availability filter: fallback to all prices
            listOfNotNull(
                station.slowPrice.toDouble(),
                station.mediumPrice.toDouble(),
                station.fastPrice.toDouble()
            ).minOrNull() ?: Double.MAX_VALUE
        } else {
            // With availability: only consider available slot speeds
            val filteredSlots = slots?.filter { it.available } ?: emptyList()
            filteredSlots.minOfOrNull { getPriceForSpeed(station, it.speed) } ?: Double.MAX_VALUE
        }) as Double
    }

    private fun getMaxPrice(station: ChargerStation, availabilityFilterActive: Boolean): Double {
        val slots = _stationSlotsCache[station.id]

        return (if (!availabilityFilterActive) {
            // No availability filter: fallback to all prices
            listOfNotNull(
                station.slowPrice.toDouble(),
                station.mediumPrice.toDouble(),
                station.fastPrice.toDouble()
            ).maxOrNull() ?: Double.MIN_VALUE
        } else {
            // With availability: only consider available slot speeds
            val filteredSlots = slots?.filter { it.available } ?: emptyList()
            filteredSlots.maxOfOrNull { getPriceForSpeed(station, it.speed) } ?: Double.MIN_VALUE
        }) as Double
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

    private fun getPriceForSpeed(station: ChargerStation, speed: ChargeSpeed): Double {
        return when (speed) {
            ChargeSpeed.F -> station.fastPrice?.toDouble() ?: Double.MAX_VALUE
            ChargeSpeed.M -> station.mediumPrice?.toDouble() ?: Double.MAX_VALUE
            ChargeSpeed.S -> station.slowPrice?.toDouble() ?: Double.MAX_VALUE
        }
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

    fun toggleFavorite(stationId: String) {
        viewModelScope.launch {
            val uid = authRepo.getCurrentUser().uid

            // Optimistically update UI
            val currentFavorites = (_favouriteStationIds.value as? UiState.Success)?.data as? List<String>


            val updatedFavorites = if (currentFavorites?.contains(stationId) == true) {
                currentFavorites - stationId
            } else {
                currentFavorites?.plus(stationId) ?: emptyList()
            }
            _favouriteStationIds.value = UiState.Success(updatedFavorites)

            dbRepo.toggleFavorite(uid, stationId)
                .onFailure {
                    _errors.emit("Favorite update failed: ${it.message}")
                    // Optional: Revert optimistic update or re-fetch
                    getFavorites()
                }
        }
    }


    fun getFavorites() {
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

    fun getCurrentLocation(onResult: (LatLng?) -> Unit) {
        viewModelScope.launch {
            try {
                val location = locationClient.lastLocation.await()
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    _cameraPosition.value = latLng
                    _forceLocationUpdate.value++
                    onResult(latLng)
                } else {
                    _errors.emit("Location unavailable")
                    onResult(null)
                }
            } catch (e: SecurityException) {
                _errors.emit("Permission required")
                onResult(null)
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
    fun isUserAnonymous(): Boolean {
        return authRepo.isUserAnonymous()
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

    private val _damageReports = mutableStateMapOf<String, Long?>()
    val damageReports: Map<String, Long?> get() = _damageReports

    fun checkDamageReport(slotId: String) {
        viewModelScope.launch {
            val result = dbRepo.getLatestDamageReportTimestamp(slotId)
            result.onSuccess { timestamp ->
                _damageReports[slotId] = timestamp
            }.onFailure {
                Timber.e(it)
                _damageReports[slotId] = null
            }
        }
    }

    fun fixSlot(slotId: String) {
        viewModelScope.launch {
            val result = dbRepo.fixSlot(slotId)
            result.onSuccess {
                _damageReports[slotId] = null
            }.onFailure {
                Timber.e(it)
                _damageReports[slotId] = null
            }
        }
    }

    fun reportProblems(slotId: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            _damageReports[slotId] = now // Optimistic update

            val result = dbRepo.reportDamagedSlot(slotId)
            result.onFailure {
                Timber.e(it)
                _damageReports[slotId] = null // Revert if it fails
            }
        }
    }

}




