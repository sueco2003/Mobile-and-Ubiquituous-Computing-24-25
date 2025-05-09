package com.ist.chargist.presentation.login


import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext

import com.ist.chargist.ui.theme.BackgroundColor
import com.ist.chargist.utils.UiState
import com.stevdzasan.onetap.OneTapSignInWithGoogle
import com.stevdzasan.onetap.rememberOneTapSignInState

@ExperimentalMaterial3Api
@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    navigateToHome: () -> Unit,
    onLoginClicked: (email: String, password: String) -> Unit,
    onSuccessfulSignIn: (String) -> Unit,
    onDialogDismissed: (String) -> Unit,
    goToSignup: () -> Unit
) {
    val context = LocalContext.current
    val signInUserUiState by viewModel.signInUser
    val googleSignInUserUiState by viewModel.googleSignInUser
    val oneTapState = rememberOneTapSignInState()
    Scaffold(
        modifier = Modifier
            .statusBarsPadding()
            .navigationBarsPadding(),
        content = { paddingValues ->
            Column(
                modifier = Modifier
                    .background(BackgroundColor)
                    .fillMaxSize()
                    .padding(top = paddingValues.calculateTopPadding()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                LoginContent(
                    signInUserUiState = signInUserUiState,
                    googleSignInUserUiState = googleSignInUserUiState,
                    onButtonClicked = onLoginClicked,
                    onGoogleSignClicked = {
                        oneTapState.open()
                        viewModel.setGoogleSigningState(UiState.Loading)
                    },
                    goToSignup = goToSignup
                )
            }
        }
    )
    OneTapSignInWithGoogle(
        state = oneTapState,
        clientId = viewModel.authClientId,
        onTokenIdReceived = { tokenId ->
            onSuccessfulSignIn(tokenId)
        },
        onDialogDismissed = { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            onDialogDismissed(message)
        }
    )
    LaunchedEffect(key1 = signInUserUiState) {
        when (signInUserUiState) {
            is UiState.Success -> {
                navigateToHome()
            }

            is UiState.Error -> {
                Toast.makeText(
                    context,
                    (signInUserUiState as UiState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> Unit
        }
    }
}