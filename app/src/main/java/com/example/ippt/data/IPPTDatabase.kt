package com.example.ippt.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ExerciseEntry::class, RunPathPoint::class], version = 1)
abstract class IPPTDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun runPathDao(): RunPathDao

    companion object {
        @Volatile private var INSTANCE: IPPTDatabase? = null

        fun getDatabase(context: Context): IPPTDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IPPTDatabase::class.java,
                    "ippt_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
