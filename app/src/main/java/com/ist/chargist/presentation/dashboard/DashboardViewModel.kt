package com.ist.chargist.presentation.dashboard

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.ist.chargist.domain.DatabaseRepository
import com.ist.chargist.domain.ImageRepository
import com.ist.chargist.domain.AuthenticationRepository
import com.ist.chargist.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val dbRepo: DatabaseRepository,
    private val imgRepo: ImageRepository,
    private val authRepository: AuthenticationRepository
) : ViewModel() {

    private val _collectionsList: MutableState<UiState> = mutableStateOf(UiState.Idle)
    val collectionsList: State<UiState> get() = _collectionsList





    fun signOutUser() {
        authRepository.signOutUser()
    }
}