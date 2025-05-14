package com.ist.chargist.navigation

sealed class Screen(val route: String) {
    data object Login : Screen(route = "sign_in")
    data object Signup : Screen(route = "signup")
    data object Map : Screen(route = "map")
}