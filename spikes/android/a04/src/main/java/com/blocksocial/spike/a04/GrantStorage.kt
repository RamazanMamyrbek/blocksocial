package com.blocksocial.spike.a04

import android.content.Context
import android.content.SharedPreferences

interface GrantStorage {
    fun readAll(): List<TemporaryAccessGrant>
    fun write(grant: TemporaryAccessGrant)
    fun remove(packageName: String)
}

class SharedPreferencesGrantStorage(context: Context) : GrantStorage {

    private val preferences: SharedPreferences =
        context.getSharedPreferences("temporary_access_grants", Context.MODE_PRIVATE)

    override fun readAll(): List<TemporaryAccessGrant> =
        preferences.all.entries.mapNotNull { (packageName, encoded) ->
            (encoded as? String)?.let { decode(packageName, it) }
        }

    override fun write(grant: TemporaryAccessGrant) {
        preferences.edit().putString(grant.packageName, encode(grant)).apply()
    }

    override fun remove(packageName: String) {
        preferences.edit().remove(packageName).apply()
    }

    private fun encode(grant: TemporaryAccessGrant) = listOf(
        grant.grantedAtWallClockMillis,
        grant.expiresAtWallClockMillis,
        grant.grantedAtElapsedRealtimeMillis,
        grant.expiresAtElapsedRealtimeMillis
    ).joinToString(SEPARATOR)

    private fun decode(packageName: String, encoded: String): TemporaryAccessGrant? {
        val parts = encoded.split(SEPARATOR).mapNotNull(String::toLongOrNull)
        if (parts.size != 4) {
            return null
        }
        return TemporaryAccessGrant(
            packageName = packageName,
            grantedAtWallClockMillis = parts[0],
            expiresAtWallClockMillis = parts[1],
            grantedAtElapsedRealtimeMillis = parts[2],
            expiresAtElapsedRealtimeMillis = parts[3]
        )
    }

    private companion object {
        const val SEPARATOR = "|"
    }
}
