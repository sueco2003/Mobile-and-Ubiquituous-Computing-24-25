package com.ist.chargist.utils

import android.content.Context
import java.util.Locale

object AppUtils {

    // Load the saved language preference (default to English)
    fun loadLanguagePreference(context: Context): String {
        val preferences = context.getSharedPreferences("language_pref", Context.MODE_PRIVATE)
        return preferences.getString("current_language", "en") ?: "en"
    }

    // Save the selected language preference
    fun saveLanguagePreference(context: Context, languageCode: String) {
        val preferences = context.getSharedPreferences("language_pref", Context.MODE_PRIVATE)
        preferences.edit().putString("current_language", languageCode).apply()
    }

    fun setLocale(base: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = base.resources.configuration
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }
}
