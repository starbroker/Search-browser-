@file:Suppress("DEPRECATION")
package com.example.ui

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.MainThread
import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.URL

data class TabState(
    val id: Int,
    val url: String,
    val title: String,
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val faviconUrl: String? = null
)

data class WebsitePermission(
    val domain: String,
    val notificationsAllowed: Boolean? = null,
    val locationAllowed: Boolean? = null,
    val cameraAllowed: Boolean? = null,
    val microphoneAllowed: Boolean? = null
)

class BrowserViewModel(
    application: Application,
    private val repository: BrowserRepository
) : AndroidViewModel(application) {

    // Support state for WebView availability
    private val _isWebViewSupported = MutableStateFlow<Boolean?>(null)
    val isWebViewSupported: StateFlow<Boolean?> = _isWebViewSupported.asStateFlow()

    private val prefs = getApplication<Application>().getSharedPreferences("browser_prefs", Context.MODE_PRIVATE)

    // Trigger state to forcefully redraw WebViews if the render engine crashes
    private val _webViewUpdateTrigger = MutableStateFlow(0)
    val webViewUpdateTrigger: StateFlow<Int> = _webViewUpdateTrigger.asStateFlow()

    private val _forceSimulatedMode = MutableStateFlow<Boolean>(false)
    val forceSimulatedMode: StateFlow<Boolean> = _forceSimulatedMode.asStateFlow()

    private fun isEmulator(): Boolean {
        return true
        val finger = android.os.Build.FINGERPRINT.lowercase()
        val model = android.os.Build.MODEL.lowercase()
        val brand = android.os.Build.BRAND.lowercase()
        val device = android.os.Build.DEVICE.lowercase()
        val product = android.os.Build.PRODUCT.lowercase()
        val hardware = android.os.Build.HARDWARE.lowercase()
        val board = android.os.Build.BOARD.lowercase()
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        
        return finger.startsWith("generic") ||
                finger.startsWith("unknown") ||
                finger.contains("test-keys") ||
                model.contains("google_sdk") ||
                model.contains("emulator") ||
                model.contains("android sdk") ||
                model.contains("virtual") ||
                model.contains("gphone") ||
                model.contains("sdk") ||
                brand.startsWith("generic") ||
                brand.startsWith("unknown") ||
                device.startsWith("generic") ||
                device.contains("vsoc") ||
                device.contains("emulator") ||
                device.contains("vbox") ||
                device.contains("cutf") ||
                device.contains("cuttlefish") ||
                product.contains("google_sdk") ||
                product.contains("sdk_gphone") ||
                product.contains("redroid") ||
                product.contains("emulator") ||
                product.contains("virtual") ||
                product.contains("aosp") ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu") ||
                hardware.contains("vbox") ||
                hardware.contains("cutf") ||
                hardware.contains("cuttlefish") ||
                hardware.contains("noflinger") ||
                hardware.contains("virtio") ||
                hardware.contains("pc") ||
                board.contains("vbox") ||
                board.contains("goldfish") ||
                board.contains("ranchu") ||
                manufacturer.contains("genymotion") ||
                manufacturer.contains("google") && (model.startsWith("sdk") || model.contains("gphone"))
    }

    fun setForceSimulatedMode(enabled: Boolean) {
        _forceSimulatedMode.value = enabled
        prefs.edit().putBoolean("force_simulated_mode", enabled).apply()
        if (enabled) {
            _isWebViewSupported.value = false
        } else {
            // Assume supported until attempt in Compose
            _isWebViewSupported.value = true
        }
    }

    // Active Tab ID
    private val _activeTabId = MutableStateFlow<Int>(-1)
    val activeTabId: StateFlow<Int> = _activeTabId.asStateFlow()

    // Tab configurations
    private val _tabs = MutableStateFlow<List<TabState>>(emptyList())
    val tabs: StateFlow<List<TabState>> = _tabs.asStateFlow()

    // Manage actual WebView objects to keep their running state in memory
    private val webViewMap = mutableStateMapOf<Int, WebView>()

    // Global Statistics / UI Toggles
    private val _currentUrlInput = MutableStateFlow("")
    val currentUrlInput: StateFlow<String> = _currentUrlInput.asStateFlow()

    private val _blockedAdsSession = MutableStateFlow(0)
    val blockedAdsSession: StateFlow<Int> = _blockedAdsSession.asStateFlow()

    private val _blockedTrackersSession = MutableStateFlow(0)
    val blockedTrackersSession: StateFlow<Int> = _blockedTrackersSession.asStateFlow()

    // Thread-safe in-memory counters to batch disk updates
    private val pendingAdsCount = java.util.concurrent.atomic.AtomicInteger(0)
    private val pendingTrackersCount = java.util.concurrent.atomic.AtomicInteger(0)

    // Sheets / Dialogs Visibility UI state
    val showTabsOverview = MutableStateFlow(false)
    val showSettings = MutableStateFlow(false)
    val showBookmarks = MutableStateFlow(false)
    val showHistory = MutableStateFlow(false)
    val showDownloads = MutableStateFlow(false)
    val showShieldPanel = MutableStateFlow(false)
    val showMenuDrawer = MutableStateFlow(false)

    val tabPreviews = androidx.compose.runtime.mutableStateMapOf<Int, android.graphics.Bitmap>()

    fun openTabsOverview() {
        val tabId = _activeTabId.value
        val webView = webViewMap[tabId]
        if (webView != null) {
            try {
                val width = webView.width
                val height = webView.height
                if (width > 0 && height > 0) {
                    val ratio = 0.25f
                    val scaledWidth = (width * ratio).toInt()
                    val scaledHeight = (height * ratio).toInt()
                    val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
                    val canvas = android.graphics.Canvas(bitmap)
                    webView.draw(canvas)
                    val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true)
                    tabPreviews[tabId] = scaled
                    if (scaled != bitmap) {
                        bitmap.recycle()
                    }
                }
            } catch(e: Exception) {}
        }
        showTabsOverview.value = true
    }

    // Redirect proposal data
    data class AppRedirectProposal(
        val url: String,
        val appName: String,
        val tabId: Int
    )
    val appRedirectProposal = MutableStateFlow<AppRedirectProposal?>(null)

    private val _showOnboarding = MutableStateFlow(false)
    val showOnboarding: StateFlow<Boolean> = _showOnboarding

    fun finishOnboarding() {
        _showOnboarding.value = false
        prefs.edit().putBoolean("has_seen_onboarding", true).apply()
    }

    init {
        // Pre-create cache directories to prevent Chromium logcat noise regarding missing opendir
        try {
            val appCtx = getApplication<Application>()
            val baseCache = java.io.File(appCtx.cacheDir, "WebView/Default/HTTP Cache/Code Cache")
            java.io.File(baseCache, "js").mkdirs()
            java.io.File(baseCache, "wasm").mkdirs()
        } catch (e: Exception) {}
    }

    data class ImageDownloadProposal(val url: String)
    val imageDownloadProposal = MutableStateFlow<ImageDownloadProposal?>(null)
    
    data class PermissionProposal(
        val domain: String,
        val request: android.webkit.PermissionRequest? = null,
        val resourcesNeeded: List<String> = emptyList(),
        val geoCallback: android.webkit.GeolocationPermissions.Callback? = null,
        val geoOrigin: String? = null
    )
    val permissionRequestProposal = MutableStateFlow<PermissionProposal?>(null)
    val allowedInBrowserUrls = mutableSetOf<String>()

    // Live download speeds and times track
    val downloadSpeeds = mutableStateMapOf<Int, String>()
    val downloadEtas = mutableStateMapOf<Int, String>()

    // iOS-style notifications state
    data class IosNotification(
        val id: String,
        val title: String,
        val message: String,
        val type: String, // "DOWNLOAD_START", "DOWNLOAD_COMPLETED", "DOWNLOAD_FAILED", "WEBSITE_ALLOWED", "WEBSITE_BLOCKED"
        val subtext: String? = null
    )
    private val _iosNotifications = MutableStateFlow<List<IosNotification>>(emptyList())
    val iosNotifications: StateFlow<List<IosNotification>> = _iosNotifications.asStateFlow()

    fun showIosNotification(title: String, message: String, type: String, subtext: String? = null) {
        val id = System.currentTimeMillis().toString() + "_" + (1..1000).random()
        val notif = IosNotification(id, title, message, type, subtext)
        _iosNotifications.update { it + notif }
        // Auto-dismiss after 4.5 seconds
        viewModelScope.launch {
            kotlinx.coroutines.delay(4500)
            dismissIosNotification(id)
        }
    }

    fun dismissIosNotification(id: String) {
        _iosNotifications.update { list -> list.filter { it.id != id } }
    }

    // Flow integration for Bookmarks, History, Downloads, and Settings
    val bookmarks = repository.bookmarksFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history = repository.historyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val downloads = repository.downloadsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = repository.settingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowserSettings())
    val websitePermissions = repository.sitePermissionsFlow.map { list ->
        list.groupBy { it.origin }.map { (origin, perms) ->
            WebsitePermission(
                domain = origin,
                notificationsAllowed = perms.find { it.permissionType == "notifications" }?.isGranted,
                locationAllowed = perms.find { it.permissionType == "geolocation" }?.isGranted,
                cameraAllowed = perms.find { it.permissionType == "camera" }?.isGranted,
                microphoneAllowed = perms.find { it.permissionType == "microphone" }?.isGranted
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun saveSitePermissionProxy(domain: String, type: String, allowed: Boolean) {
        viewModelScope.launch {
            val existing = repository.getSitePermission(domain, type)
            if (existing != null) {
                repository.updateSitePermission(existing.copy(isGranted = allowed, timestamp = System.currentTimeMillis()))
            } else {
                repository.saveSitePermission(com.example.data.SitePermission(origin = domain, permissionType = type, isGranted = allowed))
            }
        }
    }

    fun toggleWebsiteNotification(domain: String, allowed: Boolean) {
        saveSitePermissionProxy(domain, "notifications", allowed)
        showIosNotification(
            title = if (allowed) "Notification Allowed" else "Notification Restricted",
            message = if (allowed) "Allowing notifications on $domain" else "Restricting notifications on $domain",
            type = if (allowed) "WEBSITE_ALLOWED" else "WEBSITE_BLOCKED",
            subtext = domain
        )
    }

    fun toggleWebsiteLocation(domain: String, allowed: Boolean) {
        saveSitePermissionProxy(domain, "geolocation", allowed)
        showIosNotification(
            title = if (allowed) "Location Access Allowed" else "Location Access Restricted",
            message = if (allowed) "Allowing location access on $domain" else "Restricting location access on $domain",
            type = if (allowed) "WEBSITE_ALLOWED" else "WEBSITE_BLOCKED",
            subtext = domain
        )
    }

    fun toggleWebsiteCamera(domain: String, allowed: Boolean) {
        saveSitePermissionProxy(domain, "camera", allowed)
        showIosNotification(
            title = if (allowed) "Camera Access Allowed" else "Camera Access Restricted",
            message = if (allowed) "Allowing camera access on $domain" else "Restricting camera access on $domain",
            type = if (allowed) "WEBSITE_ALLOWED" else "WEBSITE_BLOCKED",
            subtext = domain
        )
    }

    fun toggleWebsiteMicrophone(domain: String, allowed: Boolean) {
        saveSitePermissionProxy(domain, "microphone", allowed)
        showIosNotification(
            title = if (allowed) "Microphone Access Allowed" else "Microphone Access Restricted",
            message = if (allowed) "Allowing microphone access on $domain" else "Restricting microphone access on $domain",
            type = if (allowed) "WEBSITE_ALLOWED" else "WEBSITE_BLOCKED",
            subtext = domain
        )
    }

    fun removeWebsitePermission(domain: String) {
        viewModelScope.launch {
            repository.removeSitePermissions(domain)
            showIosNotification(
                title = "Website Permission Revoked",
                message = "Removed all permissions for $domain",
                type = "WEBSITE_BLOCKED",
                subtext = domain
            )
        }
    }

    fun clearAllWebsitePermissions() {
        viewModelScope.launch {
            repository.clearAllSitePermissions()
            showIosNotification(
                title = "Permissions Cleared",
                message = "All stored website permissions have been cleared",
                type = "WEBSITE_BLOCKED"
            )
        }
    }

    init {
        // Onboarding Check Logic - Detect Fresh Install vs Update
        val isFirstLaunch = prefs.getBoolean("is_first_launch_v2", true)
        val hasSeen = prefs.getBoolean("has_seen_onboarding", false)
        
        if (isFirstLaunch) {
            prefs.edit().putBoolean("is_first_launch_v2", false).apply()
            
            // Check PackageInfo to see if this is an update or fresh install
            var isUpdate = false
            try {
                val context = getApplication<Application>()
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                isUpdate = packageInfo.firstInstallTime != packageInfo.lastUpdateTime
            } catch (e: Exception) {
                isUpdate = false
            }
            
            if (!isUpdate && !hasSeen) {
                // Fresh Install -> Show Onboarding
                _showOnboarding.value = true
            } else {
                // Update -> Don't show Onboarding
                prefs.edit().putBoolean("has_seen_onboarding", true).apply()
            }
        } else {
            if (!hasSeen) {
                _showOnboarding.value = true
            }
        }

        val startSimulated = prefs.getBoolean("force_simulated_mode", false)
        _forceSimulatedMode.value = startSimulated

        if (startSimulated) {
            _isWebViewSupported.value = false
        } else {
            // Assume supported until attempt in Compose
            _isWebViewSupported.value = true
        }

        // Start periodic database sync for blocked ads and trackers to prevent SQLite deadlock & main-thread freezes
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                kotlinx.coroutines.delay(3000)
                val ads = pendingAdsCount.getAndSet(0)
                val trackers = pendingTrackersCount.getAndSet(0)
                if (ads > 0 || trackers > 0) {
                    try {
                        val current = repository.getSettings()
                        repository.saveSettings(
                            current.copy(
                                totalAdsBlocked = current.totalAdsBlocked + ads,
                                totalTrackersBlocked = current.totalTrackersBlocked + trackers
                            )
                        )
                    } catch (e: Exception) {
                        // Restore pending count to attempt sync later if DB is locked
                        pendingAdsCount.addAndGet(ads)
                        pendingTrackersCount.addAndGet(trackers)
                    }
                }
            }
        }

        // Load initial state
        viewModelScope.launch {
            val dbTabs = repository.getAllTabs()
            val initialSettings = repository.getSettings()
            
            if (dbTabs.isEmpty()) {
                // Create custom homepage tab to start
                addNewTab(initialSettings.homeUrl)
            } else {
                _tabs.value = dbTabs.map { tab ->
                    val finalUrl = if (tab.url == "homepage") "https://search.stormx.ninja/" else tab.url
                    TabState(id = tab.id, url = finalUrl, title = tab.title)
                }
                _activeTabId.value = dbTabs.first().id
                val firstUrl = dbTabs.first().url
                _currentUrlInput.value = if (firstUrl == "homepage") "https://search.stormx.ninja/" else firstUrl
            }
        }
    }

    // Tab Management
    fun addNewTab(url: String = "https://search.stormx.ninja/") {
        viewModelScope.launch {
            val title = "New Tab"
            val id = repository.addTab(url, title)
            val newTab = TabState(id = id, url = url, title = title)
            _tabs.value = _tabs.value + newTab
            _activeTabId.value = id
            _currentUrlInput.value = url
            showTabsOverview.value = false
        }
    }

    fun selectTab(tabId: Int) {
        _activeTabId.value = tabId
        val tab = _tabs.value.find { it.id == tabId }
        _currentUrlInput.value = tab?.url ?: ""
        showTabsOverview.value = false
    }

    fun removeTab(tabId: Int) {
        viewModelScope.launch {
            val currentList = _tabs.value
            val tabToRemove = currentList.find { it.id == tabId }
            if (tabToRemove != null) {
                repository.deleteTab(BrowserTab(id = tabToRemove.id, url = tabToRemove.url, title = tabToRemove.title))
                _tabs.value = currentList.filter { it.id != tabId }
                
                // Remove WebView cleanly. Let Compose detach it from the view tree first,
                // then destroy it to avoid "Channel is unrecoverably broken" crashes.
                val viewToDestroy = webViewMap.remove(tabId)
                if (viewToDestroy != null) {
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        try {
                            viewToDestroy.loadUrl("about:blank")
                            (viewToDestroy.parent as? android.view.ViewGroup)?.removeView(viewToDestroy)
                            viewToDestroy.destroy()
                        } catch (e: Throwable) {}
                    }, 1000) // Increase delay to let Compose unmount and ensure input channel detaches
                }

                if (_activeTabId.value == tabId) {
                    val remaining = _tabs.value
                    if (remaining.isNotEmpty()) {
                        val first = remaining.first()
                        _activeTabId.value = first.id
                        _currentUrlInput.value = first.url
                    } else {
                        // Create a blank slate if all tabs are closed
                        addNewTab()
                    }
                }
            }
        }
    }

    fun markWebViewUnsupported() {
        _isWebViewSupported.value = false
        showIosNotification(
            title = "Aquamorphic Engine Simulator",
            message = "Fell back to simulated mode due to native graphics exception.",
            type = "DOWNLOAD_FAILED"
        )
    }

    // Get or Create dynamic WebView for stable multi-tab navigation
    fun applyThemeToWebViews(isDark: Boolean) {
        try {
            if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.ALGORITHMIC_DARKENING)) {
                for (webView in webViewMap.values) {
                    androidx.webkit.WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, isDark)
                }
            } else if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.FORCE_DARK)) {
                for (webView in webViewMap.values) {
                    androidx.webkit.WebSettingsCompat.setForceDark(
                        webView.settings,
                        if (isDark) androidx.webkit.WebSettingsCompat.FORCE_DARK_ON else androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF
                    )
                }
            }
        } catch (e: Exception) {}
    }

    fun getOrCreateWebView(tabId: Int, context: Context): WebView {
        return webViewMap[tabId] ?: createWebViewInstance(tabId, context).also {
            webViewMap[tabId] = it
            val isSystemDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            val isDark = when(settings.value.themeMode) {
                "DARK" -> true
                "LIGHT" -> false
                else -> isSystemDark
            }
            try {
                if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.ALGORITHMIC_DARKENING)) {
                    androidx.webkit.WebSettingsCompat.setAlgorithmicDarkeningAllowed(it.settings, isDark)
                } else if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.FORCE_DARK)) {
                    androidx.webkit.WebSettingsCompat.setForceDark(
                        it.settings,
                        if (isDark) androidx.webkit.WebSettingsCompat.FORCE_DARK_ON else androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF
                    )
                }
            } catch (e: Exception) {}
        }
    }

    private fun applyWebSettings(webView: WebView) {
        val prefHelper = com.example.util.PreferenceHelper
        webView.settings.apply {
            textZoom = prefHelper.textSize.toInt()
        }
        val zoomScale = prefHelper.pageZoom
        webView.setInitialScale(zoomScale)
    }

    private fun createWebViewInstance(tabId: Int, context: Context): WebView {
        
        try {
            val wasmDir = java.io.File(context.cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
            val jsDir = java.io.File(context.cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
            if (!wasmDir.exists()) wasmDir.mkdirs()
            if (!jsDir.exists()) jsDir.mkdirs()
        } catch (e: Exception) {}
        
        val webView = WebView(context).apply {
            if (isEmulator()) {
                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            }
            layoutParams = android.view.ViewGroup.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                mediaPlaybackRequiresUserGesture = false
            }
            applyWebSettings(this)
            
            // Allow cookies
            val cookieManager = CookieManager.getInstance()
            cookieManager.setAcceptCookie(true)
            cookieManager.setAcceptThirdPartyCookies(this, true)

            // Dynamic ad and tracker interceptor client
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val url = request?.url?.toString() ?: return false
                    
                    if (allowedInBrowserUrls.contains(url)) {
                        return false
                    }

                    val appName = getSocialAppName(url)
                    if (appName != null) {
                        val currentProposal = appRedirectProposal.value
                        if (currentProposal?.url != url) {
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                appRedirectProposal.value = AppRedirectProposal(url, appName, tabId)
                            }
                            return true
                        }
                    }

                    // Handle standard protocols
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        return false
                    }
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                        context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    return true
                }

                override fun shouldInterceptRequest(
                    view: WebView?,
                    request: WebResourceRequest?
                ): WebResourceResponse? {
                    val urlStr = request?.url?.toString() ?: return null
                    val isAdBlockOn = this@BrowserViewModel.settings.value.adBlockEnabled
                    val isTrackerBlockOn = this@BrowserViewModel.settings.value.trackerBlockEnabled

                    if (isAdBlockOn || isTrackerBlockOn) {
                        val host = try {
                            URL(urlStr).host.lowercase()
                        } catch (e: Exception) {
                            ""
                        }

                        val isTracker = isTrackerHost(host)
                        val isAd = isAdHost(host, urlStr)

                        if ((isTracker && isTrackerBlockOn) || (isAd && isAdBlockOn)) {
                            if (isTracker) {
                                _blockedTrackersSession.update { it + 1 }
                                pendingTrackersCount.incrementAndGet()
                            } else {
                                _blockedAdsSession.update { it + 1 }
                                pendingAdsCount.incrementAndGet()
                            }
                            // Intercept and return empty asset
                            return WebResourceResponse(
                                "text/plain",
                                "UTF-8",
                                ByteArrayInputStream("".toByteArray())
                            )
                        }
                    }
                    return super.shouldInterceptRequest(view, request)
                }

                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    url?.let {
                        updateTabProperties(tabId, url = it, isLoading = true)
                        if (tabId == _activeTabId.value) {
                            _currentUrlInput.value = it
                        }
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    url?.let {
                        val title = view?.title ?: "StormX Search"
                        updateTabProperties(
                            tabId, 
                            url = it, 
                            title = title, 
                            isLoading = false,
                            canGoBack = view?.canGoBack() ?: false,
                            canGoForward = view?.canGoForward() ?: false
                        )
                        viewModelScope.launch {
                            repository.addHistory(it, title)
                            repository.updateTab(BrowserTab(id = tabId, url = it, title = title))
                        }
                    }
                }

                override fun onRenderProcessGone(
                    view: WebView?,
                    detail: android.webkit.RenderProcessGoneDetail?
                ): Boolean {
                    // Recover from out of memory or webview engine crashes
                    if (view != null) {
                        (view.parent as? android.view.ViewGroup)?.removeView(view)
                        view.destroy()
                    }
                    webViewMap.remove(tabId)
                    _webViewUpdateTrigger.value += 1
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        val currentUrl = _tabs.value.find { it.id == tabId }?.url ?: "https://search.stormx.ninja"
                        val newView = getOrCreateWebView(tabId, context)
                        newView.loadUrl(currentUrl)
                        _webViewUpdateTrigger.value += 1
                    }, 500)
                    return true
                }
            }

            // WebChromeClient to track load progress
            webChromeClient = object : android.webkit.WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    updateTabProperties(tabId, progress = newProgress)
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    title?.let {
                        updateTabProperties(tabId, title = it)
                    }
                }

                override fun onReceivedIcon(view: WebView?, icon: android.graphics.Bitmap?) {
                    // Custom favicon fetch from URL or load directly
                }

                override fun onPermissionRequest(request: android.webkit.PermissionRequest?) {
                    if (request == null) return
                    val origin = request.origin?.toString() ?: ""
                    val domain = getDomainFromUrl(origin)
                    val perm = websitePermissions.value.find { it.domain == domain }
                    
                    val cameraAllowed = perm?.cameraAllowed
                    val micAllowed = perm?.microphoneAllowed
                    
                    val autoGrantedResources = mutableListOf<String>()
                    val resourcesNeeded = mutableListOf<String>()
                    for (res in request.resources) {
                        if (res == android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                            when (cameraAllowed) {
                                true -> autoGrantedResources.add(res)
                                null -> resourcesNeeded.add(res)
                                false -> { /* Denied, do not add */ }
                            }
                        } else if (res == android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                            when (micAllowed) {
                                true -> autoGrantedResources.add(res)
                                null -> resourcesNeeded.add(res)
                                false -> { /* Denied, do not add */ }
                            }
                        } else {
                            autoGrantedResources.add(res)
                        }
                    }
                    if (resourcesNeeded.isNotEmpty()) {
                        permissionRequestProposal.value = PermissionProposal(domain, request, resourcesNeeded)
                    } else if (autoGrantedResources.isNotEmpty()) {
                        request.grant(autoGrantedResources.toTypedArray())
                    } else {
                        request.deny()
                    }
                }

                override fun onGeolocationPermissionsShowPrompt(
                    origin: String?,
                    callback: android.webkit.GeolocationPermissions.Callback?
                ) {
                    if (origin == null || callback == null) return
                    val domain = getDomainFromUrl(origin)
                    val allowed = websitePermissions.value.find { it.domain == domain }?.locationAllowed
                    
                    if (allowed == true) {
                        callback.invoke(origin, true, true)
                    } else if (allowed == null) {
                        permissionRequestProposal.value = PermissionProposal(
                            domain = domain,
                            geoCallback = callback,
                            geoOrigin = origin
                        )
                    } else {
                        callback.invoke(origin, false, false)
                    }
                }
            }

            // Browser download listener to support file downloading
            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                triggerDownload(url, userAgent, contentDisposition, mimetype, contentLength, context)
            }
            
            setOnLongClickListener {
                val hitTestResult = this.hitTestResult
                if (hitTestResult.type == android.webkit.WebView.HitTestResult.IMAGE_TYPE || hitTestResult.type == android.webkit.WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                    val imageUrl = hitTestResult.extra
                    if (imageUrl != null) {
                        imageDownloadProposal.value = ImageDownloadProposal(imageUrl)
                        return@setOnLongClickListener true
                    }
                }
                false
            }
        }
        
        // Load initial url
        val currentTab = _tabs.value.find { it.id == tabId }
        webView.loadUrl(currentTab?.url ?: "https://search.stormx.ninja")
        return webView
    }

    fun getDomainFromUrl(url: String?): String {
        if (url.isNullOrEmpty()) return ""
        return try {
            val uri = java.net.URI(url)
            var host = uri.host ?: ""
            if (host.startsWith("www.")) {
                host = host.substring(4)
            }
            host.ifEmpty { "" }
        } catch (e: Exception) {
            ""
        }
    }
    
    fun handlePermissionProposal(grant: Boolean, remember: Boolean) {
        val proposal = permissionRequestProposal.value ?: return
        if (grant) {
            try {
                proposal.request?.grant(proposal.resourcesNeeded.toTypedArray())
            } catch (e: Exception) {}
            try {
                proposal.geoCallback?.invoke(proposal.geoOrigin, true, remember) // geoCallback takes remember boolean directly
            } catch (e: Exception) {}
            
            if (remember) {
                val needsCamera = proposal.resourcesNeeded.contains(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                val needsMic = proposal.resourcesNeeded.contains(android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                if (needsCamera) toggleWebsiteCamera(proposal.domain, true)
                if (needsMic) toggleWebsiteMicrophone(proposal.domain, true)
                if (proposal.geoCallback != null) toggleWebsiteLocation(proposal.domain, true)
            }
        } else {
            try {
                proposal.request?.deny()
            } catch (e: Exception) {}
            try {
                proposal.geoCallback?.invoke(proposal.geoOrigin, false, remember)
            } catch (e: Exception) {}
            
            if (remember) {
                val needsCamera = proposal.resourcesNeeded.contains(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE)
                val needsMic = proposal.resourcesNeeded.contains(android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE)
                if (needsCamera) toggleWebsiteCamera(proposal.domain, false)
                if (needsMic) toggleWebsiteMicrophone(proposal.domain, false)
                if (proposal.geoCallback != null) toggleWebsiteLocation(proposal.domain, false)
            }
        }
        permissionRequestProposal.value = null
    }

    private fun isTrackerHost(host: String): Boolean {
        val trackers = listOf(
            "google-analytics.com", "analytics.", "quantserve.com", "scorecardresearch.com",
            "statcounter.com", "mixpanel.com", "hotjar.com", "segment.io", "amplitude.com",
            "facebook.net", "fbcdn.net", "tracker", "telemetry", "metrics", "crazyegg.com",
            "userreport.com", "optimizely.com", "yandex.ru/clck", "mc.yandex.ru", "adjust.com",
            "appsflyer.com", "branch.io", "kochava.com"
        )
        return trackers.any { host.contains(it) }
    }

    private fun isAdHost(host: String, url: String): Boolean {
        val adDomains = listOf(
            "doubleclick.net", "googlesyndication.com", "adservice.google.com", 
            "amazon-adsystem.com", "taboola.com", "outbrain.com", "criteo.com", 
            "popads.net", "trafficjunky.com", "pubmatic.com", "adnxs.com", 
            "rubiconproject.com", "openx.net", "casalemedia.com", "adsystem",
            "adserver", "adroll.com", "buysellads.com", "exoclick.com", 
            "popcash.net", "propellerads.com", "adsterra.com", "adform.net",
            "yieldlab.net", "smartadserver.com", "adskeeper", "mgid.com",
            "indexww.com", "revcontent.com", "addthis.com", "outbrain"
        )
        val urlLower = url.lowercase()
        val containsAdPatterns = urlLower.contains("/ads/") || urlLower.contains("/adserver/") || 
                urlLower.contains("?ad_id") || urlLower.contains("&ad_") || urlLower.contains("/banners/") ||
                urlLower.contains("googleads") || urlLower.contains("pagead") || urlLower.contains("analytics") ||
                urlLower.contains("adservice") || urlLower.contains("/ad/")
        
        return adDomains.any { host.contains(it) } || containsAdPatterns
    }

    private fun updateTabProperties(
        tabId: Int,
        url: String? = null,
        title: String? = null,
        progress: Int? = null,
        isLoading: Boolean? = null,
        canGoBack: Boolean? = null,
        canGoForward: Boolean? = null
    ) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(
                    url = url ?: tab.url,
                    title = title ?: tab.title,
                    progress = progress ?: tab.progress,
                    isLoading = isLoading ?: tab.isLoading,
                    canGoBack = canGoBack ?: tab.canGoBack,
                    canGoForward = canGoForward ?: tab.canGoForward
                )
            } else {
                tab
            }
        }
    }

    // Navigation Interactions
    fun navigateActiveTab(url: String, context: Context) {
        var formattedUrl = url.trim()
        if (formattedUrl.isEmpty()) return

        if (!URLUtil.isValidUrl(formattedUrl)) {
            // Search query default to storms search or google
            formattedUrl = if (formattedUrl.contains(".") && !formattedUrl.contains(" ")) {
                "https://$formattedUrl"
            } else {
                val engine = settings.value.searchEngine
                when (engine) {
                    "Google" -> "https://www.google.com/search?q=${Uri.encode(formattedUrl)}"
                    "Bing" -> "https://www.bing.com/search?q=${Uri.encode(formattedUrl)}"
                    "Yahoo" -> "https://search.yahoo.com/search?p=${Uri.encode(formattedUrl)}"
                    "DuckDuckGo" -> "https://duckduckgo.com/?q=${Uri.encode(formattedUrl)}"
                    "Baidu" -> "https://www.baidu.com/s?wd=${Uri.encode(formattedUrl)}"
                    else -> {
                        if (engine.startsWith("http")) {
                            "${engine}${Uri.encode(formattedUrl)}"
                        } else {
                            "https://search.stormx.ninja/search?q=${Uri.encode(formattedUrl)}"
                        }
                    }
                }
            }
        }

        _currentUrlInput.value = formattedUrl
        val activeId = _activeTabId.value
        
        if (activeId == -1) {
            addNewTab(formattedUrl)
            return
        }

        if (_isWebViewSupported.value == false) {
            viewModelScope.launch {
                updateTabProperties(activeId, url = formattedUrl, isLoading = true, progress = 20)
                kotlinx.coroutines.delay(200)
                updateTabProperties(activeId, progress = 65)
                kotlinx.coroutines.delay(300)
                val domain = getDomainFromUrl(formattedUrl)
                val simulatedTitle = if (domain.isNotEmpty()) {
                    domain.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } else {
                    "Simulated Page"
                }
                updateTabProperties(activeId, url = formattedUrl, title = simulatedTitle, isLoading = false, progress = 100)
                repository.addHistory(formattedUrl, simulatedTitle)
                repository.updateTab(BrowserTab(id = activeId, url = formattedUrl, title = simulatedTitle))
            }
            return
        }
        try {
            val webView = webViewMap[activeId] ?: getOrCreateWebView(activeId, context)
            webView.loadUrl(formattedUrl)
        } catch (e: Throwable) {
            markWebViewUnsupported()
        }
    }

    fun activeTabGoBack(context: Context) {
        try {
            val webView = webViewMap[_activeTabId.value]
            if (webView != null && webView.canGoBack()) {
                webView.goBack()
            }
        } catch (e: Throwable) {}
    }
    
    fun activeTabClearHistory() {
        try {
            val webView = webViewMap[_activeTabId.value]
            webView?.clearHistory()
        } catch (e: Throwable) {}
    }

    fun activeTabGoForward(context: Context) {
        try {
            val webView = webViewMap[_activeTabId.value]
            if (webView != null && webView.canGoForward()) {
                webView.goForward()
            }
        } catch (e: Throwable) {}
    }

    fun activeTabRefresh(context: Context) {
        val activeId = _activeTabId.value
        if (_isWebViewSupported.value == false) {
            viewModelScope.launch {
                updateTabProperties(activeId, isLoading = true, progress = 15)
                kotlinx.coroutines.delay(200)
                updateTabProperties(activeId, progress = 70)
                kotlinx.coroutines.delay(200)
                updateTabProperties(activeId, isLoading = false, progress = 100)
            }
            return
        }
        try {
            val webView = webViewMap[activeId]
            webView?.reload()
        } catch (e: Throwable) {}
    }

    fun setUrlInput(input: String) {
        _currentUrlInput.value = input
    }

    // Toggle Bookmarks
    fun toggleBookmark(url: String, title: String) {
        viewModelScope.launch {
            val isCurrentlyBookmarked = bookmarks.value.any { it.url == url }
            if (isCurrentlyBookmarked) {
                repository.removeBookmark(url)
            } else {
                repository.addBookmark(url, title)
            }
        }
    }

    // Settings adjustments
    fun updateThemeMode(mode: String, activity: android.app.Activity? = null) {
        viewModelScope.launch {
            val modeInt = when (mode) {
                "LIGHT" -> 1
                "DARK" -> 2
                else -> 0
            }
            com.example.util.PreferenceHelper.themeMode = modeInt
            val current = repository.getSettings()
            repository.saveSettings(current.copy(themeMode = mode))
            com.example.BrowserApplication.applyThemeMode(modeInt)
            activity?.recreate()
        }
    }

    fun updateThemeColor(colorIndex: Int) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(customThemeColor = colorIndex))
        }
    }

    fun updateFontFamily(font: String) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(fontFamily = font))
        }
    }

    fun updateLayoutDensity(density: String) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(layoutDensity = density))
        }
    }

    fun updateTextSize(size: Float) {
        com.example.util.PreferenceHelper.textSize = size
        webViewMap.values.forEach { applyWebSettings(it) }
    }

    fun updatePageZoom(zoom: Int) {
        com.example.util.PreferenceHelper.pageZoom = zoom
        webViewMap.values.forEach { applyWebSettings(it) }
    }

    fun updateSearchEngine(engine: String, context: android.content.Context) {
        viewModelScope.launch {
            val current = repository.getSettings()
            val newHomeUrl = when (engine) {
                "Google" -> "https://www.google.com/"
                "Bing" -> "https://www.bing.com/"
                "Yahoo" -> "https://www.yahoo.com/"
                "DuckDuckGo" -> "https://duckduckgo.com/"
                "Baidu" -> "https://www.baidu.com/"
                "search.stormx.ninja" -> "https://search.stormx.ninja/"
                else -> {
                    if (engine.startsWith("http")) engine else "https://search.stormx.ninja/"
                }
            }
            // Update the stateflow directly to prevent the race condition
            // where navigateActiveTab uses the OLD searchEngine since repository flow hasn't emitted yet.
            val updatedSettings = current.copy(searchEngine = engine, homeUrl = newHomeUrl)
            com.example.util.PreferenceHelper.searchEngine = engine
            com.example.util.PreferenceHelper.homePage = newHomeUrl
            repository.saveSettings(updatedSettings)
            
            // Give Flow a tiny delay to update Or you can just update the home page
            kotlinx.coroutines.delay(50)
            navigateActiveTab(newHomeUrl, context)
        }
    }

    fun updateLanguage(lang: String, activity: android.app.Activity? = null) {
        viewModelScope.launch {
            com.example.util.PreferenceHelper.language = lang
            val current = repository.getSettings()
            repository.saveSettings(current.copy(language = lang))
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                val locale = when (lang) {
                    "简体中文" -> java.util.Locale.SIMPLIFIED_CHINESE
                    "Español" -> java.util.Locale("es")
                    "Deutsch" -> java.util.Locale("de")
                    "Français" -> java.util.Locale("fr")
                    "Italiano" -> java.util.Locale("it")
                    "日本語" -> java.util.Locale.JAPANESE
                    "한국어" -> java.util.Locale.KOREAN
                    "Русский" -> java.util.Locale("ru")
                    "Português" -> java.util.Locale("pt")
                    "Hindi" -> java.util.Locale("hi")
                    "Arabic" -> java.util.Locale("ar")
                    "Türkçe" -> java.util.Locale("tr")
                    "Nederlands" -> java.util.Locale("nl")
                    "Polski" -> java.util.Locale("pl")
                    "ภาษาไทย" -> java.util.Locale("th")
                    "Bahasa Indonesia" -> java.util.Locale("id")
                    "Tiếng Việt" -> java.util.Locale("vi")
                    "Svenska" -> java.util.Locale("sv")
                    "Ελληνικά" -> java.util.Locale("el")
                    "Dansk" -> java.util.Locale("da")
                    "Suomi" -> java.util.Locale("fi")
                    "Norsk" -> java.util.Locale("no")
                    "Čeština" -> java.util.Locale("cs")
                    "Magyar" -> java.util.Locale("hu")
                    "Română" -> java.util.Locale("ro")
                    "Українська" -> java.util.Locale("uk")
                    "עברית" -> java.util.Locale("iw")
                    "Bahasa Melayu" -> java.util.Locale("ms")
                    "Filipino" -> java.util.Locale("fil")
                    "Slovenčina" -> java.util.Locale("sk")
                    "Български" -> java.util.Locale("bg")
                    "Hrvatski" -> java.util.Locale("hr")
                    "Srpski" -> java.util.Locale("sr")
                    else -> java.util.Locale("en")
                }
                activity?.getSystemService(android.app.LocaleManager::class.java)?.applicationLocales = android.os.LocaleList(locale)
            }
            activity?.recreate()
        }
    }

    fun updateFluidAnimationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(fluidAnimationsEnabled = enabled))
        }
    }

    fun updateSpeedDialLayout(layout: String) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(speedDialLayout = layout))
        }
    }

    fun toggleAdBlock() {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(adBlockEnabled = !current.adBlockEnabled))
        }
    }

    fun toggleTrackerBlock() {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(trackerBlockEnabled = !current.trackerBlockEnabled))
        }
    }

    fun clearBrowsingData() {
        viewModelScope.launch {
            repository.clearHistory()
            val text = BrowserTranslator.translateText("History cleared successfully", settings.value.language)
            Toast.makeText(getApplication(), text, Toast.LENGTH_SHORT).show()
        }
    }

    // Download Handler
    fun triggerDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long,
        context: Context
    ) {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType) ?: "download_file"
        
        viewModelScope.launch(Dispatchers.IO) {
            // Save in Room DB
            val destinationPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/" + fileName
            val downloadId = repository.addDownload(url, fileName, destinationPath, contentLength, mimeType)
            
            if (url.startsWith("data:")) {
                try {
                    val base64Data = url.substringAfter(",")
                    val decodedBytes = android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                    val file = java.io.File(destinationPath)
                    java.io.FileOutputStream(file).use { it.write(decodedBytes) }
                    
                    val initialItem = repository.downloadsFlow.first().find { it.id == downloadId }
                    if (initialItem != null) {
                        repository.updateDownload(initialItem.copy(status = "COMPLETED", downloadedBytes = decodedBytes.size.toLong(), totalBytes = decodedBytes.size.toLong()))
                    }
                    withContext(Dispatchers.Main) {
                        val text = BrowserTranslator.translateText("Download completed: $fileName", settings.value.language)
                        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    val initialItem = repository.downloadsFlow.first().find { it.id == downloadId }
                    if (initialItem != null) {
                        repository.updateDownload(initialItem.copy(status = "FAILED"))
                    }
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                try {
                    val request = DownloadManager.Request(Uri.parse(url)).apply {
                        setMimeType(mimeType)
                        addRequestHeader("User-Agent", userAgent)
                        setDescription("Downloading from StormX Browser")
                        setTitle(fileName)
                        setAllowedOverMetered(true)
                        setAllowedOverRoaming(true)
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    }
    
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val dmId = downloadManager.enqueue(request)
    
                    // Polling tracking job in IO coroutine to monitor raw bytes and compute speed in real-time
                    viewModelScope.launch(Dispatchers.IO) {
                        var isRunning = true
                        var previousBytes = 0L
                        var lastTime = System.currentTimeMillis()
                        
                        // Set status to DOWNLOADING in RoomDB
                        val initialItem = repository.downloadsFlow.first().find { it.id == downloadId }
                        if (initialItem != null) {
                            repository.updateDownload(initialItem.copy(status = "DOWNLOADING"))
                        }
    
                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            val channel = android.app.NotificationChannel("stormx_downloads", "StormX Downloads", android.app.NotificationManager.IMPORTANCE_LOW)
                            notificationManager.createNotificationChannel(channel)
                        }

                        while (isRunning) {
                            val query = DownloadManager.Query().setFilterById(dmId)
                            val cursor = downloadManager.query(query)
                            if (cursor != null && cursor.moveToFirst()) {
                                val bytesDownloadedColumn = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                val totalBytesColumn = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                val statusColumn = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
    
                                val bytesDownloaded = if (bytesDownloadedColumn != -1) cursor.getLong(bytesDownloadedColumn) else 0L
                                val totalBytes = if (totalBytesColumn != -1) cursor.getLong(totalBytesColumn) else contentLength
                                val status = if (statusColumn != -1) cursor.getInt(statusColumn) else DownloadManager.STATUS_RUNNING
    
                                val currentTime = System.currentTimeMillis()
                                val timeDelta = (currentTime - lastTime) / 1000f
                                
                                val speed = if (timeDelta >= 0.5f) {
                                    val calc = ((bytesDownloaded - previousBytes) / timeDelta).toLong()
                                    lastTime = currentTime
                                    previousBytes = bytesDownloaded
                                    if (calc < 0) 0L else calc
                                } else {
                                    null
                                }
    
                                if (speed != null) {
                                    val speedText = formatSpeed(speed)
                                    val etaText = if (speed > 0 && totalBytes > 0 && totalBytes > bytesDownloaded) formatEta((totalBytes - bytesDownloaded) / speed) else ""
                                    withContext(Dispatchers.Main) {
                                        downloadSpeeds[downloadId] = speedText
                                        if (etaText.isNotEmpty()) {
                                            downloadEtas[downloadId] = etaText
                                        } else {
                                            downloadEtas.remove(downloadId)
                                        }
                                    }
                                }

                                // Update Android notification dynamically
                                // Channel created outside the loop
                                val progressPercent = if (totalBytes > 0) (bytesDownloaded * 100 / totalBytes).toInt() else 0
                                val notifText = if (speed != null) "Downloading... ${formatSpeed(speed)}" else "Downloading..."
                                
                                val builder = androidx.core.app.NotificationCompat.Builder(context, "stormx_downloads")
                                    .setContentTitle(fileName)
                                    .setContentText(notifText)
                                    .setSmallIcon(android.R.drawable.stat_sys_download)
                                    .setOngoing(true)
                                    .setProgress(100, progressPercent, totalBytes <= 0)
                                    .setOnlyAlertOnce(true)
                                
                                notificationManager.notify(downloadId, builder.build())

                                val mappedStatus = when (status) {
                                    DownloadManager.STATUS_RUNNING -> "DOWNLOADING"
                                    DownloadManager.STATUS_SUCCESSFUL -> {
                                        isRunning = false
                                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                        val builder = androidx.core.app.NotificationCompat.Builder(context, "stormx_downloads")
                                            .setContentTitle(fileName)
                                            .setContentText("Download Complete")
                                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                                            .setAutoCancel(true)
                                        notificationManager.notify(downloadId, builder.build())
                                        "COMPLETED"
                                    }
                                    DownloadManager.STATUS_FAILED -> {
                                        isRunning = false
                                        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                                        val builder = androidx.core.app.NotificationCompat.Builder(context, "stormx_downloads")
                                            .setContentTitle(fileName)
                                            .setContentText("Download Failed")
                                            .setSmallIcon(android.R.drawable.stat_notify_error)
                                            .setAutoCancel(true)
                                        notificationManager.notify(downloadId, builder.build())
                                        "FAILED"
                                    }
                                    DownloadManager.STATUS_PAUSED -> "PAUSED"
                                    else -> "DOWNLOADING"
                                }
    
                                val dbItem = repository.downloadsFlow.first().find { it.id == downloadId }
                                if (dbItem != null) {
                                    repository.updateDownload(dbItem.copy(
                                        status = mappedStatus,
                                        downloadedBytes = bytesDownloaded,
                                        totalBytes = if (totalBytes > 0) totalBytes else dbItem.totalBytes
                                    ))
                                }
                            } else {
                                isRunning = false
                            }
                            cursor?.close()
                            if (isRunning) {
                                kotlinx.coroutines.delay(1000)
                            }
                        }
                        
                        // Cleanup speed display on termination
                        withContext(Dispatchers.Main) {
                            downloadSpeeds.remove(downloadId)
                            downloadEtas.remove(downloadId)
                        }
                    }
    
                    withContext(Dispatchers.Main) {
                        val startText = BrowserTranslator.translateText("Download started: $fileName", settings.value.language)
                        Toast.makeText(context, startText, Toast.LENGTH_LONG).show()
                    }
    
                } catch (e: Exception) {
                    viewModelScope.launch {
                        val downloadsList = repository.downloadsFlow.first()
                        val currentDownload = downloadsList.find { it.id == downloadId }
                        if (currentDownload != null) {
                            repository.updateDownload(currentDownload.copy(status = "FAILED"))
                        }
                    }
                    withContext(Dispatchers.Main) {
                        val failText = BrowserTranslator.translateText("Download failed: ${e.localizedMessage}", settings.value.language)
                        Toast.makeText(context, failText, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun formatSpeed(bytesPerSecond: Long): String {
        if (bytesPerSecond <= 0) return "0 B/s"
        val kb = bytesPerSecond / 1024f
        if (kb < 1024) return String.format(java.util.Locale.US, "%.1f KB/s", kb)
        val mb = kb / 1024f
        return String.format(java.util.Locale.US, "%.1f MB/s", mb)
    }

    private fun formatEta(seconds: Long): String {
        if (seconds <= 0) return ""
        if (seconds < 60) return "${seconds}s left"
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        if (minutes < 60) return "${minutes}m ${remainingSeconds}s left"
        val hours = minutes / 60
        val remainingMinutes = minutes % 60
        return "${hours}h ${remainingMinutes}m left"
    }

    fun deleteHistory(id: Int) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    fun deleteDownload(id: Int) {
        viewModelScope.launch {
            repository.deleteDownload(id)
        }
    }

    fun pauseDownload(id: Int, context: Context) {
        viewModelScope.launch {
            val dbItem = repository.downloadsFlow.first().find { it.id == id } ?: return@launch
            try {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val cursor = dm.query(DownloadManager.Query())
                var dmId: Long = -1
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                        if (titleIndex != -1 && cursor.getString(titleIndex) == dbItem.fileName) {
                            val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                            if (idIndex != -1) {
                                dmId = cursor.getLong(idIndex)
                                break
                            }
                        }
                    }
                    cursor.close()
                }
                if (dmId != -1L) {
                    val values = android.content.ContentValues()
                    values.put("control", 1) // 1 for pause
                    context.contentResolver.update(android.net.Uri.parse("content://downloads/my_downloads/$dmId"), values, null, null)
                    repository.updateDownload(dbItem.copy(status = "PAUSED"))
                }
            } catch (e: Exception) {}
        }
    }

    fun resumeDownload(id: Int, context: Context) {
        viewModelScope.launch {
            val dbItem = repository.downloadsFlow.first().find { it.id == id } ?: return@launch
            try {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                val cursor = dm.query(DownloadManager.Query())
                var dmId: Long = -1
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                        if (titleIndex != -1 && cursor.getString(titleIndex) == dbItem.fileName) {
                            val idIndex = cursor.getColumnIndex(DownloadManager.COLUMN_ID)
                            if (idIndex != -1) {
                                dmId = cursor.getLong(idIndex)
                                break
                            }
                        }
                    }
                    cursor.close()
                }
                if (dmId != -1L) {
                    val values = android.content.ContentValues()
                    values.put("control", 0) // 0 for resume
                    context.contentResolver.update(android.net.Uri.parse("content://downloads/my_downloads/$dmId"), values, null, null)
                    repository.updateDownload(dbItem.copy(status = "DOWNLOADING"))
                }
            } catch (e: Exception) {}
        }
    }

    fun getSocialAppName(url: String): String? {
        val host = try {
            URL(url).host.lowercase()
        } catch (e: Exception) {
            ""
        }
        return when {
            host.contains("instagram.com") || host.contains("instagr.am") -> "Instagram"
            host.contains("facebook.com") || host.contains("fb.com") -> "Facebook"
            host.contains("twitter.com") || host.contains("x.com") || host.contains("t.co") -> "Twitter / X"
            host.contains("youtube.com") || host.contains("youtu.be") -> "YouTube"
            host.contains("tiktok.com") -> "TikTok"
            host.contains("reddit.com") -> "Reddit"
            host.contains("linkedin.com") -> "LinkedIn"
            host.contains("spotify.com") -> "Spotify"
            host.contains("snapchat.com") -> "Snapchat"
            host.contains("pinterest.com") -> "Pinterest"
            host.contains("whatsapp.com") || host.contains("wa.me") -> "WhatsApp"
            host.contains("telegram.org") || host.contains("t.me") -> "Telegram"
            else -> null
        }
    }

    fun proceedWithBrowser(url: String, tabId: Int) {
        allowedInBrowserUrls.add(url)
        appRedirectProposal.value = null
        val webView = webViewMap[tabId]
        webView?.loadUrl(url)
    }

    override fun onCleared() {
        super.onCleared()
        // Safely destroy WebViews on activity clear to prevent memory leaks and parent view state crashes
        webViewMap.forEach { (_, webView) ->
            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
            webView.destroy()
        }
        webViewMap.clear()
    }
}
