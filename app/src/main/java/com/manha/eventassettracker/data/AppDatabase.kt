package com.manha.eventassettracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.manha.eventassettracker.data.dao.AdminDao
import com.manha.eventassettracker.data.dao.AssetDao
import com.manha.eventassettracker.data.dao.EventDao
import com.manha.eventassettracker.data.dao.QrLabelDao
import com.manha.eventassettracker.data.dao.ScanDao
import com.manha.eventassettracker.data.dao.StaffDao
import com.manha.eventassettracker.data.entity.AdminEntity
import com.manha.eventassettracker.data.entity.AssetEntity
import com.manha.eventassettracker.data.entity.EventEntity
import com.manha.eventassettracker.data.entity.QrLabelEntity
import com.manha.eventassettracker.data.entity.ScanEntity
import com.manha.eventassettracker.data.entity.StaffEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

const val DEFAULT_ADMIN_USERNAME = "admin"
const val DEFAULT_ADMIN_PASSWORD = "admin123"

@Database(
    entities = [
        AdminEntity::class,
        StaffEntity::class,
        EventEntity::class,
        AssetEntity::class,
        ScanEntity::class,
        QrLabelEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun adminDao(): AdminDao
    abstract fun staffDao(): StaffDao
    abstract fun eventDao(): EventDao
    abstract fun assetDao(): AssetDao
    abstract fun scanDao(): ScanDao
    abstract fun qrLabelDao(): QrLabelDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "event_asset_tracker.db"
                ).addCallback(object : RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed one default Admin account (admin / admin123) so the app is usable
                        // on a completely fresh install with zero setup.
                        CoroutineScope(Dispatchers.IO).launch {
                            val instance = INSTANCE
                            instance?.adminDao()?.insert(
                                AdminEntity(
                                    id = "admin-default",
                                    name = "Admin",
                                    username = DEFAULT_ADMIN_USERNAME,
                                    password = DEFAULT_ADMIN_PASSWORD,
                                    note = "Default account",
                                    isDefault = true
                                )
                            )
                        }
                    }
                }).build().also { INSTANCE = it }
            }
        }
    }
}
