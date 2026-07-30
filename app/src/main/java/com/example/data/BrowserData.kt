package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Entity(tableName = "browser_tabs")
data class BrowserTab(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val lastActive: Long = System.currentTimeMillis()
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "history_items")
data class HistoryItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val title: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "downloads")
data class DownloadItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val fileName: String,
    val filePath: String,
    val mimeType: String?,
    val totalBytes: Long,
    val downloadedBytes: Long,
    val status: String, // PENDING, DOWNLOADING, COMPLETED, FAILED
    val dmId: Long = -1L,
    val timestamp: Long = System.currentTimeMillis()
)

enum class PermissionState {
    ASK, BLOCK, ALLOW
}

@Entity(tableName = "browser_settings")
data class BrowserSettings(
    @PrimaryKey val id: Int = 1,
    val homeUrl: String = "https://search.stormx.ninja/",
    val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val customThemeColor: Int = 0, // 0 means default ColorOS 16 Emerald Green, 1=Sky Blue, 2=Sunset Orange, 3=Cyber Lavender, 4=Obsidian Slate
    val fontFamily: String = "Default", // Sans-serif, Playfair, Monospace
    val layoutDensity: String = "MODERATE", // COMPACT, MODERATE, COMFORTABLE
    val adBlockEnabled: Boolean = true,
    val trackerBlockEnabled: Boolean = true,
    val totalAdsBlocked: Int = 0,
    val totalTrackersBlocked: Int = 0,
    val searchEngine: String = "search.stormx.ninja", // search.stormx.ninja, Google, Bing, Yahoo, DuckDuckGo
    val language: String = "English (US)", // English (US), 简体中文, Español, Deutsch, Français
    val fluidAnimationsEnabled: Boolean = true,
    val speedDialLayout: String = "4x2 Grid", // 4x2 Grid, 3x3 Grid, 5x2 Grid
    // Global defaults for permissions
    val defaultLocation: PermissionState = PermissionState.ASK,
    val defaultCamera: PermissionState = PermissionState.ASK,
    val defaultMicrophone: PermissionState = PermissionState.ASK,
    val defaultNotifications: PermissionState = PermissionState.ASK,
    val defaultPopups: PermissionState = PermissionState.BLOCK,
    val batterySaverModeEnabled: Boolean = false
)

@Entity(tableName = "website_permissions")
data class WebsitePermission(
    @PrimaryKey val domain: String,
    val notifications: PermissionState = PermissionState.ASK,
    val location: PermissionState = PermissionState.ASK,
    val camera: PermissionState = PermissionState.ASK,
    val microphone: PermissionState = PermissionState.ASK,
    val files: PermissionState = PermissionState.ASK,
    val popups: PermissionState = PermissionState.ASK,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface BrowserDao {
    // Tabs
    @Query("SELECT * FROM browser_tabs ORDER BY lastActive DESC")
    fun getAllTabsFlow(): Flow<List<BrowserTab>>

    @Query("SELECT * FROM browser_tabs ORDER BY lastActive DESC")
    suspend fun getAllTabs(): List<BrowserTab>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTab(tab: BrowserTab): Long

    @Update
    suspend fun updateTab(tab: BrowserTab)

    @Delete
    suspend fun deleteTab(tab: BrowserTab)

    @Query("DELETE FROM browser_tabs")
    suspend fun deleteAllTabs()

    // Bookmarks
    @Query("SELECT * FROM bookmarks ORDER BY timestamp DESC")
    fun getAllBookmarksFlow(): Flow<List<Bookmark>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: Bookmark)

    @Query("DELETE FROM bookmarks WHERE url = :url")
    suspend fun deleteBookmarkByUrl(url: String)

    @Query("SELECT EXISTS(SELECT 1 FROM bookmarks WHERE url = :url)")
    fun isBookmarkedFlow(url: String): Flow<Boolean>

    // History
    @Query("SELECT * FROM history_items ORDER BY timestamp DESC LIMIT 200")
    fun getHistoryFlow(): Flow<List<HistoryItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistoryItem(item: HistoryItem)

    @Query("DELETE FROM history_items WHERE id = :id")
    suspend fun deleteHistoryItem(id: Int)

    @Query("DELETE FROM history_items")
    suspend fun clearHistory()

    // Downloads
    @Query("SELECT * FROM downloads ORDER BY timestamp DESC")
    fun getAllDownloadsFlow(): Flow<List<DownloadItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadItem): Long

    @Update
    suspend fun updateDownload(download: DownloadItem)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteDownload(id: Int)

    // Settings
    @Query("SELECT * FROM browser_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<BrowserSettings?>

    @Query("SELECT * FROM browser_settings WHERE id = 1")
    suspend fun getSettings(): BrowserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSettings(settings: BrowserSettings)

    // Website permissions
    @Query("SELECT * FROM website_permissions ORDER BY timestamp DESC")
    fun getAllWebsitePermissionsFlow(): Flow<List<WebsitePermission>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebsitePermission(permission: WebsitePermission)

    @Query("DELETE FROM website_permissions WHERE domain = :domain")
    suspend fun deleteWebsitePermission(domain: String)

    @Query("DELETE FROM website_permissions")
    suspend fun deleteAllWebsitePermissions()
}

@Database(
    entities = [
        BrowserTab::class, 
        Bookmark::class, 
        HistoryItem::class, 
        DownloadItem::class, 
        BrowserSettings::class,
        WebsitePermission::class
    ],
    version = 8,
    exportSchema = false
)
abstract class BrowserDatabase : RoomDatabase() {
    abstract fun browserDao(): BrowserDao

    companion object {
        @Volatile
        private var INSTANCE: BrowserDatabase? = null

        fun getDatabase(context: Context): BrowserDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BrowserDatabase::class.java,
                    "stormx_browser_db"
                )
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

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

    suspend fun addDownload(url: String, fileName: String, filePath: String, totalBytes: Long, mimeType: String?, dmId: Long = -1L): Int {
        val id = dao.insertDownload(
            DownloadItem(
                url = url,
                fileName = fileName,
                filePath = filePath,
                mimeType = mimeType,
                totalBytes = totalBytes,
                downloadedBytes = 0,
                dmId = dmId,
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
    val settingsFlow: Flow<BrowserSettings> = dao.getSettingsFlow().map { 
        val current = it ?: BrowserSettings()
        if (current.homeUrl == "homepage") {
            current.copy(homeUrl = "https://search.stormx.ninja/")
        } else {
            current
        }
    }

    suspend fun getSettings(): BrowserSettings {
        val settings = dao.getSettings() ?: BrowserSettings()
        return if (settings.homeUrl == "homepage") {
            val updated = settings.copy(homeUrl = "https://search.stormx.ninja/")
            dao.insertOrUpdateSettings(updated)
            updated
        } else {
            settings
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
        dao.insertWebsitePermission(WebsitePermission(domain = domain, notifications = if (allowed) PermissionState.ALLOW else PermissionState.BLOCK))
    }

    suspend fun saveWebsitePermission(permission: WebsitePermission) {
        dao.insertWebsitePermission(permission)
    }

    suspend fun removeWebsitePermission(domain: String) {
        dao.deleteWebsitePermission(domain)
    }

    suspend fun clearAllWebsitePermissions() {
        dao.deleteAllWebsitePermissions()
    }
}

