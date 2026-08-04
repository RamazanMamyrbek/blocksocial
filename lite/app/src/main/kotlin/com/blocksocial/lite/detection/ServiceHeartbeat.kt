package com.blocksocial.lite.detection

class ServiceHeartbeat {

    @Volatile
    private var owner: Any? = null

    @Volatile
    var connectedAtMillis: Long? = null
        private set

    @Volatile
    var lastEventAtMillis: Long? = null
        private set

    @Volatile
    var lastOverlayFailure: String? = null
        private set

    fun answering(nowMillis: Long): Boolean {
        val connected = connectedAtMillis ?: return false
        val lastHeard = lastEventAtMillis ?: connected
        return nowMillis - lastHeard < SILENCE_THAT_MEANS_STOPPED_MILLIS
    }

    companion object {
        const val SILENCE_THAT_MEANS_STOPPED_MILLIS = 60_000L
    }

    fun onConnected(owner: Any, nowMillis: Long) {
        this.owner = owner
        connectedAtMillis = nowMillis
        lastEventAtMillis = null
        lastOverlayFailure = null
    }

    fun onEvent(nowMillis: Long) {
        if (connectedAtMillis == null) connectedAtMillis = nowMillis
        lastEventAtMillis = nowMillis
    }

    fun onOverlayFailed(reason: String) {
        lastOverlayFailure = reason
    }

    fun onDisconnected(owner: Any) {
        if (this.owner !== owner) return
        this.owner = null
        connectedAtMillis = null
        lastEventAtMillis = null
    }
}
