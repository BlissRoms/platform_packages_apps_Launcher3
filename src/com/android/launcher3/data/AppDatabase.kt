package com.android.launcher3.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery

import com.android.launcher3.data.wallpaper.Wallpaper
import com.android.launcher3.data.wallpaper.service.WallpaperDao

import kotlinx.coroutines.runBlocking

@Database(entities = [Wallpaper::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wallpaperDao(): WallpaperDao

    suspend fun checkpoint() {
        wallpaperDao().checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
    }

    fun checkpointSync() {
        runBlocking {
            checkpoint()
        }
    }

    class DatabaseHolder {
        @Volatile
        private var db: AppDatabase? = null

        fun get(context: Context): AppDatabase {
            return db ?: synchronized(this) {
                db ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "preferences",
                ).build().also { db = it }
            }
        }
    }

    companion object {
        @JvmField
        val INSTANCE = DatabaseHolder()
    }
}
