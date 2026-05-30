package com.example.download

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DownloadTask::class], version = 1, exportSchema = false)
abstract class DownloadDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile private var instance: DownloadDatabase? = null
        fun getInstance(context: Context): DownloadDatabase {
            return instance ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext, DownloadDatabase::class.java, "downloads_tasks_db")
                    .fallbackToDestructiveMigration(true)
                    .build().also { instance = it }
            }
        }
    }
}
