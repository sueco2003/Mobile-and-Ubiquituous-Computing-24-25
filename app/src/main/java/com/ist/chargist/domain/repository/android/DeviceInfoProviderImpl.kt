package com.ist.chargist.domain.repository.android

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.ist.chargist.domain.DeviceInfoProvider
import javax.inject.Inject

class DeviceInfoProviderImpl @Inject constructor(
    val application: Context,
) : DeviceInfoProvider {
    override fun hasInternetConnection(): Boolean {
        val connectivityManager = ContextCompat
            .getSystemService(application, ConnectivityManager::class.java)
            ?: return false

        val activeNetwork = connectivityManager.activeNetwork ?: return false

        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }
}