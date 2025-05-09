package com.ist.chargist.presentation.signup

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
import com.hocel.assetmanager.presentation.authentication.signup.SignupViewModel
import com.ist.chargist.ui.theme.BackgroundColor
import com.ist.chargist.utils.UiState

@ExperimentalMaterial3Api
@Composable
fun SignupScreen(
    viewModel: SignupViewModel,
    onSignupClicked: (email: String, password: String) -> Unit,
    goToLogin: () -> Unit
) {
    val context = LocalContext.current
    val signUpUserUiState by viewModel.signUpUser

    LaunchedEffect(key1 = signUpUserUiState) {
        when (signUpUserUiState) {
            is UiState.Success -> {
                goToLogin()
            }

            is UiState.Error -> {
                Toast.makeText(
                    context,
                    (signUpUserUiState as UiState.Error).message,
                    Toast.LENGTH_SHORT
                ).show()
            }

            is UiState.Fail -> {
                Toast.makeText(
                    context,
                    (signUpUserUiState as UiState.Fail).message,
                    Toast.LENGTH_SHORT
                ).show()
            }

            UiState.Idle,
            UiState.Loading -> {
                Unit
            }
        }
    }

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
                SignupContent(
                    signUpUserUiState = signUpUserUiState,
                    onButtonClicked = onSignupClicked,
                    goToLogin = goToLogin
                )
            }
        }
    )
}