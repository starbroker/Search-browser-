package com.example

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val sharedPrefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val initialOnboarded = sharedPrefs.getBoolean("onboarded", false)

        setContent {
            MaterialTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        var hasOnboarded by remember { mutableStateOf(initialOnboarded) }

                        if (hasOnboarded) {
                            WebViewScreen()
                        } else {
                            OnboardingScreen(onComplete = {
                                sharedPrefs.edit().putBoolean("onboarded", true).apply()
                                hasOnboarded = true
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var selectedLanguage by remember { mutableStateOf<String?>(null) }
    val languages = listOf("English", "Spanish", "French", "German", "Hindi", "Japanese", "Korean", "Chinese")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Please select your language:",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(languages) { language ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedLanguage = language }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedLanguage == language,
                        onClick = { selectedLanguage = language }
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = language, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        
        // Hide next button if no language is selected
        if (selectedLanguage != null) {
            Button(
                onClick = onComplete,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text("Next")
            }
        } else {
            // Keep the space so layout doesn't shift abruptly
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun WebViewScreen() {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            return false
                        }
                        return true
                    }
                }
                webChromeClient = WebChromeClient()
                loadUrl("https://search.stormx.ninja/")
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}
