package nl.teunk.currere.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper

@Database(
    entities = [
        RunSessionEntity::class,
        HeartRateSampleEntity::class,
        PaceSampleEntity::class,
        PaceSplitEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class CurrereDatabase : RoomDatabase() {
    abstract fun runSessionDao(): RunSessionDao
    abstract fun runDetailDao(): RunDetailDao

    companion object {
        @Volatile
        private var INSTANCE: CurrereDatabase? = null

        fun getInstance(
            context: Context,
            factory: SupportSQLiteOpenHelper.Factory? = null,
        ): CurrereDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CurrereDatabase::class.java,
                    "currere.db",
                ).apply {
                    if (factory != null) openHelperFactory(factory)
                }.build().also { INSTANCE = it }
            }
        }
    }
}
