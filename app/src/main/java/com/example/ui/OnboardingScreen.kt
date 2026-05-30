package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen(
    viewModel: BrowserViewModel,
    isDark: Boolean,
    onFinish: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    val context = LocalContext.current
    
    // Page 0: Welcome (Hello in Different Languages)
    // Page 1: Language Selection
    // Page 2: Search Engine Selection

    Box(
        modifier = Modifier
            .fillMaxSize()
            .colorOSGradientBackground(isDark)
    ) {
        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                fadeIn(animationSpec = tween(500)) with fadeOut(animationSpec = tween(500))
            },
            label = "OnboardingTransition"
        ) { page ->
            when (page) {
                0 -> WelcomePage(isDark) { currentPage = 1 }
                1 -> LanguageSelectionPage(viewModel, isDark) { currentPage = 2 }
                2 -> SearchEngineSelectionPage(viewModel, isDark) { onFinish() }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun WelcomePage(isDark: Boolean, onNext: () -> Unit) {
    val languages = listOf("Hello", "Hola", "Bonjour", "你好", "Hallo", "Ciao", "こんにちは")
    var currentHelloIndex by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            currentHelloIndex = (currentHelloIndex + 1) % languages.size
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AnimatedContent(
            targetState = currentHelloIndex,
            transitionSpec = {
                fadeIn(tween(800)) with fadeOut(tween(800))
            },
            label = "HelloTranslation"
        ) { index ->
            Text(
                text = languages[index],
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to StormX Browser",
            fontSize = 18.sp,
            color = if (isDark) Color.LightGray else Color.DarkGray,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(64.dp))

        FloatingActionButton(
            onClick = onNext,
            containerColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
            contentColor = Color.White
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
        }

        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "Developed by Himank.J",
            fontSize = 12.sp,
            color = if (isDark) Color.Gray else Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
    }
}

@Composable
fun LanguageSelectionPage(viewModel: BrowserViewModel, isDark: Boolean, onNext: () -> Unit) {
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color.LightGray else Color.DarkGray
    val cardBg = glassCardColor(isDark)
    val settings by viewModel.settings.collectAsState()
    val availableLanguages = listOf("English (US)", "简体中文", "Español", "Deutsch", "Français")

    val selectedLang = settings.language

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Language / 选择语言", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Spacer(modifier = Modifier.height(24.dp))

        availableLanguages.forEach { lang ->
            val isSelected = selectedLang == lang
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) (if (isDark) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.1f)) else cardBg)
                    .clickable { viewModel.updateLanguage(lang, null) }
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(lang, fontSize = 16.sp, color = textPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color.White else Color.Black, contentColor = if (isDark) Color.Black else Color.White),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Next", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SearchEngineSelectionPage(viewModel: BrowserViewModel, isDark: Boolean, onFinish: () -> Unit) {
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color.LightGray else Color.DarkGray
    val cardBg = glassCardColor(isDark)
    val settings by viewModel.settings.collectAsState()
    val availableEngines = listOf("search.stormx.ninja", "Google", "Bing", "DuckDuckGo")
    val context = LocalContext.current

    val selectedEngine = settings.searchEngine

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Search Engine", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Spacer(modifier = Modifier.height(24.dp))

        availableEngines.forEach { engine ->
            val isSelected = selectedEngine == engine
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) (if (isDark) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.1f)) else cardBg)
                    .clickable { viewModel.updateSearchEngine(engine, context) }
                    .padding(16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(engine, fontSize = 16.sp, color = textPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7), contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Finish & Start Exploring", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
