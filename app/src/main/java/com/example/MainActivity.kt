package com.example

import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.BuildConfig
import com.example.data.BrowserDatabase
import com.example.data.BrowserRepository
import com.example.ui.BrowserScreen
import com.example.ui.BrowserViewModel
import com.example.ui.theme.SearchAppTheme
import com.example.util.PreferenceHelper
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

class MainActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        PreferenceHelper.init(newBase)
        val language = PreferenceHelper.language
        val locale = when (language) {
            "简体中文" -> Locale.SIMPLIFIED_CHINESE
            "Español" -> Locale("es")
            "Deutsch" -> Locale("de")
            "Français" -> Locale("fr")
            else -> Locale("en") // English (US)
        }
        val config = Configuration(newBase.resources.configuration)
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            val localeList = LocaleList(locale)
            LocaleList.setDefault(localeList)
            config.setLocales(localeList)
        } else {
            Locale.setDefault(locale)
            config.setLocale(locale)
        }
        val context = newBase.createConfigurationContext(config)
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Custom edge-to-edge drawing capabilities (ColorOS 16 Full Bleed look)
        enableEdgeToEdge()

        // Local Room persistence wiring
        val database = BrowserDatabase.getDatabase(applicationContext)
        val repository = BrowserRepository(database.browserDao())

        setContent {
            // Instantiate ViewModel using context safe factory supplier
            val browserViewModel: BrowserViewModel = viewModel(
                factory = BrowserViewModelFactory(application, repository)
            )

            val settings by browserViewModel.settings.collectAsState()
            var onboardingComplete by androidx.compose.runtime.remember { 
                androidx.compose.runtime.mutableStateOf(PreferenceHelper.isOnboardingComplete) 
            }
            var showChangelog by androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf(PreferenceHelper.lastVersionCode < BuildConfig.VERSION_CODE)
            }

            SearchAppTheme(settings = settings) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(com.example.ui.LocalAppLanguage provides settings.language) {
                        if (onboardingComplete) {
                            BrowserScreen(viewModel = browserViewModel)
                            
                            if (showChangelog) {
                                androidx.compose.material3.AlertDialog(
                                    onDismissRequest = { 
                                        showChangelog = false
                                        PreferenceHelper.lastVersionCode = BuildConfig.VERSION_CODE
                                    },
                                    title = { androidx.compose.material3.Text(com.example.ui.BrowserTranslator.translateText("What's New", settings.language) + " ${BuildConfig.VERSION_NAME}") },
                                    text = { 
                                        androidx.compose.foundation.layout.Column {
                                            androidx.compose.material3.Text("• " + com.example.ui.BrowserTranslator.translateText("Reduced APK Size", settings.language))
                                            androidx.compose.material3.Text("• " + com.example.ui.BrowserTranslator.translateText("Fast compile speeds", settings.language))
                                            androidx.compose.material3.Text("• " + com.example.ui.BrowserTranslator.translateText("Removed buggy tools", settings.language))
                                            androidx.compose.material3.Text("• " + com.example.ui.BrowserTranslator.translateText("Update welcome UI", settings.language))
                                        }
                                    },
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                    tonalElevation = 10.dp,
                                    confirmButton = {
                                        androidx.compose.material3.TextButton(onClick = { 
                                            showChangelog = false 
                                            PreferenceHelper.lastVersionCode = BuildConfig.VERSION_CODE
                                        }) {
                                            androidx.compose.material3.Text(com.example.ui.BrowserTranslator.translateText("Got it", settings.language))
                                        }
                                    }
                                )
                            }
                        } else {
                            com.example.ui.onboarding.OnboardingFlow(
                                viewModel = browserViewModel,
                                onComplete = {
                                    PreferenceHelper.isOnboardingComplete = true
                                    onboardingComplete = true
                                    PreferenceHelper.lastVersionCode = BuildConfig.VERSION_CODE
                                    this@MainActivity.recreate()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// Custom lifecycle provider safe factory supplying the dynamic Repo state
class BrowserViewModelFactory(
    private val application: Application,
    private val repository: BrowserRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BrowserViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BrowserViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class representation")
    }
}
