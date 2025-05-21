package com.ist.chargist.presentation.addCharger

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.ist.chargist.domain.DatabaseRepository
import com.ist.chargist.domain.ImageRepository
import com.ist.chargist.domain.model.ChargerSlot
import com.ist.chargist.domain.model.ChargerStation
import com.ist.chargist.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AddChargerViewModel @Inject constructor(
    private val dbRepo: DatabaseRepository,
    private val imageDb: ImageRepository,
    private val locationClient: FusedLocationProviderClient
) : ViewModel() {

    private val _creatingCharger: MutableState<UiState> = mutableStateOf(UiState.Idle)
    val creatingCharger: State<UiState> get() = _creatingCharger

    private val _chargerLocation: MutableState<UiState> = mutableStateOf(UiState.Idle)
    val chargerLocation: State<UiState> get() = _chargerLocation

    private val _myLocation: MutableState<UiState> = mutableStateOf(UiState.Idle)
    val myLocation: State<UiState> get() = _myLocation

    var onSelectCamera: (() -> Unit)? = null
    var onSelectGallery: (() -> Unit)? = null

    var name by mutableStateOf("")
    var selectedMethods by mutableStateOf<List<String>>(emptyList())
    var chargers by mutableStateOf(listOf<ChargerSlot>())
    var imageUri by mutableStateOf<Uri?>(null)
    var fastPrice by mutableStateOf("")
    var mediumPrice by mutableStateOf("")
    var slowPrice by mutableStateOf("")

    fun createCharger(station: ChargerStation, slots: List<ChargerSlot>) {
        viewModelScope.launch {
            _creatingCharger.value = UiState.Loading

            dbRepo.createOrUpdateChargerStation(station, slots)
                .onSuccess { createdChargerList ->
                    createdChargerList.map { stationToInsert ->
                        if (station.imageUri == null) {
                            _creatingCharger.value = UiState.Success(createdChargerList)
                            return@onSuccess
                        }

                        imageDb.uploadImage(
                            fileName = station.name,
                            fileUri = station.imageUri.toUri(),
                            referenceId = station.id
                        ).onSuccess { imageUrl ->
                            dbRepo.createOrUpdateChargerStation(
                                stationToInsert.copy(imageUri = imageUrl),
                                emptyList()
                            )
                                .onSuccess {
                                    _creatingCharger.value = UiState.Success(it)
                                }
                                .onFailure {
                                    _creatingCharger.value = UiState.Error(it.message)
                                }
                        }.onFailure {
                            _creatingCharger.value = UiState.Error(it.message)
                        }
                    }
                }.onFailure {
                    _creatingCharger.value = UiState.Error(it.message)
                }
        }
    }

    fun searchLocation(
        query: String,
        context: Context,
    ) {
        viewModelScope.launch {
            _chargerLocation.value = UiState.Loading
            try {
                if (!Geocoder.isPresent()) {
                    _chargerLocation.value = UiState.Error("Geocoder not available on this device")
                    return@launch
                }

                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses = withContext(Dispatchers.IO) {
                    geocoder.getFromLocationName(query, 1)
                }
                val address = addresses?.firstOrNull()

                if (address != null) {
                    val lat = address.latitude.toString()
                    val lon = address.longitude.toString()
                    _chargerLocation.value = UiState.Success(listOf(lat, lon))
                } else {
                    _chargerLocation.value = UiState.Error("Location not found")
                }
            } catch (e: Exception) {
                _chargerLocation.value =
                    UiState.Error("Search failed: ${e.message ?: "Unknown error"}")
            }
        }
    }


    fun getCurrentLocation() {
        viewModelScope.launch {
            _myLocation.value = UiState.Loading
            try {
                val location = locationClient.lastLocation.await()
                if (location != null) {
                    _myLocation.value =
                        UiState.Success(listOf(location.latitude.toString(), location.longitude.toString()))
                } else {
                    _myLocation.value = UiState.Error("Location unavailable")
                }
            } catch (e: SecurityException) {
                _myLocation.value = UiState.Error("Permission required")
            } catch (e: Exception) {
                _myLocation.value = UiState.Error("Error: ${e.message ?: "Unknown error"}")
            }
        }
    }
}