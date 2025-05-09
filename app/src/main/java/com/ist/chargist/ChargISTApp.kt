package com.ist.chargist

import android.app.Application
import com.google.firebase.FirebaseApp
import com.ist.chargist.utils.AppUtils
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import timber.log.Timber.DebugTree

@HiltAndroidApp
class ChargISTApp : Application() {

    override fun onCreate() {
        // Apply the saved language preference
        val currentLanguage = AppUtils.loadLanguagePreference(applicationContext)
        AppUtils.setLocale(applicationContext, currentLanguage)

        super.onCreate()

        Timber.plant(DebugTree())

    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Timber.i("onTrimMemory")
    }

    override fun onTerminate() {
        super.onTerminate()
        Timber.i("onTerminate")
    }
}
