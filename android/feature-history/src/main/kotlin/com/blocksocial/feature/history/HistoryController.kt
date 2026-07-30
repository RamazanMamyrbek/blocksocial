package com.blocksocial.feature.history

import com.blocksocial.core.data.repository.BlockEventRepository
import com.blocksocial.core.data.repository.RestrictedAppRepository
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BlockEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

data class HistoryPage(
    val events: List<BlockEvent>,
    val displayNames: Map<AppRef, String>,
) {
    fun displayNameOf(event: BlockEvent): String =
        displayNames[event.restrictedAppRef] ?: event.restrictedAppRef.value

    companion object {
        val Empty = HistoryPage(emptyList(), emptyMap())
    }
}

@Singleton
class HistoryController @Inject constructor(
    private val blockEvents: BlockEventRepository,
    private val restrictedApps: RestrictedAppRepository,
) {

    fun page(limit: Int = DEFAULT_LIMIT): Flow<HistoryPage> = combine(
        blockEvents.observeRecent(limit),
        restrictedApps.observeAll(),
    ) { events, apps ->
        HistoryPage(events = events, displayNames = apps.associate { it.ref to it.displayName })
    }

    private companion object {
        const val DEFAULT_LIMIT = 200
    }
}
