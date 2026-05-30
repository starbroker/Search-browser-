package com.example.download

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_tasks ORDER BY id DESC")
    fun getAllDownloads(): Flow<List<DownloadTask>>

    @Insert
    suspend fun insertTask(task: DownloadTask): Long

    @Update
    suspend fun updateTask(task: DownloadTask)
    
    @Query("DELETE FROM download_tasks WHERE id = :id")
    suspend fun deleteTask(id: Int)
    
    @Query("SELECT * FROM download_tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): DownloadTask?
}
