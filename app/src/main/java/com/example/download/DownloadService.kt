package com.example.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import okhttp3.Call
import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

object DownloadHttpClient {
    val client = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .dispatcher(Dispatcher().apply { maxRequestsPerHost = 5 })
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Accept-Encoding", "identity")
                .build()
            chain.proceed(request)
        }
        .build()
}

data class DownloadProfile(val numChunks: Int, val bufferSize: Int)

class DownloadService : Service() {
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val client = DownloadHttpClient.client
    private val activeCalls = mutableMapOf<Int, MutableList<Call>>()
    private val activeJobs = mutableMapOf<Int, Job>()
    
    private lateinit var repository: DownloadRepository
    private lateinit var notificationManager: NotificationManager
    
    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_CANCEL = "ACTION_CANCEL"
        const val EXTRA_ID = "EXTRA_ID"
        const val FOREGROUND_NOTIF_ID = 9999
    }

    override fun onCreate() {
        super.onCreate()
        val dao = DownloadDatabase.getInstance(this).downloadDao()
        repository = DownloadRepository(dao)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        NotificationHelper.createChannels(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val id = intent?.getIntExtra(EXTRA_ID, -1) ?: -1
        
        serviceScope.launch {
            val task = if (id != -1) repository.getTask(id) else null
            val notif = if (task != null) {
                NotificationHelper.buildProgressNotification(this@DownloadService, task)
            } else {
                NotificationCompat.Builder(this@DownloadService, NotificationHelper.CHANNEL_DOWNLOADS)
                    .setContentTitle("Download Manager")
                    .setContentText("Service active")
                    .setSmallIcon(android.R.drawable.stat_sys_download)
                    .build()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(task?.id ?: FOREGROUND_NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(task?.id ?: FOREGROUND_NOTIF_ID, notif)
            }
        }
        
        if (id != -1) {
            when (action) {
                ACTION_START -> startDownload(id)
                ACTION_PAUSE -> pauseDownload(id)
                ACTION_RESUME -> resumeDownload(id)
                ACTION_CANCEL -> cancelDownload(id)
            }
        }
        
        return START_NOT_STICKY
    }

    private fun startDownload(id: Int) {
        val job = serviceScope.launch {
            val task = repository.getTask(id) ?: return@launch
            repository.update(task.copy(status = DownloadStatus.RUNNING))
            downloadFile(task)
        }
        activeJobs[id] = job
    }

    private fun pauseDownload(id: Int) {
        synchronized(activeCalls) {
            activeCalls[id]?.forEach { it.cancel() }
            activeCalls.remove(id)
        }
        activeJobs[id]?.cancel()
        serviceScope.launch {
            val task = repository.getTask(id)
            if (task != null && task.status == DownloadStatus.RUNNING) {
                val updated = task.copy(status = DownloadStatus.PAUSED)
                repository.update(updated)
                updateNotification(updated)
            }
            activeJobs.remove(id)
            if (activeJobs.isEmpty()) {
                stopForeground(true)
            }
        }
    }

    private fun resumeDownload(id: Int) {
        startDownload(id)
    }

    private fun getNetworkProfile(): DownloadProfile {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return DownloadProfile(2, 64 * 1024)
        val caps = cm.getNetworkCapabilities(network) ?: return DownloadProfile(2, 64 * 1024)
        return if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            DownloadProfile(4, 256 * 1024)
        } else {
            DownloadProfile(2, 64 * 1024)
        }
    }
    
    private suspend fun downloadFile(task: DownloadTask) {
        val file = File(task.filePath)
        val profile = getNetworkProfile()
        
        val isResume = task.downloadedBytes > 0 && file.exists()
        
        if (!isResume) {
            try {
                val headRequest = Request.Builder().url(task.url).head().build()
                val headCall = client.newCall(headRequest)
                synchronized(activeCalls) {
                    activeCalls.getOrPut(task.id) { mutableListOf() }.add(headCall)
                }
                val headResponse = headCall.execute()
                val acceptRanges = headResponse.header("Accept-Ranges") == "bytes"
                val contentLength = headResponse.body?.contentLength() ?: -1L
                headResponse.close()
                synchronized(activeCalls) {
                    activeCalls[task.id]?.remove(headCall)
                }

                if (acceptRanges && contentLength > 5 * 1024 * 1024L) {
                    downloadChunksParallel(task, contentLength, profile)
                    return
                }
            } catch (e: Exception) {
            }
        }
        
        downloadSingleStream(task, file, isResume, profile)
    }

    private suspend fun downloadChunksParallel(task: DownloadTask, totalBytes: Long, profile: DownloadProfile) {
        val chunkSize = totalBytes / profile.numChunks
        repository.update(task.copy(totalBytes = totalBytes))
        
        val downloadedCounter = AtomicLong(0)
        var lastUpdate = System.currentTimeMillis()
        var lastProgressPercent = 0
        
        val chunkJobs = mutableListOf<Deferred<Boolean>>()
        val partFiles = mutableListOf<File>()
        
        var isCancelled = false
        
        coroutineScope {
            for (i in 0 until profile.numChunks) {
                val startByte = i * chunkSize
                val endByte = if (i == profile.numChunks - 1) totalBytes - 1 else startByte + chunkSize - 1
                val partFile = File(task.filePath + ".part$i")
                partFiles.add(partFile)
                
                val job = async(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(task.url)
                        .header("Range", "bytes=$startByte-$endByte")
                        .build()
                        
                    val call = client.newCall(request)
                    synchronized(activeCalls) {
                        activeCalls.getOrPut(task.id) { mutableListOf() }.add(call)
                    }
                    
                    try {
                        val response = call.execute()
                        if (!response.isSuccessful) return@async false
                        
                        val inputStream = response.body?.byteStream() ?: return@async false
                        val outputStream = BufferedOutputStream(FileOutputStream(partFile, false), profile.bufferSize)
                        
                        val buffer = ByteArray(profile.bufferSize)
                        var bytes = inputStream.read(buffer)
                        var bytesSinceFlush = 0L
                        
                        while (bytes >= 0) {
                            outputStream.write(buffer, 0, bytes)
                            bytesSinceFlush += bytes
                            
                            val currentDownloaded = downloadedCounter.addAndGet(bytes.toLong())
                            
                            if (bytesSinceFlush >= 2 * 1024 * 1024L) {
                                outputStream.flush()
                                bytesSinceFlush = 0L
                            }
                            
                            val now = System.currentTimeMillis()
                            val progressPercent = (currentDownloaded * 100 / totalBytes).toInt()
                            
                            if (progressPercent > lastProgressPercent || now - lastUpdate > 500) {
                                val updated = repository.getTask(task.id)!!.copy(downloadedBytes = currentDownloaded)
                                repository.update(updated)
                                updateNotification(updated)
                                lastUpdate = now
                                lastProgressPercent = progressPercent
                            }
                            
                            if (!kotlin.coroutines.coroutineContext.isActive || call.isCanceled()) {
                                isCancelled = true
                                break
                            }
                            
                            bytes = inputStream.read(buffer)
                        }
                        
                        outputStream.flush()
                        outputStream.close()
                        inputStream.close()
                        
                        !isCancelled
                    } catch (e: Exception) {
                        false
                    } finally {
                        synchronized(activeCalls) {
                            activeCalls[task.id]?.remove(call)
                        }
                    }
                }
                chunkJobs.add(job)
            }
        }
        
        val results = chunkJobs.awaitAll()
        val allSuccess = results.all { it }
        
        if (isCancelled || !allSuccess) {
            partFiles.forEach { it.delete() }
            if (isCancelled) {
                repository.update(repository.getTask(task.id)!!.copy(downloadedBytes = 0L))
            } else {
                val updated = repository.getTask(task.id)!!.copy(status = DownloadStatus.FAILED, downloadedBytes = 0L)
                repository.update(updated)
                updateNotification(updated)
            }
            cleanupJob(task.id)
            return
        }
        
        try {
            val finalFile = File(task.filePath)
            val finalOut = BufferedOutputStream(FileOutputStream(finalFile, false), profile.bufferSize)
            
            for (part in partFiles) {
                val partIn = FileInputStream(part)
                val buffer = ByteArray(profile.bufferSize)
                var bytes = partIn.read(buffer)
                while (bytes >= 0) {
                    finalOut.write(buffer, 0, bytes)
                    bytes = partIn.read(buffer)
                }
                partIn.close()
                part.delete()
            }
            finalOut.flush()
            finalOut.close()
            
            val updated = repository.getTask(task.id)!!.copy(downloadedBytes = totalBytes, status = DownloadStatus.COMPLETED)
            repository.update(updated)
            updateNotification(updated)
            
        } catch (e: Exception) {
            val updated = repository.getTask(task.id)!!.copy(status = DownloadStatus.FAILED)
            repository.update(updated)
            updateNotification(updated)
        } finally {
            cleanupJob(task.id)
        }
    }
    
    private suspend fun downloadSingleStream(task: DownloadTask, file: File, isResume: Boolean, profile: DownloadProfile) {
        val requestBuilder = Request.Builder().url(task.url)
        
        if (isResume) {
            requestBuilder.addHeader("Range", "bytes=${task.downloadedBytes}-")
        }
        
        val call = client.newCall(requestBuilder.build())
        synchronized(activeCalls) {
            activeCalls.getOrPut(task.id) { mutableListOf() }.add(call)
        }
        
        try {
            val response = call.execute()
            if (!response.isSuccessful) {
                val updated = task.copy(status = DownloadStatus.FAILED)
                repository.update(updated)
                updateNotification(updated)
                return
            }
            
            val contentLength = response.body?.contentLength() ?: 0L
            val totalBytes = if (isResume) task.totalBytes else (task.downloadedBytes + contentLength)
            repository.update(task.copy(totalBytes = totalBytes))
            
            val inputStream = response.body?.byteStream() ?: return
            val outputStream = BufferedOutputStream(FileOutputStream(file, isResume), profile.bufferSize)
            
            var bytesCopied = task.downloadedBytes
            var lastUpdate = System.currentTimeMillis()
            var lastProgressPercent = if (totalBytes > 0) (bytesCopied * 100 / totalBytes).toInt() else 0
            
            val buffer = ByteArray(profile.bufferSize)
            var bytes = inputStream.read(buffer)
            var bytesSinceFlush = 0L
            var isCancelled = false
            
            while (bytes >= 0) {
                outputStream.write(buffer, 0, bytes)
                bytesCopied += bytes
                bytesSinceFlush += bytes
                
                if (bytesSinceFlush >= 2 * 1024 * 1024L) {
                    outputStream.flush()
                    bytesSinceFlush = 0L
                }
                
                val now = System.currentTimeMillis()
                val progressPercent = if (totalBytes > 0) (bytesCopied * 100 / totalBytes).toInt() else 0
                
                if (progressPercent > lastProgressPercent || now - lastUpdate > 500) {
                    val updated = repository.getTask(task.id)!!.copy(downloadedBytes = bytesCopied)
                    repository.update(updated)
                    updateNotification(updated)
                    lastUpdate = now
                    lastProgressPercent = progressPercent
                }
                
                if (!kotlin.coroutines.coroutineContext.isActive || call.isCanceled()) {
                    isCancelled = true
                    break
                }
                
                bytes = inputStream.read(buffer)
            }
            
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            
            if (!isCancelled) {
                val updated = repository.getTask(task.id)!!.copy(downloadedBytes = bytesCopied, status = DownloadStatus.COMPLETED)
                repository.update(updated)
                updateNotification(updated)
            }
            
        } catch (e: Exception) {
            if (!call.isCanceled()) {
                val updated = repository.getTask(task.id)!!.copy(status = DownloadStatus.FAILED)
                repository.update(updated)
                updateNotification(updated)
            }
        } finally {
            synchronized(activeCalls) {
                activeCalls[task.id]?.remove(call)
                if (activeCalls[task.id]?.isEmpty() == true) {
                    activeCalls.remove(task.id)
                }
            }
            cleanupJob(task.id)
        }
    }
    
    private fun cleanupJob(id: Int) {
        activeJobs.remove(id)
        if (activeJobs.isEmpty()) {
            stopForeground(true)
        }
    }
    
    private suspend fun updateNotification(task: DownloadTask) {
        val notif = when (task.status) {
            DownloadStatus.COMPLETED -> {
                notificationManager.cancel(task.id)
                NotificationHelper.buildCompleteNotification(this, task).also {
                    notificationManager.notify(task.id, it)
                }
            }
            DownloadStatus.FAILED -> {
                notificationManager.cancel(task.id)
                NotificationHelper.buildFailedNotification(this, task).also {
                    notificationManager.notify(task.id, it)
                }
            }
            else -> {
                NotificationHelper.buildProgressNotification(this, task).also {
                    notificationManager.notify(task.id, it)
                }
            }
        }
    }

    private fun cancelDownload(id: Int) {
        synchronized(activeCalls) {
            activeCalls[id]?.forEach { it.cancel() }
            activeCalls.remove(id)
        }
        activeJobs[id]?.cancel()
        serviceScope.launch {
            val task = repository.getTask(id)
            if (task != null) {
                repository.delete(task)
                val file = File(task.filePath)
                if (file.exists()) file.delete()
                for (i in 0 until 4) {
                    val part = File(task.filePath + ".part$i")
                    if (part.exists()) part.delete()
                }
                notificationManager.cancel(task.id)
            }
            activeJobs.remove(id)
            if (activeJobs.isEmpty()) {
                stopForeground(true)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

