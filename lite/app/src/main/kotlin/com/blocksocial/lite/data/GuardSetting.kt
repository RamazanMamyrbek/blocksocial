package com.blocksocial.lite.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.guardDataStore: DataStore<Preferences> by preferencesDataStore(name = "guard")

class GuardSetting(context: Context) {

    private val store = context.applicationContext.guardDataStore

    val enabled: Flow<Boolean> = store.data.map { preferences ->
        preferences[ENABLED] ?: DEFAULT_ENABLED
    }

    suspend fun setEnabled(enabled: Boolean) {
        store.edit { preferences -> preferences[ENABLED] = enabled }
    }

    companion object {
        const val DEFAULT_ENABLED = true

        private val ENABLED = booleanPreferencesKey("guard.enabled")
    }
}
