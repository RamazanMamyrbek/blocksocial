package com.blocksocial.shell

import com.blocksocial.core.model.AppRef

sealed interface Destination {

    data object Dashboard : Destination

    data object Apps : Destination

    data object History : Destination

    data object Protection : Destination

    data class Rules(val app: AppRef, val displayName: String) : Destination

    companion object {
        val tabs: List<Destination> = listOf(Dashboard, Apps, History)
    }
}

val Destination.tab: Destination
    get() = when (this) {
        Destination.Dashboard, Destination.Protection -> Destination.Dashboard
        Destination.Apps -> Destination.Apps
        Destination.History -> Destination.History
        is Destination.Rules -> Destination.Apps
    }

private const val SEPARATOR = "|"

internal fun Destination.encode(): String = when (this) {
    Destination.Dashboard -> "dashboard"
    Destination.Apps -> "apps"
    Destination.History -> "history"
    Destination.Protection -> "protection"
    is Destination.Rules -> listOf("rules", app.value, displayName).joinToString(SEPARATOR)
}

internal fun decodeDestination(encoded: String): Destination {
    val parts = encoded.split(SEPARATOR, limit = 3)
    return when (parts.first()) {
        "apps" -> Destination.Apps
        "history" -> Destination.History
        "protection" -> Destination.Protection
        "rules" -> Destination.Rules(AppRef(parts[1]), parts[2])
        else -> Destination.Dashboard
    }
}
