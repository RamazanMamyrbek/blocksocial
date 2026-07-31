package com.blocksocial.shell

import com.blocksocial.core.model.AppRef

sealed interface Destination {

    data object Home : Destination

    data object Today : Destination

    data object History : Destination

    data object Protection : Destination

    data object AddApp : Destination

    data class Rules(val app: AppRef, val displayName: String) : Destination

    companion object {
        val tabs: List<Destination> = listOf(Home, Today, History)
    }
}

val Destination.tab: Destination
    get() = when (this) {
        Destination.Home, Destination.Protection, Destination.AddApp -> Destination.Home
        Destination.Today -> Destination.Today
        Destination.History -> Destination.History
        is Destination.Rules -> Destination.Home
    }

private const val SEPARATOR = "|"

internal fun Destination.encode(): String = when (this) {
    Destination.Home -> "home"
    Destination.Today -> "today"
    Destination.History -> "history"
    Destination.Protection -> "protection"
    Destination.AddApp -> "add-app"
    is Destination.Rules -> listOf("rules", app.value, displayName).joinToString(SEPARATOR)
}

internal fun decodeDestination(encoded: String): Destination {
    val parts = encoded.split(SEPARATOR, limit = 3)
    return when (parts.first()) {
        "today" -> Destination.Today
        "history" -> Destination.History
        "protection" -> Destination.Protection
        "add-app" -> Destination.AddApp
        "rules" -> Destination.Rules(AppRef(parts[1]), parts[2])
        else -> Destination.Home
    }
}
