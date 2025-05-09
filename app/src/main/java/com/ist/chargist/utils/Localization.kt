package com.ist.chargist.utils

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf

val LocalizedContext = staticCompositionLocalOf<Context> {
    error("No localized context provided")
}