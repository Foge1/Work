package com.loaderapp

import android.app.Application
import com.loaderapp.data.AppDatabase
import com.loaderapp.data.preferences.UserPreferences
import com.loaderapp.data.repository.AppRepository

class LoaderApplication : Application() {
    
    lateinit var database: AppDatabase
        private set
    
    lateinit var repository: AppRepository
        private set
    
    lateinit var userPreferences: UserPreferences
        private set
    
    override fun onCreate() {
        super.onCreate()
        
        database = AppDatabase.getDatabase(this)
        repository = AppRepository(
            orderDao = database.orderDao(),
            userDao = database.userDao()
        )
        userPreferences = UserPreferences(this)
    }
}
