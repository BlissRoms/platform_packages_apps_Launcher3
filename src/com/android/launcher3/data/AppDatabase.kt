package com.android.launcher3.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SimpleSQLiteQuery

import com.android.launcher3.data.wallpaper.Wallpaper
import com.android.launcher3.data.wallpaper.service.WallpaperDao

@Database(entities = [Wallpaper::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wallpaperDao(): WallpaperDao

    suspend fun checkpoint() {
        wallpaperDao().checkpoint(SimpleSQLiteQuery("pragma wal_checkpoint(full)"))
    }

    companion object {

        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(
                context,
                AppDatabase::class.java,
                "preferences",
            ).build()
    }
}
