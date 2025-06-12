package com.ist.chargist.presentation.map

import android.content.Context
import android.location.Geocoder
import androidx.compose.runtime.MutableState
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
import com.ist.chargist.domain.DeviceInfoProvider
import com.ist.chargist.domain.model.ChargerSlot
import com.ist.chargist.domain.model.ChargerStation
import com.ist.chargist.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val deviceInfo: DeviceInfoProvider,
    private val authRepo: AuthenticationRepository,
    private val dbRepo: DatabaseRepository,
    private val locationClient: FusedLocationProviderClient
) : ViewModel() {
    private val _userLocation = mutableStateOf<LatLng?>(null)
    val userLocation: MutableState<LatLng?> get() = _userLocation

    private val _selectedStation = mutableStateOf<UiState>(UiState.Idle)
    val selectedStation: State<UiState> get() = _selectedStation

    private val _mapStations = mutableStateOf<UiState>(UiState.Idle)
    val mapStations: State<UiState> get() = _mapStations

    private val _searchResults = mutableStateOf<UiState>(UiState.Idle)
    val searchResults: State<UiState> get() = _searchResults

    private val _favouriteStationIds = mutableStateOf<UiState>(UiState.Idle)
    val favouriteStationIds: State<UiState> get() = _favouriteStationIds

    private val _cameraPosition = MutableStateFlow<LatLng?>(null)
    val cameraPosition = _cameraPosition.asStateFlow()

    private val _slotLocation = mutableStateOf<UiState>(UiState.Idle)
    val slotLocation: State<UiState> get() = _slotLocation

    private val _errors = MutableSharedFlow<String>()

    private val _forceLocationUpdate = MutableStateFlow(0)
    val locationUpdates = _forceLocationUpdate.asStateFlow()

    private val _userRatings = mutableStateMapOf<String, Int?>()
    val userRatings: Map<String, Int?> get() = _userRatings

    // Search & Filter State - Only affects search results
    private val _searchQuery = mutableStateOf("")
    private val _activeFilters = mutableStateListOf<String>()
    private var _selectedSort by mutableStateOf("distance")
    private var _sortAscending by mutableStateOf(true)

    private var _searchLastStation = ChargerStation()
    private var _isLoadingMoreSearch = mutableStateOf(false)
    private var _hasMoreSearchData = mutableStateOf(true)
    private var _allSearchResults = mutableStateListOf<ChargerStation>()

    val searchQuery: String get() = _searchQuery.value
    val isLoadingMoreSearch: State<Boolean> get() = _isLoadingMoreSearch
    val hasMoreSearchData: State<Boolean> get() = _hasMoreSearchData

    suspend fun getAllKnownStations() {
        _mapStations.value = UiState.Loading
        try {
            dbRepo.getAllKnownStations().onSuccess { stations ->
                _mapStations.value = UiState.Success(stations)
            }.onFailure { error ->
                _mapStations.value = UiState.Fail(error.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            _mapStations.value = UiState.Fail(e.message ?: "Loading failed")
        }
    }

    fun triggerGetNearbyStations(position: LatLng) {
        viewModelScope.launch {
            getNearbyStations(position)
        }
    }

    suspend fun getNearbyStations(position: LatLng? = null) {
        try {
            val pos = position ?: userLocation.value
            if (pos != null) {
                dbRepo.getNearbyStations(pos, 1000.0).onSuccess {
                    getAllKnownStations()
                }
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun getFilteredStationsForSearch(isLoadingMore: Boolean = false) {
        if (!isLoadingMore) {
            _searchResults.value = UiState.Loading
        }

        try {
            val filters = buildFilterList()

            dbRepo.getFilteredStations(
                lastStation = _searchLastStation,
                searchQuery = _searchQuery.value,
                filters = filters,
                userLocation = _userLocation.value
            ).onSuccess { stations ->
                if (stations.isEmpty()) {
                    _hasMoreSearchData.value = false
                } else {
                    if (isLoadingMore) {
                        _allSearchResults.addAll(stations)
                    } else {
                        _allSearchResults.clear()
                        _allSearchResults.addAll(stations)
                    }

                    // Update last station ID for pagination
                    _searchLastStation = stations.lastOrNull() ?: ChargerStation()

                    // IMPORTANT: Always update the UI state with the complete list
                    _searchResults.value = UiState.Success(_allSearchResults.toList())
                }
                getAllKnownStations()
            }.onFailure { error ->
                if (!isLoadingMore) {
                    _searchResults.value = UiState.Fail(error.message ?: "Unknown error")
                } else {
                    _errors.emit("Failed to load more search results: ${error.message}")
                }
            }
        } catch (e: Exception) {
            if (!isLoadingMore) {
                _searchResults.value = UiState.Fail(e.message ?: "Loading failed")
            } else {
                _errors.emit("Failed to load more search results: ${e.message}")
            }
        }
    }

    // Also update the loadMoreSearchResults method to ensure proper state management:
    fun loadMoreSearchResults() {
        if (_isLoadingMoreSearch.value || !_hasMoreSearchData.value) return

        viewModelScope.launch {
            _isLoadingMoreSearch.value = true
            try {
                getFilteredStationsForSearch(isLoadingMore = true)
            } finally {
                _isLoadingMoreSearch.value = false
            }
        }
    }

    private fun resetSearchPagination() {
        _searchLastStation = ChargerStation()
        _hasMoreSearchData.value = true
        _allSearchResults.clear()
    }

    fun handleSearchInput(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            // Clear search results when query is empty
            _searchResults.value = UiState.Success(emptyList<ChargerStation>())
            resetSearchPagination()
        } else {
            viewModelScope.launch {
                resetSearchPagination()
                getFilteredStationsForSearch()
            }
        }
    }

    fun applyFiltersAndSort(filters: List<String>, sort: String, ascending: Boolean) {
        _activeFilters.clear()
        _activeFilters.addAll(filters)
        _selectedSort = sort
        _sortAscending = ascending

        Timber.tag("MapViewModel")
            .d("Applying filters: $filters, sort: $sort, ascending: $ascending")
        // Only trigger search if there's a query or filters are applied
        if (_searchQuery.value.isNotBlank() || filters.isNotEmpty() || sort.isNotBlank()) {
            viewModelScope.launch {
                resetSearchPagination()
                getFilteredStationsForSearch()
            }
        }
    }

    fun triggerInitialSearch() {
        viewModelScope.launch {
            resetSearchPagination()
            getFilteredStationsForSearch()
        }
    }

    private fun buildFilterList(): List<String> {
        val filters = mutableListOf<String>()

        filters.addAll(_activeFilters)

        filters.add(_selectedSort)
        if (_sortAscending) {
            filters.add("ascending")
        } else {
            filters.add("descending")
        }

        return filters
    }

    fun toggleFavorite(stationId: String) {
        viewModelScope.launch {
            val uid = authRepo.getCurrentUser().uid

            val currentFavorites = (_favouriteStationIds.value as? UiState.Success)?.data as? List<String>

            val updatedFavorites = if (currentFavorites?.contains(stationId) == true) {
                currentFavorites - stationId
            } else {
                currentFavorites?.plus(stationId) ?: emptyList()
            }
            _favouriteStationIds.value = UiState.Success(updatedFavorites)

            dbRepo.toggleFavorite(uid, stationId)
                .onSuccess {
                    getAllKnownStations()
                }
                .onFailure {
                    _errors.emit("Favorite update failed: ${it.message}")
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
                    _userLocation.value = latLng
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

    fun updateSlots(slots: List<ChargerSlot>) {
        viewModelScope.launch {
            slots.forEach { slot ->
                dbRepo.createOrUpdateChargerSlot(slot.stationId, slot)
                    .onFailure { Timber.e("Slot update failed: ${it.message}") }
            }
            getAllKnownStations()
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

    fun clearSelectedStation() {
        _selectedStation.value = UiState.Idle
    }

    fun getStationById(stationId: String) {
        viewModelScope.launch {
            _selectedStation.value = UiState.Loading
            try {
                dbRepo.getChargerStation(stationId)
                    .onSuccess { station ->
                        _selectedStation.value = UiState.Success(station)
                        getAllKnownStations()
                    }
                    .onFailure {
                        _selectedStation.value = UiState.Fail(it.message ?: "Failed to fetch station")
                    }
            } catch (e: Exception) {
                _selectedStation.value = UiState.Fail(e.message ?: "Unexpected error")
            }
        }
    }

    fun getSlotsForStation(stationId: String) {
        viewModelScope.launch {
            _slotLocation.value = UiState.Loading
            try {
                dbRepo.getSlotsForStation(stationId)
                    .onSuccess {
                        _slotLocation.value = UiState.Success(it)
                        getAllKnownStations()
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
                getAllKnownStations()
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
                getAllKnownStations()
            }.onFailure {
                Timber.e(it)
                _damageReports[slotId] = null
            }
        }
    }

    fun reportProblems(slotId: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            _damageReports[slotId] = now

            val result = dbRepo.reportDamagedSlot(slotId)
            result.onFailure {
                Timber.e(it)
                _damageReports[slotId] = null
            }
        }
    }

    fun getUserRatingForStation(stationId: String) {
        if (isUserAnonymous()) return

        viewModelScope.launch {
            val userId = authRepo.getCurrentUser().uid
            dbRepo.getUserRatingForStation(userId, stationId)
                .onSuccess { rating ->
                    _userRatings[stationId] = rating
                    getAllKnownStations()
                }
                .onFailure { error ->
                    Timber.e("Failed to get user rating: ${error.message}")
                }
        }
    }

    fun submitRating(stationId: String, rating: Int) {
        viewModelScope.launch {
            try {
                val userId = authRepo.getCurrentUser().uid

                dbRepo.submitStationRating(userId, stationId, rating)
                    .onSuccess {
                        _userRatings[stationId] = rating
                        getAllKnownStations()
                    }
                    .onFailure { error ->
                        _errors.emit("Failed to submit rating: ${error.message}")
                    }
            } catch (e: Exception) {
            }
        }
    }

    fun isOnWifi(): Boolean {
        return deviceInfo.isOnWifi()
    }
}