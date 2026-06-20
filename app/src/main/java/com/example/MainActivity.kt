package com.example

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.BrowserDatabase
import com.example.data.BrowserRepository
import com.example.ui.BrowserScreen
import com.example.ui.BrowserViewModel
import com.example.ui.theme.SearchAppTheme

class MainActivity : ComponentActivity() {
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

            SearchAppTheme(settings = settings) {
                androidx.compose.material3.Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    BrowserScreen(viewModel = browserViewModel)
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
