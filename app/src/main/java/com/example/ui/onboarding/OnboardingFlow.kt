package com.example.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.BrowserSettings
import com.example.ui.BrowserViewModel
import com.example.ui.Text
import com.example.ui.glassCardColor
import com.example.ui.glassBorderColor
import com.example.ui.colorOSGradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingFlow(
    viewModel: BrowserViewModel,
    onComplete: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var step by remember { mutableIntStateOf(0) }
    
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }

    val activeFont = when (settings.fontFamily) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .colorOSGradientBackground(isDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
            ) {
                // Top Skip & Progress Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProgressDots(currentStep = step, totalSteps = 3, isDark = isDark)
                    
                    if (step < 2) {
                        TextButton(
                            onClick = { step = 2 },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (isDark) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(com.example.ui.BrowserTranslator.translateText("Skip", settings.language),
                                fontFamily = activeFont,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                // Interactive Slide Pages
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 24.dp)
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            val customEasing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f) // Smooth ColorOS slide transition
                            if (targetState > initialState) {
                                (slideInHorizontally(animationSpec = tween(500, easing = customEasing)) { width -> width / 2 } + fadeIn(tween(500))).togetherWith(
                                    slideOutHorizontally(animationSpec = tween(500, easing = customEasing)) { width -> -width / 2 } + fadeOut(tween(500)))
                            } else {
                                (slideInHorizontally(animationSpec = tween(500, easing = customEasing)) { width -> -width / 2 } + fadeIn(tween(500))).togetherWith(
                                    slideOutHorizontally(animationSpec = tween(500, easing = customEasing)) { width -> width / 2 } + fadeOut(tween(500)))
                            }
                        }, label = "onboarding_step_transitions"
                    ) { currentStep ->
                        when (currentStep) {
                            0 -> WelcomeScreen(activeFont = activeFont, isDark = isDark)
                            1 -> LanguageScreen(viewModel = viewModel, settings = settings, activeFont = activeFont, isDark = isDark)
                            2 -> SearchEngineScreen(viewModel = viewModel, settings = settings, activeFont = activeFont, isDark = isDark)
                        }
                    }
                }

                // Action Call Button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        color = glassCardColor(isDark),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(64.dp)
                            .border(1.dp, glassBorderColor(isDark), CircleShape)
                            .clickable {
                                if (step < 2) {
                                    step += 1
                                } else {
                                    onComplete()
                                }
                            }
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = if (step == 2) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = if (step == 2) "Complete" else "Continue",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProgressDots(currentStep: Int, totalSteps: Int, isDark: Boolean) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until totalSteps) {
            val isSelected = i == currentStep
            val color by animateColorAsState(
                targetValue = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
                },
                label = "color"
            )
            val width by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                label = "width"
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun WelcomeScreen(activeFont: FontFamily, isDark: Boolean) {
    val greetings = listOf(
        "Hello" to "en",
        "Bonjour" to "fr",
        "Hola" to "es",
        "नमस्ते" to "hi",
        "こんにちは" to "ja",
        "你好" to "zh",
        "مرحباً" to "ar",
        "Привет" to "ru",
        "Ciao" to "it",
        "Cześć" to "pl",
        "Olá" to "pt"
    )
    
    var index by remember { mutableIntStateOf(0) }
    
    LaunchedEffect(Unit) {
        while(true) {
            kotlinx.coroutines.delay(2000)
            index = (index + 1) % greetings.size
        }
    }
    
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Beautiful launcher icon with floating frosted board
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(26.dp))
                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(26.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.app_icon_custom),
                contentDescription = "App Icon",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
            )
        }

        Spacer(modifier = Modifier.height(36.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = index,
                transitionSpec = {
                    val scaleEasing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)
                    (slideInHorizontally(tween(600, easing = scaleEasing)) { width -> width / 4 } + fadeIn(tween(600))).togetherWith(
                        slideOutHorizontally(tween(600, easing = scaleEasing)) { width -> -width / 4 } + fadeOut(tween(600))
                    )
                },
                label = "welcome_greetings"
            ) { targetIndex ->
                Text(
                    text = greetings[targetIndex].first,
                    style = MaterialTheme.typography.displayLarge.copy(fontSize = 54.sp),
                    fontFamily = activeFont,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Welcome to Search",
            fontFamily = activeFont,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 24.sp),
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            color = if (isDark) Color.White else Color(0xFF1C1C1E)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Experience the web with fluid elegance\nand uncompromising privacy.",
            fontFamily = activeFont,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1C1C1E).copy(alpha = 0.6f),
            lineHeight = 24.sp
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Developed by Himank.J",
            fontFamily = activeFont,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF1C1C1E).copy(alpha = 0.4f)
        )
    }
}

@Composable
fun LanguageScreen(
    viewModel: BrowserViewModel,
    settings: BrowserSettings,
    activeFont: FontFamily,
    isDark: Boolean
) {
    var searchQuery by remember { mutableStateOf("") }
    
    val allLanguages = listOf(
        "English", "简体中文", "Español", "Deutsch", "Français", "Italiano", "日本語", "한국어", "Русский", "Português", "Hindi", "Arabic",
        "Türkçe", "Nederlands", "Polski", "ภาษาไทย", "Bahasa Indonesia", "Tiếng Việt", "Svenska", "Ελληνικά", "Dansk", "Suomi", "Norsk",
        "Čeština", "Magyar", "Română", "Українська", "עברית", "Bahasa Melayu", "Filipino", "Slovenčina", "Български", "Hrvatski", "Srpski"
    )
    
    val displayLanguages = if (searchQuery.isBlank()) allLanguages else allLanguages.filter { it.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Column {
            Text(com.example.ui.BrowserTranslator.translateText("Languages", settings.language),
                fontFamily = activeFont,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF1C1C1E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(com.example.ui.BrowserTranslator.translateText("Select your preferred language for searches and interface.", settings.language),
                fontFamily = activeFont,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1C1C1E).copy(alpha = 0.6f)
            )
        }

        // ColorOS 16 Frosted style Search Bar (Exactly matching LanguagesSubPage)
        Surface(
            color = glassCardColor(isDark),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = LocalTextStyle.current.copy(
                        color = if (isDark) Color.White else Color.Black,
                        fontFamily = activeFont,
                        fontSize = 16.sp
                    ),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(com.example.ui.BrowserTranslator.translateText("Search languages...", settings.language),
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                fontFamily = activeFont,
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // ColorOS 16 frosted List Container (Exactly matching LanguagesSubPage)
        Surface(
            color = glassCardColor(isDark),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
        ) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(displayLanguages.size) { index ->
                    val lang = displayLanguages[index]
                    val mappedLang = if (lang == "English") "English (US)" else lang
                    val isSelected = settings.language == mappedLang
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Save preferences directly to prevent recreate-crashes during slide transitions
                                com.example.util.PreferenceHelper.language = mappedLang
                                viewModel.updateSettings(settings.copy(language = mappedLang))
                            }
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(vertical = 14.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mappedLang,
                            fontFamily = activeFont,
                            fontSize = 16.sp,
                            color = if (isDark) Color.White else Color(0xFF1C1C1E),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (index < displayLanguages.size - 1) {
                        HorizontalDivider(
                            color = glassBorderColor(isDark),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchEngineScreen(
    viewModel: BrowserViewModel,
    settings: BrowserSettings,
    activeFont: FontFamily,
    isDark: Boolean
) {
    val engines = listOf(
        "StormX" to "search.stormx.ninja",
        "Google" to "Google",
        "Bing" to "Bing",
        "Yahoo" to "Yahoo",
        "DuckDuckGo" to "DuckDuckGo",
        "Baidu" to "Baidu"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Column {
            Text(com.example.ui.BrowserTranslator.translateText("Search Engine", settings.language),
                fontFamily = activeFont,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDark) Color.White else Color(0xFF1C1C1E)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(com.example.ui.BrowserTranslator.translateText("Choose the default service used when typing queries in the address bar.", settings.language),
                fontFamily = activeFont,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1C1C1E).copy(alpha = 0.6f)
            )
        }

        // ColorOS 16 frosted List Container (Exactly matching SearchEngineSubPage)
        Surface(
            color = glassCardColor(isDark),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
        ) {
            LazyColumn(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                items(engines.size) { index ->
                    val (name, value) = engines[index]
                    val isSelected = settings.searchEngine == value
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // Save preferences directly to prevent navigation side effects when no tabs are present yet
                                val newHomeUrl = when (value) {
                                    "Google" -> "https://www.google.com/"
                                    "Bing" -> "https://www.bing.com/"
                                    "Yahoo" -> "https://www.yahoo.com/"
                                    "DuckDuckGo" -> "https://duckduckgo.com/"
                                    "Baidu" -> "https://www.baidu.com/"
                                    "StormX" -> "https://search.stormx.ninja/"
                                    "search.stormx.ninja" -> "https://search.stormx.ninja/"
                                    else -> {
                                        if (value.startsWith("http")) value else "https://search.stormx.ninja/"
                                    }
                                }
                                com.example.util.PreferenceHelper.searchEngine = value
                                com.example.util.PreferenceHelper.homePage = newHomeUrl
                                viewModel.updateSettings(settings.copy(searchEngine = value, homeUrl = newHomeUrl))
                            }
                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                            .padding(vertical = 16.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            fontFamily = activeFont,
                            fontSize = 16.sp,
                            color = if (isDark) Color.White else Color(0xFF1C1C1E),
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (index < engines.size - 1) {
                        HorizontalDivider(
                            color = glassBorderColor(isDark),
                            thickness = 0.5.dp
                        )
                    }
                }
            }
        }
    }
}
