package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BrowserRepository(private val dao: BrowserDao) {

    // Tabs
    val allTabsFlow: Flow<List<BrowserTab>> = dao.getAllTabsFlow()
    
    suspend fun getAllTabs(): List<BrowserTab> = dao.getAllTabs()
    
    suspend fun addTab(url: String, title: String): Int {
        val id = dao.insertTab(BrowserTab(url = url, title = title))
        return id.toInt()
    }

    suspend fun updateTab(tab: BrowserTab) {
        dao.updateTab(tab)
    }

    suspend fun deleteTab(tab: BrowserTab) {
        dao.deleteTab(tab)
    }

    suspend fun clearTabs() {
        dao.deleteAllTabs()
    }

    // Bookmarks
    val bookmarksFlow: Flow<List<Bookmark>> = dao.getAllBookmarksFlow()

    suspend fun addBookmark(url: String, title: String) {
        dao.insertBookmark(Bookmark(url = url, title = title))
    }

    suspend fun removeBookmark(url: String) {
        dao.deleteBookmarkByUrl(url)
    }

    fun isBookmarked(url: String): Flow<Boolean> = dao.isBookmarkedFlow(url)

    // History
    val historyFlow: Flow<List<HistoryItem>> = dao.getHistoryFlow()

    suspend fun addHistory(url: String, title: String) {
        // Prevent empty or duplicate items being added too quickly
        dao.insertHistoryItem(HistoryItem(url = url, title = title))
    }

    suspend fun deleteHistory(id: Int) {
        dao.deleteHistoryItem(id)
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    // Downloads
    val downloadsFlow: Flow<List<DownloadItem>> = dao.getAllDownloadsFlow()

    suspend fun addDownload(url: String, fileName: String, filePath: String, totalBytes: Long, mimeType: String?): Int {
        val id = dao.insertDownload(
            DownloadItem(
                url = url,
                fileName = fileName,
                filePath = filePath,
                mimeType = mimeType,
                totalBytes = totalBytes,
                downloadedBytes = 0,
                status = "PENDING"
            )
        )
        return id.toInt()
    }

    suspend fun updateDownload(download: DownloadItem) {
        dao.updateDownload(download)
    }

    suspend fun deleteDownload(id: Int) {
        dao.deleteDownload(id)
    }

    // Settings
    // Ensure that settings are never null by providing default fallback of BrowserSettings()
    val settingsFlow: Flow<BrowserSettings> = dao.getSettingsFlow().map { it ?: BrowserSettings() }

    suspend fun getSettings(): BrowserSettings {
        return dao.getSettings() ?: BrowserSettings().also {
            dao.insertOrUpdateSettings(it)
        }
    }

    suspend fun saveSettings(settings: BrowserSettings) {
        dao.insertOrUpdateSettings(settings)
    }

    suspend fun incrementTrackers(count: Int) {
        val current = getSettings()
        saveSettings(current.copy(totalTrackersBlocked = current.totalTrackersBlocked + count))
    }

    suspend fun incrementAds(count: Int) {
        val current = getSettings()
        saveSettings(current.copy(totalAdsBlocked = current.totalAdsBlocked + count))
    }

    // Website notification permissions
    val websitePermissionsFlow: Flow<List<WebsitePermission>> = dao.getAllWebsitePermissionsFlow()

    suspend fun addWebsitePermission(domain: String, allowed: Boolean) {
        dao.insertWebsitePermission(WebsitePermission(domain = domain, notificationsAllowed = allowed))
    }

    suspend fun removeWebsitePermission(domain: String) {
        dao.deleteWebsitePermission(domain)
    }

    suspend fun clearAllWebsitePermissions() {
        dao.deleteAllWebsitePermissions()
    }
}
