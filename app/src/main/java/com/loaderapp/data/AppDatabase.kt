package com.loaderapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.loaderapp.data.dao.OrderDao
import com.loaderapp.data.dao.OrderWorkerDao
import com.loaderapp.data.dao.UserDao
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderWorker
import com.loaderapp.data.model.User

@Database(
    entities = [Order::class, User::class, OrderWorker::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun orderDao(): OrderDao
    abstract fun userDao(): UserDao
    abstract fun orderWorkerDao(): OrderWorkerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "loader_app_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
