package com.ist.chargist.navigation

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hocel.assetmanager.presentation.authentication.signup.SignupViewModel
import com.ist.chargist.presentation.login.LoginScreen
import com.ist.chargist.presentation.login.LoginViewModel
import com.ist.chargist.presentation.map.MapScreen
import com.ist.chargist.presentation.map.MapViewModel
import com.ist.chargist.presentation.signup.SignupScreen
import com.ist.chargist.utils.UiState
import timber.log.Timber

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    startDestination: String
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {

        Timber.d("Setting up Navigation Host with routes...")

        loginRoute(
            navigateToHome = {
                navController.navigate(Screen.Map.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            goToSignup = {
                navController.navigate(Screen.Signup.route)
            }
        )

        signupRoute(
            goToLogin = {
                navController.navigateUp()
            }
        )

        mapRoute(
            onLogoutClick = {
                navController.navigate(Screen.Login.route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            }
        )


        Timber.d("Finished setting up Navigation Host with routes.")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.loginRoute(
    navigateToHome: () -> Unit,
    goToSignup: () -> Unit,
) {
    composable(route = Screen.Login.route) {
        val viewModel: LoginViewModel = hiltViewModel()
        LoginScreen(
            viewModel = viewModel,
            navigateToHome = navigateToHome,
            onLoginClicked = { email, password ->
                viewModel.login(email, password)
            },
            onSuccessfulSignIn = { tokenId ->
                viewModel.firebaseAuthWithGoogle(
                    tokenId,
                    onSuccess = {
                        viewModel.setGoogleSigningState(UiState.Success())
                        navigateToHome()
                    },
                    onError = {
                        viewModel.setGoogleSigningState(UiState.Error())
                    })
            },
            onDialogDismissed = {
                viewModel.setGoogleSigningState(UiState.Idle)
            },
            goToSignup = goToSignup
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
fun NavGraphBuilder.signupRoute(
    goToLogin: () -> Unit
) {
    composable(route = Screen.Signup.route) {
        val viewModel: SignupViewModel = hiltViewModel()
        SignupScreen(
            viewModel = viewModel,
            onSignupClicked = { email, password ->
                viewModel.signup(email, password)
            },
            goToLogin = goToLogin
        )
    }
}

fun NavGraphBuilder.mapRoute(
    onLogoutClick: () -> Unit,
) {
    composable(route = Screen.Map.route) {
        val viewModel: MapViewModel = hiltViewModel()

        MapScreen(
            viewModel = viewModel,
            onLogoutClick = {
                viewModel.signOutUser()
                onLogoutClick()
            },
            navigateToAddChargerStation = null,
            onChargerStationClick = null
        )
    }
}



