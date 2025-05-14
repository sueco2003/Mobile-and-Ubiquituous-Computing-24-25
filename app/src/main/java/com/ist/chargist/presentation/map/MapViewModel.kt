package com.ist.chargist.presentation.map


import android.content.Context
import android.location.Geocoder
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.model.LatLng
import com.ist.chargist.domain.AuthenticationRepository
import com.ist.chargist.domain.DatabaseRepository
import com.ist.chargist.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val authRepo: AuthenticationRepository,
    private val dbRepo: DatabaseRepository,
    private val locationClient: FusedLocationProviderClient
) : ViewModel() {

    // State management
    private val _chargerStationList = mutableStateOf<UiState>(UiState.Idle)
    val chargerStationList: State<UiState> get() = _chargerStationList

    private val _cameraPosition = MutableStateFlow<LatLng?>(null)
    val cameraPosition = _cameraPosition.asStateFlow()

    private val _errors = MutableSharedFlow<String>()
    val errors = _errors.asSharedFlow()

    private val _forceLocationUpdate = MutableStateFlow(0)

    val locationUpdates = _forceLocationUpdate.asStateFlow()

    init {
        getChargerStations()
    }

    private fun getChargerStations() {
        viewModelScope.launch {
            _chargerStationList.value = UiState.Loading
            try {
                dbRepo.getChargerStations()
                    .onSuccess {
                        _chargerStationList.value = UiState.Success(it)
                    }
                    .onFailure {
                        _chargerStationList.value = UiState.Fail(it.message ?: "Unknown error")
                    }
            } catch (e: Exception) {
                _chargerStationList.value = UiState.Fail(e.message ?: "Loading failed")
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

    fun signOutUser() {
        authRepo.signOutUser()
    }
}