@file:Suppress("DEPRECATION")
package com.example.ui

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.res.painterResource
 
import androidx.compose.foundation.isSystemInDarkTheme
import android.webkit.WebView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.Bookmark
import com.example.data.BrowserSettings
import com.example.data.DownloadItem
import com.example.data.HistoryItem
import kotlinx.coroutines.launch

val LocalAppLanguage = androidx.compose.runtime.staticCompositionLocalOf { "English (US)" }

@Composable
fun trans(text: String): String {
    val lang = LocalAppLanguage.current
    return BrowserTranslator.translateText(text, lang)
}

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    fontStyle: androidx.compose.ui.text.font.FontStyle? = null,
    fontWeight: androidx.compose.ui.text.font.FontWeight? = null,
    fontFamily: androidx.compose.ui.text.font.FontFamily? = null,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    textDecoration: androidx.compose.ui.text.style.TextDecoration? = null,
    textAlign: androidx.compose.ui.text.style.TextAlign? = null,
    lineHeight: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    overflow: androidx.compose.ui.text.style.TextOverflow = androidx.compose.ui.text.style.TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: ((androidx.compose.ui.text.TextLayoutResult) -> Unit)? = null,
    style: androidx.compose.ui.text.TextStyle = androidx.compose.material3.LocalTextStyle.current
) {
    androidx.compose.material3.Text(
        text = trans(text),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style
    )
}

// Layout Density Model Configuration to support customization
enum class LayoutDensity(
    val mainPadding: Dp,
    val itemSpacing: Dp,
    val barHeight: Dp,
    val fontOffset: Float
) {
    COMPACT(6.dp, 4.dp, 48.dp, -1.5f),
    MODERATE(12.dp, 8.dp, 58.dp, 0f),
    COMFORTABLE(18.dp, 12.dp, 68.dp, 2f)
}

@Composable
fun SearchBrowserLogo(modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 36.dp) {
    Box(
        modifier = modifier
            .size(size)
            .background(Color(0xFF141415), shape = RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.material3.Text(
            text = "S",
            color = Color.White,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = (size.value * 0.65f).sp,
            textAlign = TextAlign.Center,
            style = LocalTextStyle.current.copy(
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                    includeFontPadding = false
                )
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen(viewModel: BrowserViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val notificationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // Observe State Flow values from ViewModel
    val isWebViewSupported by viewModel.isWebViewSupported.collectAsState()
    val activeTabId by viewModel.activeTabId.collectAsState()
    val tabsList by viewModel.tabs.collectAsState()
    val currentUrlInput by viewModel.currentUrlInput.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val webViewTrigger by viewModel.webViewUpdateTrigger.collectAsState()
    
    val adsBlockedSession by viewModel.blockedAdsSession.collectAsState()
    val trackersBlockedSession by viewModel.blockedTrackersSession.collectAsState()
    val downloadsList by viewModel.downloads.collectAsState()
    val iosNotifications by viewModel.iosNotifications.collectAsState()

    val showTabs by viewModel.showTabsOverview.collectAsState()
    val showSettings by viewModel.showSettings.collectAsState()
    val showBookmarks by viewModel.showBookmarks.collectAsState()
    val showHistory by viewModel.showHistory.collectAsState()
    val showDownloads by viewModel.showDownloads.collectAsState()
    val showShield by viewModel.showShieldPanel.collectAsState()
    val showMenuDrawer by viewModel.showMenuDrawer.collectAsState()
    val imageDownloadProposal by viewModel.imageDownloadProposal.collectAsState()
    val permissionProposal by viewModel.permissionRequestProposal.collectAsState()
    
    val multiplePermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.handlePermissionProposal(allGranted, true)
        if (!allGranted) {
            android.widget.Toast.makeText(context, "OS Permissions Denied", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    
    val isAnyDrawerOpen = showTabs || showSettings || showBookmarks || showHistory || showDownloads || showShield || showMenuDrawer

    var isSearchFocused by remember { mutableStateOf(false) }

    var lastActiveDownloadId by remember { mutableStateOf<Int?>(null) }
    var recentlyCompletedDownload by remember { mutableStateOf<DownloadItem?>(null) }
    var showCompletedBubble by remember { mutableStateOf(false) }

    LaunchedEffect(downloadsList) {
        val currentActive = downloadsList.find { it.status == "DOWNLOADING" || it.status == "PENDING" }
        if (currentActive != null) {
            lastActiveDownloadId = currentActive.id
        } else {
            val lastId = lastActiveDownloadId
            if (lastId != null) {
                val completedItem = downloadsList.find { it.id == lastId && it.status == "COMPLETED" }
                if (completedItem != null) {
                    recentlyCompletedDownload = completedItem
                    showCompletedBubble = true
                }
                lastActiveDownloadId = null
            }
        }
    }

    LaunchedEffect(showCompletedBubble) {
        if (showCompletedBubble) {
            kotlinx.coroutines.delay(5000)
            showCompletedBubble = false
        }
    }

    // Resolve Font and Color configurations
    val currentDensity = when (settings.layoutDensity) {
        "COMPACT" -> LayoutDensity.COMPACT
        "COMFORTABLE" -> LayoutDensity.COMFORTABLE
        else -> LayoutDensity.MODERATE
    }
    
    val activeFont = when (settings.fontFamily) {
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        else -> FontFamily.Default
    }

    // Active tab model helper
    val activeTab = tabsList.find { it.id == activeTabId }

    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemDark
    }

    LaunchedEffect(isDark) {
        viewModel.applyThemeToWebViews(isDark)
    }

    val view = androidx.compose.ui.platform.LocalView.current
    if (!view.isInEditMode) {
        androidx.compose.runtime.SideEffect {
            val window = (view.context as? android.app.Activity)?.window
            if (window != null) {
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
            }
        }
    }
    
    androidx.activity.compose.BackHandler(enabled = showSettings) { viewModel.showSettings.value = false }
    androidx.activity.compose.BackHandler(enabled = showHistory) { viewModel.showHistory.value = false }
    androidx.activity.compose.BackHandler(enabled = showBookmarks) { viewModel.showBookmarks.value = false }
    androidx.activity.compose.BackHandler(enabled = showDownloads) { viewModel.showDownloads.value = false }
    androidx.activity.compose.BackHandler(enabled = showTabs) { viewModel.showTabsOverview.value = false }
    androidx.activity.compose.BackHandler(enabled = showMenuDrawer) { viewModel.showMenuDrawer.value = false }
    androidx.activity.compose.BackHandler(enabled = showShield) { viewModel.showShieldPanel.value = false }
    
    val handledByOverlays = showTabs || showSettings || showBookmarks || showHistory || showDownloads || showMenuDrawer || showShield
    val canGoBackInWebView = activeTab?.canGoBack == true
    val isNotHomepage = !isHomepageUrl(activeTab?.url, settings)
    val hasMultipleTabs = tabsList.size > 1

    androidx.activity.compose.BackHandler(enabled = !handledByOverlays && (canGoBackInWebView || isNotHomepage || hasMultipleTabs)) {
        if (canGoBackInWebView) {
            viewModel.activeTabGoBack(context)
        } else if (hasMultipleTabs) {
            val activeId = activeTab?.id
            if (activeId != null) {
                viewModel.removeTab(activeId)
            }
        } else {
            viewModel.navigateActiveTab(settings.homeUrl, context)
            viewModel.activeTabClearHistory()
        }
    }

    CompositionLocalProvider(LocalAppLanguage provides settings.language) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .colorOSGradientBackground(isDark)
        ) {
                // Determine layout direction for navigation bar
                val isTablet = LocalConfiguration.current.screenWidthDp >= 600

                val blurRadius by androidx.compose.animation.core.animateDpAsState(
                    targetValue = if (isAnyDrawerOpen) 32.dp else 0.dp,
                    animationSpec = androidx.compose.animation.core.tween(300),
                    label = "baseBlur"
                )
                Box(modifier = Modifier.fillMaxSize().blur(blurRadius)) {
                    if (isTablet) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AnimatedVisibility(
                            visible = !isSearchFocused,
                            enter = slideInHorizontally(initialOffsetX = { -it }) + fadeIn(),
                            exit = slideOutHorizontally(targetOffsetX = { -it }) + fadeOut(),
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            PersistentNavigationBar(
                                viewModel = viewModel,
                                activeTab = activeTab,
                                tabsCount = tabsList.size,
                                density = currentDensity,
                                scope = scope,
                                activeFont = activeFont
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(bottom = innerPadding.calculateBottomPadding() / 4)
                        ) {
                            BrowserHeader(
                                urlInput = currentUrlInput,
                                activeTab = activeTab,
                                density = currentDensity,
                                fontFamily = activeFont,
                                settings = settings,
                                isFocused = isSearchFocused,
                                onFocusChange = { isSearchFocused = it },
                                onUrlChange = { viewModel.setUrlInput(it) },
                                onNavigate = { viewModel.navigateActiveTab(currentUrlInput, context); focusManager.clearFocus() },
                                onRefresh = { viewModel.activeTabRefresh(context) },
                                viewModel = viewModel
                            )

                            // ColorOS 16 Dropdown
                            AnimatedVisibility(
                                visible = isSearchFocused && currentUrlInput.isNotEmpty() && !currentUrlInput.startsWith("http"),
                                enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                            ) {
                                ColorOSSearchSuggestionsOverlay(
                                    query = currentUrlInput,
                                    isDark = isDark,
                                    fontFamily = activeFont,
                                    onSuggestionClick = { suggestion ->
                                        viewModel.setUrlInput(suggestion)
                                        viewModel.navigateActiveTab(suggestion, context)
                                        focusManager.clearFocus()
                                    }
                                )
                            }

                            // Dynamic Progress Bar Indicator 
                            AnimatedVisibility(
                                visible = activeTab?.isLoading == true,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                LinearProgressIndicator(
                                    progress = { (activeTab?.progress ?: 0) / 100f },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }

                            // WebView canvas wrapper (Fully floating card style)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(glassCardColor(isDark))
                                    .border(
                                        width = 1.dp,
                                        color = glassBorderColor(isDark),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                            ) {
                                if (activeTabId != -1) {
                                    if (isWebViewSupported == false) {
                                        FallbackBrowserSimulator(
                                            viewModel = viewModel,
                                            activeTab = activeTab,
                                            isDark = isDark,
                                            fontFamily = activeFont,
                                            onNavigate = { targetUrl ->
                                                viewModel.navigateActiveTab(targetUrl, context)
                                            }
                                        )
                                    } else if (isWebViewSupported == true) {
                                        key(activeTabId, webViewTrigger) {
                                            AndroidView<android.widget.FrameLayout>(
                                                factory = { ctx ->
                                                    android.widget.FrameLayout(ctx).apply {
                                                        layoutParams = android.view.ViewGroup.LayoutParams(
                                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                        )
                                                        try {
                                                            val webView = viewModel.getOrCreateWebView(activeTabId, ctx)
                                                            val parent = webView.parent as? android.view.ViewGroup
                                                            parent?.removeView(webView)
                                                            addView(webView)
                                                        } catch (e: Throwable) {
                                                            viewModel.markWebViewUnsupported()
                                                        }
                                                    }
                                                },
                                                onRelease = { frame ->
                                                    frame.removeAllViews()
                                                },
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    } else {
                                        Box(modifier = Modifier.fillMaxSize())
                                    }
                                } else {
                                    Box(modifier = Modifier.fillMaxSize())
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(bottom = innerPadding.calculateBottomPadding() / 4)
                    ) {
                        // URL and Shield Header bar
                        BrowserHeader(
                            urlInput = currentUrlInput,
                            activeTab = activeTab,
                            density = currentDensity,
                            fontFamily = activeFont,
                            settings = settings,
                            isFocused = isSearchFocused,
                            onFocusChange = { isSearchFocused = it },
                            onUrlChange = { viewModel.setUrlInput(it) },
                            onNavigate = { viewModel.navigateActiveTab(currentUrlInput, context); focusManager.clearFocus() },
                            onRefresh = { viewModel.activeTabRefresh(context) },
                            viewModel = viewModel
                        )

                        // ColorOS 16 Dropdown
                        AnimatedVisibility(
                            visible = isSearchFocused && currentUrlInput.isNotEmpty() && !currentUrlInput.startsWith("http"),
                            enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
                            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top)
                        ) {
                            ColorOSSearchSuggestionsOverlay(
                                query = currentUrlInput,
                                isDark = isDark,
                                fontFamily = activeFont,
                                onSuggestionClick = { suggestion ->
                                    viewModel.setUrlInput(suggestion)
                                    viewModel.navigateActiveTab(suggestion, context)
                                    focusManager.clearFocus()
                                }
                            )
                        }

                        // Dynamic Progress Bar Indicator 
                        AnimatedVisibility(
                            visible = activeTab?.isLoading == true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            LinearProgressIndicator(
                                progress = { (activeTab?.progress ?: 0) / 100f },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }

                        // WebView canvas wrapper (Fully floating card style)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(glassCardColor(isDark))
                                .border(
                                    width = 1.dp,
                                    color = glassBorderColor(isDark),
                                    shape = RoundedCornerShape(20.dp)
                                )
                        ) {
                            if (activeTabId != -1) {
                                if (isWebViewSupported == false) {
                                    FallbackBrowserSimulator(
                                        viewModel = viewModel,
                                        activeTab = activeTab,
                                        isDark = isDark,
                                        fontFamily = activeFont,
                                        onNavigate = { targetUrl ->
                                            viewModel.navigateActiveTab(targetUrl, context)
                                        }
                                    )
                                } else if (isWebViewSupported == true) {
                                    key(activeTabId, webViewTrigger) {
                                        AndroidView<android.widget.FrameLayout>(
                                            factory = { ctx ->
                                                android.widget.FrameLayout(ctx).apply {
                                                    layoutParams = android.view.ViewGroup.LayoutParams(
                                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                                        android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                                    )
                                                    try {
                                                        val webView = viewModel.getOrCreateWebView(activeTabId, ctx)
                                                        val parent = webView.parent as? android.view.ViewGroup
                                                        parent?.removeView(webView)
                                                        addView(webView)
                                                    } catch (e: Throwable) {
                                                        viewModel.markWebViewUnsupported()
                                                    }
                                                }
                                            },
                                            onRelease = { frame ->
                                                frame.removeAllViews()
                                            },
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    Box(modifier = Modifier.fillMaxSize())
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxSize())
                            }
                        }

                        // Beautiful floating bottom bar in ColorOS 16 theme
                        AnimatedVisibility(
                            visible = !isSearchFocused,
                            enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(300, easing = LinearEasing)) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300, easing = LinearEasing)) + fadeOut()
                        ) {
                            PersistentNavigationBar(
                                viewModel = viewModel,
                                activeTab = activeTab,
                                tabsCount = tabsList.size,
                                density = currentDensity,
                                scope = scope,
                                activeFont = activeFont
                            )
                        }
                    }
                }
                } // End Blur Box

            // Overlay Sheets/Dialogs triggered dynamically
            AnimatedVisibility(
                visible = showTabs,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                TabsOverviewPage(
                    viewModel = viewModel,
                    tabsList = tabsList,
                    activeTabId = activeTabId,
                    activeFont = activeFont,
                    onDismiss = { viewModel.showTabsOverview.value = false }
                )
            }

            // Standalone immersive Page view for Settings with smooth slide transitions matching HTML design
            AnimatedVisibility(
                visible = showSettings,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                SettingsSheet(
                    viewModel = viewModel,
                    settings = settings,
                    activeFont = activeFont,
                    sessionAds = adsBlockedSession,
                    sessionTrackers = trackersBlockedSession
                )
            }

            // Standalone immersive Page view for Bookmarks with smooth slide transitions
            AnimatedVisibility(
                visible = showBookmarks,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                BookmarksPage(
                    viewModel = viewModel,
                    activeFont = activeFont,
                    onDismiss = { viewModel.showBookmarks.value = false }
                )
            }

            // Standalone immersive Page view for History with smooth slide transitions
            AnimatedVisibility(
                visible = showHistory,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                HistoryPage(
                    viewModel = viewModel,
                    activeFont = activeFont,
                    onDismiss = { viewModel.showHistory.value = false }
                )
            }

            // Standalone immersive Page view for downloads with smooth slide transitions
            AnimatedVisibility(
                visible = showDownloads,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                DownloadsPage(
                    viewModel = viewModel,
                    activeFont = activeFont,
                    onDismiss = { viewModel.showDownloads.value = false }
                )
            }

            if (showMenuDrawer) {
                MenuDrawerSheet(
                    viewModel = viewModel,
                    settings = settings,
                    activeFont = activeFont,
                    onDismiss = { viewModel.showMenuDrawer.value = false }
                )
            }

            if (showShield) {
                ShieldDashboardSheet(
                    viewModel = viewModel,
                    settings = settings,
                    activeFont = activeFont,
                    sessionAds = adsBlockedSession,
                    sessionTrackers = trackersBlockedSession
                )
            }

            // Image download handler dialogue
            androidx.compose.animation.AnimatedVisibility(
                visible = imageDownloadProposal != null,
                enter = androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                imageDownloadProposal?.let { proposal ->
                    androidx.activity.compose.BackHandler {
                        viewModel.imageDownloadProposal.value = null
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isDark) Color(0x66000000) else Color(0x33000000))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { viewModel.imageDownloadProposal.value = null }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = {} // Consume click to prevent dismiss
                                ),
                            shape = RoundedCornerShape(28.dp),
                            color = Color.Transparent,
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(glassCardColor(isDark))
                                    .border(1.dp, if (isDark) Color(0x4DFFFFFF) else Color(0x80FFFFFF), RoundedCornerShape(28.dp))
                                    .padding(24.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .background(
                                                color = if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000),
                                                shape = androidx.compose.foundation.shape.CircleShape
                                            )
                                            .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1A000000), androidx.compose.foundation.shape.CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Download Image",
                                            tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                    
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Save Image",
                                            fontFamily = activeFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 22.sp,
                                            color = if (isDark) Color.White else Color(0xFF1C1C1E),
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = "Do you want to download this image to your device?",
                                            fontFamily = activeFont,
                                            fontSize = 15.sp,
                                            color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.7f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(16.dp))
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000))
                                                .border(1.dp, if (isDark) Color(0x33FFFFFF) else Color(0x1A000000), RoundedCornerShape(16.dp))
                                                .clickable { viewModel.imageDownloadProposal.value = null },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Cancel",
                                                fontFamily = activeFont,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDark) Color.White else Color(0xFF1C1C1E),
                                                fontSize = 16.sp
                                            )
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(52.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFF0066FF))
                                                .clickable {
                                                    try {
                                                        viewModel.triggerDownload(
                                                            url = proposal.url,
                                                            userAgent = null,
                                                            contentDisposition = null,
                                                            mimeType = "image/*",
                                                            contentLength = 0L,
                                                            context = context
                                                        )
                                                    } catch (e: Exception) {
                                                        android.widget.Toast.makeText(context, "Download failed", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                    viewModel.imageDownloadProposal.value = null
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "Download",
                                                fontFamily = activeFont,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                fontSize = 16.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // High-fidelity social redirects handler dialogue
            val appRedirectProposal by viewModel.appRedirectProposal.collectAsState()
            appRedirectProposal?.let { proposal ->
                AlertDialog(
                    onDismissRequest = {
                        viewModel.proceedWithBrowser(proposal.url, proposal.tabId)
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(proposal.url)).apply {
                                        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    viewModel.appRedirectProposal.value = null
                                } catch (e: Exception) {
                                    viewModel.proceedWithBrowser(proposal.url, proposal.tabId)
                                    android.widget.Toast.makeText(context, "App not installed, loading in browser", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Open App", fontFamily = activeFont, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                viewModel.proceedWithBrowser(proposal.url, proposal.tabId)
                            }
                        ) {
                            Text("Stay in Browser", fontFamily = activeFont, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Launch,
                            contentDescription = "External App",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "Open in ${proposal.appName}?",
                            fontFamily = activeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Text(
                            text = "This link can be viewed more smoothly inside the official ${proposal.appName} application.",
                            fontFamily = activeFont,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                )
            }

            permissionProposal?.let { proposal ->
                val involvesCamera = proposal.resourcesNeeded.contains(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                val involvesMic = proposal.resourcesNeeded.contains(android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                val involvesLocation = proposal.geoCallback != null

                AlertDialog(
                    onDismissRequest = { 
                        viewModel.handlePermissionProposal(false, true)
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val permissionsToRequest = mutableListOf<String>()
                                if (involvesCamera) permissionsToRequest.add(android.Manifest.permission.CAMERA)
                                if (involvesMic) permissionsToRequest.add(android.Manifest.permission.RECORD_AUDIO)
                                if (involvesLocation) permissionsToRequest.add(android.Manifest.permission.ACCESS_FINE_LOCATION)
                                
                                if (permissionsToRequest.isNotEmpty()) {
                                    multiplePermissionLauncher.launch(permissionsToRequest.toTypedArray())
                                } else {
                                    viewModel.handlePermissionProposal(true, true)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Allow", fontFamily = activeFont, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { 
                            viewModel.handlePermissionProposal(false, true)
                        }) {
                            Text("Deny", fontFamily = activeFont, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    },
                    icon = {
                        Icon(
                            imageVector = if (involvesCamera && involvesMic) Icons.Default.Videocam else if (involvesCamera) Icons.Default.Videocam else if (involvesMic) Icons.Default.Mic else Icons.Default.LocationOn,
                            contentDescription = "Permission Request",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        val requestedResourceText = if (involvesCamera && involvesMic) "Camera & Microphone" else if (involvesCamera) "Camera" else if (involvesMic) "Microphone" else if (involvesLocation) "Location" else "Unknown Resource"
                        Text(
                            text = "Allow $requestedResourceText?",
                            fontFamily = activeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${proposal.domain} wants to use your device's features.",
                                fontFamily = activeFont,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    },
                    shape = RoundedCornerShape(24.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                )
            }
        }
    }
}
}

// Capsule address and Shield Control header bar
@Composable
fun BrowserHeader(
    urlInput: String,
    activeTab: TabState?,
    density: LayoutDensity,
    fontFamily: FontFamily,
    settings: BrowserSettings,
    isFocused: Boolean = false,
    onFocusChange: (Boolean) -> Unit = {},
    onUrlChange: (String) -> Unit,
    onNavigate: () -> Unit,
    onRefresh: () -> Unit,
    viewModel: BrowserViewModel
) {
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    Surface(
        color = glassCardColor(isDark),
        tonalElevation = 6.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 4.dp)
            .border(
                width = 1.dp,
                color = glassBorderColor(isDark),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Anti-tracking indicator capsule showing stats
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color(0xFF0066FF).copy(alpha = 0.15f))
                    .clickable { viewModel.showShieldPanel.value = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("shield_button"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (settings.adBlockEnabled) Icons.Default.Shield else Icons.Default.ShieldMoon,
                        contentDescription = "Anti-tracker config",
                        tint = Color(0xFF0066FF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Address bar input field capsule
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDark) Color(0x33000000) else Color(0x12000000))
                    .border(
                        width = 1.dp,
                        color = glassBorderColor(isDark).copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (activeTab?.url?.startsWith("https://") == true) Icons.Default.Lock else Icons.Default.Public,
                    contentDescription = "Connection Security Status",
                    tint = if (activeTab?.url?.startsWith("https://") == true) Color(0xFF34C759) else (if (isDark) Color(0xFFA1A1A6) else Color(0xFF6E6E73)),
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(modifier = Modifier.weight(1f)) {
                    if (urlInput.isEmpty()) {
                        Text(
                            text = "Search or Enter URL",
                            fontFamily = fontFamily,
                            fontSize = (14f + density.fontOffset).sp,
                            color = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF6E6E73)
                        )
                    }
                    androidx.compose.foundation.text.BasicTextField(
                        value = urlInput,
                        onValueChange = onUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { onFocusChange(it.isFocused) }
                            .testTag("url_input_field"),
                        textStyle = LocalTextStyle.current.copy(
                            color = if (isDark) Color.White else Color(0xFF1C1C1E),
                            fontFamily = fontFamily,
                            fontSize = (14f + density.fontOffset).sp
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(if (isDark) Color.White else Color.Black),
                        singleLine = true,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(onGo = { onNavigate() })
                    )
                }

                if (urlInput.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear address text",
                        tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF6E6E73),
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onUrlChange("") }
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Refresh action icon button
            IconButton(
                onClick = onRefresh,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Web Page",
                    tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// Helpers for Adaptive Layouts
@Composable
fun AdaptiveNavLayout(
    isTablet: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (isTablet) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            content()
        }
    }
}

// Persistent Navigation bar offering tablet and mobile compliance layout
@Composable
fun PersistentNavigationBar(
    viewModel: BrowserViewModel,
    activeTab: TabState?,
    tabsCount: Int,
    density: LayoutDensity,
    scope: kotlinx.coroutines.CoroutineScope,
    activeFont: FontFamily
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600
    
    Surface(
        color = glassCardColor(isDark),
        tonalElevation = 6.dp,
        shadowElevation = 0.dp,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .padding(
                start = if (isTablet) 8.dp else 14.dp,
                end = if (isTablet) 8.dp else 14.dp,
                top = 4.dp, 
                bottom = 8.dp
            )
            .border(
                width = 1.dp,
                color = glassBorderColor(isDark),
                shape = RoundedCornerShape(24.dp)
            )
    ) {
        AdaptiveNavLayout(
            isTablet = isTablet,
            modifier = if (isTablet) {
                Modifier
                    .width(density.barHeight + 12.dp)
                    .fillMaxHeight(0.6f)
                    .padding(vertical = 14.dp)
            } else {
                Modifier
                    .fillMaxWidth()
                    .height(density.barHeight - 2.dp)
                    .padding(horizontal = 14.dp)
            }
        ) {
            // Back Action (Chevron Left)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(enabled = activeTab?.canGoBack == true) { viewModel.activeTabGoBack(context) }
                    .padding(8.dp)
                    .testTag("back_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "Go Back",
                    tint = if (activeTab?.canGoBack == true) {
                        if (isDark) Color.White else Color(0xFF1C1C1E)
                    } else {
                        (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.3f)
                    },
                    modifier = Modifier.size(28.dp)
                )
            }

            // Forward Action (Chevron Right)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(enabled = activeTab?.canGoForward == true) { viewModel.activeTabGoForward(context) }
                    .padding(8.dp)
                    .testTag("forward_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Go Forward",
                    tint = if (activeTab?.canGoForward == true) {
                        if (isDark) Color.White else Color(0xFF1C1C1E)
                    } else {
                        (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.3f)
                    },
                    modifier = Modifier.size(28.dp)
                )
            }

            // Home Button (Primary Color)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable {
                        scope.launch {
                            val currentSettings = viewModel.settings.value
                            viewModel.navigateActiveTab(currentSettings.homeUrl, context)
                        }
                    }
                    .padding(8.dp)
                    .testTag("home_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = "Search App Homepage",
                    tint = Color(0xFF0066FF),
                    modifier = Modifier.size(26.dp)
                )
            }

            // Tabs button as beautiful rounded square containing count
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.openTabsOverview() }
                    .padding(8.dp)
                    .testTag("tabs_button"),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(
                            width = 2.dp,
                            color = if (isDark) Color.White else Color(0xFF1C1C1E),
                            shape = RoundedCornerShape(6.dp)
                        ),
                     contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabsCount.toString(),
                        fontFamily = activeFont,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                    )
                }
            }

            // Menu trigger (List layout icon)
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { viewModel.showMenuDrawer.value = true }
                    .padding(8.dp)
                    .testTag("settings_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Menu Items",
                    tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                    modifier = Modifier.size(26.dp)
                )
            }
        }
    }
}



// BOTTOM APP DRAWER & OVERLAY MATCHING COLOROS PROTOTYPE
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuDrawerSheet(
    viewModel: BrowserViewModel,
    settings: BrowserSettings,
    activeFont: FontFamily,
    onDismiss: () -> Unit
) {
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    val solidColor = if (isDark) Color(0xA61E1E23) else Color(0xA6FAFAFA)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = solidColor,
        tonalElevation = 10.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.2f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 40.dp, top = 8.dp)
        ) {
            // Header: Menu & Close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Menu",
                    fontFamily = activeFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                )

                // Close Button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Menu",
                        tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Grid Layout for Buttons - 2 Columns using Column + Row
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // First Row: Bookmarks & History
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuDrawerItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Bookmark,
                        iconTint = Color(0xFF0066FF),
                        label = "Bookmarks",
                        activeFont = activeFont,
                        isDark = isDark,
                        onClick = {
                            viewModel.showBookmarks.value = true
                            onDismiss()
                        }
                    )
                    MenuDrawerItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.History,
                        iconTint = Color(0xFF0066FF),
                        label = "History",
                        activeFont = activeFont,
                        isDark = isDark,
                        onClick = {
                            viewModel.showHistory.value = true
                            onDismiss()
                        }
                    )
                }

                // Second Row: Downloads & Theme Mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MenuDrawerItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Download,
                        iconTint = Color(0xFF34C759),
                        label = "Downloads",
                        activeFont = activeFont,
                        isDark = isDark,
                        onClick = {
                            viewModel.showDownloads.value = true
                            onDismiss()
                        }
                    )
                    MenuDrawerItem(
                        modifier = Modifier.weight(1f),
                        icon = if (isDark) Icons.Default.WbSunny else Icons.Default.NightsStay,
                        iconTint = Color(0xFF0066FF),
                        label = if (isDark) "Light Mode" else "Dark Mode",
                        activeFont = activeFont,
                        isDark = isDark,
                        onClick = {
                            viewModel.updateThemeMode(if (isDark) "LIGHT" else "DARK", context as? android.app.Activity)
                        }
                    )
                }

                // Third Row spanning across (grid-column: span 2) - Shield Privacy Guard
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            viewModel.showShieldPanel.value = true
                            onDismiss()
                        }
                ) {
                    val sessionAds = viewModel.blockedAdsSession.collectAsState().value
                    val sessionTrackers = viewModel.blockedTrackersSession.collectAsState().value
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (isDark) Color(0xFF4CAF50).copy(alpha = 0.15f) else Color(0xFF4CAF50).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (settings.adBlockEnabled) Icons.Default.Shield else Icons.Default.ShieldMoon,
                                contentDescription = "Shield Guard",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Shield Privacy Guard",
                                fontFamily = activeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White else Color(0xFF1C1C1E)
                            )
                            if (settings.adBlockEnabled || settings.trackerBlockEnabled) {
                                Text(
                                    text = trans("Blocked") + " ${sessionAds + sessionTrackers} " + trans("trackers"),
                                    fontFamily = activeFont,
                                    fontSize = 12.sp,
                                    color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.6f)
                                )
                            } else {
                                Text(
                                    text = trans("Shield inactive"),
                                    fontFamily = activeFont,
                                    fontSize = 12.sp,
                                    color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                // Fourth Row spanning across - Browser Settings
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable {
                            viewModel.showSettings.value = true
                            onDismiss()
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (isDark) Color(0xFF0066FF).copy(alpha = 0.15f) else Color(0xFF0066FF).copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Browser Settings",
                                tint = Color(0xFF0066FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Browser Settings",
                            fontFamily = activeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isDark) Color.White else Color(0xFF1C1C1E)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MenuDrawerItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    label: String,
    activeFont: FontFamily,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isDark) iconTint.copy(alpha = 0.15f) else iconTint.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = label,
                fontFamily = activeFont,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (isDark) Color.White else Color(0xFF1C1C1E)
            )
        }
    }
}


// Sheet 1: Interactive Privacy Shield Dashboard
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShieldDashboardSheet(
    viewModel: BrowserViewModel,
    settings: BrowserSettings,
    activeFont: FontFamily,
    sessionAds: Int,
    sessionTrackers: Int
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val listTabs by viewModel.tabs.collectAsState()
    val listBookmarks by viewModel.bookmarks.collectAsState()
    val activId by viewModel.activeTabId.collectAsState()
    val permissionsList by viewModel.websitePermissions.collectAsState()
    
    val currentActiveTab = listTabs.find { it.id == activId }
    val currentDomain = currentActiveTab?.url?.let { urlStr ->
        try {
            val uri = java.net.URI(urlStr)
            var host = uri.host ?: ""
            if (host.startsWith("www.")) {
                host = host.substring(4)
            }
            host.ifEmpty { "this website" }
        } catch (e: Exception) {
            "this website"
        }
    } ?: "this website"

    val locationPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.toggleWebsiteLocation(currentDomain, true)
        else {
            android.widget.Toast.makeText(context, "Location Permission Denied", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.toggleWebsiteLocation(currentDomain, false)
        }
    }

    val cameraPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.toggleWebsiteCamera(currentDomain, true)
        else {
            android.widget.Toast.makeText(context, "Camera Permission Denied", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.toggleWebsiteCamera(currentDomain, false)
        }
    }

    val micPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) viewModel.toggleWebsiteMicrophone(currentDomain, true)
        else {
            android.widget.Toast.makeText(context, "Microphone Permission Denied", android.widget.Toast.LENGTH_SHORT).show()
            viewModel.toggleWebsiteMicrophone(currentDomain, false)
        }
    }
    
    val notificationsAllowed = permissionsList.any { it.domain == currentDomain && it.notificationsAllowed == true }

    val totalAds = settings.totalAdsBlocked
    val totalTrackers = settings.totalTrackersBlocked
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    val solidColor = if (isDark) Color(0xA6141416) else Color(0xA6F5F6F8)

    ModalBottomSheet(
        onDismissRequest = { viewModel.showShieldPanel.value = false },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = solidColor,
        tonalElevation = 10.dp,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(androidx.compose.foundation.rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Branded ColorOS 16 style premium Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchBrowserLogo(size = 40.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Shield Privacy Guard",
                        fontFamily = activeFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "ColorOS 16 Adaptive Interceptor • Running securely",
                        fontFamily = activeFont,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Score Dashboard Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0x1F00ADB5) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (sessionAds + sessionTrackers).toString(),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = activeFont
                        )
                        Text(
                            text = "Session Blocked",
                            fontSize = 11.sp,
                            fontFamily = activeFont,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(44.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = (totalAds + totalTrackers).toString(),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = activeFont
                        )
                        Text(
                            text = "Total Blocked",
                            fontSize = 11.sp,
                            fontFamily = activeFont,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Bookmark Quick Action
            Row(
                 modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isDark) Color(0x10FFFFFF) else Color(0x0A000000))
                    .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(18.dp))
                    .clickable { 
                        if (currentActiveTab != null && currentActiveTab.url.isNotEmpty() && !currentActiveTab.url.startsWith("https://search.")) {
                            viewModel.toggleBookmark(currentActiveTab.url, currentActiveTab.title)
                        } else {
                            android.widget.Toast.makeText(context, "Cannot bookmark this page", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isBookmarked = currentActiveTab != null && listBookmarks.any { it.url == currentActiveTab.url }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Bookmark",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(if (isBookmarked) "Saved to Bookmarks" else "Add to Bookmarks", fontFamily = activeFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        text = "Access this page later",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Individual Toggle Switches
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Ad Blocking
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color(0x10FFFFFF) else Color(0x0A000000))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(18.dp))
                        .clickable { viewModel.toggleAdBlock() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Block,
                            contentDescription = "Block Ads",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ad-Blocking Protection", fontFamily = activeFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Stop intrusive ads and malicious popups", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Switch(
                        checked = settings.adBlockEnabled,
                        onCheckedChange = { viewModel.toggleAdBlock() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Tracker Blocking
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color(0x10FFFFFF) else Color(0x0A000000))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(18.dp))
                        .clickable { viewModel.toggleTrackerBlock() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = "Block Trackers",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Anti-Tracker Shield", fontFamily = activeFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Prevent tracking pixels from recording cookies", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                    Switch(
                        checked = settings.trackerBlockEnabled,
                        onCheckedChange = { viewModel.toggleTrackerBlock() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Website Permissions Section
                val activePerm = permissionsList.find { it.domain == currentDomain } ?: WebsitePermission(domain = currentDomain, notificationsAllowed = false, locationAllowed = false, cameraAllowed = false, microphoneAllowed = false)

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Site Permissions",
                    fontFamily = activeFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                )

                // Website Notifications Permission Toggle Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color(0x10FFFFFF) else Color(0x0A000000))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(18.dp))
                        .clickable { viewModel.toggleWebsiteNotification(currentDomain, activePerm.notificationsAllowed != true) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Website Notifications",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow Website Notifications", fontFamily = activeFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (activePerm.notificationsAllowed == true) "Allowed for $currentDomain" else "Receive instant updates from $currentDomain",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = activePerm.notificationsAllowed == true,
                        onCheckedChange = { viewModel.toggleWebsiteNotification(currentDomain, activePerm.notificationsAllowed != true) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Website Location Access Row
                Row(
                     modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color(0x10FFFFFF) else Color(0x0A000000))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(18.dp))
                        .clickable { 
                            if (activePerm.locationAllowed != true) locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            else viewModel.toggleWebsiteLocation(currentDomain, false)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Website Location",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow Location Access", fontFamily = activeFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (activePerm.locationAllowed == true) "Location permission active" else "Allow site to ask for location",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = activePerm.locationAllowed == true,
                        onCheckedChange = { 
                            if (activePerm.locationAllowed != true) locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                            else viewModel.toggleWebsiteLocation(currentDomain, false)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Website Camera Access Row
                Row(
                     modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color(0x10FFFFFF) else Color(0x0A000000))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(18.dp))
                        .clickable { 
                            if (activePerm.cameraAllowed != true) cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            else viewModel.toggleWebsiteCamera(currentDomain, false)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Website Camera",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow Camera Access", fontFamily = activeFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (activePerm.cameraAllowed == true) "Camera permission active" else "Allow site to ask for camera",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = activePerm.cameraAllowed == true,
                        onCheckedChange = { 
                            if (activePerm.cameraAllowed != true) cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                            else viewModel.toggleWebsiteCamera(currentDomain, false)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                // Website Microphone Access Row
                Row(
                     modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDark) Color(0x10FFFFFF) else Color(0x0A000000))
                        .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(18.dp))
                        .clickable { 
                            if (activePerm.microphoneAllowed != true) micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            else viewModel.toggleWebsiteMicrophone(currentDomain, false)
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Website Microphone",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Allow Microphone Access", fontFamily = activeFont, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (activePerm.microphoneAllowed == true) "Microphone permission active" else "Allow site to ask for microphone",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = activePerm.microphoneAllowed == true,
                        onCheckedChange = { 
                            if (activePerm.microphoneAllowed != true) micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                            else viewModel.toggleWebsiteMicrophone(currentDomain, false)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

@Composable
fun TabCard(
    viewModel: BrowserViewModel,
    tab: TabState,
    isActive: Boolean,
    activeFont: FontFamily,
    isDark: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit
) {
    val hostSnippet = remember(tab.url) {
        try {
            val uri = java.net.URI(tab.url)
            val rawHost = uri.host ?: ""
            if (rawHost.startsWith("www.")) rawHost.substring(4) else rawHost
        } catch (e: Exception) {
            ""
        }
    }

    val avatarChar = if (hostSnippet.isNotEmpty()) {
        hostSnippet.first().uppercaseChar().toString()
    } else {
        "?"
    }

    val avatarColor = remember(hostSnippet) {
        var hash = 0
        for (char in hostSnippet) {
            hash = char.code + ((hash shl 5) - hash)
        }
        val h = Math.abs(hash % 360).toFloat()
        androidx.compose.ui.graphics.Color.hsv(h, if (isDark) 0.5f else 0.7f, if (isDark) 0.8f else 0.7f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clickable { onClick() }
            .border(
                width = if (isActive) 2.dp else 1.dp,
                color = if (isActive) androidx.compose.ui.graphics.Color(0xFF0066FF) else glassBorderColor(isDark),
                shape = RoundedCornerShape(20.dp)
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (isActive) androidx.compose.ui.graphics.Color(0xFF0066FF).copy(alpha = 0.05f) else glassCardColor(isDark),
        tonalElevation = if (isActive) 4.dp else 1.dp
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(color = avatarColor.copy(alpha = 0.15f), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarChar,
                            fontFamily = activeFont,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            fontSize = 11.sp,
                            color = avatarColor
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (tab.title.isBlank() || tab.title == "New Tab") "Start Page" else tab.title,
                        fontFamily = activeFont,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        color = if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF1C1C1E)
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Space",
                        tint = if (isDark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f) else androidx.compose.ui.graphics.Color(0xFF6E6E73),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (isDark) androidx.compose.ui.graphics.Color.White.copy(alpha = 0.08f) else androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.04f))
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                var bitmap by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
                val context = androidx.compose.ui.platform.LocalContext.current
                
                androidx.compose.runtime.LaunchedEffect(tab.id) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        try {
                            val webView = viewModel.getOrCreateWebView(tab.id, context)
                            val width = if (webView.width > 0) webView.width else 800
                            val height = if (webView.height > 0) webView.height else 1200
                            val bm = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bm)
                            webView.draw(canvas)
                            bitmap = bm.asImageBitmap()
                        } catch (e: Exception) {
                            // Ignored
                        }
                    }
                }
                
                if (bitmap != null) {
                    androidx.compose.foundation.Image(
                        bitmap = bitmap!!,
                        contentDescription = "Page Preview",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        alignment = Alignment.TopCenter
                    )
                } else {
                    Icon(
                        imageVector = if (tab.url == "about:blank" || tab.url.isEmpty()) Icons.Default.Language else Icons.Default.Public,
                        contentDescription = null,
                        tint = (if (isDark) androidx.compose.ui.graphics.Color.White else androidx.compose.ui.graphics.Color(0xFF0066FF)).copy(alpha = 0.12f),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
    }
}

// Full-Screen Page: High-fidelity, smooth and modern Tabs space manager (ColorOS 16 style)
@Composable
fun TabsOverviewPage(
    viewModel: BrowserViewModel,
    tabsList: List<TabState>,
    activeTabId: Int,
    activeFont: FontFamily,
    onDismiss: () -> Unit
) {
    var searchText by remember { mutableStateOf("") }
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    // Filtered tabs list
    val filteredTabs = remember(tabsList, searchText) {
        if (searchText.isEmpty()) {
            tabsList
        } else {
            tabsList.filter {
                it.title.contains(searchText, ignoreCase = true) ||
                        it.url.contains(searchText, ignoreCase = true)
            }
        }
    }

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .colorOSGradientBackground(isDark, alpha = 0.65f),
        color = Color.Transparent
    ) {
        val navBar: @Composable () -> Unit = {
            Surface(
                color = glassCardColor(isDark),
                shape = RoundedCornerShape(34.dp),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .padding(
                        start = if (isTablet) 8.dp else 16.dp,
                        end = if (isTablet) 8.dp else 16.dp,
                        top = if (isTablet) 16.dp else 12.dp,
                        bottom = if (isTablet) 16.dp else 24.dp
                    )
                    .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(34.dp))
            ) {
                AdaptiveNavLayout(
                    isTablet = isTablet,
                    modifier = if (isTablet) {
                        Modifier
                            .fillMaxHeight(0.7f)
                            .width(68.dp)
                            .padding(vertical = 24.dp)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .padding(horizontal = 24.dp)
                    }
                ) {
                    IconButton(onClick = { onDismiss() }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = { /* Forward Action Decoration */ }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Forward Action",
                            tint = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.3f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    IconButton(onClick = {
                        viewModel.navigateActiveTab(settings.homeUrl, context)
                        onDismiss()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home Page Launcher",
                            tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable { /* We are already on Tabs Page */ }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(
                                    color = Color(0xFF0066FF).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .border(
                                    width = 2.dp,
                                    color = Color(0xFF0066FF),
                                    shape = RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabsList.size.toString(),
                                fontFamily = activeFont,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0066FF)
                            )
                        }
                    }
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu Placeholder",
                            tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        if (isTablet) {
            Row(modifier = Modifier.fillMaxSize()) {
                navBar()
                Column(modifier = Modifier.fillMaxHeight().weight(1f)) {
                    // Search Header
                    Surface(
                        color = glassCardColor(isDark),
                        shape = RoundedCornerShape(28.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(56.dp)
                            .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(28.dp))
                    ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = if (isDark) Color(0xFFA1A1A6) else Color(0xFF6E6E73),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchText.isEmpty()) {
                            Text(
                                text = "Search or enter address",
                                fontFamily = activeFont,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF6E6E73)
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            textStyle = LocalTextStyle.current.copy(
                                color = if (isDark) Color.White else Color(0xFF1C1C1E),
                                fontFamily = activeFont,
                                fontSize = 15.sp
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 2. Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${tabsList.size} ${if (tabsList.size == 1) "Tab" else "Tabs"}",
                        fontFamily = activeFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.addNewTab()
                                onDismiss()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "New Tab",
                                tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (filteredTabs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LayersClear,
                                contentDescription = "No pages found",
                                tint = if (isDark) Color(0xFF2C2C30) else Color(0xFFE5E5EA),
                                modifier = Modifier.size(96.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = if (searchText.isNotEmpty()) "No active spaces match your search" else "No active tabs",
                                fontFamily = activeFont,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.7f),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredTabs, key = { it.id }) { tab ->
                            val isSelected = tab.id == activeTabId
                            
                            val hostSnippet = remember(tab.url) {
                                try {
                                    val uri = java.net.URI(tab.url)
                                    val rawHost = uri.host ?: ""
                                    if (rawHost.startsWith("www.")) rawHost.substring(4) else rawHost
                                } catch (e: Exception) {
                                    ""
                                }
                            }

                            val avatarChar = if (hostSnippet.isNotEmpty()) {
                                hostSnippet.first().uppercaseChar().toString()
                            } else {
                                "?"
                            }

                            val avatarColor = remember(hostSnippet) {
                                var hash = 0
                                for (char in hostSnippet) {
                                    hash = char.code + ((hash shl 5) - hash)
                                }
                                val h = Math.abs(hash % 360).toFloat()
                                Color.hsv(h, if (isDark) 0.5f else 0.7f, if (isDark) 0.8f else 0.7f)
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clickable {
                                        viewModel.selectTab(tab.id)
                                        onDismiss()
                                    }
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF0066FF) else glassBorderColor(isDark),
                                        shape = RoundedCornerShape(20.dp)
                                    ),
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) Color(0xFF0066FF).copy(alpha = 0.05f) else glassCardColor(isDark),
                                tonalElevation = if (isSelected) 4.dp else 1.dp
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    // Card top row (padding 10px / approx 12.dp)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            // Site Favicon Circle
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .background(
                                                        color = avatarColor.copy(alpha = 0.15f),
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = avatarChar,
                                                    fontFamily = activeFont,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = avatarColor
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (tab.title.isBlank() || tab.title == "New Tab") "Start Page" else tab.title,
                                                fontFamily = activeFont,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                            )
                                        }

                                        IconButton(
                                            onClick = { viewModel.removeTab(tab.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Close Space",
                                                tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF6E6E73),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f))
                                    )

                                    // Mid section placeholder with beautiful opacity browser/globe icon
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val previewBitmap = viewModel.tabPreviews[tab.id]
                                        if (previewBitmap != null) {
                                            androidx.compose.foundation.Image(
                                                bitmap = previewBitmap.asImageBitmap(),
                                                contentDescription = "Tab Preview",
                                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(
                                                imageVector = if (tab.url == "about:blank" || tab.url.isEmpty()) Icons.Default.Language else Icons.Default.Public,
                                                contentDescription = null,
                                                tint = (if (isDark) Color.White else Color(0xFF0066FF)).copy(alpha = 0.12f),
                                                modifier = Modifier.size(56.dp)
                                            )
                                        }
                                    } // closes internal Box
                                } // closes Column
                            } // closes Surface
                        } // closes Box
                    } // closes items
                } // closes LazyVerticalGrid
            } // closes else
            } // closes Column
            } // closes Row
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 104.dp)
                ) {
                    // Search Header
                    Surface(
                            color = glassCardColor(isDark),
                            shape = RoundedCornerShape(28.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .height(56.dp)
                                .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(28.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search Icon",
                                    tint = if (isDark) Color(0xFFA1A1A6) else Color(0xFF6E6E73),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (searchText.isEmpty()) {
                                        Text(
                                            text = "Search or enter address",
                                            fontFamily = activeFont,
                                            fontSize = 15.sp,
                                            color = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF6E6E73)
                                        )
                                    }
                                    androidx.compose.foundation.text.BasicTextField(
                                        value = searchText,
                                        onValueChange = { searchText = it },
                                        singleLine = true,
                                        textStyle = androidx.compose.ui.text.TextStyle(
                                            color = if (isDark) Color.White else Color(0xFF1C1C1E),
                                            fontSize = 15.sp,
                                            fontFamily = activeFont
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // 2. Beautiful Staggered Grid for tabs
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(if (LocalConfiguration.current.screenWidthDp > 600) 3 else 2),
                            contentPadding = PaddingValues(16.dp),
                            verticalItemSpacing = 16.dp,
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            item(span = StaggeredGridItemSpan.FullLine, key = "open_tabs_title") {
                                Text(
                                    text = "Open Tabs",
                                    fontFamily = activeFont,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = if (isDark) Color.White else Color(0xFF1C1C1E),
                                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 8.dp)
                                )
                            }
                            items(filteredTabs, key = { it.id }) { tab ->
                                Box(modifier = Modifier) {
                                    TabCard(
                                        viewModel = viewModel,
                                        tab = tab,
                                        isActive = tab.id == activeTabId,
                                        activeFont = activeFont,
                                        isDark = isDark,
                                        onClick = {
                                            viewModel.selectTab(tab.id)
                                            onDismiss()
                                        },
                                        onClose = {
                                            viewModel.removeTab(tab.id)
                                            if (tabsList.size == 1) {
                                                onDismiss()
                                            }
                                        }
                                    )
                                }
                            }

                            // Add New Tab Button styling as a glass tile
                            item(key = "new_tab_btn") {
                                Box(modifier = Modifier) {
                                    Surface(
                                        color = if (isDark) Color(0x0aFFFFFF) else Color(0x0a000000),
                                        shape = RoundedCornerShape(24.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.85f)
                                            .clip(RoundedCornerShape(24.dp))
                                            .clickable {
                                                viewModel.addNewTab()
                                                onDismiss()
                                            }
                                            .border(
                                                1.dp,
                                                if (isDark) Color(0x1aFFFFFF) else Color(0x1a000000),
                                                RoundedCornerShape(24.dp)
                                            )
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxSize(),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .background(Color(0xFF0066FF).copy(alpha = 0.1f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "New Tab",
                                                    tint = Color(0xFF0066FF),
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                ) {
                    navBar()
                }
            }
        }
    }
}


// Sheet 3: Core Settings & Personalization panel – Revamped to match the gorgeous fullscreen HTML design with an interactive customization suite
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    viewModel: BrowserViewModel,
    settings: BrowserSettings,
    activeFont: FontFamily,
    sessionAds: Int,
    sessionTrackers: Int
) {
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val tabsList by viewModel.tabs.collectAsState()
    val webPerms by viewModel.websitePermissions.collectAsState()

    var showAppearancePage by remember { mutableStateOf(false) }
    var showSearchEnginePage by remember { mutableStateOf(false) }
    var showLanguagesPage by remember { mutableStateOf(false) }
    var settingsSearchQuery by remember { mutableStateOf("") }

    androidx.activity.compose.BackHandler(enabled = showAppearancePage) { showAppearancePage = false }
    androidx.activity.compose.BackHandler(enabled = showSearchEnginePage) { showSearchEnginePage = false }
    androidx.activity.compose.BackHandler(enabled = showLanguagesPage) { showLanguagesPage = false }

    val query = settingsSearchQuery.trim().lowercase()

    val showGeneralGroup = query.isEmpty() || 
        "search engine".contains(query) || "google".contains(query) || "bing".contains(query) || "yahoo".contains(query) || "duckduckgo".contains(query) || "searchbrowser".contains(query) ||
        "languages".contains(query) || "english".contains(query) || "chinese".contains(query) || "spanish".contains(query) || "german".contains(query) || "french".contains(query)

    val showCustomizationGroup = query.isEmpty() ||
        "appearance".contains(query) || "typography".contains(query) || "font".contains(query) || "spacing".contains(query) || "theme".contains(query) || "accent".contains(query) || "customization".contains(query) ||
        "fluid animations".contains(query) || "motion".contains(query) || "coloros".contains(query) || "animation".contains(query)

    val showSecurityGroup = query.isEmpty() ||
        "ad shield block".contains(query) || "ad".contains(query) || "shield".contains(query) || "tracker shield block".contains(query) || "tracker".contains(query) || "security".contains(query) || "protect".contains(query) || "block".contains(query)

    val showNotificationGroup = webPerms.isNotEmpty() && (query.isEmpty() ||
        "notifications".contains(query) || "website permissions".contains(query) || "allowed notifications".contains(query) ||
        webPerms.any { it.domain.lowercase().contains(query) })

    val showDataGroup = query.isEmpty() ||
        "clear browsing statistics".contains(query) || "clear statistics".contains(query) || "erase history".contains(query) || "delete data".contains(query) || "clear browsing".contains(query) || "reset".contains(query)

    val showAboutGroup = query.isEmpty() || "about".contains(query) || "onboarding".contains(query) || "welcome".contains(query) || "start screen".contains(query)

    val noSettingsFound = !showGeneralGroup && !showCustomizationGroup && !showSecurityGroup && !showNotificationGroup && !showDataGroup && !showAboutGroup

    // Local wallpaper picker carousel (simulating dynamic glass wall backdrops)
    var wallpaperIndex by remember { mutableStateOf(0) }
    val wallpapers = listOf("Aquamorphic Glass", "Nordic Frost", "Sunset Horizon", "Cosmic Slate", "Forest Mist")
    val selectedWallpaper = wallpapers[wallpaperIndex % wallpapers.size]

    var showClearSuccessMessage by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .colorOSGradientBackground(isDark, alpha = 0.65f),
        color = Color.Transparent
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (showAppearancePage) {
                AppearanceSubPage(
                    viewModel = viewModel,
                    settings = settings,
                    activeFont = activeFont,
                    isDark = isDark,
                    onBack = { showAppearancePage = false }
                )
            } else if (showSearchEnginePage) {
                SearchEngineSubPage(
                    viewModel = viewModel,
                    settings = settings,
                    activeFont = activeFont,
                    isDark = isDark,
                    onBack = { showSearchEnginePage = false }
                )
            } else if (showLanguagesPage) {
                LanguagesSubPage(
                    viewModel = viewModel,
                    settings = settings,
                    activeFont = activeFont,
                    isDark = isDark,
                    onBack = { showLanguagesPage = false }
                )
            } else {
                // 1. Sleek Search Header (Top-bar)
                Surface(
                    color = glassCardColor(isDark),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(56.dp)
                        .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(28.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = if (isDark) Color(0xFFA1A1A6) else Color(0xFF6E6E73),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (settingsSearchQuery.isEmpty()) {
                                Text(
                                    text = "Search settings...",
                                    fontFamily = activeFont,
                                    fontSize = 14.sp,
                                    color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f)
                                )
                            }
                            androidx.compose.foundation.text.BasicTextField(
                                value = settingsSearchQuery,
                                onValueChange = { settingsSearchQuery = it },
                                textStyle = LocalTextStyle.current.copy(
                                    color = if (isDark) Color.White else Color(0xFF1C1C1E),
                                    fontFamily = activeFont,
                                    fontSize = 14.sp
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (settingsSearchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { settingsSearchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search query",
                                    tint = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Scrollable Settings Content (Main content area)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                // Settings Title
                Text(
                    text = "Settings",
                    fontFamily = activeFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 28.sp,
                    color = if (isDark) Color.White else Color(0xFF1C1C1E),
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                )

                // 1. GENERAL GROUP
                if (showGeneralGroup) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "GENERAL",
                        fontFamily = activeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                    Surface(
                        color = glassCardColor(isDark),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Search Engine Option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSearchEnginePage = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF0066FF).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search Icon",
                                        tint = Color(0xFF0066FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Search Engine",
                                        fontFamily = activeFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                    )
                                    val displayEngine = if (settings.searchEngine == "search.stormx.ninja") "Search Browser" else settings.searchEngine
                                    Text(
                                        text = "Active: $displayEngine",
                                        fontFamily = activeFont,
                                        fontSize = 12.sp,
                                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Go to Search Engine Data",
                                    tint = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            HorizontalDivider(color = glassBorderColor(isDark), thickness = 0.5.dp)

                            // Languages Option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showLanguagesPage = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF0066FF).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = "Language Icon",
                                        tint = Color(0xFF0066FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Languages",
                                        fontFamily = activeFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                    )
                                    Text(
                                        text = "Active: ${settings.language}",
                                        fontFamily = activeFont,
                                        fontSize = 12.sp,
                                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowRight,
                                    contentDescription = "Go to Language Data",
                                    tint = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.3f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
                }

                // 2. CUSTOMIZATION GROUP
                if (showCustomizationGroup) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "CUSTOMIZATION",
                        fontFamily = activeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )

                    Surface(
                        color = glassCardColor(isDark),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Appearance Option
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showAppearancePage = true }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFE1306C).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Palette,
                                        contentDescription = "Appearance Icon",
                                        tint = Color(0xFFE1306C),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Appearance",
                                        fontFamily = activeFont,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                    )
                                    Text(
                                        text = "Typography, Spacing Details, Themes, Accent Color",
                                        fontFamily = activeFont,
                                        fontSize = 13.sp,
                                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.6f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Go to Appearance settings",
                                    tint = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            HorizontalDivider(color = glassBorderColor(isDark), thickness = 0.5.dp)

                            // Fluid Animations Toggle
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.updateFluidAnimationsEnabled(!settings.fluidAnimationsEnabled) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF8E8E93).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = "Animations Icon",
                                        tint = Color(0xFF8E8E93),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Fluid Animations",
                                        fontFamily = activeFont,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                    )
                                    Text(
                                        text = "ColorOS motion effects",
                                        fontFamily = activeFont,
                                        fontSize = 12.sp,
                                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f)
                                    )
                                }
                                Switch(
                                    checked = settings.fluidAnimationsEnabled,
                                    onCheckedChange = { viewModel.updateFluidAnimationsEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF0066FF)
                                    )
                                )
                            }
                        }
                    }
                }
                }

                // 3. SECURITY & SHIELD GROUP
                if (showSecurityGroup) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "SECURITY & SHIELD",
                        fontFamily = activeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )

                    Surface(
                        color = glassCardColor(isDark),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Ad-Blocker Toggle Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleAdBlock() }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF4CAF50).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Shield Icon",
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Ad Shield Block",
                                        fontFamily = activeFont,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                    )
                                    Text(
                                        text = if (settings.adBlockEnabled) "Blocked $sessionAds ads this session (Total: ${settings.totalAdsBlocked})" else "Shield inactive",
                                        fontFamily = activeFont,
                                        fontSize = 12.sp,
                                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f)
                                    )
                                }
                                Switch(
                                    checked = settings.adBlockEnabled,
                                    onCheckedChange = { viewModel.toggleAdBlock() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF4CAF50)
                                    )
                                )
                            }

                            HorizontalDivider(color = glassBorderColor(isDark), thickness = 0.5.dp)

                            // Tracker-Blocker Toggle Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleTrackerBlock() }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF3F51B5).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Security Icon",
                                        tint = Color(0xFF3F51B5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Tracker Shield Block",
                                        fontFamily = activeFont,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                    )
                                    Text(
                                        text = if (settings.trackerBlockEnabled) "Blocked $sessionTrackers cookies/trackers (Total: ${settings.totalTrackersBlocked})" else "Shield inactive",
                                        fontFamily = activeFont,
                                        fontSize = 12.sp,
                                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f)
                                    )
                                }
                                Switch(
                                    checked = settings.trackerBlockEnabled,
                                    onCheckedChange = { viewModel.toggleTrackerBlock() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF3F51B5)
                                    )
                                )
                            }
                        }
                    }
                }
                }

                // 4. SITE PERMISSIONS GROUP
                if (showNotificationGroup) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "SITE PERMISSIONS",
                            fontFamily = activeFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f),
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )

                        Surface(
                            color = glassCardColor(isDark),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Saved Preferences",
                                        fontFamily = activeFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                    )
                                    TextButton(onClick = { viewModel.clearAllWebsitePermissions() }) {
                                        Text(
                                            text = "Revoke All",
                                            fontFamily = activeFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    webPerms.forEach { perm ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(if (isDark) Color(0x10FFFFFF) else Color(0x05000000), RoundedCornerShape(12.dp))
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Security,
                                                contentDescription = "Site Permissions",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = perm.domain,
                                                    fontFamily = activeFont,
                                                    fontWeight = FontWeight.Medium,
                                                    fontSize = 14.sp,
                                                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                                )
                                                val activeTextList = mutableListOf<String>()
                                                if (perm.notificationsAllowed != null) activeTextList.add("Notifications: ${if (perm.notificationsAllowed == true) "Allowed" else "Denied"}")
                                                if (perm.locationAllowed != null) activeTextList.add("Location: ${if (perm.locationAllowed == true) "Allowed" else "Denied"}")
                                                if (perm.cameraAllowed != null) activeTextList.add("Camera: ${if (perm.cameraAllowed == true) "Allowed" else "Denied"}")
                                                if (perm.microphoneAllowed != null) activeTextList.add("Microphone: ${if (perm.microphoneAllowed == true) "Allowed" else "Denied"}")
                                                val statusText = if (activeTextList.isEmpty()) "No permissions saved" else activeTextList.joinToString(", ")
                                                
                                                Text(
                                                    text = statusText,
                                                    fontFamily = activeFont,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.removeWebsitePermission(perm.domain) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "Delete website permission",
                                                    tint = Color.Red.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. DATA OPERATIONS GROUP
                if (showDataGroup) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "DATA OPERATIONS",
                        fontFamily = activeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )

                    Surface(
                        color = glassCardColor(isDark),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.clearBrowsingData()
                                        showClearSuccessMessage = true
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFFE53935).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteForever,
                                        contentDescription = "Delete Icon",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Clear Browsing Statistics",
                                        fontFamily = activeFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color(0xFFE53935)
                                    )
                                    Text(
                                        text = "Erase history, cached pages, search keywords",
                                        fontFamily = activeFont,
                                        fontSize = 12.sp,
                                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
                }

                // 6. ABOUT GROUP
                if (showAboutGroup) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ABOUT",
                        fontFamily = activeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f),
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )

                    Surface(
                        color = glassCardColor(isDark),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.startOnboarding()
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Color(0xFF38BDF8).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Welcome Setup Icon",
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Welcome Setup",
                                        fontFamily = activeFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                    )
                                    Text(
                                        text = "Restart onboarding configuration",
                                        fontFamily = activeFont,
                                        fontSize = 12.sp,
                                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }
                }

                if (noSettingsFound) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "No results",
                                tint = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No settings found matching your search",
                                fontFamily = activeFont,
                                fontSize = 15.sp,
                                color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Banner alert for clearing history success feedback
                if (showClearSuccessMessage) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (isDark) Color(0xFF1E3A1E) else Color(0xFFEAF8EC)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Success",
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "All browsing data erased!",
                                    fontFamily = activeFont,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32)
                                )
                            }
                            TextButton(onClick = { showClearSuccessMessage = false }) {
                                Text("OK", color = if (isDark) Color(0xFF81C784) else Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }

            // 3. Floating Bottom Navigation Bar matching the other immersive pages
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Surface(
                    color = glassCardColor(isDark),
                    shape = RoundedCornerShape(28.dp),
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(28.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.showSettings.value = false }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "Back",
                                tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(onClick = { /* Forward Decoration */ }) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Forward decoration",
                                tint = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.3f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(onClick = {
                            viewModel.navigateActiveTab(settings.homeUrl, context)
                            viewModel.showSettings.value = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home Page Launcher",
                                tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    viewModel.openTabsOverview()
                                    viewModel.showSettings.value = false
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(
                                        color = if (isDark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .border(
                                        width = 1.5.dp,
                                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f),
                                        shape = RoundedCornerShape(6.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tabsList.size.toString(),
                                    fontFamily = activeFont,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                )
                            }
                        }
                        IconButton(onClick = { /* Already on Menu/Settings page */ }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu active",
                                tint = Color(0xFF0066FF),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnScope.AppearanceSubPage(
    viewModel: BrowserViewModel,
    settings: BrowserSettings,
    activeFont: FontFamily,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Immersive Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back to Settings",
                    tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Appearance",
                fontFamily = activeFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = if (isDark) Color.White else Color(0xFF1C1C1E)
            )
        }

        Text(
            text = "THEME & BRAND ACCENT",
            fontFamily = activeFont,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
        )

        Surface(
            color = glassCardColor(isDark),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Theme Selection Row
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF0066FF).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isDark) Icons.Default.NightsStay else Icons.Default.WbSunny,
                                contentDescription = "Theme Icon",
                                tint = Color(0xFF0066FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Theme Mode",
                                fontFamily = activeFont,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color(0xFF1C1C1E)
                            )
                            Text(
                                text = "Active: ${settings.themeMode}",
                                fontFamily = activeFont,
                                fontSize = 12.sp,
                                color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0x0CFFFFFF) else Color(0x06000000), RoundedCornerShape(12.dp))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("LIGHT", "DARK", "SYSTEM").forEach { mode ->
                            val isSelected = settings.themeMode == mode
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF0066FF) else Color.Transparent)
                                    .clickable { viewModel.updateThemeMode(mode, context as? android.app.Activity) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = mode,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = activeFont,
                                    color = if (isSelected) Color.White else (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = glassBorderColor(isDark), thickness = 0.5.dp)

                // Brand Accent Color Selection Row
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF00BFA5).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Accent Icon",
                                tint = Color(0xFF00BFA5),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Brand Accent Color",
                                fontFamily = activeFont,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color(0xFF1C1C1E)
                            )
                            Text(
                                text = "Custom Theme Color",
                                fontFamily = activeFont,
                                fontSize = 12.sp,
                                color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0x0CFFFFFF) else Color(0x06000000), RoundedCornerShape(14.dp))
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val colors = listOf(Color(0xFF00BFA5), Color(0xFF00B0FF), Color(0xFFFF6D00), Color(0xFF7B1FA2), Color(0xFF37474F))
                        colors.forEachIndexed { idx, col ->
                            val isSelected = settings.customThemeColor == idx
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 0.dp,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E),
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.updateThemeColor(idx) }
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected Accent",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp).align(Alignment.Center)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "TYPOGRAPHY & LAYOUT",
            fontFamily = activeFont,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
        )

        Surface(
            color = glassCardColor(isDark),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Layout Spacing Density Row
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF7B1FA2).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DensityMedium,
                                contentDescription = "Density Icon",
                                tint = Color(0xFF7B1FA2),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Layout Spacing Density",
                                fontFamily = activeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color(0xFF1C1C1E)
                            )
                            Text(
                                text = "Active: ${settings.layoutDensity}",
                                fontFamily = activeFont,
                                fontSize = 12.sp,
                                color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0x0CFFFFFF) else Color(0x06000000), RoundedCornerShape(12.dp))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("COMPACT", "MODERATE", "COMFORTABLE").forEach { d ->
                            val isSelected = settings.layoutDensity == d
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF0066FF) else Color.Transparent)
                                    .clickable { viewModel.updateLayoutDensity(d) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = d,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = activeFont,
                                    color = if (isSelected) Color.White else (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = glassBorderColor(isDark), thickness = 0.5.dp)

                // Text Size and Page Zoom Rows
                var textSizeValue by remember { mutableStateOf(com.example.util.PreferenceHelper.textSize) }
                var pageZoomValue by remember { mutableStateOf(com.example.util.PreferenceHelper.pageZoom.toFloat()) }
                
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Text Size: ${textSizeValue.toInt()}%",
                        fontFamily = activeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                    )
                    Slider(
                        value = textSizeValue,
                        onValueChange = { textSizeValue = it },
                        onValueChangeFinished = { viewModel.updateTextSize(textSizeValue) },
                        valueRange = 50f..200f
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Page Zoom: ${pageZoomValue.toInt()}%",
                        fontFamily = activeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                    )
                    Slider(
                        value = pageZoomValue,
                        onValueChange = { pageZoomValue = it },
                        onValueChangeFinished = { viewModel.updatePageZoom(pageZoomValue.toInt()) },
                        valueRange = 50f..200f
                    )
                }

                HorizontalDivider(color = glassBorderColor(isDark), thickness = 0.5.dp)

                // Typography Style Row
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFFFF6D00).copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FontDownload,
                                contentDescription = "Font Icon",
                                tint = Color(0xFFFF6D00),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Typography Font Style",
                                fontFamily = activeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color(0xFF1C1C1E)
                            )
                            Text(
                                text = "Active: ${settings.fontFamily}",
                                fontFamily = activeFont,
                                fontSize = 12.sp,
                                color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) Color(0x0CFFFFFF) else Color(0x06000000), RoundedCornerShape(12.dp))
                            .border(1.dp, if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Default", "Serif", "Monospace").forEach { f ->
                            val isSelected = settings.fontFamily == f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0xFF0066FF) else Color.Transparent)
                                    .clickable { viewModel.updateFontFamily(f) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = f,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = activeFont,
                                    color = if (isSelected) Color.White else (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Sheet 4: Standalone Bookmarks page with premium search and bottom bar
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksPage(
    viewModel: BrowserViewModel,
    activeFont: FontFamily,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val bookmarksList by viewModel.bookmarks.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val tabsList by viewModel.tabs.collectAsState()
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter bookmarks list
    val filteredBookmarks = remember(bookmarksList, searchQuery) {
        bookmarksList.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.url.contains(searchQuery, ignoreCase = true)
        }
    }
    
    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .colorOSGradientBackground(isDark, alpha = 0.65f),
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (isTablet) 80.dp else 0.dp,
                        bottom = if (isTablet) 0.dp else 80.dp
                    )
            ) {
                // 1. Sleek Search Header (Top-bar)
            Surface(
                color = glassCardColor(isDark),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(56.dp)
                    .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(28.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = if (isDark) Color(0xFFA1A1A6) else Color(0xFF6E6E73),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search bookmarks or enter address",
                                fontFamily = activeFont,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF6E6E73)
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = LocalTextStyle.current.copy(
                                color = if (isDark) Color.White else Color(0xFF1C1C1E),
                                fontFamily = activeFont,
                                fontSize = 15.sp
                            ),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Search
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.navigateActiveTab(searchQuery, context)
                                        onDismiss()
                                    }
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 2. Main Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bookmarks",
                        fontFamily = activeFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                    )
                    
                    val activeId by viewModel.activeTabId.collectAsState()
                    val currentTab = tabsList.find { it.id == activeId }
                    val currentUrl = currentTab?.url ?: ""
                    val currentTitle = currentTab?.title ?: ""
                    val isBookmarked = bookmarksList.any { it.url == currentUrl }
                    
                    IconButton(
                        onClick = { 
                            if (currentUrl.isNotEmpty() && !currentUrl.startsWith("https://search.")) {
                                viewModel.toggleBookmark(currentUrl, currentTitle)
                            } else {
                                android.widget.Toast.makeText(context, "Cannot bookmark this page", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Add current tab to Bookmark",
                            tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                if (filteredBookmarks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyPlaceholderUI("No Bookmarks saved yet.", activeFont)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredBookmarks, key = { it.id }) { b ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(glassCardColor(isDark))
                                    .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
                                    .clickable {
                                        viewModel.navigateActiveTab(b.url, context)
                                        onDismiss()
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Dynamic favicon badge from domain host name hash
                                val fallbackColor = remember(b.url) {
                                    val hash = b.url.hashCode()
                                    val r = (hash and 0xFF0000 shr 16) % 140 + 60
                                    val g = (hash and 0x00FF00 shr 8) % 140 + 60
                                    val bCol = (hash and 0x0000FF) % 140 + 60
                                    Color(r, g, bCol)
                                }
                                val letter = remember(b.url) {
                                    val host = try { android.net.Uri.parse(b.url).host ?: "S" } catch(e: Exception) { "S" }
                                    val clean = host.removePrefix("www.")
                                    if (clean.isNotEmpty()) clean.first().uppercase() else "S"
                                }

                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(fallbackColor.copy(alpha = 0.1f), shape = RoundedCornerShape(14.dp))
                                        .border(1.dp, fallbackColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letter,
                                        color = fallbackColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        fontFamily = activeFont
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = b.title.ifBlank { "Unlabeled Bookmark" },
                                        fontFamily = activeFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                    )
                                    Text(
                                        text = b.url,
                                        fontFamily = activeFont,
                                        fontSize = 13.sp,
                                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF6E6E73),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(glassCardColor(isDark))
                                        .border(1.dp, glassBorderColor(isDark), CircleShape)
                                        .clickable { viewModel.toggleBookmark(b.url, b.title) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete bookmark",
                                        tint = Color(0xFFFF3B30),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            } // CLOSE COLUMN

            // 3. Adaptive Navigation Bar
            AdaptiveNavLayout(
                isTablet = isTablet,
                modifier = Modifier.then(
                    if (isTablet) {
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .width(68.dp)
                            .fillMaxHeight(0.6f)
                            .background(glassCardColor(isDark), RoundedCornerShape(34.dp))
                            .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(34.dp))
                            .padding(vertical = 24.dp)
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth()
                            .height(68.dp)
                            .background(glassCardColor(isDark), RoundedCornerShape(28.dp))
                            .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(28.dp))
                            .padding(horizontal = 24.dp)
                    }
                )
            ) {
                IconButton(onClick = { onDismiss() }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = { /* Forward Action Handled via context */ }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Forward Action",
                        tint = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.3f),
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = {
                    viewModel.navigateActiveTab(settings.homeUrl, context)
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home Page Launcher",
                        tint = Color(0xFF0066FF),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            viewModel.openTabsOverview()
                            onDismiss()
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(
                                width = 2.dp,
                                color = if (isDark) Color.White else Color(0xFF1C1C1E),
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabsList.size.toString(),
                            fontFamily = activeFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1C1C1E)
                        )
                    }
                }
                IconButton(onClick = {
                    viewModel.showMenuDrawer.value = true
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu Drawer Launcher",
                        tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                        modifier = Modifier.size(28.dp)
                    )
                }
            } // AdaptiveNavLayout
        } // Box
    } // Surface
} // Fun

// Sheet 5: Standalone History page with timeline and clear capability
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryPage(
    viewModel: BrowserViewModel,
    activeFont: FontFamily,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val historyList by viewModel.history.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val tabsList by viewModel.tabs.collectAsState()
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    
    // Filter history list
    val filteredHistory = remember(historyList, searchQuery) {
        historyList.filter { 
            it.title.contains(searchQuery, ignoreCase = true) || 
            it.url.contains(searchQuery, ignoreCase = true)
        }
    }
    
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All History", fontFamily = activeFont, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete all browsing logs?", fontFamily = activeFont) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearBrowsingData()
                    showClearConfirmDialog = false
                }) {
                    Text("Clear All", color = Color(0xFFFF3B30), fontFamily = activeFont, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", fontFamily = activeFont)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = if (isDark) Color(0xFF1E1E22) else Color.White
        )
    }

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .colorOSGradientBackground(isDark, alpha = 0.65f),
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = if (isTablet) 80.dp else 0.dp,
                        bottom = if (isTablet) 0.dp else 80.dp
                    )
            ) {
                // 1. Sleek Search Header (Top-bar)
            Surface(
                color = glassCardColor(isDark),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .height(56.dp)
                    .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(28.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search Icon",
                        tint = if (isDark) Color(0xFFA1A1A6) else Color(0xFF6E6E73),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search history or enter address",
                                fontFamily = activeFont,
                                fontSize = 15.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF6E6E73)
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = LocalTextStyle.current.copy(
                                color = if (isDark) Color.White else Color(0xFF1C1C1E),
                                fontFamily = activeFont,
                                fontSize = 15.sp
                            ),
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                imeAction = androidx.compose.ui.text.input.ImeAction.Search
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onSearch = {
                                    if (searchQuery.isNotBlank()) {
                                        viewModel.navigateActiveTab(searchQuery, context)
                                        onDismiss()
                                    }
                                }
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 2. Main Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "History",
                        fontFamily = activeFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                    )
                    IconButton(
                        onClick = { showClearConfirmDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Clear All History Logs",
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                if (filteredHistory.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyPlaceholderUI("No browsing history catalog logged.", activeFont)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        item {
                            Text(
                                text = "Today",
                                fontFamily = activeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF6E6E73),
                                modifier = Modifier.padding(bottom = 4.dp, top = 8.dp)
                            )
                        }
                        
                        items(filteredHistory, key = { it.id }) { h ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(glassCardColor(isDark))
                                    .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
                                    .clickable {
                                        viewModel.navigateActiveTab(h.url, context)
                                        onDismiss()
                                    }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(Color(0xFF6E6E73).copy(alpha = 0.1f), shape = RoundedCornerShape(14.dp))
                                        .border(1.dp, Color(0xFF6E6E73).copy(alpha = 0.2f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Public,
                                        contentDescription = "Public Site Sphere Icon",
                                        tint = if (isDark) Color(0xFFA1A1A6) else Color(0xFF6E6E73),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = h.title.ifBlank { "Blank Space Log" },
                                        fontFamily = activeFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                                    )
                                    Text(
                                        text = h.url,
                                        fontFamily = activeFont,
                                        fontSize = 13.sp,
                                        color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF6E6E73),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(glassCardColor(isDark))
                                        .border(1.dp, glassBorderColor(isDark), CircleShape)
                                        .clickable { viewModel.deleteHistory(h.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete history log",
                                        tint = Color(0xFFFF3B30),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            } // CLOSE COLUMN

            // 3. Adaptive Navigation Bar
            AdaptiveNavLayout(
                isTablet = isTablet,
                modifier = Modifier.then(
                    if (isTablet) {
                        Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .width(68.dp)
                            .fillMaxHeight(0.6f)
                            .background(glassCardColor(isDark), RoundedCornerShape(34.dp))
                            .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(34.dp))
                            .padding(vertical = 24.dp)
                    } else {
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth()
                            .height(68.dp)
                            .background(glassCardColor(isDark), RoundedCornerShape(28.dp))
                            .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(28.dp))
                            .padding(horizontal = 24.dp)
                    }
                )
            ) {
                IconButton(onClick = { onDismiss() }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = { /* Forward Action Mock */ }) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Forward Action",
                        tint = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.3f),
                        modifier = Modifier.size(28.dp)
                    )
                }
                IconButton(onClick = {
                    viewModel.navigateActiveTab(settings.homeUrl, context)
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home Page Launcher",
                        tint = Color(0xFF0066FF),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable {
                            viewModel.openTabsOverview()
                            onDismiss()
                        }
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .border(
                                width = 2.dp,
                                color = if (isDark) Color.White else Color(0xFF1C1C1E),
                                shape = RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabsList.size.toString(),
                            fontFamily = activeFont,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1C1C1E)
                        )
                    }
                }
                IconButton(onClick = {
                    viewModel.showMenuDrawer.value = true
                    onDismiss()
                }) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu Drawer",
                        tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                        modifier = Modifier.size(26.dp)
                    )
                }
            } // AdaptiveNavLayout
        } // Box
    } // Surface
} // Fun

// Standalone Page: Downloads directory manager (ColorOS 16 styled)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsPage(
    viewModel: BrowserViewModel,
    activeFont: FontFamily,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsState()
    val isDark = when (settings.themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }
    var selectedTab by remember { mutableStateOf(0) }
    
    // Theme Colors
    val bgColor = if (isDark) Color(0xFF121214) else Color(0xFFF2F2F7)
    val glassBg = if (isDark) Color(0xA61E1E23) else Color(0xFFE5E5EA)
    val glassBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFD1D1D6)
    val textMain = if (isDark) Color(0xFFF5F5F7) else Color(0xFF1C1C1E)
    val textMuted = if (isDark) Color(0xFFA1A1A6) else Color(0xFF6E6E73)
    val accentColor = if (isDark) Color(0xFF4D94FF) else Color(0xFF0066FF)
    val successColor = if (isDark) Color(0xFF30D158) else Color(0xFF34C759)
    val dangerColor = if (isDark) Color(0xFFFF453A) else Color(0xFFFF3B30)
    val itemBg = if (isDark) Color(0x0DFFFFFF) else Color(0xFFFFFFFF)
    val itemBorder = if (isDark) Color(0x1AFFFFFF) else Color(0xFFC7C7CC)

    val allDownloads by viewModel.downloads.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val searchFocusRequester = remember { FocusRequester() }
    
    val filteredDownloads = remember(selectedTab, allDownloads, searchQuery) {
        val list = if (searchQuery.isNotBlank()) {
            allDownloads.filter { it.fileName.contains(searchQuery, ignoreCase = true) }
        } else {
            allDownloads
        }
        when (selectedTab) {
            1 -> list.filter { it.status == "DOWNLOADING" || it.status == "PENDING" }
            2 -> list.filter { it.status == "COMPLETED" || it.status == "FAILED" }
            else -> list
        }
    }

    val isTablet = LocalConfiguration.current.screenWidthDp >= 600

    var itemToDelete by remember { mutableStateOf<DownloadItem?>(null) }
    var deleteFromStorage by remember { mutableStateOf(false) }

    if (itemToDelete != null) {
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Delete Download", fontFamily = activeFont, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Are you sure you want to remove this download from your history?", fontFamily = activeFont)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier.clickable { deleteFromStorage = !deleteFromStorage }
                    ) {
                        Checkbox(
                            checked = deleteFromStorage,
                            onCheckedChange = { deleteFromStorage = it }
                        )
                        Text("Delete file from device", fontFamily = activeFont)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val item = itemToDelete
                        if (item != null) {
                            if (deleteFromStorage) {
                                try {
                                    val file = java.io.File(item.filePath)
                                    if (file.exists()) {
                                        file.delete()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                            viewModel.deleteDownload(item.id)
                        }
                        itemToDelete = null
                        deleteFromStorage = false
                    }
                ) {
                    Text("Delete", color = dangerColor, fontFamily = activeFont, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { itemToDelete = null }) {
                    Text("Cancel", fontFamily = activeFont)
                }
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .colorOSGradientBackground(isDark, alpha = 0.65f),
        color = Color.Transparent
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Content
            Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = if (isTablet) 100.dp else 20.dp,
                    end = 20.dp,
                    bottom = if (isTablet) 20.dp else 120.dp,
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 80.dp
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Downloads", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = textMain, fontFamily = activeFont)
            }

            // Tabs
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(glassBg, shape = RoundedCornerShape(24.dp))
                    .border(1.dp, glassBorder, RoundedCornerShape(24.dp))
                    .padding(4.dp)
            ) {
                val config = LocalConfiguration.current
                val tabWidth = with(LocalDensity.current) { (config.screenWidthDp - 48)/3 } 
                
                // Animated Indicator
                val indicatorOffset by animateDpAsState(
                    targetValue = (tabWidth * selectedTab).dp, 
                    animationSpec = tween(400, easing = FastOutSlowInEasing),
                    label = ""
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                ) {
                    Box(
                        modifier = Modifier
                            .width((tabWidth).dp)
                            .fillMaxHeight()
                            .offset(x = indicatorOffset)
                            .background(itemBg, shape = RoundedCornerShape(20.dp))
                            .border(1.dp, itemBorder, RoundedCornerShape(20.dp))
                    )
                }
                
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("All", "Ongoing", "Complete").forEachIndexed { index, title ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedTab = index }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (selectedTab == index) textMain else textMuted,
                                fontFamily = activeFont
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lists with Smooth Transition
            AnimatedContent(
                targetState = filteredDownloads,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "Download List Transition"
            ) { items ->
                if (items.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                        Text("No Downloads", color = textMuted, fontFamily = activeFont, fontSize = 15.sp)
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items.forEach { downloadItem ->
                            var isMenuExpanded by remember { mutableStateOf(false) }
                            val isCompleted = downloadItem.status == "COMPLETED"
                            val isDownloading = downloadItem.status == "DOWNLOADING"
                            val progress = if (downloadItem.totalBytes > 0) downloadItem.downloadedBytes.toFloat() / downloadItem.totalBytes.toFloat() else 0f
                            val animatedProgress by animateFloatAsState(targetValue = progress, animationSpec = tween(300, easing = LinearEasing), label = "")
                            val baseIconTint = if (downloadItem.status == "FAILED") dangerColor else if (isDownloading) accentColor else successColor

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(itemBg, RoundedCornerShape(20.dp))
                                    .border(1.dp, itemBorder, RoundedCornerShape(20.dp))
                                    .clickable {
                                        if (isCompleted) {
                                            openDownloadedFile(context, downloadItem.filePath, downloadItem.mimeType)
                                        }
                                    }
                                    .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .background(baseIconTint.copy(alpha = 0.1f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(getFileTypeIcon(downloadItem.fileName, downloadItem.status), contentDescription = null, tint = baseIconTint, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(downloadItem.fileName, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = textMain, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = activeFont)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    if (isCompleted || downloadItem.status == "FAILED") {
                                        Text("${formatBytes(downloadItem.totalBytes)} • ${downloadItem.status}", fontSize = 13.sp, color = textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, fontFamily = activeFont)
                                    } else {
                                        val speed = viewModel.downloadSpeeds[downloadItem.id] ?: "Active"
                                        val eta = viewModel.downloadEtas[downloadItem.id]
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(downloadItem.status, fontSize = 13.sp, color = textMuted, fontFamily = activeFont)
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = speed, 
                                                    fontSize = 13.sp, 
                                                    color = textMuted, 
                                                    fontFamily = activeFont, 
                                                    style = androidx.compose.ui.text.TextStyle(
                                                        fontFeatureSettings = "tnum",
                                                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                                                    ),
                                                    modifier = Modifier.width(72.dp)
                                                )
                                                if (eta != null) {
                                                    Text(
                                                        text = eta, 
                                                        fontSize = 13.sp, 
                                                        color = textMuted, 
                                                        fontFamily = activeFont, 
                                                        style = androidx.compose.ui.text.TextStyle(
                                                            fontFeatureSettings = "tnum",
                                                            textAlign = androidx.compose.ui.text.style.TextAlign.End
                                                        ),
                                                        modifier = Modifier.width(52.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .background(if(isDark) Color(0x1AFFFFFF) else Color(0x0D000000), RoundedCornerShape(3.dp))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(animatedProgress.coerceIn(0f, 1f))
                                                    .fillMaxHeight()
                                                    .background(baseIconTint, RoundedCornerShape(3.dp))
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))

                                // Real Functionality: Select and Delete dropdown
                                Box {
                                    IconButton(onClick = { isMenuExpanded = true }) {
                                        Icon(Icons.Default.MoreVert, contentDescription = "More", tint = textMain, modifier = Modifier.size(24.dp))
                                    }
                                    DropdownMenu(
                                        expanded = isMenuExpanded,
                                        onDismissRequest = { isMenuExpanded = false },
                                        modifier = Modifier.background(glassBg)
                                    ) {
                                        if (downloadItem.status == "DOWNLOADING" || downloadItem.status == "PENDING") {
                                            DropdownMenuItem(
                                                text = { Text("Pause", color = textMain, fontFamily = activeFont) },
                                                onClick = {
                                                    isMenuExpanded = false
                                                    viewModel.pauseDownload(downloadItem.id, context)
                                                }
                                            )
                                        }
                                        if (downloadItem.status == "PAUSED") {
                                            DropdownMenuItem(
                                                text = { Text("Unpause", color = textMain, fontFamily = activeFont) },
                                                onClick = {
                                                    isMenuExpanded = false
                                                    viewModel.resumeDownload(downloadItem.id, context)
                                                }
                                            )
                                        }
                                        if (downloadItem.status == "COMPLETED") {
                                            DropdownMenuItem(
                                                text = { Text("Share file", color = textMain, fontFamily = activeFont) },
                                                onClick = {
                                                    isMenuExpanded = false
                                                    shareDownloadedFile(context, downloadItem.filePath, downloadItem.mimeType)
                                                }
                                            )
                                        }
                                        DropdownMenuItem(
                                            text = { Text("Delete file", color = dangerColor, fontFamily = activeFont) },
                                            onClick = {
                                                isMenuExpanded = false
                                                itemToDelete = downloadItem
                                                deleteFromStorage = false
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

        // Top Bar Gap + Search Bar
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 16.dp)
                .padding(
                    start = if (isTablet) 100.dp else 16.dp, 
                    end = 16.dp
                )
                .fillMaxWidth()
                .background(glassBg, RoundedCornerShape(28.dp))
                .border(1.5f.dp, glassBorder, RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = textMuted, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = textMain,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = activeFont
                ),
                modifier = Modifier.weight(1f).focusRequester(searchFocusRequester),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (searchQuery.isEmpty()) {
                        Text("Search downloads", color = textMuted, fontWeight = FontWeight.Medium, fontSize = 16.sp, fontFamily = activeFont)
                    }
                    innerTextField()
                }
            )
            if (searchQuery.isNotEmpty()) {
                IconButton(
                    onClick = { searchQuery = "" },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = textMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Adaptive Navigation Bar
        AdaptiveNavLayout(
            isTablet = isTablet,
            modifier = if (isTablet) {
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .width(68.dp)
                    .fillMaxHeight(0.6f)
                    .background(glassBg, RoundedCornerShape(34.dp))
                    .border(1.5f.dp, glassBorder, RoundedCornerShape(34.dp))
                    .padding(vertical = 24.dp)
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 24.dp)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth()
                    .background(glassBg, RoundedCornerShape(34.dp))
                    .border(1.5f.dp, glassBorder, RoundedCornerShape(34.dp))
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            }
        ) {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Back", tint = textMain, modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Forward", tint = textMain, modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = { 
                viewModel.navigateActiveTab("https://search.stormx.ninja/", context)
                onDismiss() 
            }) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = textMain, modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = { searchFocusRequester.requestFocus() }) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = textMain, modifier = Modifier.size(26.dp))
            }
            IconButton(onClick = { }) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = accentColor, modifier = Modifier.size(26.dp))
            }
        }
    }
    }
}

// Select suitable file icon type based on suffix safely using core material icons
fun getFileTypeIcon(fileName: String, status: String): ImageVector {
    if (status == "FAILED") return Icons.Default.Close
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "mp3", "wav", "m4a", "ogg", "flac" -> Icons.Default.PlayArrow
        "mp4", "mkv", "avi", "webm", "mov" -> Icons.Default.PlayArrow
        "zip", "rar", "tar", "gz", "7z", "iso", "apk", "aab" -> Icons.Default.Folder
        else -> Icons.Default.FileDownload
    }
}

fun getResolvedMimeType(filePath: String, fallbackMimeType: String?): String {
    val extension = filePath.substringAfterLast('.', "").lowercase()
    return when (extension) {
        "apk" -> "application/vnd.android.package-archive"
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "image/*"
        "mp4", "mkv", "avi", "webm", "mov", "ts" -> "video/*"
        "mp3", "wav", "m4a", "ogg", "flac", "aac" -> "audio/*"
        else -> fallbackMimeType ?: "*/*"
    }
}

// Launches system level intents to explore / open completed files safely
fun openDownloadedFile(context: android.content.Context, filePath: String, mimeType: String?) {
    try {
        val file = java.io.File(filePath)
        if (!file.exists()) {
            android.widget.Toast.makeText(context, "File not found: $filePath", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val resolvedMimeType = getResolvedMimeType(filePath, mimeType)
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, resolvedMimeType)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Cannot open file: $filePath", android.widget.Toast.LENGTH_LONG).show()
    }
}

fun shareDownloadedFile(context: android.content.Context, filePath: String, mimeType: String?) {
    try {
        val file = java.io.File(filePath)
        if (!file.exists()) {
            android.widget.Toast.makeText(context, "File not found: $filePath", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val resolvedMimeType = getResolvedMimeType(filePath, mimeType)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = resolvedMimeType
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = android.content.Intent.createChooser(intent, "Share file")
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Cannot share file: $filePath", android.widget.Toast.LENGTH_LONG).show()
    }
}

@Composable
fun EmptyPlaceholderUI(text: String, fontFamily: FontFamily) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = "Empty",
            tint = Color.LightGray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            fontFamily = fontFamily,
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

// Format bytes into readable filesystem sizes
fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.size - 1) {
        value /= 1024
        index++
    }
    return String.format(java.util.Locale.US, "%.1f %s", value, units[index])
}

@Composable
fun getWebsiteThemedColor(url: String?): Color {
    val isDark = isSystemInDarkTheme()
    val surfaceColor = MaterialTheme.colorScheme.surface
    if (url.isNullOrBlank()) {
        return surfaceColor.copy(alpha = 0.82f)
    }
    
    val host = try {
        val uri = java.net.URI(url)
        val h = uri.host ?: ""
        if (h.startsWith("www.")) h.substring(4) else h
    } catch (e: Exception) {
        ""
    }
    
    if (host.isEmpty()) return surfaceColor.copy(alpha = 0.82f)
    
    val presetColor = when {
        host.contains("google", ignoreCase = true) -> Color(0xFF4285F4)
        host.contains("github", ignoreCase = true) -> if (isDark) Color(0xFF24292E) else Color(0xFFF6F8FA)
        host.contains("wikipedia", ignoreCase = true) -> Color(0xFF9E9E9E)
        host.contains("searchbrowser", ignoreCase = true) || host.contains("ninja", ignoreCase = true) -> Color(0xFF00ADB5)
        host.contains("youtube", ignoreCase = true) -> Color(0xFFFF0000)
        host.contains("twitter", ignoreCase = true) || host.contains("x.com", ignoreCase = true) -> Color(0xFF1DA1F2)
        host.contains("facebook", ignoreCase = true) -> Color(0xFF1877F2)
        host.contains("reddit", ignoreCase = true) -> Color(0xFFFF4500)
        host.contains("amazon", ignoreCase = true) -> Color(0xFFFF9900)
        else -> null
    }

    val baseColor = if (presetColor != null) {
        presetColor
    } else {
        var hash = 0
        for (char in host) {
            hash = char.code + ((hash shl 5) - hash)
        }
        val h = Math.abs(hash % 360).toFloat()
        val s = if (isDark) 0.35f else 0.12f
        val v = if (isDark) 0.18f else 0.96f
        Color.hsv(h, s, v)
    }

    return if (isDark) {
        Color(
            red = (surfaceColor.red * 0.78f) + (baseColor.red * 0.22f),
            green = (surfaceColor.green * 0.78f) + (baseColor.green * 0.22f),
            blue = (surfaceColor.blue * 0.78f) + (baseColor.blue * 0.22f),
            alpha = 0.82f
        )
    } else {
        Color(
            red = (surfaceColor.red * 0.82f) + (baseColor.red * 0.18f),
            green = (surfaceColor.green * 0.82f) + (baseColor.green * 0.18f),
            blue = (surfaceColor.blue * 0.82f) + (baseColor.blue * 0.18f),
            alpha = 0.82f
        )
    }
}

// Helpers for ColorOS 16 Adaptive Interceptor layout
fun isHomepageUrl(url: String?, settings: BrowserSettings? = null): Boolean {
    if (url == null) return true
    val trimmed = url.trim().lowercase()
    if (settings != null && (trimmed == settings.homeUrl.trim().lowercase() || trimmed == settings.homeUrl.trim().lowercase().dropLastWhile { it == '/' })) {
        return true
    }
    return trimmed.isEmpty() ||
           trimmed == "homepage" ||
           trimmed == "about:blank" ||
           trimmed == "https://search.stormx.ninja" ||
           trimmed == "https://search.stormx.ninja/" ||
           trimmed == "http://search.stormx.ninja" ||
           trimmed == "http://search.stormx.ninja/"
}

@Composable
fun glassCardColor(isDark: Boolean) = if (isDark) {
    Color(0xA61E1E23) // rgba(30, 30, 35, 0.65)
} else {
    Color(0xA6FFFFFF) // rgba(255, 255, 255, 0.65)
}

@Composable
fun glassBorderColor(isDark: Boolean) = if (isDark) {
    Color(0x1AFFFFFF) // rgba(255, 255, 255, 0.1)
} else {
    Color(0x80FFFFFF) // rgba(255, 255, 255, 0.5)
}

fun Modifier.colorOSGradientBackground(isDark: Boolean, alpha: Float = 1.0f): Modifier = this.drawBehind {
    val size = this.size
    // 1. Base Gradient
    val baseBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(Color(0xFF060913), Color(0xFF111424)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(Color(0xFFFDFBFB), Color(0xFFEBEDEE)),
            start = androidx.compose.ui.geometry.Offset(0f, 0f),
            end = androidx.compose.ui.geometry.Offset(size.width, size.height)
        )
    }
    drawRect(brush = baseBrush, alpha = alpha)

    // 2. Top-Right Orb
    val topRightColor = if (isDark) Color(0xFF2DD4BF).copy(alpha = 0.15f) else Color(0xFFE2D1C3)
    val topRightCenter = androidx.compose.ui.geometry.Offset(size.width, 0f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(topRightColor, Color.Transparent),
            center = topRightCenter,
            radius = size.width * 0.85f
        ),
        center = topRightCenter,
        radius = size.width * 0.85f,
        alpha = alpha
    )

    // 3. Bottom-Left Orb
    val bottomLeftColor = if (isDark) Color(0xFF8B5CF6).copy(alpha = 0.15f) else Color(0xFFD4E4F9)
    val bottomLeftCenter = androidx.compose.ui.geometry.Offset(0f, size.height)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(bottomLeftColor, Color.Transparent),
            center = bottomLeftCenter,
            radius = size.width * 0.85f
        ),
        center = bottomLeftCenter,
        radius = size.width * 0.85f,
        alpha = alpha
    )
    
    // 4. Center-Right Orb (ColorOS 16 specific vibe)
    if (isDark) {
        val centerRightColor = Color(0xFF3B82F6).copy(alpha = 0.12f)
        val centerRightOffset = androidx.compose.ui.geometry.Offset(size.width, size.height * 0.5f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(centerRightColor, Color.Transparent),
                center = centerRightOffset,
                radius = size.width * 0.7f
            ),
            center = centerRightOffset,
            radius = size.width * 0.7f,
            alpha = alpha
        )
    }
}

@Composable
fun SpeedDialUI(
    viewModel: BrowserViewModel,
    fontFamily: FontFamily,
    isDark: Boolean,
    onNavigate: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentSettings by viewModel.settings.collectAsState()
    val activeSearchEngine = currentSettings.searchEngine
    
    var searchQuery by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        
        // Logo Container with Search Engine Website theme flavor
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Public,
                contentDescription = "Browser Logo",
                tint = Color(0xFFFF1B2D),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Storm Browser",
                fontFamily = fontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp,
                color = if (isDark) Color.White else Color(0xFF1C1C1E)
            )
        }

        // Subtitle highlighting active secure engine
        Text(
            text = trans("Security-enhanced search powered by $activeSearchEngine"),
            fontFamily = fontFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF6E6E73),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Central Elegant Search Engine Bar on the Homepage (Direct Answer to Request)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 8.dp, end = 8.dp)
                .height(56.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(if (isDark) Color(0x33FFFFFF) else Color(0x0F000000))
                .border(
                    width = 1.dp,
                    color = if (isDark) Color(0x1AFFFFFF) else Color(0x1A000000),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search active engine",
                tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF6E6E73),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = trans("Search with $activeSearchEngine or enter address"),
                        fontFamily = fontFamily,
                        fontSize = 14.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.4f) else Color(0xFF6E6E73)
                    )
                }
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("homepage_search_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = if (isDark) Color.White else Color(0xFF1C1C1E),
                        fontFamily = fontFamily,
                        fontSize = 14.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (searchQuery.isNotBlank()) {
                                onNavigate(searchQuery)
                            }
                        }
                    )
                )
            }
            if (searchQuery.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear search",
                    tint = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF6E6E73),
                    modifier = Modifier
                        .size(18.dp)
                        .clickable { searchQuery = "" }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "Execute Search",
                    tint = Color(0xFFFF1B2D),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onNavigate(searchQuery) }
                )
            }
        }
    }
}

data class DialItemData(
    val label: String,
    val url: String,
    val color: Color,
    val icon: ImageVector
)

@Composable
fun FloatingIOSDownloadHUD(
    downloadsList: List<DownloadItem>,
    recentlyCompletedDownload: DownloadItem?,
    showCompletedBubble: Boolean,
    onDismissCompleted: () -> Unit,
    showDownloadsPage: () -> Unit,
    viewModel: BrowserViewModel,
    activeFont: FontFamily,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeDownload = remember(downloadsList) {
        downloadsList.find { it.status == "DOWNLOADING" || it.status == "PENDING" }
    }

    val isVisible = activeDownload != null || showCompletedBubble

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(dampingRatio = 0.75f, stiffness = 300f)
        ) + fadeIn(),
        exit = slideOutVertically(
            targetOffsetY = { -it / 2 },
            animationSpec = spring(stiffness = 200f)
        ) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 20.dp)
    ) {
        val displayItem = activeDownload ?: recentlyCompletedDownload
        if (displayItem != null) {
            val isFinished = displayItem.status == "COMPLETED"
            
            val percent = if (displayItem.totalBytes > 0) {
                (displayItem.downloadedBytes * 100 / displayItem.totalBytes).toInt()
            } else {
                0
            }

            val speed = viewModel.downloadSpeeds[displayItem.id] ?: "Active"
            
            Surface(
                color = if (isDark) Color(0xEC1C1C1E) else Color(0xECFFFFFF),
                shape = RoundedCornerShape(26.dp),
                tonalElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        if (isFinished) {
                            openDownloadedFile(context, displayItem.filePath, displayItem.mimeType)
                            onDismissCompleted()
                        } else {
                            showDownloadsPage()
                        }
                    }
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(26.dp)
                    )
                    .shadow(16.dp, RoundedCornerShape(26.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                color = if (isFinished) Color(0x1F34C759) else Color(0x1F0066FF),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFinished) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completed",
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize().padding(3.dp)) {
                                val strokeWidth = 2.5.dp.toPx()
                                drawArc(
                                    color = if (isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = strokeWidth,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                                drawArc(
                                    color = Color(0xFF0066FF),
                                    startAngle = -90f,
                                    sweepAngle = (percent.toFloat() / 100f) * 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = strokeWidth,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDownward,
                                contentDescription = "Downloading",
                                tint = Color(0xFF0066FF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isFinished) "Download Complete" else "Downloading File",
                                fontFamily = activeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isDark) Color.White else Color(0xFF1C1C1E)
                            )
                            if (!isFinished) {
                                Text(
                                    text = "$percent%",
                                    fontFamily = activeFont,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF0066FF)
                                )
                            }
                        }
                        
                        Text(
                            text = displayItem.fileName,
                            fontFamily = activeFont,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.7f)
                        )

                        if (!isFinished) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val animatedPercent by animateFloatAsState(
                                targetValue = percent.toFloat() / 100f,
                                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                label = "HUDProgressBar"
                            )
                            LinearProgressIndicator(
                                progress = { animatedPercent },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(1.5.dp)),
                                color = Color(0xFF0066FF),
                                trackColor = if (isDark) Color(0xFF2C2C2F) else Color(0xFFE5E5EA)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val eta = viewModel.downloadEtas[displayItem.id]
                            val infoText = if (eta != null) "Speed: $speed • $eta • ${formatBytes(displayItem.downloadedBytes)} of ${formatBytes(displayItem.totalBytes)}" else "Speed: $speed • ${formatBytes(displayItem.downloadedBytes)} of ${formatBytes(displayItem.totalBytes)}"
                            Text(
                                text = infoText,
                                fontFamily = activeFont,
                                fontSize = 10.sp,
                                color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF6E6E73)
                            )
                        } else {
                            Text(
                                text = "Tap to open details",
                                fontFamily = activeFont,
                                fontSize = 10.sp,
                                color = Color(0xFF34C759)
                            )
                        }
                    }

                    if (isFinished) {
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = { 
                                openDownloadedFile(context, displayItem.filePath, displayItem.mimeType)
                                onDismissCompleted()
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = Color(0xFF34C759)
                            )
                        ) {
                            Text(
                                text = "OPEN",
                                fontFamily = activeFont,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = onDismissCompleted) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close Bubble",
                                tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF6E6E73),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// iOS-Style Dynamic Notification Core HUD
// ==========================================
@Composable
fun IosNotificationHUD(
    notifications: List<BrowserViewModel.IosNotification>,
    onDismiss: (String) -> Unit,
    activeFont: FontFamily,
    isDark: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.92f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            notifications.forEach { notif ->
                key(notif.id) {
                    IosNotificationItem(
                        notification = notif,
                        onDismiss = { onDismiss(notif.id) },
                        activeFont = activeFont,
                        isDark = isDark
                    )
                }
            }
        }
    }
}

@Composable
fun IosNotificationItem(
    notification: BrowserViewModel.IosNotification,
    onDismiss: () -> Unit,
    activeFont: FontFamily,
    isDark: Boolean
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        val backgroundColor = if (isDark) {
            Color(0xEB1C1C1E) // Premium dark translucent glass
        } else {
            Color(0xF2F2F2F7) // Pure light glass
        }
        
        val borderColor = if (isDark) {
            Color(0x3DFFFFFF)
        } else {
            Color(0x1F000000)
        }

        val cardShadow = if (isDark) 6.dp else 12.dp

        Surface(
            color = backgroundColor,
            shape = RoundedCornerShape(26.dp), // Premium iOS rounded capsule style
            tonalElevation = cardShadow,
            shadowElevation = cardShadow,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(26.dp))
                .clickable { onDismiss() }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Determine icon & color based on type
                val iconInfo = when (notification.type) {
                    "DOWNLOAD_START" -> Pair(Icons.Default.Download, Color(0xFF007AFF)) // Blue
                    "DOWNLOAD_COMPLETED" -> Pair(Icons.Default.CheckCircle, Color(0xFF34C759)) // Green
                    "DOWNLOAD_FAILED" -> Pair(Icons.Default.Warning, Color(0xFFFF3B30)) // Red
                    "WEBSITE_ALLOWED" -> Pair(Icons.Default.Notifications, Color(0xFF5856D6)) // Indigo
                    "WEBSITE_BLOCKED" -> Pair(Icons.Default.Notifications, Color(0xFF8E8E93)) // Grey
                    else -> Pair(Icons.Default.Info, Color(0xFF007AFF))
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(iconInfo.second.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconInfo.first,
                        contentDescription = "Notification Icon",
                        tint = iconInfo.second,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = notification.title,
                        fontFamily = activeFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isDark) Color.White else Color(0xFF1C1C1E)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = notification.message,
                        fontFamily = activeFont,
                        fontSize = 12.sp,
                        color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        isVisible = false
                        onDismiss()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Notification",
                        tint = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.4f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FallbackBrowserSimulator(
    viewModel: BrowserViewModel,
    activeTab: TabState?,
    isDark: Boolean,
    fontFamily: FontFamily,
    onNavigate: (String) -> Unit
) {
    val scrollState = rememberScrollState()
    val url = activeTab?.url ?: ""
    val isSearch = url.contains("q=")
    val query = if (isSearch) {
        val qIndex = url.indexOf("q=") + 2
        var rawQ = url.substring(qIndex)
        val ampIndex = rawQ.indexOf("&")
        if (ampIndex != -1) {
            rawQ = rawQ.substring(0, ampIndex)
        }
        android.net.Uri.decode(rawQ)
    } else {
        ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) Color(0xFF0F0F10) else Color(0xFFF9FAFA))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Elegant badge explaining the Simulated Active status
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    androidx.compose.material3.Text(
                        text = trans("Simulator Active • WebView engine is uninstalled on this device host"),
                        fontFamily = fontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (isSearch && query.isNotEmpty()) {
                    androidx.compose.material3.Text(
                        text = trans("Search results for") + " \"$query\"",
                        fontFamily = fontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    val mockResults = remember(query) {
                        listOf(
                            Pair("Wikipedia: $query", "https://en.wikipedia.org/wiki/${android.net.Uri.encode(query)}"),
                            Pair("Official website for $query", "https://www.google.com/search?q=${android.net.Uri.encode(query)}+official+site"),
                            Pair("Latest developments on $query (News)", "https://news.google.com/search?q=${android.net.Uri.encode(query)}"),
                            Pair("GitHub repositories matching $query", "https://github.com/search?q=${android.net.Uri.encode(query)}")
                        )
                    }

                    mockResults.forEach { result ->
                        ElevatedCard(
                            onClick = { onNavigate(result.second) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = if (isDark) Color(0xFF1E1E20) else Color.White
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = "Search icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    androidx.compose.material3.Text(
                                        text = result.first,
                                        fontFamily = fontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp,
                                        color = if (isDark) Color.White else Color.Black
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                androidx.compose.material3.Text(
                                    text = result.second,
                                    fontFamily = fontFamily,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    val domainName = viewModel.getDomainFromUrl(url)
                    val siteName = if (domainName.isNotEmpty()) {
                        domainName.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    } else {
                        "Simulated Page"
                    }

                    // Simulated Page Header Banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                )
                            )
                            .padding(20.dp),
                        contentAlignment = Alignment.BottomStart
                    ) {
                        Column {
                            androidx.compose.material3.Text(
                                text = siteName,
                                fontFamily = fontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            androidx.compose.material3.Text(
                                text = url,
                                fontFamily = fontFamily,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = if (isDark) Color(0xFF1E1E20) else Color.White
                        )
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = "Web",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                androidx.compose.material3.Text(
                                    text = trans("Reader Mode Active"),
                                    fontFamily = fontFamily,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color.Black
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            androidx.compose.material3.Text(
                                text = trans("This high-fidelity article reader provides an elegant layout optimized for") + " $siteName.",
                                fontFamily = fontFamily,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.85f)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            androidx.compose.material3.Text(
                                text = trans("Since this host container does not have an active Android WebView system package, our custom Simulator intercepted the request securely to allow browsing, bookmarking, and local research without crashing."),
                                fontFamily = fontFamily,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.7f)
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Button(
                                onClick = { onNavigate("https://search.stormx.ninja/") },
                                modifier = Modifier.align(Alignment.End),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Text("Go Home")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColorOSSearchSuggestionsOverlay(
    query: String,
    isDark: Boolean,
    fontFamily: FontFamily,
    onSuggestionClick: (String) -> Unit
) {
    if (query.isEmpty()) return

    val suggestions = remember(query) {
        listOf(
            query,
            "$query app",
            "$query download",
            "$query login",
            "about $query"
        )
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = glassCardColor(isDark)
        ),
        elevation = CardDefaults.elevatedCardElevation(
            defaultElevation = 8.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            suggestions.forEach { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.5f),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = suggestion,
                        fontFamily = fontFamily,
                        fontSize = 16.sp,
                        color = if (isDark) Color.White else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun ColumnScope.SearchEngineSubPage(
    viewModel: BrowserViewModel,
    settings: BrowserSettings,
    activeFont: FontFamily,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var customSearchUrl by remember { mutableStateOf(if (settings.searchEngine.startsWith("http")) settings.searchEngine else "") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Search Engine",
                fontFamily = activeFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = if (isDark) Color.White else Color(0xFF1C1C1E)
            )
        }

        Surface(
            color = glassCardColor(isDark),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val engines = listOf(
                    "Search Browser" to "search.stormx.ninja",
                    "Google" to "Google",
                    "Bing" to "Bing",
                    "Yahoo" to "Yahoo",
                    "DuckDuckGo" to "DuckDuckGo",
                    "Baidu" to "Baidu"
                )

                engines.forEachIndexed { index, (name, value) ->
                    val isSelected = settings.searchEngine == value
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateSearchEngine(value, context) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = name,
                            fontFamily = activeFont,
                            fontSize = 16.sp,
                            color = if (isDark) Color.White else Color(0xFF1C1C1E),
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = Color(0xFF0066FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (index < engines.size - 1) {
                        HorizontalDivider(color = glassBorderColor(isDark), thickness = 0.5.dp)
                    }
                }
            }
        }

        Text(
            text = "CUSTOM SEARCH ENGINE",
            fontFamily = activeFont,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = (if (isDark) Color.White else Color(0xFF1C1C1E)).copy(alpha = 0.5f),
            modifier = Modifier.padding(start = 16.dp, bottom = 0.dp)
        )

        Surface(
            color = glassCardColor(isDark),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = customSearchUrl,
                    onValueChange = { customSearchUrl = it },
                    placeholder = { Text("https://my-search.com/search?q=") },
                    textStyle = LocalTextStyle.current.copy(fontFamily = activeFont),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { if (customSearchUrl.isNotBlank()) viewModel.updateSearchEngine(customSearchUrl, context) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Set Custom Search Engine", fontFamily = activeFont)
                }
            }
        }
    }
}

@Composable
fun ColumnScope.LanguagesSubPage(
    viewModel: BrowserViewModel,
    settings: BrowserSettings,
    activeFont: FontFamily,
    isDark: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    
    val allLanguages = listOf(
        "English", "简体中文", "Español", "Deutsch", "Français", "Italiano", "日本語", "한국어", "Русский", "Português", "Hindi", "Arabic",
        "Türkçe", "Nederlands", "Polski", "ภาษาไทย", "Bahasa Indonesia", "Tiếng Việt", "Svenska", "Ελληνικά", "Dansk", "Suomi", "Norsk",
        "Čeština", "Magyar", "Română", "Українська", "עברית", "Bahasa Melayu", "Filipino", "Slovenčina", "Български", "Hrvatski", "Srpski"
    )
    val displayLanguages = if (searchQuery.isBlank()) allLanguages else allLanguages.filter { it.contains(searchQuery, ignoreCase = true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = if (isDark) Color.White else Color(0xFF1C1C1E),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Languages",
                fontFamily = activeFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                color = if (isDark) Color.White else Color(0xFF1C1C1E)
            )
        }

        // Search Bar
        Surface(
            color = glassCardColor(isDark),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f))
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    textStyle = LocalTextStyle.current.copy(
                        color = if (isDark) Color.White else Color.Black,
                        fontFamily = activeFont
                    ),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Search languages...", color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f))
                        }
                        innerTextField()
                    }
                )
            }
        }

        Surface(
            color = glassCardColor(isDark),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .border(1.dp, glassBorderColor(isDark), RoundedCornerShape(20.dp))
        ) {
            androidx.compose.foundation.lazy.LazyColumn(modifier = Modifier.padding(16.dp)) {
                items(displayLanguages.size) { index ->
                    val lang = displayLanguages[index]
                    val mappedLang = if (lang == "English") "English (US)" else lang
                    val isSelected = settings.language == mappedLang
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.updateLanguage(mappedLang, context as? android.app.Activity) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mappedLang,
                            fontFamily = activeFont,
                            fontSize = 16.sp,
                            color = if (isDark) Color.White else Color(0xFF1C1C1E),
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = Color(0xFF0066FF),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (index < displayLanguages.size - 1) {
                        HorizontalDivider(color = glassBorderColor(isDark), thickness = 0.5.dp)
                    }
                }
            }
        }
    }
}