package com.example

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.util.PreferenceHelper

class BrowserApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        PreferenceHelper.init(this)
        applyThemeMode(PreferenceHelper.themeMode)
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
