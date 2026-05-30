package com.example.util

import android.content.Context
import android.content.SharedPreferences

object PreferenceHelper {
    private const val PREFS_NAME = "browser_prefs"
    
    private const val KEY_LANGUAGE = "language"
    private const val KEY_THEME_MODE = "themeMode"
    private const val KEY_TEXT_SIZE = "textSize"
    private const val KEY_PAGE_ZOOM = "pageZoom"
    private const val KEY_HOME_PAGE = "homePage"
    private const val KEY_SEARCH_ENGINE = "searchEngine"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!this::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) = prefs.edit().putString(KEY_LANGUAGE, value).apply()

    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, 0)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    var textSize: Float
        get() = prefs.getFloat(KEY_TEXT_SIZE, 100f)
        set(value) = prefs.edit().putFloat(KEY_TEXT_SIZE, value).apply()

    var pageZoom: Int
        get() = prefs.getInt(KEY_PAGE_ZOOM, 100)
        set(value) = prefs.edit().putInt(KEY_PAGE_ZOOM, value).apply()

    var homePage: String
        get() = prefs.getString(KEY_HOME_PAGE, "https://google.com/") ?: "https://google.com/"
        set(value) = prefs.edit().putString(KEY_HOME_PAGE, value).apply()

    var searchEngine: String
        get() = prefs.getString(KEY_SEARCH_ENGINE, "Google") ?: "Google"
        set(value) = prefs.edit().putString(KEY_SEARCH_ENGINE, value).apply()
}
