package com.ist.chargist.domain

import com.google.firebase.auth.FirebaseUser

interface AuthenticationRepository {

    val clientId: String

    val isUserLoggedIn: Boolean

    suspend fun loginUser(email: String, password: String): Result<String>

    suspend fun signupUser(email: String, password: String): Result<String>

    suspend fun createUser(user: FirebaseUser?): Result<Unit>

    fun getCurrentUser(): FirebaseUser

    fun isUserAnonymous(): Boolean

    fun signOutUser()

}