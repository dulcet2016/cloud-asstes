package com.manha.eventassettracker.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "eat_settings")

data class AppSession(
    val role: String = "", // "admin" or "staff" or "" (not logged in)
    val id: String = "",
    val name: String = ""
) {
    val isLoggedIn: Boolean get() = role.isNotEmpty()
    val isAdmin: Boolean get() = role == ROLE_ADMIN

    companion object {
        const val ROLE_ADMIN = "admin"
        const val ROLE_STAFF = "staff"
    }
}

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val SESSION_ROLE = stringPreferencesKey("session_role")
        val SESSION_ID = stringPreferencesKey("session_id")
        val SESSION_NAME = stringPreferencesKey("session_name")
        val ACTIVE_EVENT_ID = stringPreferencesKey("active_event_id")
        val ACTIVE_EVENT_NAME = stringPreferencesKey("active_event_name")
        val SCAN_MODE = stringPreferencesKey("scan_mode")
        val DEVICE_NAME = stringPreferencesKey("device_name")
    }

    val session: Flow<AppSession> = context.dataStore.data.map { prefs ->
        AppSession(
            role = prefs[Keys.SESSION_ROLE] ?: "",
            id = prefs[Keys.SESSION_ID] ?: "",
            name = prefs[Keys.SESSION_NAME] ?: ""
        )
    }

    val activeEventId: Flow<String> = context.dataStore.data.map { it[Keys.ACTIVE_EVENT_ID] ?: "" }
    val activeEventName: Flow<String> = context.dataStore.data.map { it[Keys.ACTIVE_EVENT_NAME] ?: "" }
    val scanMode: Flow<String> = context.dataStore.data.map { it[Keys.SCAN_MODE] ?: "OUT" }
    val deviceName: Flow<String> = context.dataStore.data.map { it[Keys.DEVICE_NAME] ?: "" }

    suspend fun setDeviceName(name: String) {
        context.dataStore.edit { prefs -> prefs[Keys.DEVICE_NAME] = name }
    }

    suspend fun setSession(role: String, id: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SESSION_ROLE] = role
            prefs[Keys.SESSION_ID] = id
            prefs[Keys.SESSION_NAME] = name
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.SESSION_ROLE)
            prefs.remove(Keys.SESSION_ID)
            prefs.remove(Keys.SESSION_NAME)
        }
    }

    suspend fun setActiveEvent(id: String, name: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_EVENT_ID] = id
            prefs[Keys.ACTIVE_EVENT_NAME] = name
        }
    }

    suspend fun setScanMode(mode: String) {
        context.dataStore.edit { prefs -> prefs[Keys.SCAN_MODE] = mode }
    }
}
