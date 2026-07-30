package com.blocksocial.detection

import com.blocksocial.core.data.repository.BlockEventRepository
import com.blocksocial.core.data.repository.TemporaryAccessGrantRepository
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.TemporaryAccessGrant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WriteScope

@Singleton
class DurableWrites @Inject constructor(
    private val blockEvents: BlockEventRepository,
    private val grants: TemporaryAccessGrantRepository,
    @WriteScope private val scope: CoroutineScope,
) {

    fun record(event: BlockEvent) = write("record-failed") { blockEvents.record(event) }

    fun keep(grant: TemporaryAccessGrant) = write("grant-failed") { grants.put(grant) }

    fun forget(app: AppRef) = write("grant-clear-failed") { grants.clear(app) }

    private fun write(failure: String, body: suspend () -> Unit) {
        scope.launch {
            runCatching { body() }
                .onFailure { DetectionLog.lifecycle(failure, it.javaClass.simpleName) }
        }
    }
}
