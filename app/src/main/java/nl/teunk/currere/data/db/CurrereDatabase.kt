package nl.teunk.currere.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [RunSessionEntity::class], version = 1, exportSchema = false)
abstract class CurrereDatabase : RoomDatabase() {
    abstract fun runSessionDao(): RunSessionDao

    companion object {
        @Volatile
        private var INSTANCE: CurrereDatabase? = null

        fun getInstance(context: Context): CurrereDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CurrereDatabase::class.java,
                    "currere.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
