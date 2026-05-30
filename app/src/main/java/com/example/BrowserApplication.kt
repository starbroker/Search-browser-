package com.example

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.util.PreferenceHelper

class BrowserApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenceHelper.init(this)
        applyThemeMode(PreferenceHelper.themeMode)
        
        try {
            val wasmDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            val jsDir = java.io.File(cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            if (!wasmDir.exists()) wasmDir.mkdirs()
            if (!jsDir.exists()) jsDir.mkdirs()
        } catch (e: Exception) {}
    }

    companion object {
        fun applyThemeMode(mode: Int) {
            when (mode) {
                1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) // Light
                2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) // Dark
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) // System
            }
        }
    }
}
