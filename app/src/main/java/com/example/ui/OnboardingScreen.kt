@file:OptIn(ExperimentalAnimationApi::class)
package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
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
            .systemBarsPadding()
            .imePadding()
    ) {
        AnimatedContent(
            targetState = currentPage,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f)) { width -> width } + fadeIn(animationSpec = tween(400))) togetherWith
                            slideOutHorizontally(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f)) { width -> -width } + fadeOut(animationSpec = tween(400))
                } else {
                    (slideInHorizontally(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f)) { width -> -width } + fadeIn(animationSpec = tween(400))) togetherWith
                            slideOutHorizontally(animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.8f, stiffness = 300f)) { width -> width } + fadeOut(animationSpec = tween(400))
                }
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

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedContent(
                targetState = currentHelloIndex,
                transitionSpec = {
                    (scaleIn(initialScale = 0.8f, animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.6f, stiffness = 200f)) + fadeIn(tween(600, easing = FastOutSlowInEasing))) togetherWith 
                    (scaleOut(targetScale = 1.2f, animationSpec = tween(600, easing = FastOutSlowInEasing)) + fadeOut(tween(600, easing = FastOutSlowInEasing)))
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
                text = "Welcome to Search Browser",
                fontSize = 18.sp,
                color = if (isDark) Color.LightGray else Color.DarkGray,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(64.dp))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                            colors = if (isDark) listOf(Color(0xFF38BDF8), Color(0xFF818CF8)) else listOf(Color(0xFF0284C7), Color(0xFF4F46E5))
                        )
                    )
                    .clickable { onNext() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = Color.White)
            }
        }
        
        Text(
            text = "Developed by Himank.J",
            fontSize = 12.sp,
            color = if (isDark) Color.Gray else Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
        )
    }
}

@Composable
fun LanguageSelectionPage(viewModel: BrowserViewModel, isDark: Boolean, onNext: () -> Unit) {
    val textPrimary = if (isDark) Color.White else Color.Black
    val textSecondary = if (isDark) Color.LightGray else Color.DarkGray
    val cardBg = glassCardColor(isDark)
    val settings by viewModel.settings.collectAsState()
    
    var searchQuery by remember { mutableStateOf("") }
    
    val availableLanguages = listOf(
        "English", "简体中文", "Español", "Deutsch", "Français", "Italiano", "日本語", "한국어", "Русский", "Português", "Hindi", "Arabic",
        "Türkçe", "Nederlands", "Polski", "ภาษาไทย", "Bahasa Indonesia", "Tiếng Việt", "Svenska", "Ελληνικά", "Dansk", "Suomi", "Norsk",
        "Čeština", "Magyar", "Română", "Українська", "עברית", "Bahasa Melayu", "Filipino", "Slovenčina", "Български", "Hrvatski", "Srpski"
    )

    val filteredLanguages = availableLanguages.filter {
        it.contains(searchQuery, ignoreCase = true)
    }

    val selectedLang = settings.language

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Select Language", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            placeholder = { Text("Search language...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7),
                unfocusedBorderColor = Color.Transparent,
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(filteredLanguages) { lang ->
                val isSelected = selectedLang == lang
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
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
        }

        Spacer(modifier = Modifier.height(24.dp))

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
    val availableEngines = listOf(
        "Search Browser" to "search.stormx.ninja",
        "Google" to "Google",
        "Bing" to "Bing",
        "Yahoo" to "Yahoo",
        "DuckDuckGo" to "DuckDuckGo",
        "Baidu" to "Baidu"
    )
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

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            items(availableEngines) { enginePair ->
                val engineLabel = enginePair.first
                val engineValue = enginePair.second
                val isSelected = selectedEngine == engineValue
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) (if (isDark) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color(0xFF0284C7).copy(alpha = 0.1f)) else cardBg)
                        .clickable { viewModel.updateSearchEngine(engineValue, context) }
                        .padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(engineLabel, fontSize = 16.sp, color = textPrimary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                        if (isSelected) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = if (isDark) Color(0xFF38BDF8) else Color(0xFF0284C7), contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Finish & Start Exploring", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
