package com.loaderapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class UserPreferences(private val context: Context) {
    
    companion object {
        private val CURRENT_USER_ID = longPreferencesKey("current_user_id")
        private val DARK_THEME_KEY = booleanPreferencesKey("dark_theme")
        private val NOTIFICATIONS_KEY = booleanPreferencesKey("notifications_enabled")
        private val SOUND_KEY = booleanPreferencesKey("sound_enabled")
        private val VIBRATION_KEY = booleanPreferencesKey("vibration_enabled")
    }
    
    val currentUserId: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[CURRENT_USER_ID]
    }
    
    val isDarkTheme: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[DARK_THEME_KEY] ?: false
    }
    
    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_KEY] ?: true
    }
    
    val isSoundEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SOUND_KEY] ?: true
    }
    
    val isVibrationEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VIBRATION_KEY] ?: true
    }
    
    suspend fun setCurrentUserId(userId: Long) {
        context.dataStore.edit { preferences ->
            preferences[CURRENT_USER_ID] = userId
        }
    }
    
    suspend fun clearCurrentUser() {
        context.dataStore.edit { preferences ->
            preferences.remove(CURRENT_USER_ID)
        }
    }
    
    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_THEME_KEY] = enabled
        }
    }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_KEY] = enabled
        }
    }
    
    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SOUND_KEY] = enabled
        }
    }
    
    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VIBRATION_KEY] = enabled
        }
    }
}
