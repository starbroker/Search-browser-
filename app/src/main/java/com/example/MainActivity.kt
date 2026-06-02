package com.example
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.border


import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.togetherWith
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
                        androidx.compose.animation.AnimatedContent(
                            targetState = onboardingComplete,
                            transitionSpec = {
                                if (targetState) {
                                    (androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(600)) + 
                                     androidx.compose.animation.scaleIn(
                                         animationSpec = androidx.compose.animation.core.tween(600),
                                         initialScale = 0.9f
                                     )).togetherWith(
                                         androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(400)) +
                                         androidx.compose.animation.scaleOut(
                                             animationSpec = androidx.compose.animation.core.tween(400),
                                             targetScale = 1.1f
                                         )
                                     )
                                } else {
                                    androidx.compose.animation.fadeIn().togetherWith(androidx.compose.animation.fadeOut())
                                }
                            },
                            label = "OnboardingTransition"
                        ) { isComplete ->
                            if (isComplete) {
                                val isDarkContext = androidx.compose.foundation.isSystemInDarkTheme()
                                val glassColor = if (isDarkContext) androidx.compose.ui.graphics.Color(0x33000000) else androidx.compose.ui.graphics.Color(0xB3FFFFFF)
                                val glassBorder = if (isDarkContext) androidx.compose.ui.graphics.Color(0x4DFFFFFF) else androidx.compose.ui.graphics.Color(0x80FFFFFF)
                                
                                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                                    val blurRadius by androidx.compose.animation.core.animateDpAsState(targetValue = if (showChangelog) 32.dp else 0.dp)
                                    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().then(if (blurRadius > 0.dp) Modifier.blur(blurRadius) else Modifier)) {
                                        BrowserScreen(viewModel = browserViewModel)
                                    }
                                    
                                    if (showChangelog) {
                                        androidx.compose.material3.AlertDialog(
                                            onDismissRequest = { 
                                                showChangelog = false
                                                PreferenceHelper.lastVersionCode = BuildConfig.VERSION_CODE
                                            },
                                            modifier = Modifier.border(1.dp, glassBorder, androidx.compose.foundation.shape.RoundedCornerShape(26.dp)),
                                            title = { androidx.compose.material3.Text(com.example.ui.BrowserTranslator.translateText("What's New", settings.language) + " ${BuildConfig.VERSION_NAME}") },
                                            text = { 
                                                androidx.compose.foundation.layout.Column {
                                                    androidx.compose.material3.Text("• " + com.example.ui.BrowserTranslator.translateText("All popups now feature a blurry glassy aesthetic", settings.language))
                                                    androidx.compose.material3.Text("• " + com.example.ui.BrowserTranslator.translateText("Proper closing animation for welcome screen UI", settings.language))
                                                    androidx.compose.material3.Text("• " + com.example.ui.BrowserTranslator.translateText("Bug fixes and performance improvements", settings.language))
                                                }
                                            },
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                                            containerColor = glassColor,
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
                                }
                            } else {
                                com.example.ui.onboarding.OnboardingFlow(
                                    viewModel = browserViewModel,
                                    onComplete = {
                                        PreferenceHelper.isOnboardingComplete = true
                                        onboardingComplete = true
                                        PreferenceHelper.lastVersionCode = BuildConfig.VERSION_CODE
                                    }
                                )
                            }
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
