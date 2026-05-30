package com.example.download

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus {
    QUEUED, RUNNING, PAUSED, COMPLETED, FAILED
}

@Entity(tableName = "download_tasks")
data class DownloadTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val fileName: String,
    val filePath: String,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val status: DownloadStatus = DownloadStatus.QUEUED,
    val mimeType: String? = null
)
