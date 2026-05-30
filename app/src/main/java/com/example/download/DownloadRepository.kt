package com.example.download

import kotlinx.coroutines.flow.Flow

class DownloadRepository(private val dao: DownloadDao) {
    val downloads: Flow<List<DownloadTask>> = dao.getAllDownloads()

    suspend fun insert(task: DownloadTask): Int {
        return dao.insertTask(task).toInt()
    }

    suspend fun update(task: DownloadTask) {
        dao.updateTask(task)
    }

    suspend fun delete(task: DownloadTask) {
        dao.deleteTask(task.id)
    }

    suspend fun getTask(id: Int): DownloadTask? = dao.getTaskById(id)
}
