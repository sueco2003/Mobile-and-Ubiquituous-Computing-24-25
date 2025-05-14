package com.ist.chargist.domain.repository.firebase



import androidx.compose.ui.platform.LocalContext
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.firestore
import com.ist.chargist.domain.AuthenticationRepository
import com.ist.chargist.domain.DeviceInfoProvider
import com.ist.chargist.domain.model.User
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class FirebaseAuthImpl @Inject constructor(
    private val deviceInfo: DeviceInfoProvider,
) : AuthenticationRepository {

    companion object {

        const val USERS_DATABASE = "users"
        const val CLIENT_ID = "765464418634-gtprkqbnk51fgmkoce6iri4n17rerbdq.apps.googleusercontent.com"

        private const val ERROR_NO_INTERNET = "ERROR_NO_INTERNET"
        private const val ERROR_INPUTS_EMPTY = "ERROR_INPUTS_EMPTY"
        private const val ERROR_SIGNING_IN = "ERROR_SIGNING_IN"
        private const val ERROR_UNEXPECTED = "ERROR_UNEXPECTED"
        private const val ERROR_CHECK_INPUTS = "ERROR_CHECK_INPUTS"
        private const val ERROR_NULL_USER_AFTER_CREATING_ACCOUNT = "ERROR_NULL_USER_AFTER_CREATING_ACCOUNT"
        private const val ERROR_COULD_NOT_CREATE_USER = "ERROR_COULD_NOT_CREATE_USER"
        private const val ERROR_USER_ID_NOT_AVAILABLE = "ERROR_USER_ID_NOT_AVAILABLE"
        private const val ERROR_INVALID_USER = "ERROR_INVALID_USER"

    }

    override val clientId: String
        get() = CLIENT_ID

    override val isUserLoggedIn: Boolean
        get() = FirebaseAuth.getInstance().currentUser != null


    override suspend fun loginUser(email: String, password: String): Result<String> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        // Validate email and password
        if (email.isEmpty() || password.isEmpty()) {
            return Result.failure(Throwable(ERROR_INPUTS_EMPTY))
        }

        return try {
            // Create a suspend coroutine to handle asynchronous task
            suspendCoroutine { continuation ->
                FirebaseAuth.getInstance()
                    .signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener { authResult ->
                        // Sign in was successful, return user UID
                        continuation.resume(
                            Result.success(authResult.user?.uid ?: ERROR_USER_ID_NOT_AVAILABLE)
                        )
                    }
                    .addOnFailureListener { exception ->
                        // Sign in failed, return the exception
                        continuation.resume(Result.failure(Throwable(ERROR_SIGNING_IN + exception.message)))
                    }
            }
        } catch (e: Exception) {
            // Return a failure result in case of an unexpected exception
            Result.failure(Throwable(ERROR_UNEXPECTED + e.message))
        }
    }

    override suspend fun signupUser(email: String, password: String): Result<String> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        // Validate email and password
        if (email.isEmpty() || password.isEmpty()) {
            return Result.failure(Throwable(ERROR_CHECK_INPUTS))
        }

        return try {
            // Create a user account with email and password
            val task = FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(email, password)

            // Handle the result using await
            val authResult = task.await()

            // User creation succeeded
            val user = authResult.user
                ?: return Result.failure(Throwable(ERROR_NULL_USER_AFTER_CREATING_ACCOUNT))

            // Call createUser within a coroutine body
            val createUserResult = createUser(user)

            // Check the result of createUser
            if (createUserResult.isFailure) {
                val exception = createUserResult.exceptionOrNull() ?: Throwable(ERROR_COULD_NOT_CREATE_USER)
                return Result.failure(exception)
            }

            Result.success(user.uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createUser(user: FirebaseUser?): Result<Unit> {
        if (!deviceInfo.hasInternetConnection()) {
            return Result.failure(Throwable(ERROR_NO_INTERNET))
        }

        // Validate user input
        val newUser = user?.let {
            User(
                userId = it.uid,
                username = it.displayName ?: "Unknown",
                favourites = emptyList()// Provide default value if displayName is null
            )
        } ?: return Result.failure(Throwable(ERROR_INVALID_USER))

        return try {
            // Use Firebase's suspend function to add new user data
            Firebase.firestore
                .collection(USERS_DATABASE)
                .document(newUser.userId)
                .set(newUser)
                .await() // Use await() to handle the operation as a suspend function

            Result.success(Unit) // Return success if the operation completes
        } catch (e: Exception) {
            // Return failure with the caught exception
            Result.failure(e)
        }
    }

    override fun getCurrentUser(): FirebaseUser = FirebaseAuth.getInstance().currentUser!!

    override fun signOutUser() = FirebaseAuth.getInstance().signOut()

}