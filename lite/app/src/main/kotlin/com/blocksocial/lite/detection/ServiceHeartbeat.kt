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

    val running: Boolean get() = connectedAtMillis != null

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
