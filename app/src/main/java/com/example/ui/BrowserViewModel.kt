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

class BrowserViewModel(
    application: Application,
    private val repository: BrowserRepository
) : AndroidViewModel(application) {

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

    // Sheets / Dialogs Visibility UI state
    val showTabsOverview = MutableStateFlow(false)
    val showSettings = MutableStateFlow(false)
    val showBookmarks = MutableStateFlow(false)
    val showHistory = MutableStateFlow(false)
    val showDownloads = MutableStateFlow(false)
    val showShieldPanel = MutableStateFlow(false)

    // Redirect proposal data
    data class AppRedirectProposal(
        val url: String,
        val appName: String,
        val tabId: Int
    )
    val appRedirectProposal = MutableStateFlow<AppRedirectProposal?>(null)
    val allowedInBrowserUrls = mutableSetOf<String>()

    // Live download speeds track
    val downloadSpeeds = mutableStateMapOf<Int, String>()

    // Flow integration for Bookmarks, History, Downloads, and Settings
    val bookmarks = repository.bookmarksFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val history = repository.historyFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val downloads = repository.downloadsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = repository.settingsFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BrowserSettings())

    init {
        // Load initial state
        viewModelScope.launch {
            val dbTabs = repository.getAllTabs()
            val initialSettings = repository.getSettings()
            
            if (dbTabs.isEmpty()) {
                // Create custom homepage tab to start
                addNewTab(initialSettings.homeUrl)
            } else {
                _tabs.value = dbTabs.map { 
                    TabState(id = it.id, url = it.url, title = it.title)
                }
                _activeTabId.value = dbTabs.first().id
                _currentUrlInput.value = dbTabs.first().url
            }
        }
    }

    // Tab Management
    fun addNewTab(url: String = "https://search.stormx.ninja") {
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

    // Get or Create dynamic WebView for stable multi-tab navigation
    fun getOrCreateWebView(tabId: Int, context: Context): WebView {
        val webView = webViewMap[tabId] ?: createWebViewInstance(tabId, context).also {
            webViewMap[tabId] = it
        }
        // Force-remove from any stale parent before Compose attempts to re-attach this WebView instance
        (webView.parent as? android.view.ViewGroup)?.removeView(webView)
        return webView
    }

    private fun createWebViewInstance(tabId: Int, context: Context): WebView {
        val webView = WebView(context).apply {
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
                    // Prevent crash on app intents/stores
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
                            viewModelScope.launch {
                                if (isTracker) {
                                    _blockedTrackersSession.value++
                                    repository.incrementTrackers(1)
                                } else {
                                    _blockedAdsSession.value++
                                    repository.incrementAds(1)
                                }
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
            }

            // Browser download listener to support file downloading
            setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                triggerDownload(url, userAgent, contentDisposition, mimetype, contentLength, context)
            }
        }
        
        // Load initial url
        val currentTab = _tabs.value.find { it.id == tabId }
        webView.loadUrl(currentTab?.url ?: "https://search.stormx.ninja")
        return webView
    }

    private fun isTrackerHost(host: String): Boolean {
        val trackers = listOf(
            "google-analytics.com", "analytics.", "quantserve.com", "scorecardresearch.com",
            "statcounter.com", "mixpanel.com", "hotjar.com", "segment.io", "amplitude.com",
            "facebook.net", "fbcdn.net", "tracker", "telemetry", "metrics"
        )
        return trackers.any { host.contains(it) }
    }

    private fun isAdHost(host: String, url: String): Boolean {
        val adDomains = listOf(
            "doubleclick.net", "googlesyndication.com", "adservice.google.com", 
            "amazon-adsystem.com", "taboola.com", "outbrain.com", "criteo.com", 
            "popads.net", "trafficjunky.com", "pubmatic.com", "adnxs.com", 
            "rubiconproject.com", "openx.net", "casalemedia.com", "adsystem",
            "adserver", "adroll.com", "buysellads.com"
        )
        val urlLower = url.lowercase()
        val containsAdPatterns = urlLower.contains("/ads/") || urlLower.contains("/adserver/") || 
                urlLower.contains("?ad_id") || urlLower.contains("&ad_") || urlLower.contains("/banners/")
        
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
                "https://search.stormx.ninja/search?q=${Uri.encode(formattedUrl)}"
            }
        }

        _currentUrlInput.value = formattedUrl
        val activeId = _activeTabId.value
        val webView = getOrCreateWebView(activeId, context)
        webView.loadUrl(formattedUrl)
    }

    fun activeTabGoBack(context: Context) {
        val webView = webViewMap[_activeTabId.value]
        if (webView != null && webView.canGoBack()) {
            webView.goBack()
        }
    }

    fun activeTabGoForward(context: Context) {
        val webView = webViewMap[_activeTabId.value]
        if (webView != null && webView.canGoForward()) {
            webView.goForward()
        }
    }

    fun activeTabRefresh(context: Context) {
        val webView = webViewMap[_activeTabId.value]
        webView?.reload()
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
            Toast.makeText(getApplication(), "History cleared successfully", Toast.LENGTH_SHORT).show()
        }
    }

    // Download Handler
    private fun triggerDownload(
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
            
            withContext(Dispatchers.Main) {
                try {
                    val request = DownloadManager.Request(Uri.parse(url)).apply {
                        setMimeType(mimeType)
                        addRequestHeader("User-Agent", userAgent)
                        setDescription("Downloading from StormX Browser")
                        setTitle(fileName)
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
                                    withContext(Dispatchers.Main) {
                                        downloadSpeeds[downloadId] = speedText
                                    }
                                }
    
                                val mappedStatus = when (status) {
                                    DownloadManager.STATUS_RUNNING -> "DOWNLOADING"
                                    DownloadManager.STATUS_SUCCESSFUL -> {
                                        isRunning = false
                                        "COMPLETED"
                                    }
                                    DownloadManager.STATUS_FAILED -> {
                                        isRunning = false
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
                        }
                    }
    
                    Toast.makeText(context, "Download started: $fileName", Toast.LENGTH_LONG).show()
    
                } catch (e: Exception) {
                    viewModelScope.launch {
                        val downloadsList = repository.downloadsFlow.first()
                        val currentDownload = downloadsList.find { it.id == downloadId }
                        if (currentDownload != null) {
                            repository.updateDownload(currentDownload.copy(status = "FAILED"))
                        }
                    }
                    Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
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
