package com.blocksocial.lite.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.limitDataStore: DataStore<Preferences> by preferencesDataStore(name = "limits")

data class Limit(
    val minutes: Int,
    val countingFromMillis: Long,
)

class LimitStore(context: Context) {

    private val store = context.applicationContext.limitDataStore

    val limits: Flow<Map<String, Limit>> = store.data.map { preferences ->
        preferences.asMap()
            .mapNotNull { (key, value) ->
                val app = key.name.removeSuffix(MINUTES_SUFFIX)
                if (app == key.name) return@mapNotNull null
                val minutes = value as? Int ?: return@mapNotNull null
                if (minutes !in MINIMUM_MINUTES..MAXIMUM_MINUTES) return@mapNotNull null
                app to Limit(
                    minutes = minutes,
                    countingFromMillis = preferences[countingFromKey(app)] ?: 0L,
                )
            }
            .toMap()
    }

    suspend fun setLimit(app: String, minutes: Int, nowMillis: Long) {
        store.edit { preferences ->
            preferences[minutesKey(app)] = minutes.coerceIn(MINIMUM_MINUTES, MAXIMUM_MINUTES)
            preferences[countingFromKey(app)] = nowMillis
        }
    }

    suspend fun removeLimit(app: String) {
        store.edit { preferences ->
            preferences.remove(minutesKey(app))
            preferences.remove(countingFromKey(app))
        }
    }

    private fun minutesKey(app: String) = intPreferencesKey("$app$MINUTES_SUFFIX")

    private fun countingFromKey(app: String) = longPreferencesKey("$app$COUNTING_FROM_SUFFIX")

    companion object {
        const val MINIMUM_MINUTES = 1
        const val MAXIMUM_MINUTES = 12 * 60
        const val DEFAULT_MINUTES = 30

        private const val MINUTES_SUFFIX = ".minutes"
        private const val COUNTING_FROM_SUFFIX = ".countingFrom"
    }
}
