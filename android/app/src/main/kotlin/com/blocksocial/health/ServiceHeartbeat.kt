package com.blocksocial.health

import java.time.Instant
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceHeartbeat @Inject constructor() {

    private val connectedSince = AtomicReference<Instant?>(null)
    private val lastEventAt = AtomicReference<Instant?>(null)
    private val probeStartedAt = AtomicReference<Instant?>(null)

    val isConnected: Boolean get() = connectedSince.get() != null

    val lastEvent: Instant? get() = lastEventAt.get()

    val probeStarted: Instant? get() = probeStartedAt.get()

    fun onConnected(at: Instant) {
        connectedSince.set(at)
        lastEventAt.set(null)
    }

    fun onDisconnected() {
        connectedSince.set(null)
        lastEventAt.set(null)
    }

    fun onEvent(at: Instant) {
        lastEventAt.set(at)
    }

    fun startProbe(at: Instant) {
        probeStartedAt.set(at)
    }
}
