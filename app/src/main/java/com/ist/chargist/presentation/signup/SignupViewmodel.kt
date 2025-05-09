package com.hocel.assetmanager.presentation.authentication.signup

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ist.chargist.domain.AuthenticationRepository
import com.ist.chargist.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepo: AuthenticationRepository,
) : ViewModel() {

    private val _signUpUser: MutableState<UiState> = mutableStateOf(UiState.Idle)
    val signUpUser: State<UiState> get() = _signUpUser

    fun signup(email: String, password: String) {
        viewModelScope.launch {
            _signUpUser.value = UiState.Loading

            authRepo.signupUser(email, password)
                .onSuccess {
                    _signUpUser.value = UiState.Success(it)
                }.onFailure {
                    _signUpUser.value = UiState.Error(it.message)
                }
        }
    }
}