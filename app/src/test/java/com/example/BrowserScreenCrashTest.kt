package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.example.ui.BrowserScreen
import com.example.ui.BrowserViewModel
import androidx.test.core.app.ApplicationProvider
import com.example.data.BrowserDatabase
import com.example.data.BrowserRepository

@RunWith(AndroidJUnit4::class)
class BrowserScreenCrashTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun testBrowserScreenComposes() {
        val app = ApplicationProvider.getApplicationContext<BrowserApplication>()
        val db = BrowserDatabase.getDatabase(app)
        val vm = BrowserViewModel(app, BrowserRepository(db.browserDao()))
        
        composeTestRule.setContent {
            BrowserScreen(viewModel = vm)
        }
        
        composeTestRule.waitForIdle()
    }
}
