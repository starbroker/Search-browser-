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
import java.io.ByteArrayInputStream
import java.net.URL
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext




data class TabState(
    val id: Int,
    val url: String,
    val title: String,
    val progress: Int = 0,
    val isLoading: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val faviconUrl: String? = null,
    val isReadingMode: Boolean = false
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

    val showAIVoicePill = MutableStateFlow(false)
    val showAIChatHistory = MutableStateFlow(false)

    fun openAIChatHistory() {
        showAIChatHistory.value = true
        showMenuDrawer.value = false
    }
    fun closeAIChatHistory() {
        showAIChatHistory.value = false
    }

    fun clearAIChatHistory() {
        val prefs = getApplication<android.app.Application>().getSharedPreferences("ai_chat_history", android.content.Context.MODE_PRIVATE)
        prefs.edit().remove("history").apply()
    }

    fun addAIChatMessage(role: String, content: String) {
        val prefs = getApplication<android.app.Application>().getSharedPreferences("ai_chat_history", android.content.Context.MODE_PRIVATE)
        val historyString = prefs.getString("history", "[]") ?: "[]"
        val historyArray = org.json.JSONArray(historyString)
        val newObj = org.json.JSONObject()
        newObj.put("role", role)
        newObj.put("content", content)
        historyArray.put(newObj)
        prefs.edit().putString("history", historyArray.toString()).apply()
    }

    fun getAIChatHistory(): List<Map<String, String>> {
        val prefs = getApplication<android.app.Application>().getSharedPreferences("ai_chat_history", android.content.Context.MODE_PRIVATE)
        val historyString = prefs.getString("history", "[]") ?: "[]"
        val historyArray = org.json.JSONArray(historyString)
        val list = mutableListOf<Map<String, String>>()
        for (i in 0 until historyArray.length()) {
            val obj = historyArray.getJSONObject(i)
            list.add(mapOf("role" to obj.optString("role"), "content" to obj.optString("content")))
        }
        return list
    }

    lateinit var aiAssistantManager: AIAssistantManager

    fun toggleAIVoicePill() {
        showAIVoicePill.value = !showAIVoicePill.value
        if (!showAIVoicePill.value) {
            aiAssistantManager.stopListeningAndProcess()
            aiAssistantManager.stopSpeaking()
        }
    }

    fun performSearchFromAI(query: String) {
        // we can navigate to search url
        navigateActiveTabFromAI("https://duckduckgo.com/?q=$query")
    }

    fun navigateActiveTabFromAI(url: String) {
        webViewMap[_activeTabId.value]?.loadUrl(url)
    }

    suspend fun getActiveTabContent(): String = kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        val wv = webViewMap[_activeTabId.value]
        if (wv == null) {
            cont.resume("")
            return@suspendCancellableCoroutine
        }
        val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
        mainHandler.post {
            wv.evaluateJavascript("(function() { return document.body ? document.body.innerText.substring(0, 5000) : \"\"; })();") { result ->
                var text = result ?: ""
                if (text.startsWith("\"") && text.endsWith("\"")) {
                    text = text.substring(1, text.length - 1)
                }
                text = text.replace("\\n", "\n").replace("\\t", "\t")
                cont.resume(text)
            }
        }
    }

    fun injectJSInActiveTab(js: String) {
        val wv = webViewMap[_activeTabId.value] ?: return
        wv.evaluateJavascript(js, null)
    }

    fun scrollActiveTabFromAI(direction: String) {
        val wv = webViewMap[_activeTabId.value] ?: return
        if (direction.contains("down", ignoreCase = true)) {
            wv.scrollBy(0, 1000)
        } else {
            wv.scrollBy(0, -1000)
        }
    }

    private val _forceSimulatedMode = MutableStateFlow<Boolean>(false)
    val forceSimulatedMode: StateFlow<Boolean> = _forceSimulatedMode.asStateFlow()

    private fun isEmulator(): Boolean {
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

    private fun closeAllPages() {
        showSettings.value = false
        showBookmarks.value = false
        showHistory.value = false
        showDownloads.value = false
        showShieldPanel.value = false
    }

    fun openBookmarks() {
        closeAllPages()
        showBookmarks.value = true
    }

    fun openHistory() {
        closeAllPages()
        showHistory.value = true
    }

    fun openDownloads() {
        closeAllPages()
        showDownloads.value = true
    }

    fun updateSettings(newSettings: BrowserSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
        }
    }

    fun openSettings() {
        closeAllPages()
        showSettings.value = true
    }

    fun openShield() {
        closeAllPages()
        showShieldPanel.value = true
    }

    fun openMenuDrawer() {
        showMenuDrawer.value = true
    }

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

    data class ImageDownloadProposal(val url: String)
    val imageDownloadProposal = MutableStateFlow<ImageDownloadProposal?>(null)
    
    data class PermissionProposal(
        val domain: String,
        val request: android.webkit.PermissionRequest? = null,
        val resourcesNeeded: List<String> = emptyList(),
        val allResourcesToGrant: List<String> = emptyList(),
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
    val websitePermissions = repository.websitePermissionsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun toggleWebsiteNotification(domain: String, allowed: Boolean) {
        viewModelScope.launch {
            val existing = websitePermissions.value.find { it.domain == domain } ?: WebsitePermission(domain = domain)
            val updated = existing.copy(notifications = if (allowed) PermissionState.ALLOW else PermissionState.BLOCK)
            repository.saveWebsitePermission(updated)
            showIosNotification(
                title = if (allowed) "Notification Allowed" else "Notification Restricted",
                message = if (allowed) "Allowing notifications on $domain" else "Restricting notifications on $domain",
                type = if (allowed) "WEBSITE_ALLOWED" else "WEBSITE_BLOCKED",
                subtext = domain
            )
        }
    }

    fun toggleWebsiteLocation(domain: String, allowed: Boolean) {
        viewModelScope.launch {
            val existing = websitePermissions.value.find { it.domain == domain } ?: WebsitePermission(domain = domain)
            val updated = existing.copy(location = if (allowed) PermissionState.ALLOW else PermissionState.BLOCK)
            repository.saveWebsitePermission(updated)
            showIosNotification(
                title = if (allowed) "Location Access Allowed" else "Location Access Restricted",
                message = if (allowed) "Allowing location access on $domain" else "Restricting location access on $domain",
                type = if (allowed) "WEBSITE_ALLOWED" else "WEBSITE_BLOCKED",
                subtext = domain
            )
        }
    }

    fun toggleWebsiteCamera(domain: String, allowed: Boolean) {
        viewModelScope.launch {
            val existing = websitePermissions.value.find { it.domain == domain } ?: WebsitePermission(domain = domain)
            val updated = existing.copy(camera = if (allowed) PermissionState.ALLOW else PermissionState.BLOCK)
            repository.saveWebsitePermission(updated)
            showIosNotification(
                title = if (allowed) "Camera Access Allowed" else "Camera Access Restricted",
                message = if (allowed) "Allowing camera access on $domain" else "Restricting camera access on $domain",
                type = if (allowed) "WEBSITE_ALLOWED" else "WEBSITE_BLOCKED",
                subtext = domain
            )
        }
    }

    fun toggleWebsiteMicrophone(domain: String, allowed: Boolean) {
        viewModelScope.launch {
            val existing = websitePermissions.value.find { it.domain == domain } ?: WebsitePermission(domain = domain)
            val updated = existing.copy(microphone = if (allowed) PermissionState.ALLOW else PermissionState.BLOCK)
            repository.saveWebsitePermission(updated)
            showIosNotification(
                title = if (allowed) "Microphone Access Allowed" else "Microphone Access Restricted",
                message = if (allowed) "Allowing microphone access on $domain" else "Restricting microphone access on $domain",
                type = if (allowed) "WEBSITE_ALLOWED" else "WEBSITE_BLOCKED",
                subtext = domain
            )
        }
    }

    fun toggleWebsiteFiles(domain: String, allowed: Boolean) {
        viewModelScope.launch {
            val existing = websitePermissions.value.find { it.domain == domain } ?: WebsitePermission(domain = domain)
            val updated = existing.copy(files = if (allowed) PermissionState.ALLOW else PermissionState.BLOCK)
            repository.saveWebsitePermission(updated)
            showIosNotification(
                title = if (allowed) "Files Access Allowed" else "Files Access Restricted",
                message = if (allowed) "Allowing files access on $domain" else "Restricting files access on $domain",
                type = if (allowed) "WEBSITE_ALLOWED" else "WEBSITE_BLOCKED",
                subtext = domain
            )
        }
    }

    fun removeWebsitePermission(domain: String) {
        viewModelScope.launch {
            repository.removeWebsitePermission(domain)
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
            repository.clearAllWebsitePermissions()
            showIosNotification(
                title = "Permissions Cleared",
                message = "All stored website permissions have been cleared",
                type = "WEBSITE_BLOCKED"
            )
        }
    }

    init {
        aiAssistantManager = AIAssistantManager(application, this)
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

            // Resume downloading tracked items
            repository.downloadsFlow.first().forEach { dbItem ->
                if ((dbItem.status == "DOWNLOADING" || dbItem.status == "PAUSED") && dbItem.dmId != -1L) {
                    trackDownload(dbItem.id, dbItem.dmId, application, dbItem.fileName, dbItem.totalBytes)
                }
            }
            
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
                
                // Remove WebView safely by detaching from parent view first
                webViewMap.remove(tabId)?.let { webView ->
                    (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                    webView.destroy()
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
        val color = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        try {
            for (webView in webViewMap.values) {
                webView.setBackgroundColor(color)
            }
            if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.ALGORITHMIC_DARKENING)) {
                for (webView in webViewMap.values) {
                    androidx.webkit.WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.settings, isDark)
                }
            }
            if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.FORCE_DARK)) {
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
            val color = if (isDark) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            it.setBackgroundColor(color)
            try {
                if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.ALGORITHMIC_DARKENING)) {
                    androidx.webkit.WebSettingsCompat.setAlgorithmicDarkeningAllowed(it.settings, isDark)
                }
                if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.FORCE_DARK)) {
                    androidx.webkit.WebSettingsCompat.setForceDark(
                        it.settings,
                        if (isDark) androidx.webkit.WebSettingsCompat.FORCE_DARK_ON else androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF
                    )
                }
            } catch (e: Exception) {}
        }
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
                mediaPlaybackRequiresUserGesture = false
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                setGeolocationEnabled(true)
                allowFileAccess = false
                allowContentAccess = false
                cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                
                // Speed optimizations
                offscreenPreRaster = true

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    safeBrowsingEnabled = true
                }
            }
            
            if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.ALGORITHMIC_DARKENING)) {
                androidx.webkit.WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
            } else if (androidx.webkit.WebViewFeature.isFeatureSupported(androidx.webkit.WebViewFeature.FORCE_DARK)) {
                val isDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
                androidx.webkit.WebSettingsCompat.setForceDark(settings, if (isDark) androidx.webkit.WebSettingsCompat.FORCE_DARK_ON else androidx.webkit.WebSettingsCompat.FORCE_DARK_OFF)
            }
            
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
                    if (appName != null && isAppInstalled(context, appName)) {
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
                    try {
                        val wasmDir = java.io.File(context.cacheDir, "WebView/Default/HTTP Cache/Code Cache/wasm")
                        val jsDir = java.io.File(context.cacheDir, "WebView/Default/HTTP Cache/Code Cache/js")
                        if (!wasmDir.exists()) wasmDir.mkdirs()
                        if (!jsDir.exists()) jsDir.mkdirs()
                    } catch (e: Exception) {}
                    
                    url?.let {
                        updateTabProperties(tabId, url = it, isLoading = true, isReadingMode = false)
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
                    
                    val cameraAllowed = perm?.camera == com.example.data.PermissionState.ALLOW
                    val cameraBlocked = perm?.camera == com.example.data.PermissionState.BLOCK
                    val micAllowed = perm?.microphone == com.example.data.PermissionState.ALLOW
                    val micBlocked = perm?.microphone == com.example.data.PermissionState.BLOCK
                    
                    val context = getApplication<Application>()
                    val hasCameraOsPerm = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val hasMicOsPerm = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED

                    val autoGrantedResources = mutableListOf<String>()
                    val resourcesNeeded = mutableListOf<String>()
                    for (res in request.resources) {
                        if (res == android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE) {
                            if (cameraAllowed && hasCameraOsPerm) {
                                autoGrantedResources.add(res)
                            } else if (!cameraBlocked) {
                                resourcesNeeded.add(res)
                            }
                        } else if (res == android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE) {
                            if (micAllowed && hasMicOsPerm) {
                                autoGrantedResources.add(res)
                            } else if (!micBlocked) {
                                resourcesNeeded.add(res)
                            }
                        } else {
                            autoGrantedResources.add(res)
                        }
                    }
                    if (resourcesNeeded.isNotEmpty()) {
                        val allResourcesToGrant = resourcesNeeded + autoGrantedResources
                        permissionRequestProposal.value = PermissionProposal(
                            domain = domain,
                            request = request,
                            resourcesNeeded = resourcesNeeded,
                            allResourcesToGrant = allResourcesToGrant
                        )
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
                    val permState = websitePermissions.value.find { it.domain == domain }?.location
                    
                    val context = getApplication<Application>()
                    val hasLocationOsPerm = androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    
                    if (permState == com.example.data.PermissionState.ALLOW && hasLocationOsPerm) {
                        callback.invoke(origin, true, true)
                    } else if (permState == com.example.data.PermissionState.BLOCK) {
                        callback.invoke(origin, false, true)
                    } else {
                        permissionRequestProposal.value = PermissionProposal(
                            domain = domain,
                            geoCallback = callback,
                            geoOrigin = origin,
                            resourcesNeeded = listOf("location"),
                            allResourcesToGrant = emptyList()
                        )
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
    
    fun handlePermissionProposal(grant: Boolean) {
        val proposal = permissionRequestProposal.value ?: return
        if (grant) {
            try {
                if (proposal.allResourcesToGrant.isNotEmpty()) {
                    proposal.request?.grant(proposal.allResourcesToGrant.toTypedArray())
                }
            } catch (e: Exception) {}
            try {
                proposal.geoCallback?.invoke(proposal.geoOrigin, true, true)
            } catch (e: Exception) {}
            
            val needsCamera = proposal.resourcesNeeded.contains(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE)
            val needsMic = proposal.resourcesNeeded.contains(android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE)
            if (needsCamera) toggleWebsiteCamera(proposal.domain, true)
            if (needsMic) toggleWebsiteMicrophone(proposal.domain, true)
            if (proposal.geoCallback != null) toggleWebsiteLocation(proposal.domain, true)
        } else {
            try {
                proposal.request?.deny()
            } catch (e: Exception) {}
            try {
                proposal.geoCallback?.invoke(proposal.geoOrigin, false, true)
            } catch (e: Exception) {}
            
            val needsCamera = proposal.resourcesNeeded.contains(android.webkit.PermissionRequest.RESOURCE_VIDEO_CAPTURE)
            val needsMic = proposal.resourcesNeeded.contains(android.webkit.PermissionRequest.RESOURCE_AUDIO_CAPTURE)
            if (needsCamera) toggleWebsiteCamera(proposal.domain, false)
            if (needsMic) toggleWebsiteMicrophone(proposal.domain, false)
            if (proposal.geoCallback != null) toggleWebsiteLocation(proposal.domain, false)
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
        canGoForward: Boolean? = null,
        isReadingMode: Boolean? = null
    ) {
        _tabs.value = _tabs.value.map { tab ->
            if (tab.id == tabId) {
                tab.copy(
                    url = url ?: tab.url,
                    title = title ?: tab.title,
                    progress = progress ?: tab.progress,
                    isLoading = isLoading ?: tab.isLoading,
                    canGoBack = canGoBack ?: tab.canGoBack,
                    canGoForward = canGoForward ?: tab.canGoForward,
                    isReadingMode = isReadingMode ?: tab.isReadingMode
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
                    "Yandex" -> "https://yandex.com/search/?text=${Uri.encode(formattedUrl)}"
                    "Brave" -> "https://search.brave.com/search?q=${Uri.encode(formattedUrl)}"
                    "Ecosia" -> "https://www.ecosia.org/search?q=${Uri.encode(formattedUrl)}"
                    "Qwant" -> "https://www.qwant.com/?q=${Uri.encode(formattedUrl)}"
                    "Startpage" -> "https://www.startpage.com/sp/search?query=${Uri.encode(formattedUrl)}"
                    else -> "https://search.stormx.ninja/results?q=${Uri.encode(formattedUrl)}"
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
    fun updateThemeMode(mode: String) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(themeMode = mode))
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

    fun updateSearchEngine(engine: String, context: android.content.Context) {
        viewModelScope.launch {
            val current = repository.getSettings()
            val newHomeUrl = when (engine) {
                "Google" -> "https://www.google.com/"
                "Bing" -> "https://www.bing.com/"
                "Yahoo" -> "https://www.yahoo.com/"
                "DuckDuckGo" -> "https://duckduckgo.com/"
                "Baidu" -> "https://www.baidu.com/"
                "Yandex" -> "https://yandex.com/"
                "Brave" -> "https://search.brave.com/"
                "Ecosia" -> "https://www.ecosia.org/"
                "Qwant" -> "https://www.qwant.com/"
                "Startpage" -> "https://www.startpage.com/"
                "search.stormx.ninja" -> "https://search.stormx.ninja/"
                else -> "https://search.stormx.ninja/"
            }
            // Update the stateflow directly to prevent the race condition
            // where navigateActiveTab uses the OLD searchEngine since repository flow hasn't emitted yet.
            val updatedSettings = current.copy(searchEngine = engine, homeUrl = newHomeUrl)
            repository.saveSettings(updatedSettings)
            
            // Give Flow a tiny delay to update Or you can just update the home page
            kotlinx.coroutines.delay(50)
            navigateActiveTab(newHomeUrl, context)
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            val current = repository.getSettings()
            repository.saveSettings(current.copy(language = lang))
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
    fun trackDownload(downloadId: Int, dmId: Long, context: Context, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            var isRunning = true
            var previousBytes = -1L
            var lastTime = System.currentTimeMillis()
            
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
                    val totalBytes = if (totalBytesColumn != -1) cursor.getLong(totalBytesColumn) else 0L
                    val status = if (statusColumn != -1) cursor.getInt(statusColumn) else DownloadManager.STATUS_RUNNING

                    val currentTime = System.currentTimeMillis()
                    val timeDelta = (currentTime - lastTime) / 1000f
                    
                    val speed = if (previousBytes == -1L) {
                        previousBytes = bytesDownloaded
                        lastTime = currentTime
                        null
                    } else if (timeDelta >= 0.5f) {
                        val calc = ((bytesDownloaded - previousBytes) / timeDelta).toLong()
                        if (bytesDownloaded > previousBytes) {
                            lastTime = currentTime
                            previousBytes = bytesDownloaded
                            calc
                        } else if (timeDelta > 5.0f && status != DownloadManager.STATUS_PAUSED) {
                            lastTime = currentTime
                            0L
                        } else {
                            null
                        }
                    } else {
                        null
                    }

                    val activeSpeedText = if (speed != null) {
                        val text = formatSpeed(speed)
                        val etaText = if (speed > 0 && totalBytes > 0 && totalBytes > bytesDownloaded) formatEta((totalBytes - bytesDownloaded) / speed) else ""
                        withContext(Dispatchers.Main) {
                            downloadSpeeds[downloadId] = text
                            if (etaText.isNotEmpty()) {
                                downloadEtas[downloadId] = etaText
                            } else {
                                downloadEtas.remove(downloadId)
                            }
                        }
                        text
                    } else {
                        downloadSpeeds[downloadId]
                    }

                    val progressPercent = if (totalBytes > 0) (bytesDownloaded * 100 / totalBytes).toInt() else 0
                    val notifText = if (activeSpeedText != null && status == DownloadManager.STATUS_RUNNING) "Downloading... $activeSpeedText" else if (status == DownloadManager.STATUS_PAUSED) "Paused" else "Downloading..."
                    
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
                            val builderDone = androidx.core.app.NotificationCompat.Builder(context, "stormx_downloads")
                                .setContentTitle(fileName)
                                .setContentText("Download Complete")
                                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                                .setAutoCancel(true)
                            notificationManager.notify(downloadId, builderDone.build())
                            "COMPLETED"
                        }
                        DownloadManager.STATUS_FAILED -> {
                            isRunning = false
                            val builderFail = androidx.core.app.NotificationCompat.Builder(context, "stormx_downloads")
                                .setContentTitle(fileName)
                                .setContentText("Download Failed")
                                .setSmallIcon(android.R.drawable.stat_notify_error)
                                .setAutoCancel(true)
                            notificationManager.notify(downloadId, builderFail.build())
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
                    val refreshRate = if (settings.value.batterySaverModeEnabled) 3000L else 1000L
                    kotlinx.coroutines.delay(refreshRate)
                }
            }
            
            withContext(Dispatchers.Main) {
                downloadSpeeds.remove(downloadId)
                downloadEtas.remove(downloadId)
            }
        }
    }

    fun trackDownload(downloadId: Int, dmId: Long, context: Context, fileName: String, contentLength: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            var isRunning = true
            var previousBytes = -1L
            var lastTime = System.currentTimeMillis()
            
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
                    
                    val speed = if (previousBytes == -1L) {
                        previousBytes = bytesDownloaded
                        lastTime = currentTime
                        null
                    } else if (timeDelta >= 0.5f) {
                        val calc = ((bytesDownloaded - previousBytes) / timeDelta).toLong()
                        if (bytesDownloaded > previousBytes) {
                            lastTime = currentTime
                            previousBytes = bytesDownloaded
                            calc
                        } else if (timeDelta > 5.0f && status != DownloadManager.STATUS_PAUSED) {
                            lastTime = currentTime
                            0L
                        } else {
                            null
                        }
                    } else {
                        null
                    }

                    val activeSpeedText = if (speed != null) {
                        val text = formatSpeed(speed)
                        val etaText = if (speed > 0 && totalBytes > 0 && totalBytes > bytesDownloaded) formatEta((totalBytes - bytesDownloaded) / speed) else ""
                        withContext(Dispatchers.Main) {
                            downloadSpeeds[downloadId] = text
                            if (etaText.isNotEmpty()) {
                                downloadEtas[downloadId] = etaText
                            } else {
                                downloadEtas.remove(downloadId)
                            }
                        }
                        text
                    } else {
                        downloadSpeeds[downloadId]
                    }

                    val progressPercent = if (totalBytes > 0) (bytesDownloaded * 100 / totalBytes).toInt() else 0
                    val notifText = if (activeSpeedText != null && status == DownloadManager.STATUS_RUNNING) "Downloading... $activeSpeedText" else if (status == DownloadManager.STATUS_PAUSED) "Paused" else "Downloading..."
                    
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
                            val builderDone = androidx.core.app.NotificationCompat.Builder(context, "stormx_downloads")
                                .setContentTitle(fileName)
                                .setContentText("Download Complete")
                                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                                .setAutoCancel(true)
                            notificationManager.notify(downloadId, builderDone.build())
                            "COMPLETED"
                        }
                        DownloadManager.STATUS_FAILED -> {
                            isRunning = false
                            val builderFail = androidx.core.app.NotificationCompat.Builder(context, "stormx_downloads")
                                .setContentTitle(fileName)
                                .setContentText("Download Failed")
                                .setSmallIcon(android.R.drawable.stat_notify_error)
                                .setAutoCancel(true)
                            notificationManager.notify(downloadId, builderFail.build())
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
                    val refreshRate = if (settings.value.batterySaverModeEnabled) 3000L else 1000L
                    kotlinx.coroutines.delay(refreshRate)
                }
            }
            
            withContext(Dispatchers.Main) {
                downloadSpeeds.remove(downloadId)
                downloadEtas.remove(downloadId)
            }
        }
    }

    fun triggerDownload(
        url: String,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long,
        context: Context
    ) {
        var fileName = URLUtil.guessFileName(url, contentDisposition, mimeType) ?: "download_file"
        if (fileName.endsWith(".bin") || fileName.endsWith(".htm")) {
            val lastSegment = android.net.Uri.parse(url).lastPathSegment
            if (!lastSegment.isNullOrEmpty() && lastSegment.contains(".")) {
                fileName = lastSegment
            } else if (!contentDisposition.isNullOrEmpty()) {
                val match = Regex("""filename=["']?([^"';]+)["']?""").find(contentDisposition)
                if (match != null) {
                    fileName = match.groupValues[1]
                }
            }
        }
        
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
                        addRequestHeader("Connection", "keep-alive")
                        addRequestHeader("Accept-Encoding", "gzip, deflate, br")
                        setDescription("Downloading from StormX Browser")
                        setTitle(fileName)
                        setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                        setAllowedOverMetered(true)
                        setAllowedOverRoaming(true)
                        setRequiresCharging(false)
                        setRequiresDeviceIdle(false)
                        setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    }
    
                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    val dmId = downloadManager.enqueue(request)
    
                    var initialItem: com.example.data.DownloadItem? = null
                    for (i in 1..20) {
                        initialItem = repository.downloadsFlow.first().find { it.id == downloadId }
                        if (initialItem != null) break
                        kotlinx.coroutines.delay(100)
                    }
                    if (initialItem != null) {
                        repository.updateDownload(initialItem.copy(status = "DOWNLOADING", dmId = dmId))
                    }
    
                    trackDownload(downloadId, dmId, context, fileName, contentLength)
    
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
        if (bytesPerSecond < 0) return "0 B/s"
        val kb = bytesPerSecond / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.2f GB/s", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB/s", mb)
            kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB/s", kb)
            else -> "$bytesPerSecond B/s"
        }
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
                if (dbItem.dmId != -1L) {
                    val values = android.content.ContentValues()
                    values.put("control", 1) // 1 for pause
                    context.contentResolver.update(android.net.Uri.parse("content://downloads/my_downloads/${dbItem.dmId}"), values, null, null)
                    repository.updateDownload(dbItem.copy(status = "PAUSED"))
                }
            } catch (e: Exception) {}
        }
    }

    fun resumeDownload(id: Int, context: Context) {
        viewModelScope.launch {
            val dbItem = repository.downloadsFlow.first().find { it.id == id } ?: return@launch
            try {
                if (dbItem.dmId != -1L) {
                    val values = android.content.ContentValues()
                    values.put("control", 0) // 0 for resume
                    context.contentResolver.update(android.net.Uri.parse("content://downloads/my_downloads/${dbItem.dmId}"), values, null, null)
                    repository.updateDownload(dbItem.copy(status = "DOWNLOADING"))
                }
            } catch (e: Exception) {}
        }
    }

    fun cancelDownload(id: Int, context: Context) {
        viewModelScope.launch {
            val dbItem = repository.downloadsFlow.first().find { it.id == id } ?: return@launch
            try {
                if (dbItem.dmId != -1L) {
                    val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    dm.remove(dbItem.dmId)
                }
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                notificationManager.cancel(id)
                repository.deleteDownload(id)
                downloadSpeeds.remove(id)
                downloadEtas.remove(id)
            } catch (e: Exception) {}
        }
    }

    fun isAppInstalled(context: Context, appName: String): Boolean {
        val packageName = getSocialAppPackage(appName)
        if (packageName.isEmpty()) return false
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getSocialAppPackage(appName: String): String {
        return when (appName) {
            "Instagram" -> "com.instagram.android"
            "Facebook" -> "com.facebook.katana"
            "X" -> "com.twitter.android"
            "YouTube" -> "com.google.android.youtube"
            "TikTok" -> "com.zhiliaoapp.musically"
            "Reddit" -> "com.reddit.frontpage"
            "LinkedIn" -> "com.linkedin.android"
            "Spotify" -> "com.spotify.music"
            "Snapchat" -> "com.snapchat.android"
            "Pinterest" -> "com.pinterest"
            "WhatsApp" -> "com.whatsapp"
            "Telegram" -> "org.telegram.messenger"
            else -> ""
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
            host.contains("twitter.com") || host.contains("x.com") -> "X"
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

    fun toggleReadingMode() {
        val activeId = _activeTabId.value
        val webView = webViewMap[activeId] ?: return
        val tabState = _tabs.value.find { it.id == activeId } ?: return
        
        if (tabState.isReadingMode) {
            webView.reload()
            updateTabProperties(activeId, isReadingMode = false)
            return
        }
        
        val readingModeJS = """
            (function() {
                var contentNode = document.querySelector('article') || document.querySelector('main') || document.body;
                
                var extractedHtml = '';
                var seen = new Set();
                var elements = contentNode.querySelectorAll('h1, h2, h3, h4, p, img, picture, ul, ol, blockquote');
                
                elements.forEach(function(el) {
                    if (seen.has(el)) return;
                    
                    if (el.closest('nav, header, footer, aside, .sidebar, .menu, [class*="nav"], [class*="menu"], [class*="ad-"], [class*="cookie"]')) return;
                    
                    var cleanEl = el.cloneNode(true);
                    
                    var cleanNodeStyles = function(node) {
                        if(node.removeAttribute) {
                            node.removeAttribute('style');
                            node.removeAttribute('class');
                            node.removeAttribute('id');
                        }
                        if(node.childNodes) {
                            node.childNodes.forEach(cleanNodeStyles);
                        }
                    };
                    cleanNodeStyles(cleanEl);
                    
                    el.querySelectorAll('h1, h2, h3, h4, p, img, picture, ul, ol, blockquote').forEach(function(child) {
                        seen.add(child);
                    });
                    
                    extractedHtml += cleanEl.outerHTML + '\n';
                });

                var style = '<style>' +
                    ':root { --bg-color: #F8F9FA; --text-color: #1A1A1A; --card-bg: #FFFFFF; --accent-color: #0A59F7; --text-secondary: #666666; --radius: 32px; }' +
                    '@media (prefers-color-scheme: dark) { :root { --bg-color: #000000; --text-color: #F2F2F2; --card-bg: #121212; --accent-color: #4A8CFF; --text-secondary: #999999; } }' +
                    'body { font-family: -apple-system, BlinkMacSystemFont, "Inter", "Segoe UI", Roboto, Helvetica, Arial, sans-serif; font-size: 19px; line-height: 1.7; max-width: 900px; margin: 0 auto; padding: 16px 12px; background: var(--bg-color); color: var(--text-color); overflow-x: hidden; -webkit-font-smoothing: antialiased; }' +
                    '.coloros-card { background: var(--card-bg); border-radius: var(--radius); padding: 5% 6%; box-shadow: 0 12px 32px rgba(0,0,0,0.04); margin-top: 8px; margin-bottom: 24px; transition: background 0.3s ease; }' +
                    '@media (prefers-color-scheme: dark) { .coloros-card { box-shadow: none; border: 1px solid rgba(255,255,255,0.06); } }' +
                    'img, picture { max-width: 100%; height: auto; display: block; margin: 32px auto; border-radius: 20px; }' +
                    'a { color: var(--accent-color); text-decoration: none; font-weight: 500; }' +
                    'h1 { font-size: 2.3em; line-height: 1.25; margin: 0 0 24px 0; font-weight: 800; letter-spacing: -0.04em; }' +
                    'h2 { font-size: 1.6em; line-height: 1.3; margin: 2em 0 1em 0; font-weight: 700; letter-spacing: -0.03em; }' +
                    'h3 { font-size: 1.3em; margin: 1.5em 0 0.8em 0; font-weight: 700; letter-spacing: -0.02em; }' +
                    'p { margin-bottom: 1.6em; color: var(--text-color); letter-spacing: -0.01em; }' +
                    'blockquote { border-left: 4px solid var(--accent-color); padding-left: 20px; margin-left: 0; font-style: italic; color: var(--text-secondary); border-radius: 0 8px 8px 0; background: var(--bg-color); padding: 16px 20px; }' +
                    'ul, ol { margin-bottom: 1.6em; padding-left: 24px; }' +
                    'li { margin-bottom: 0.6em; }' +
                    '</style>';
                    
                var titleText = document.title;
                var titleHtml = document.querySelector('h1') ? '' : '<h1>' + titleText + '</h1>';
                
                document.head.innerHTML = '<meta name="viewport" content="width=device-width, initial-scale=1">' + style;
                document.body.innerHTML = '<div class="coloros-card">' + titleHtml + extractedHtml + '</div>';
                document.body.dataset.readingMode = 'true';
            })();
        """.trimIndent()
        
        webView.evaluateJavascript(readingModeJS, null)
        updateTabProperties(activeId, isReadingMode = true)
    }

    override fun onCleared() {
        super.onCleared()
        aiAssistantManager.cleanup()
        // Safely destroy WebViews on activity clear to prevent memory leaks and parent view state crashes
        webViewMap.forEach { (_, webView) ->
            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
            webView.destroy()
        }
        webViewMap.clear()
    }
}



object BrowserTranslator {
    val TRANSLATIONS = mapOf(
        "简体中文" to mapOf(
            "Search or URL" to "搜索或输入网址",
            "search" to "搜索",
            "Settings" to "设置",
            "General" to "常规",
            "GENERAL" to "常规",
            "Customization" to "个性化",
            "CUSTOMIZATION" to "个性化",
            "Languages" to "语言",
            "Active" to "当前激活",
            "Active: " to "当前激活: ",
            "Clear Browsing Data" to "清除浏览器数据",
            "History cleared successfully" to "历史记录清除成功",
            "Bookmarks" to "书签",
            "Bookmarked" to "已添加书签",
            "History" to "历史记录",
            "Blocked Trackers" to "已阻止的跟踪器",
            "Block Ads" to "屏蔽广告",
            "Ad Blocker" to "广告拦截",
            "Total Ads Blocked" to "已拦截广告数",
            "Total Trackers Blocked" to "已阻止跟踪器数",
            "New Tab" to "新标签页",
            "Tabs" to "标签页",
            "Cancel" to "取消",
            "Clear" to "清除",
            "Success" to "成功",
            "All stored website permissions have been cleared" to "所有存储的网站权限也已清除",
            "Search Engine" to "搜索引擎",
            "Private & Secure" to "私密与安全",
            "No Bookmarks saved yet." to "尚无保存的书签。",
            "No browsing history catalog logged." to "尚无浏览历史记录。",
            "Privacy" to "隐私防范",
            "PRIVACY" to "隐私防范",
            "Custom Theme Color" to "自定义主题颜色",
            "Theme Mode" to "主题模式",
            "Font Family" to "字体系列",
            "Layout Density" to "布局密度",
            "Fluid Animations" to "流畅动画",
            "Ad & Tracker Blocker" to "广告和跟踪器拦截器",
            "Total Ads & Trackers Blocked" to "已拦截的广告和跟踪器总数",
            "Emerald Green" to "祖母绿",
            "Sky Blue" to "天蓝色",
            "Sunset Orange" to "落日橙",
            "Cyber Lavender" to "网络薰衣草",
            "Obsidian Slate" to "黑曜石石板",
            "Light" to "浅色",
            "Dark" to "深色",
            "System" to "系统",
            "Sans-serif" to "无衬线体",
            "Playfair" to "Playfair 衬线",
            "Monospace" to "等宽字体",
            "Compact" to "紧凑",
            "Moderate" to "适中",
            "Comfortable" to "舒适",
            "Default" to "默认",
            "Done" to "完成",
            "OK" to "确定",
            "Close" to "关闭",
            "Open in New Tab" to "在新标签页中打开",
            "Share Link" to "分享链接",
            "Copy Link" to "复制链接",
            "Delete Bookmark" to "删除书签",
            "Delete History" to "删除历史记录",
            "Are you sure you want to clear all user history data?" to "您确定要清除所有用户历史数据吗？",
            "Are you sure you want to permanently delete all browsing logs?" to "您确定要永久删除所有浏览日志吗？",
            "Clear All History" to "清除所有历史记录",
            "Clear All" to "全部清除",
            "Delete Activity" to "删除活动",
            "Restore" to "还原",
            "Speed Dial" to "快捷访问",
            "Customize Speed Dial" to "自定义快捷访问",
            "Add Shortcut" to "添加快捷方式",
            "Shortcut Name" to "快捷方式名称",
            "Shortcut URL" to "快捷方式网址",
            "Add" to "添加",
            "Delete" to "删除",
            "Edit Shortcut" to "编辑快捷方式",
            "Save" to "保存",
            "Ad-Blocking Protection" to "广告拦截保护",
            "Stop intrusive ads and malicious popups" to "拦截侵入性广告和恶意弹窗",
            "Anti-Tracker Shield" to "反跟踪器盾牌",
            "Prevent tracking pixels from recording cookies" to "防止跟踪像素记录 Cookie",
            "Allow Website Notifications" to "允许网站通知",
            "Stay in Browser" to "留在浏览器",
            "Open App" to "打开 App",
            "Clear address text" to "清除地址文本",
            "Refresh Web Page" to "刷新网页",
            "Downloads" to "下载",
            "No downloads initiated in this session" to "此会话期间没有开始任何下载",
            "Shield inactive" to "防护未激活",
            "Total Blocked" to "已阻止总数",
            "App not installed, loading in browser" to "未安装关联应用，已在浏览器中加载",
            "Download started" to "下载已开始",
            "Download failed" to "下载失败",
            "Location Permission Denied" to "位置权限被拒绝",
            "Camera Permission Denied" to "相机权限被拒绝",
            "Microphone Permission Denied" to "麦克风权限被拒绝",
            "Cannot bookmark this page" to "无法将此页面添加为书签",
            "File not found: " to "找不到文件：",
            "Cannot open file: " to "无法打开文件：",
            "Cannot share file: " to "无法共享文件：",
            "Search or enter address" to "搜索或输入网址",
            "CUSTOMIZATION" to "个性化定制",
            "Fluid Animations" to "流畅动画",
            "ColorOS motion effects" to "ColorOS 动效",
            "SECURITY & SHIELD" to "安全与防护",
            "PRIVACY & SECURITY" to "隐私与安全",
            "DATA OPERATIONS" to "数据操作",
            "Clear Browsing Statistics" to "清除浏览统计数据",
            "Erase history, cached pages, search keywords" to "清除历史记录、缓存页面、搜索关键字",
            "Appearance" to "外观",
            "Typography, Spacing Details, Themes, Accent Color" to "排版、间距细节、主题、强调色",
            "Site Permissions" to "站点权限",
            "Open Tabs" to "打开的标签页",
            "Ad-Blocking Protection" to "广告拦截保护",
            "Stop intrusive ads and malicious popups" to "停止侵入式广告和恶意弹窗",
            "Anti-Tracker Shield" to "反跟踪器护盾",
            "Prevent tracking pixels from recording cookies" to "防止跟踪像素记录 cookie",
            "Add to Bookmarks" to "添加到书签",
            "Saved to Bookmarks" to "已保存到书签",
            "Access this page later" to "以后访问此页面",
            "Global defaults" to "全局默认值",
            "Applies to new sites. Existing site permissions are not changed." to "适用于新站点。现有站点权限保持不变。",
            "Per-site overrides" to "每个站点的覆盖",
            "Search languages..." to "搜索语言...",
            "Search settings..." to "搜索设置...",
            "Ad Shield Block" to "广告护盾拦截",
            "Tracker Shield Block" to "跟踪器护盾拦截",
            "Site Permissions" to "网站权限",
            "Allow Location Access" to "允许访问位置信息",
            "Location permission active" to "位置权限已激活",
            "Allow site to ask for location" to "允许网站请求位置",
            "Allow Camera Access" to "允许访问摄像头",
            "Camera permission active" to "摄像头权限已激活",
            "Allow site to ask for camera" to "允许网站请求摄像头",
            "Allow Microphone Access" to "允许访问麦克风",
            "Microphone permission active" to "麦克风权限已激活",
            "Allow site to ask for microphone" to "允许网站请求麦克风",
            "Shield Privacy Guard" to "Shield 隐私防线",
            "ColorOS 16 Adaptive Interceptor • Running securely" to "ColorOS 16 自适应拦截器 • 安全运行中",
            "Session Blocked" to "本会话已拦截",
            "English (US)" to "美式英语",
            "简体中文" to "简体中文",
            "Español" to "西班牙语",
            "Deutsch" to "德语",
            "Français" to "法语",
            "All" to "全部",
            "Ongoing" to "进行中",
            "Complete" to "已完成",
            "1.2 GB • Completed" to "1.2 GB • 已完成",
            "Downloading..." to "下载中...",
            "4.8 MB • Yesterday" to "4.8 MB • 昨天",
            "850 KB • May 20" to "850 KB • 5月20日",
            "Search downloads" to "搜索下载内容",
            "Shield Privacy Guard" to "Shield 隐私防线",
            "Browser Settings" to "浏览器设置",
            "Menu" to "菜单",
            "Light Mode" to "浅色模式",
            "Dark Mode" to "深色模式",
            "Blocked" to "拦截了",
            "trackers" to "个跟踪器"
        ),
        "Español" to mapOf(
            "Search or URL" to "Buscar o ingresar URL",
            "search" to "búsqueda",
            "Settings" to "Ajustes",
            "General" to "General",
            "GENERAL" to "GENERAL",
            "Customization" to "Personalización",
            "CUSTOMIZATION" to "PERSONALIZACIÓN",
            "Languages" to "Idiomas",
            "Active" to "Activo",
            "Active: " to "Activo: ",
            "Clear Browsing Data" to "Borrar datos de navegación",
            "History cleared successfully" to "Historial borrado con éxito",
            "Bookmarks" to "Marcadores",
            "Bookmarked" to "Marcado",
            "History" to "Historial",
            "Blocked Trackers" to "Rastreadores bloqueados",
            "Block Ads" to "Bloquear anuncios",
            "Ad Blocker" to "Bloqueador de anuncios",
            "Total Ads Blocked" to "Anuncios bloqueados",
            "Total Trackers Blocked" to "Rastreadores bloqueados",
            "New Tab" to "Nueva pestaña",
            "Tabs" to "Pestañas",
            "Cancel" to "Cancelar",
            "Clear" to "Borrar",
            "Success" to "Éxito",
            "All stored website permissions have been cleared" to "Todos los permisos almacenados de sitios de red han sido borrados",
            "Search Engine" to "Motor de búsqueda",
            "Private & Secure" to "Privado y seguro",
            "No Bookmarks saved yet." to "No hay marcadores guardados aún.",
            "No browsing history catalog logged." to "No se ha registrado historial de navegación.",
            "Privacy" to "Privacidad",
            "PRIVACY" to "PRIVACIDAD",
            "Custom Theme Color" to "Color de tema personalizado",
            "Theme Mode" to "Modo de tema",
            "Font Family" to "Familia de fuentes",
            "Layout Density" to "Densidad de diseño",
            "Fluid Animations" to "Animaciones fluidas",
            "Ad & Tracker Blocker" to "Bloqueador de anuncios y rastreadores",
            "Total Ads & Trackers Blocked" to "Total de anuncios y rastreadores bloqueados",
            "Emerald Green" to "Verde esmeralda",
            "Sky Blue" to "Azul cielo",
            "Sunset Orange" to "Naranja atardecer",
            "Cyber Lavender" to "Ciberlavanda",
            "Obsidian Slate" to "Pizarra obsidiana",
            "Light" to "Claro",
            "Dark" to "Oscuro",
            "System" to "Sistema",
            "Sans-serif" to "Sans-serif",
            "Playfair" to "Playfair",
            "Monospace" to "Monospace",
            "Compact" to "Compacto",
            "Moderate" to "Moderado",
            "Comfortable" to "Cómodo",
            "Default" to "Por defecto",
            "Done" to "Hecho",
            "OK" to "OK",
            "Close" to "Cerrar",
            "Open in New Tab" to "Abrir en nueva pestaña",
            "Share Link" to "Compartir enlace",
            "Copy Link" to "Copiar enlace",
            "Delete Bookmark" to "Eliminar marcador",
            "Delete History" to "Eliminar historial",
            "Are you sure you want to clear all user history data?" to "¿Está seguro de que desea borrar todos los datos de historial del usuario?",
            "Are you sure you want to permanently delete all browsing logs?" to "¿Está seguro de que desea eliminar permanentemente todos los registros de navegación?",
            "Clear All History" to "Borrar todo el historial",
            "Clear All" to "Borrar todo",
            "Delete Activity" to "Eliminar actividad",
            "Restore" to "Restaurar",
            "Speed Dial" to "Marcado rápido",
            "Customize Speed Dial" to "Personalizar marcado rápido",
            "Add Shortcut" to "Añadir acceso directo",
            "Shortcut Name" to "Nombre del acceso directo",
            "Shortcut URL" to "URL del acceso directo",
            "Add" to "Añadir",
            "Delete" to "Eliminar",
            "Edit Shortcut" to "Editar acceso directo",
            "Save" to "Guardar",
            "Ad-Blocking Protection" to "Protección de bloqueo de anuncios",
            "Stop intrusive ads and malicious popups" to "Detener anuncios invasivos y popups maliciosos",
            "Anti-Tracker Shield" to "Escudo anti-rastreo",
            "Prevent tracking pixels from recording cookies" to "Evitar que los píxeles de seguimiento registren cookies",
            "Allow Website Notifications" to "Permitir notificaciones de sitios",
            "Stay in Browser" to "Permanecer en el navegador",
            "Open App" to "Abrir App",
            "Clear address text" to "Borrar texto de dirección",
            "Refresh Web Page" to "Actualizar página",
            "Downloads" to "Descargas",
            "No downloads initiated in this session" to "No se han iniciado descargas en esta sesión",
            "Shield inactive" to "Escudo inactivo",
            "Total Blocked" to "Total bloqueados",
            "App not installed, loading in browser" to "Aplicación no instalada, abriendo en el navegador",
            "Download started" to "Descarga iniciada",
            "Download failed" to "Descarga fallida",
            "Location Permission Denied" to "Permiso de ubicación denegado",
            "Camera Permission Denied" to "Permiso de cámara denegado",
            "Microphone Permission Denied" to "Permiso de micrófono denegado",
            "Cannot bookmark this page" to "No se puede añadir esta página a marcadores",
            "File not found: " to "Archivo no encontrado: ",
            "Cannot open file: " to "No se puede abrir el archivo: ",
            "Cannot share file: " to "No se puede compartir el archivo: ",
            "Search or enter address" to "Buscar o ingresar URL",
            "CUSTOMIZATION" to "PERSONALIZACIÓN",
            "Fluid Animations" to "Animaciones Fluidas",
            "ColorOS motion effects" to "Efectos de movimiento ColorOS",
            "SECURITY & SHIELD" to "SEGURIDAD Y ESCUDO",
            "PRIVACY & SECURITY" to "PRIVACIDAD Y SEGURIDAD",
            "DATA OPERATIONS" to "OPERACIONES DE DATOS",
            "Clear Browsing Statistics" to "Borrar estadísticas de navegación",
            "Erase history, cached pages, search keywords" to "Borrar historial, páginas en caché, palabras clave de búsqueda",
            "Appearance" to "Apariencia",
            "Typography, Spacing Details, Themes, Accent Color" to "Tipografía, Espaciado, Temas, Color",
            "Site Permissions" to "Permisos del sitio",
            "Open Tabs" to "Pestañas abiertas",
            "Ad-Blocking Protection" to "Protección contra anuncios",
            "Stop intrusive ads and malicious popups" to "Bloquea anuncios e popups maliciosos",
            "Anti-Tracker Shield" to "Escudo Anti-Rastreo",
            "Prevent tracking pixels from recording cookies" to "Evite que los rastreadores guarden cookies",
            "Add to Bookmarks" to "Añadir a marcadores",
            "Saved to Bookmarks" to "Guardado en marcadores",
            "Access this page later" to "Acceda a esta página más tarde",
            "Global defaults" to "Valores predeterminados globales",
            "Applies to new sites. Existing site permissions are not changed." to "Se aplica a sitios nuevos. Los permisos existentes no se modifican.",
            "Per-site overrides" to "Excepciones por sitio",
            "Search languages..." to "Buscar idiomas...",
            "Search settings..." to "Buscar en ajustes...",
            "Ad Shield Block" to "Bloqueo de Escudo de Anuncios",
            "Tracker Shield Block" to "Bloqueo de Escudo de Rastreadores",
            "Site Permissions" to "Permisos de sitio",
            "Allow Location Access" to "Permitir acceso a ubicación",
            "Location permission active" to "Permiso de ubicación activo",
            "Allow site to ask for location" to "Permitir al sitio pedir ubicación",
            "Allow Camera Access" to "Permitir acceso a cámara",
            "Camera permission active" to "Permiso de cámara activo",
            "Allow site to ask for camera" to "Permitir al sitio pedir cámara",
            "Allow Microphone Access" to "Permitir acceso a micrófono",
            "Microphone permission active" to "Permiso de micrófono activo",
            "Allow site to ask for microphone" to "Permitir al sitio pedir micrófono",
            "Shield Privacy Guard" to "Escudo Protector de Privacidad",
            "ColorOS 16 Adaptive Interceptor • Running securely" to "Interceptor adaptativo ColorOS 16 • Ejecutando de forma segura",
            "Session Blocked" to "Bloqueados en la sesión",
            "English (US)" to "Inglés (EE. UU.)",
            "简体中文" to "Chino simplificado",
            "Español" to "Español",
            "Deutsch" to "Alemán",
            "Français" to "Francés",
            "All" to "Todo",
            "Ongoing" to "En curso",
            "Complete" to "Completado",
            "1.2 GB • Completed" to "1.2 GB • Completado",
            "Downloading..." to "Descargando...",
            "4.8 MB • Yesterday" to "4.8 MB • Ayer",
            "850 KB • May 20" to "850 KB • 20 de mayo",
            "Search downloads" to "Buscar descargas",
            "Shield Privacy Guard" to "Escudo de Privacidad",
            "Browser Settings" to "Configuración del navegador",
            "Menu" to "Menú",
            "Light Mode" to "Modo claro",
            "Dark Mode" to "Modo oscuro",
            "Blocked" to "Bloqueados",
            "trackers" to "rastreadores"
        ),
        "Deutsch" to mapOf(
            "Search or URL" to "Suchen oder URL eingeben",
            "search" to "Suche",
            "Settings" to "Einstellungen",
            "General" to "Allgemein",
            "GENERAL" to "ALLGEMEIN",
            "Customization" to "Personalisierung",
            "CUSTOMIZATION" to "PERSONALISIERUNG",
            "Languages" to "Sprachen",
            "Active" to "Aktiv",
            "Active: " to "Aktiv: ",
            "Clear Browsing Data" to "Browserdaten löschen",
            "History cleared successfully" to "Verlauf erfolgreich gelöscht",
            "Bookmarks" to "Lesezeichen",
            "Bookmarked" to "Als Lesezeichen gespeichert",
            "History" to "Verlauf",
            "Blocked Trackers" to "Blockierte Tracker",
            "Block Ads" to "Werbung blockieren",
            "Ad Blocker" to "Werbeblocker",
            "Total Ads Blocked" to "Werbung blockiert",
            "Total Trackers Blocked" to "Tracker blockiert",
            "New Tab" to "Neuer Tab",
            "Tabs" to "Tabs",
            "Cancel" to "Abbrechen",
            "Clear" to "Löschen",
            "Success" to "Erfolg",
            "All stored website permissions have been cleared" to "Alle gespeicherten Website-Berechtigungen wurden gelöscht",
            "Search Engine" to "Suchmaschine",
            "Private & Secure" to "Privat & Sicher",
            "No Bookmarks saved yet." to "Noch keine Lesezeichen gespeichert.",
            "No browsing history catalog logged." to "Kein Browserverlauf protokolliert.",
            "Privacy" to "Privatsphäre",
            "PRIVACY" to "PRIVATSPHÄRE",
            "Custom Theme Color" to "Benutzerdefinierte Designfarbe",
            "Theme Mode" to "Design-Modus",
            "Font Family" to "Schriftart",
            "Layout Density" to "Layout-Dichte",
            "Fluid Animations" to "Flüssige Animationen",
            "Ad & Tracker Blocker" to "Werbe- & Tracker-Blocker",
            "Total Ads & Trackers Blocked" to "Werbung & Tracker insgesamt blockiert",
            "Emerald Green" to "Smaragdgrün",
            "Sky Blue" to "Himmelblau",
            "Sunset Orange" to "Sonnenuntergangsorange",
            "Cyber Lavender" to "Cyber-Lavendel",
            "Obsidian Slate" to "Obsidianschiefer",
            "Light" to "Hell",
            "Dark" to "Dunkel",
            "System" to "System",
            "Sans-serif" to "Sans-Serif",
            "Playfair" to "Playfair",
            "Monospace" to "Monospace",
            "Compact" to "Kompakt",
            "Moderate" to "Moderat",
            "Comfortable" to "Komfortabel",
            "Default" to "Standard",
            "Done" to "Fertig",
            "OK" to "OK",
            "Close" to "Schließen",
            "Open in New Tab" to "In neuem Tab öffnen",
            "Share Link" to "Link teilen",
            "Copy Link" to "Link kopieren",
            "Delete Bookmark" to "Lesezeichen löschen",
            "Delete History" to "Verlauf löschen",
            "Are you sure you want to clear all user history data?" to "Möchten Sie wirklich alle Benutzerverlaufsdaten löschen?",
            "Are you sure you want to permanently delete all browsing logs?" to "Möchten Sie wirklich alle Browserprotokolle dauerhaft löschen?",
            "Clear All History" to "Ganzen Verlauf löschen",
            "Clear All" to "Alles löschen",
            "Delete Activity" to "Aktivität löschen",
            "Restore" to "Wiederherstellen",
            "Speed Dial" to "Kurzwahl",
            "Customize Speed Dial" to "Kurzwahl anpassen",
            "Add Shortcut" to "Verknüpfung hinzufügen",
            "Shortcut Name" to "Name der Verknüpfung",
            "Shortcut URL" to "URL der Verknüpfung",
            "Add" to "Hinzufügen",
            "Delete" to "Löschen",
            "Edit Shortcut" to "Verknüpfung bearbeiten",
            "Save" to "Speichern",
            "Ad-Blocking Protection" to "Werbeblocker-Schutz",
            "Stop intrusive ads and malicious popups" to "Invasive Werbung und schädliche Popups stoppen",
            "Anti-Tracker Shield" to "Anti-Tracker-Schutzschild",
            "Prevent tracking pixels from recording cookies" to "Verhindern, dass Tracking-Pixel Cookies aufzeichnen",
            "Allow Website Notifications" to "Website-Benachrichtigungen zulassen",
            "Stay in Browser" to "Im Browser bleiben",
            "Open App" to "App öffnen",
            "Clear address text" to "Adressfeld leeren",
            "Refresh Web Page" to "Seite aktualisieren",
            "Downloads" to "Downloads",
            "No downloads initiated in this session" to "In dieser Sitzung wurden keine Downloads gestartet",
            "Shield inactive" to "Schutz inaktiv",
            "Total Blocked" to "Insgesamt blockiert",
            "App not installed, loading in browser" to "App nicht installiert, wird im Browser geladen",
            "Download started" to "Download gestartet",
            "Download failed" to "Download fehlgeschlagen",
            "Location Permission Denied" to "Standortberechtigung verweigert",
            "Camera Permission Denied" to "Kameraberechtigung verweigert",
            "Microphone Permission Denied" to "Mikrofonberechtigung verweigert",
            "Cannot bookmark this page" to "Diese Seite kann nicht als Lesezeichen gespeichert werden",
            "File not found: " to "Datei nicht gefunden: ",
            "Cannot open file: " to "Datei kann nicht geöffnet werden: ",
            "Cannot share file: " to "Datei kann nicht geteilt werden: ",
            "Search or enter address" to "Suchen oder Adresse eingeben",
            "CUSTOMIZATION" to "ANPASSUNG",
            "Fluid Animations" to "Flüssige Animationen",
            "ColorOS motion effects" to "ColorOS Bewegungseffekte",
            "SECURITY & SHIELD" to "SICHERHEIT & SCHUTZ",
            "PRIVACY & SECURITY" to "DATENSCHUTZ & SICHERHEIT",
            "DATA OPERATIONS" to "DATENOPERATIONEN",
            "Clear Browsing Statistics" to "Browserstatistiken löschen",
            "Erase history, cached pages, search keywords" to "Verlauf, zwischengespeicherte Seiten, Suchbegriffe löschen",
            "Appearance" to "Erscheinungsbild",
            "Typography, Spacing Details, Themes, Accent Color" to "Typografie, Abstände, Themen, Akzentfarbe",
            "Site Permissions" to "Website-Berechtigungen",
            "Open Tabs" to "Offene Tabs",
            "Ad-Blocking Protection" to "Werbeblocker-Schutz",
            "Stop intrusive ads and malicious popups" to "Aufdringliche Anzeigen und schädliche Popups stoppen",
            "Anti-Tracker Shield" to "Anti-Tracker-Schild",
            "Prevent tracking pixels from recording cookies" to "Verhindern, dass Tracking-Pixel Cookies speichern",
            "Add to Bookmarks" to "Lesezeichen hinzufügen",
            "Saved to Bookmarks" to "Als Lesezeichen gespeichert",
            "Access this page later" to "Greifen Sie später auf diese Seite zu",
            "Global defaults" to "Globale Standardwerte",
            "Applies to new sites. Existing site permissions are not changed." to "Gilt für neue Websites. Vorhandene Berechtigungen bleiben unverändert.",
            "Per-site overrides" to "Ausnahmen pro Website",
            "Search languages..." to "Sprachen suchen...",
            "Search settings..." to "Einstellungen suchen...",
            "Ad Shield Block" to "Werbeschild-Blocker",
            "Tracker Shield Block" to "Tracker-Schild-Blocker",
            "Site Permissions" to "Website-Berechtigungen",
            "Allow Location Access" to "Standortzugriff erlauben",
            "Location permission active" to "Standortberechtigung aktiv",
            "Allow site to ask for location" to "Website nach Standort fragen lassen",
            "Allow Camera Access" to "Kamerazugriff erlauben",
            "Camera permission active" to "Kameraberechtigung aktiv",
            "Allow site to ask for camera" to "Website nach Kamera fragen lassen",
            "Allow Microphone Access" to "Mikrofonzugriff erlauben",
            "Microphone permission active" to "Mikrofonberechtigung aktiv",
            "Allow site to ask for microphone" to "Website nach Mikrofon fragen lassen",
            "Shield Privacy Guard" to "Schild Privatsphäre-Schutz",
            "ColorOS 16 Adaptive Interceptor • Running securely" to "ColorOS 16 Adaptiver Abfänger • Läuft sicher",
            "Session Blocked" to "In dieser Sitzung blockiert",
            "English (US)" to "Englisch (USA)",
            "简体中文" to "Vereinfachtes Chinesisch",
            "Español" to "Spanisch",
            "Deutsch" to "Deutsch",
            "Français" to "Französisch",
            "All" to "Alle",
            "Ongoing" to "Laufend",
            "Complete" to "Abgeschlossen",
            "1.2 GB • Completed" to "1.2 GB • Abgeschlossen",
            "Downloading..." to "Wird heruntergeladen...",
            "4.8 MB • Yesterday" to "4.8 MB • Gestern",
            "850 KB • May 20" to "850 KB • 20. Mai",
            "Search downloads" to "Downloads suchen",
            "Shield Privacy Guard" to "Schild Privatsphäre-Schutz",
            "Browser Settings" to "Browser-Einstellungen",
            "Menu" to "Menü",
            "Light Mode" to "Heller Modus",
            "Dark Mode" to "Dunkler Modus",
            "Blocked" to "Blockiert",
            "trackers" to "Tracker"
        ),
        "Français" to mapOf(
            "Search or URL" to "Rechercher ou saisir une URL",
            "search" to "recherche",
            "Settings" to "Paramètres",
            "General" to "Général",
            "GENERAL" to "GÉNÉRAL",
            "Customization" to "Personnalisation",
            "CUSTOMIZATION" to "PERSONNALISATION",
            "Languages" to "Langues",
            "Active" to "Actif",
            "Active: " to "Actif: ",
            "Clear Browsing Data" to "Effacer les données de navigation",
            "History cleared successfully" to "Historique effacé avec succès",
            "Bookmarks" to "Signets",
            "Bookmarked" to "Ajouté aux signets",
            "History" to "Historique",
            "Blocked Trackers" to "Traqueurs bloqués",
            "Block Ads" to "Bloquer les publicités",
            "Ad Blocker" to "Bloqueur de pub",
            "Total Ads Blocked" to "Publicités bloquées",
            "Total Trackers Blocked" to "Traqueurs bloqués",
            "New Tab" to "Nouvel onglet",
            "Tabs" to "Onglets",
            "Cancel" to "Annuler",
            "Clear" to "Effacer",
            "Success" to "Succès",
            "All stored website permissions have been cleared" to "Toutes les autorisations de sites stockées ont été effacées",
            "Search Engine" to "Moteur de recherche",
            "Private & Secure" to "Privé et sécurisé",
            "No Bookmarks saved yet." to "Aucun signet enregistré pour le moment.",
            "No browsing history catalog logged." to "Aucun historique de navigation enregistré.",
            "Privacy" to "Confidentialité",
            "PRIVACY" to "CONFIDENTIALITÉ",
            "Custom Theme Color" to "Couleur de thème personnalisée",
            "Theme Mode" to "Mode de thème",
            "Font Family" to "Famille de polices",
            "Layout Density" to "Densité de mise en page",
            "Fluid Animations" to "Animations fluides",
            "Ad & Tracker Blocker" to "Bloqueur de pubs et traqueurs",
            "Total Ads & Trackers Blocked" to "Total de publicités et traqueurs bloqués",
            "Emerald Green" to "Vert émeraude",
            "Sky Blue" to "Bleu ciel",
            "Sunset Orange" to "Couché de soleil orange",
            "Cyber Lavender" to "Cyber lavande",
            "Obsidian Slate" to "Ardoise obsidienne",
            "Light" to "Clair",
            "Dark" to "Sombre",
            "System" to "Système",
            "Sans-serif" to "Sans-serif",
            "Playfair" to "Playfair",
            "Monospace" to "Monospace",
            "Compact" to "Compact",
            "Moderate" to "Modéré",
            "Comfortable" to "Confortable",
            "Default" to "Par défaut",
            "Done" to "Terminé",
            "OK" to "OK",
            "Close" to "Fermer",
            "Open in New Tab" to "Ouvrir dans un nouvel onglet",
            "Share Link" to "Partager le lien",
            "Copy Link" to "Copier le lien",
            "Delete Bookmark" to "Supprimer le signet",
            "Delete History" to "Supprimer l'historique",
            "Are you sure you want to clear all user history data?" to "Êtes-vous sûr de vouloir effacer toutes les données d'historique de l'utilisateur ?",
            "Are you sure you want to permanently delete all browsing logs?" to "Êtes-vous sûr de vouloir supprimer définitivement tous les journaux de navigation ?",
            "Clear All History" to "Effacer tout l'historique",
            "Clear All" to "Tout effacer",
            "Delete Activity" to "Supprimer l'activité",
            "Restore" to "Restaurer",
            "Speed Dial" to "Numérotation rapide",
            "Customize Speed Dial" to "Personnaliser la numérotation rapide",
            "Add Shortcut" to "Ajouter un raccourci",
            "Shortcut Name" to "Nom du raccourci",
            "Shortcut URL" to "URL du raccourci",
            "Add" to "Ajouter",
            "Delete" to "Supprimer",
            "Edit Shortcut" to "Modifier le raccourci",
            "Save" to "Enregistrer",
            "Ad-Blocking Protection" to "Protection anti-pub",
            "Stop intrusive ads and malicious popups" to "Bloquer les publicités intrusives et popups malicieux",
            "Anti-Tracker Shield" to "Bouclier anti-traquage",
            "Prevent tracking pixels from recording cookies" to "Empêcher les pixels invisibles d'enregistrer des cookies",
            "Allow Website Notifications" to "Autoriser les notifications de sites",
            "Stay in Browser" to "Rester sur le navigateur",
            "Open App" to "Ouvrir l'App",
            "Clear address text" to "Effacer le texte de l'adresse",
            "Refresh Web Page" to "Rafraîchir la page",
            "Downloads" to "Téléchargements",
            "No downloads initiated in this session" to "Aucun téléchargement démarré dans cette session",
            "Shield inactive" to "Bouclier inactif",
            "Total Blocked" to "Total bloqué",
            "App not installed, loading in browser" to "App non installée, chargement sur le navigateur",
            "Download started" to "Téléchargement démarré",
            "Download failed" to "Téléchargement échoué",
            "Location Permission Denied" to "Autorisation de localisation refusée",
            "Camera Permission Denied" to "Autorisation de caméra refusée",
            "Microphone Permission Denied" to "Autorisation de microphone refusée",
            "Cannot bookmark this page" to "Impossible d'ajouter cette page aux favoris",
            "File not found: " to "Fichier introuvable : ",
            "Cannot open file: " to "Impossible d'ouvrir le fichier : ",
            "Cannot share file: " to "Impossible de partager le fichier : ",
            "Search or enter address" to "Rechercher ou saisir une adresse",
            "CUSTOMIZATION" to "PERSONNALISATION",
            "Fluid Animations" to "Animations fluides",
            "ColorOS motion effects" to "Effets de mouvement ColorOS",
            "SECURITY & SHIELD" to "SÉCURITÉ ET BOUCLIER",
            "PRIVACY & SECURITY" to "CONFIDENTIALITÉ ET SÉCURITÉ",
            "DATA OPERATIONS" to "OPÉRATIONS DE DONNÉES",
            "Clear Browsing Statistics" to "Effacer les statistiques de navigation",
            "Erase history, cached pages, search keywords" to "Effacer l'historique, les pages en cache, les mots-clés",
            "Appearance" to "Apparence",
            "Typography, Spacing Details, Themes, Accent Color" to "Typographie, Espacement, Thèmes, Couleur d'accent",
            "Site Permissions" to "Autorisations du site",
            "Open Tabs" to "Onglets ouverts",
            "Ad-Blocking Protection" to "Protection anti-pub",
            "Stop intrusive ads and malicious popups" to "Bloquer les publicités et fenêtres contextuelles intrusives",
            "Anti-Tracker Shield" to "Bouclier Anti-pistage",
            "Prevent tracking pixels from recording cookies" to "Empêcher les traceurs d'enregistrer des cookies",
            "Add to Bookmarks" to "Ajouter aux favoris",
            "Saved to Bookmarks" to "Enregistré dans les favoris",
            "Access this page later" to "Accéder à cette page plus tard",
            "Global defaults" to "Paramètres globaux",
            "Applies to new sites. Existing site permissions are not changed." to "S'applique aux nouveaux sites. Les autorisations existantes ne sont pas modifiées.",
            "Per-site overrides" to "Exceptions par site",
            "Search languages..." to "Rechercher des langues...",
            "Search settings..." to "Rechercher des paramètres...",
            "Ad Shield Block" to "Bloqueur de Bouclier Publicitaire",
            "Tracker Shield Block" to "Bloqueur de Bouclier Anti-traqueurs",
            "Site Permissions" to "Autorisations du site",
            "Allow Location Access" to "Autoriser l'accès à la position",
            "Location permission active" to "Autorisation de position active",
            "Allow site to ask for location" to "Autoriser le site à demander la position",
            "Allow Camera Access" to "Autoriser l'accès à l'appareil photo",
            "Camera permission active" to "Autorisation de l'appareil photo active",
            "Allow site to ask for camera" to "Autoriser le site à demander l'appareil photo",
            "Allow Microphone Access" to "Autoriser l'accès au micro",
            "Microphone permission active" to "Autorisation du micro active",
            "Allow site to ask for microphone" to "Autoriser le site à demander le micro",
            "Shield Privacy Guard" to "Bouclier de protection de la vie privée",
            "ColorOS 16 Adaptive Interceptor • Running securely" to "Intercepteur adaptatif ColorOS 16 • Fonctionne en toute sécurité",
            "Session Blocked" to "Bloqué cette session",
            "English (US)" to "Anglais (États-Unis)",
            "简体中文" to "Chinois simplifié",
            "Español" to "Espagnol",
            "Deutsch" to "Allemand",
            "Français" to "Français",
            "All" to "Tout",
            "Ongoing" to "En cours",
            "Complete" to "Terminé",
            "1.2 GB • Completed" to "1.2 Go • Terminé",
            "Downloading..." to "Téléchargement...",
            "4.8 MB • Yesterday" to "4.8 Mo • Hier",
            "850 KB • May 20" to "850 Ko • 20 Mai",
            "Search downloads" to "Rechercher des téléchargements",
            "Shield Privacy Guard" to "Bouclier de confidentialité",
            "Browser Settings" to "Paramètres du navigateur",
            "Menu" to "Menu",
            "Light Mode" to "Mode clair",
            "Dark Mode" to "Mode sombre",
            "Blocked" to "Bloqué",
            "trackers" to "traqueurs"
        ),
        "Italiano" to mapOf(
            "Search or enter address" to "Cerca o inserisci indirizzo",
            "CUSTOMIZATION" to "PERSONALIZZAZIONE",
            "Fluid Animations" to "Animazioni fluide",
            "ColorOS motion effects" to "Effetti di movimento ColorOS",
            "SECURITY & SHIELD" to "SICUREZZA E SCUDO",
            "PRIVACY & SECURITY" to "PRIVACY E SICUREZZA",
            "DATA OPERATIONS" to "OPERAZIONI DATI",
            "Clear Browsing Statistics" to "Cancella statistiche di navigazione",
            "Erase history, cached pages, search keywords" to "Cancella cronologia, pagine nella cache, parole chiave",
            "Location Permission Denied" to "Permesso di posizione negato",
            "Camera Permission Denied" to "Permesso della fotocamera negato",
            "Microphone Permission Denied" to "Permesso del microfono negato",
            "Cannot bookmark this page" to "Impossibile aggiungere questa pagina ai segnalibri",
            "File not found: " to "File non trovato: ",
            "Cannot open file: " to "Impossibile aprire il file: ",
            "Cannot share file: " to "Impossibile condividere il file: ",
            "App not installed, loading in browser" to "App non installata, caricamento nel browser",
            "Appearance" to "Aspetto",
            "Typography, Spacing Details, Themes, Accent Color" to "Tipografia, Dettagli spaziatura, Temi, Colore accento",
            "Site Permissions" to "Permessi sito",
            "Open Tabs" to "Schede aperte",
            "Ad-Blocking Protection" to "Protezione blocco annunci",
            "Stop intrusive ads and malicious popups" to "Ferma gli annunci intrusivi e i fastidiosi pop-up",
            "Anti-Tracker Shield" to "Inibitore Tracciamenti",
            "Prevent tracking pixels from recording cookies" to "Impedisci ai pixel di tracciamento di registrare i cookie",
            "Add to Bookmarks" to "Aggiungi ai segnalibri",
            "Saved to Bookmarks" to "Salvato nei segnalibri",
            "Access this page later" to "Accedi a questa pagina in seguito",
            "Global defaults" to "Impostazioni globali predefinite",
            "Applies to new sites. Existing site permissions are not changed." to "Si applica ai nuovi siti. I permessi esistenti rimangono invariati.",
            "Per-site overrides" to "Eccezioni per sito",
            "Search languages..." to "Cerca lingue...",
            "Languages" to "Lingue",
            "Search Engine" to "Motore di ricerca",
            "Ad Shield Block" to "Blocco Scudo Annunci",
            "Tracker Shield Block" to "Blocco Scudo Tracker"
        ),
        "日本語" to mapOf(
            "Search or enter address" to "検索またはアドレスを入力",
            "CUSTOMIZATION" to "カスタマイズ",
            "Fluid Animations" to "スムーズなアニメーション",
            "ColorOS motion effects" to "ColorOS モーションエフェクト",
            "SECURITY & SHIELD" to "セキュリティとシールド",
            "PRIVACY & SECURITY" to "プライバシーとセキュリティ",
            "DATA OPERATIONS" to "データ操作",
            "Clear Browsing Statistics" to "閲覧統計を消去",
            "Erase history, cached pages, search keywords" to "履歴、キャッシュされたページ、検索キーワードを消去",
            "Location Permission Denied" to "位置情報の権限が拒否されました",
            "Camera Permission Denied" to "カメラの権限が拒否されました",
            "Microphone Permission Denied" to "マイクの権限が拒否されました",
            "Cannot bookmark this page" to "このページをブックマークできません",
            "File not found: " to "ファイルが見つかりません：",
            "Cannot open file: " to "ファイルを開けません：",
            "Cannot share file: " to "ファイルを共有できません：",
            "App not installed, loading in browser" to "アプリがインストールされていないため、ブラウザで読み込みます",
            "Appearance" to "外観",
            "Typography, Spacing Details, Themes, Accent Color" to "タイポグラフィ、間隔、テーマ、アクセントカラー",
            "Site Permissions" to "サイトの権限",
            "Open Tabs" to "開いているタブ",
            "Ad-Blocking Protection" to "広告ブロック保護",
            "Stop intrusive ads and malicious popups" to "厄介な広告や悪意のあるポップアップを停止する",
            "Anti-Tracker Shield" to "トラッカー防止シールド",
            "Prevent tracking pixels from recording cookies" to "トラッキングピクセルによるCookieの記録を防止",
            "Add to Bookmarks" to "ブックマークに追加",
            "Saved to Bookmarks" to "ブックマークに保存しました",
            "Access this page later" to "後でこのページにアクセス",
            "Global defaults" to "グローバル デフォルト",
            "Applies to new sites. Existing site permissions are not changed." to "新しいサイトに適用されます。既存の権限は変更されません。",
            "Per-site overrides" to "サイトごとの上書き",
            "Search languages..." to "言語を検索...",
            "Languages" to "言語",
            "Search Engine" to "検索エンジン",
            "Ad Shield Block" to "広告シールドブロック",
            "Tracker Shield Block" to "トラッカーシールドブロック"
        )
    )

    val LANG_CODES = mapOf(
        "English (US)" to "en", "English (UK)" to "en", "简体中文" to "zh-CN", "繁體中文" to "zh-TW", "Español" to "es", "Deutsch" to "de", "Français" to "fr", "Italiano" to "it", "日本語" to "ja",
        "Afrikaans" to "af", "Shqip (Albanian)" to "sq", "አማርኛ (Amharic)" to "am", "العربية (Arabic)" to "ar", "Հայերեն (Armenian)" to "hy", "Azərbaycan dili (Azerbaijani)" to "az",
        "Euskara (Basque)" to "eu", "Беларуская (Belarusian)" to "be", "বাংলা (Bengali)" to "bn", "Bosanski (Bosnian)" to "bs", "Български (Bulgarian)" to "bg",
        "Català (Catalan)" to "ca", "Cebuano" to "ceb", "Chichewa" to "ny", "Corsu (Corsican)" to "co", "Hrvatski (Croatian)" to "hr", "Čeština (Czech)" to "cs", "Dansk (Danish)" to "da",
        "Nederlands (Dutch)" to "nl", "Esperanto" to "eo", "Eesti (Estonian)" to "et", "Filipino" to "tl", "Suomi (Finnish)" to "fi", "Frysk (Frisian)" to "fy", "Galego (Galician)" to "gl",
        "ქართული (Georgian)" to "ka", "Ελληνικά (Greek)" to "el", "ગુજરાતી (Gujarati)" to "gu", "Kreyòl ayisyen (Haitian Creole)" to "ht", "Hausa" to "ha", "ʻŌlelo Hawaiʻi (Hawaiian)" to "haw",
        "עברית (Hebrew)" to "iw", "हिन्दी (Hindi)" to "hi", "Hmong" to "hmn", "Magyar (Hungarian)" to "hu", "Íslenska (Icelandic)" to "is", "Igbo" to "ig", "Bahasa Indonesia (Indonesian)" to "id",
        "Gaeilge (Irish)" to "ga", "Basa Jawa (Javanese)" to "jw", "ಕನ್ನಡ (Kannada)" to "kn", "Қазақ тілі (Kazakh)" to "kk", "ខ្មែរ (Khmer)" to "km", "Kinyarwanda" to "rw",
        "한국어 (Korean)" to "ko", "Kurdî (Kurdish)" to "ku", "Кыргызча (Kyrgyz)" to "ky", "ລາວ (Lao)" to "lo", "Latina (Latin)" to "la", "Latviešu (Latvian)" to "lv",
        "Lietuvių (Lithuanian)" to "lt", "Lëtzebuergesch (Luxembourgish)" to "lb", "Македонски (Macedonian)" to "mk", "Malagasy" to "mg", "Bahasa Melayu (Malay)" to "ms",
        "മലയാളം (Malayalam)" to "ml", "Malti (Maltese)" to "mt", "Māori" to "mi", "मराठी (Marathi)" to "mr", "Монгол (Mongolian)" to "mn", "ဗမာစာ (Burmese)" to "my", "नेपाली (Nepali)" to "ne",
        "Norsk (Norwegian)" to "no", "ଓଡ଼ିଆ (Odia)" to "or", "پښتو (Pashto)" to "ps", "فارسی (Persian)" to "fa", "Polski (Polish)" to "pl", "Português (Portuguese)" to "pt",
        "ਪੰਜਾਬੀ (Punjabi)" to "pa", "Română (Romanian)" to "ro", "Русский (Russian)" to "ru", "Gagana fa'a Sāmoa (Samoan)" to "sm", "Gàidhlig (Scots Gaelic)" to "gd",
        "Српски (Serbian)" to "sr", "Sesotho" to "st", "Shona" to "sn", "سنڌي (Sindhi)" to "sd", "සිංහල (Sinhala)" to "si", "Slovenčina (Slovak)" to "sk", "Slovenščina (Slovenian)" to "sl",
        "Soomaali (Somali)" to "so", "Basa Sunda (Sundanese)" to "su", "Kiswahili (Swahili)" to "sw", "Svenska (Swedish)" to "sv", "Тоҷикӣ (Tajik)" to "tg", "தமிழ் (Tamil)" to "ta",
        "Татар (Tatar)" to "tt", "తెలుగు (Telugu)" to "te", "ไทย (Thai)" to "th", "Türkçe (Turkish)" to "tr", "Türkmen (Turkmen)" to "tk", "Українська (Ukrainian)" to "uk",
        "اردو (Urdu)" to "ur", "ئۇيغۇرچە (Uyghur)" to "ug", "O'zbek (Uzbek)" to "uz", "Tiếng Việt (Vietnamese)" to "vi", "Cymraeg (Welsh)" to "cy", "isiXhosa (Xhosa)" to "xh",
        "ייִדיש (Yiddish)" to "yi", "Yorùbá" to "yo", "isiZulu (Zulu)" to "zu"
    )

    private val memoryCache = mutableMapOf<Pair<String, String>, String>()

    fun getMemoryCached(text: String, lang: String): String? {
        val key = Pair(text, lang)
        return memoryCache[key]
    }

    suspend fun translateAsync(context: android.content.Context, text: String, lang: String): String {
        if (lang == "English (US)" || lang == "English (UK)") return text
        
        val offline = getOfflineTranslation(text, lang)
        if (offline != null) return offline

        val langCode = LANG_CODES[lang] ?: "en"
        val key = Pair(text, lang)
        
        memoryCache[key]?.let { return it }

        val prefs = context.getSharedPreferences("i18n_$langCode", android.content.Context.MODE_PRIVATE)
        val cached = prefs.getString(key.first, null)
        if (cached != null) {
            memoryCache[key] = cached
            return cached
        }

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val urlString = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=en&tl=$langCode&dt=t&q=${java.net.URLEncoder.encode(text, "UTF-8")}"
                val url = java.net.URL(urlString)
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                
                val jsonArray = org.json.JSONArray(response)
                val parts = jsonArray.getJSONArray(0)
                val sb = java.lang.StringBuilder()
                for (i in 0 until parts.length()) {
                    sb.append(parts.getJSONArray(i).getString(0))
                }
                val translated = sb.toString()
                
                memoryCache[key] = translated
                prefs.edit().putString(key.first, translated).apply()
                translated
            } catch (e: Exception) {
                text
            }
        }
    }

    fun getOfflineTranslation(text: String, lang: String): String? {
        if (lang == "English (US)") return text
        val trimText = text.trim()
        val hasColon = trimText.endsWith(":")
        val lookupKey = if (hasColon) trimText.substring(0, trimText.length - 1).trim() else trimText

        // Handle dynamic string Active state
        if (trimText.startsWith("Active: ")) {
            val value = trimText.removePrefix("Active: ")
            val translatedValue = getOfflineTranslation(value, lang) ?: value
            val prefix = if (lang == "简体中文") "当前" else if (lang == "Español") "Activo" else if (lang == "Deutsch") "Aktiv" else if (lang == "Français") "Actif" else "Active"
            return "$prefix: $translatedValue"
        }

        // Handle dynamic ad block text
        if (trimText.startsWith("Blocked ") && trimText.contains("this session")) {
            val parts = trimText.split(" ")
            val sessionCount = parts.getOrNull(1) ?: "0"
            val totalCount = trimText.substringAfter("Total: ").substringBefore(")")
            return when (lang) {
                "简体中文" -> "本会话已阻止 $sessionCount 个广告 (总计: $totalCount)"
                "Español" -> "Anuncios bloqueados en esta sesión: $sessionCount (Total: $totalCount)"
                "Deutsch" -> "$sessionCount Anzeigen in dieser Sitzung blockiert (Gesamt: $totalCount)"
                "Français" -> "$sessionCount publicités bloquées cette session (Total: $totalCount)"
                else -> null
            }
        }

        // Handle dynamic tracker block text
        if (trimText.startsWith("Blocked ") && trimText.contains("cookies/trackers")) {
            val parts = trimText.split(" ")
            val sessionCount = parts.getOrNull(1) ?: "0"
            val totalCount = trimText.substringAfter("Total: ").substringBefore(")")
            return when (lang) {
                "简体中文" -> "本会话已阻止 $sessionCount 个跟踪器/Cookie (总计: $totalCount)"
                "Español" -> "Rastreadores/cookies bloqueados en esta sesión: $sessionCount (Total: $totalCount)"
                "Deutsch" -> "$sessionCount Tracker/Cookies in dieser Sitzung blockiert (Gesamt: $totalCount)"
                "Français" -> "$sessionCount traqueurs/cookies bloqués cette session (Total: $totalCount)"
                else -> null
            }
        }

        // Handle download started
        if (trimText.startsWith("Download started:")) {
            val fileName = trimText.substringAfter("Download started: ")
            val prefix = when (lang) {
                "简体中文" -> "下载已开始"
                "Español" -> "Descarga iniciada"
                "Deutsch" -> "Download gestartet"
                "Français" -> "Téléchargement démarré"
                else -> "Download started"
            }
            return "$prefix: $fileName"
        }

        // Handle download failed
        if (trimText.startsWith("Download failed:")) {
            val error = trimText.substringAfter("Download failed: ")
            val prefix = when (lang) {
                "简体中文" -> "下载失败"
                "Español" -> "Descarga fallida"
                "Deutsch" -> "Download fehlgeschlagen"
                "Français" -> "Téléchargement échoué"
                else -> "Download failed"
            }
            return "$prefix: $error"
        }

        val translated = TRANSLATIONS[lang]?.get(lookupKey)
        if (translated != null) {
            return if (hasColon) "$translated:" else translated
        }

        val map = TRANSLATIONS[lang] ?: return null
        for ((key, value) in map) {
            if (key.equals(lookupKey, ignoreCase = true)) {
                return if (hasColon) "$value:" else value
            }
        }

        return null
    }

    fun translateText(text: String, lang: String): String {
        return getOfflineTranslation(text, lang) ?: text
    }
}
