package com.example.minilims.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Patient::class, LabTest::class, PatientRate::class, Sample::class, SampleTest::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patients(): PatientDao
    abstract fun tests(): TestDao
    abstract fun rates(): RateDao
    abstract fun samples(): SampleDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        /** Closes the live database (used right before a restore replaces the file). */
        fun closeAndReset() = synchronized(this) {
            try { instance?.close() } catch (e: Exception) { }
            instance = null
        }

        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "lims.db")
                .build().also { instance = it }
        }
    }
}
