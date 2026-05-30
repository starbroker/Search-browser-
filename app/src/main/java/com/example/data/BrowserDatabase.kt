package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

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
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "browser_settings")
data class BrowserSettings(
    @PrimaryKey val id: Int = 1,
    val homeUrl: String = "https://google.com/",
    val themeMode: String = "SYSTEM", // LIGHT, DARK, SYSTEM
    val customThemeColor: Int = 0, // 0 means default ColorOS 16 Emerald Green, 1=Sky Blue, 2=Sunset Orange, 3=Cyber Lavender, 4=Obsidian Slate
    val fontFamily: String = "Default", // Sans-serif, Playfair, Monospace
    val layoutDensity: String = "MODERATE", // COMPACT, MODERATE, COMFORTABLE
    val adBlockEnabled: Boolean = true,
    val trackerBlockEnabled: Boolean = true,
    val totalAdsBlocked: Int = 0,
    val totalTrackersBlocked: Int = 0,
    val searchEngine: String = "google.com", // google.com, Google, Bing, Yahoo, DuckDuckGo
    val language: String = "English (US)", // English (US), 简体中文, Español, Deutsch, Français
    val fluidAnimationsEnabled: Boolean = true,
    val speedDialLayout: String = "4x2 Grid" // 4x2 Grid, 3x3 Grid, 5x2 Grid
)

@Entity(tableName = "site_permissions")
data class SitePermission(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val origin: String,
    val permissionType: String,
    val isGranted: Boolean,
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
    @Query("SELECT * FROM site_permissions ORDER BY origin ASC")
    fun getAllSitePermissionsFlow(): Flow<List<SitePermission>>

    @Query("SELECT * FROM site_permissions WHERE origin = :origin AND permissionType = :type LIMIT 1")
    suspend fun getSitePermission(origin: String, type: String): SitePermission?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSitePermission(permission: SitePermission)

    @Update
    suspend fun updateSitePermission(permission: SitePermission)

    @Query("DELETE FROM site_permissions WHERE id = :id")
    suspend fun deleteSitePermission(id: Int)

    @Query("DELETE FROM site_permissions WHERE origin = :origin")
    suspend fun deleteSitePermissionsByOrigin(origin: String)

    @Query("DELETE FROM site_permissions")
    suspend fun deleteAllSitePermissions()
}

@Database(
    entities = [
        BrowserTab::class, 
        Bookmark::class, 
        HistoryItem::class, 
        DownloadItem::class, 
        BrowserSettings::class,
        SitePermission::class
    ],
    version = 5,
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
                    "search_browser_db"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
