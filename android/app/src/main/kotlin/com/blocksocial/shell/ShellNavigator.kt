package com.blocksocial.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable

class ShellNavigator(history: List<Destination> = listOf(Destination.Dashboard)) {

    private val visited = mutableStateListOf<Destination>().apply {
        addAll(history.ifEmpty { listOf(Destination.Dashboard) })
    }

    val current: Destination get() = visited.last()

    val selectedTab: Destination get() = current.tab

    val canGoBack: Boolean get() = visited.size > 1

    fun open(destination: Destination) {
        if (destination == current) return
        visited += destination
    }

    fun selectTab(tab: Destination) {
        visited.clear()
        visited += tab
    }

    fun back(): Boolean {
        if (!canGoBack) return false
        visited.removeAt(visited.lastIndex)
        return true
    }

    fun history(): List<Destination> = visited.toList()
}

@Composable
fun rememberShellNavigator(): ShellNavigator = rememberSaveable(saver = ShellNavigatorSaver) {
    ShellNavigator()
}

private val ShellNavigatorSaver = listSaver<ShellNavigator, String>(
    save = { navigator -> navigator.history().map { it.encode() } },
    restore = { encoded -> ShellNavigator(encoded.map(::decodeDestination)) },
)
