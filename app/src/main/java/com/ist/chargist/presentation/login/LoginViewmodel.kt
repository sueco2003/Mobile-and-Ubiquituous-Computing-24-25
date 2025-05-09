package com.ist.chargist.presentation.login

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.ist.chargist.domain.AuthenticationRepository
import com.ist.chargist.utils.UiState
import dagger.hilt.android.lifecycle.HiltViewModel

import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthenticationRepository,
) : ViewModel() {

    private val _signInUser: MutableState<UiState> = mutableStateOf(UiState.Idle)
    val signInUser: State<UiState> get() = _signInUser

    private val _googleSignInUser: MutableState<UiState> = mutableStateOf(UiState.Idle)
    val googleSignInUser: State<UiState> get() = _googleSignInUser

    val authClientId: String get() = authRepository.clientId

    fun setGoogleSigningState(state: UiState) {
        _googleSignInUser.value = state
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _signInUser.value = UiState.Loading
            authRepository
                .loginUser(email, password)
                .onSuccess {
                    _signInUser.value = UiState.Success(it)
                }.onFailure {
                    _signInUser.value = UiState.Error(it.message)
                }
        }
    }

    fun firebaseAuthWithGoogle(
        tokenId: String,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit,
    ) {
        viewModelScope.launch {
            val credential = GoogleAuthProvider.getCredential(tokenId, null)
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        // TODO update to error code
                        onError(Exception("User is not logged in."))
                        return@addOnCompleteListener
                    }

                    viewModelScope.launch {
                        authRepository.createUser(task.await().user)

                    }.invokeOnCompletion {
                        if (it == null) {
                            onSuccess()
                            return@invokeOnCompletion
                        }

                        // TODO update to error code
                        onError(Exception("Unable to login user"))
                    }
                }
        }
    }

}