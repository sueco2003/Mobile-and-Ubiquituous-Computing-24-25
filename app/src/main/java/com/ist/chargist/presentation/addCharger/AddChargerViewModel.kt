/*
package com.ist.chargist.presentation.addCharger

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ist.chargist.ChargISTApp
import com.ist.chargist.domain.DatabaseRepository
import com.ist.chargist.domain.ImageRepository
import com.ist.chargist.domain.model.ChargerStation
import com.ist.chargist.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import androidx.core.net.toUri

@HiltViewModel
class AddChargerViewModel @Inject constructor(
    private val dbRepo: DatabaseRepository,
    private val imageDb: ImageRepository,
) : ViewModel() {

    private val _creatingCharger: MutableState<UiState> = mutableStateOf(UiState.Idle)
    val creatingCharger: State<UiState> get() = _creatingCharger

    var onSelectCamera: (() -> Unit)? = null
    var onSelectGallery: (() -> Unit)? = null

    fun createCharger(station: ChargerStation) {
        viewModelScope.launch {
            _creatingCharger.value = UiState.Loading
            dbRepo.createOrUpdateChargerStation(station)
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
                                stationToInsert.copy(imageUri = imageUrl)
                            )
                                .onSuccess {
                                    _creatingCharger.value = UiState.Success(it)
                                }
                                .onFailure {
                                    Timber.e("EXCEPTION ERROR FROM UPDATE LOCATION:${it.message}")
                                    _creatingCharger.value = UiState.Error(it.message)
                                }
                        }.onFailure {
                            Timber.e("EXCEPTION ERROR FROM UPLOAD IMAGE LOCATION:${it.message}")
                            _creatingCharger.value = UiState.Error(it.message)
                        }
                    }
                }.onFailure {
                    _creatingCharger.value = UiState.Error(it.message)
                }
        }

    }
    private val _creatingCharger = mutableStateOf<UiState>(UiState.Idle
    val creatingCharger: State<UiState> = _creatingCharger

    fun getCurrentLocation(onSuccess: (LatLng) -> Unit) {
        viewModelScope.launch {
            try {
                val location = locationClient.lastLocation.await()
                location?.let {
                    onSuccess(LatLng(it.latitude, it.longitude))
                }
            } catch (e: SecurityException) {
                // Handle permission error
            }
        }
    }

    fun getAddressFromLocation(latLng: LatLng): String {
        return try {
            val geocoder = Geocoder(context)
            val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0) ?: ""
        } catch (e: Exception) {
            ""
        }
    }
}*/
