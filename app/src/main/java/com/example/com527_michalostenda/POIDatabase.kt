package com.example.com527_michalostenda

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = arrayOf(POIdata::class), version = 2, exportSchema = false)
public abstract class POIDatabase: RoomDatabase() {
    abstract fun PointofintrestsDao(): pointofintrestsDao

    companion object {
        @Volatile
        private var instance: POIDatabase? = null

        fun getDatabase(ctx:Context) : POIDatabase {
            var tmpInstance = instance
            if(tmpInstance == null) {
                tmpInstance = Room.databaseBuilder(
                    ctx.applicationContext,
                    POIDatabase::class.java,
                    "poiDatabase"
                ).fallbackToDestructiveMigration().build()
                instance = tmpInstance
            }
            return tmpInstance
        }
    }
}