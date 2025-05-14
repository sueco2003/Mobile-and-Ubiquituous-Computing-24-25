package com.ist.chargist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.ist.chargist.domain.AuthenticationRepository
import com.ist.chargist.navigation.Screen
import com.ist.chargist.navigation.SetupNavGraph
import com.ist.chargist.ui.theme.BackgroundColor
import com.ist.chargist.ui.theme.ChargISTTheme
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepo: AuthenticationRepository


    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.i("onCreate ssdf")
        super.onCreate(savedInstanceState)
        Timber.i("onCreate started")

        setContent {
            ChargISTTheme {
                val navController = rememberNavController()
                val systemUiController = rememberSystemUiController()
                val systemUIColor = BackgroundColor
                SideEffect {
                    systemUiController.setStatusBarColor(
                        color = systemUIColor
                    )
                }
                SetupNavGraph(
                    navController = navController,
                    startDestination = getStartDestination()
                )
            }
        }

        Timber.i("onCreate finished")
    }

    override fun onStart() {
        super.onStart()
        Timber.i("onStart")
    }

    override fun onRestart() {
        super.onRestart()
        Timber.i("onRestart")
    }

    override fun onResume() {
        super.onResume()
        Timber.i("onResume")
    }

    override fun onPause() {
        super.onPause()
        Timber.i("onPause")
    }

    override fun onStop() {
        super.onStop()
        Timber.i("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.i("onDestroy")
    }

    private fun getStartDestination(): String {
        return if (authRepo.isUserLoggedIn) Screen.Map.route
        else Screen.Login.route
    }
}