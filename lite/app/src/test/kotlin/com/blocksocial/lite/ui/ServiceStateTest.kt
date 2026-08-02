package com.blocksocial.lite.ui

import com.blocksocial.lite.detection.ServiceHeartbeat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ServiceStateTest {

    @Test
    fun aSwitchThatIsOnDoesNotByItselfMeanTheServiceIsRunning() {
        assertEquals(
            ServiceState.SWITCHED_ON_BUT_STOPPED,
            serviceStateOf(switchedOn = true, running = false),
        )
    }

    @Test
    fun onlyAnAnsweringServiceCountsAsWorking() {
        assertEquals(ServiceState.WORKING, serviceStateOf(switchedOn = true, running = true))
    }

    @Test
    fun aSwitchThatIsOffIsSaidToBeOff() {
        assertEquals(ServiceState.OFF, serviceStateOf(switchedOn = false, running = false))
    }

    @Test
    fun theHeartbeatStartsSilentAndSpeaksOnlyOnceTheServiceConnects() {
        val heartbeat = ServiceHeartbeat()
        val service = Any()

        assertFalse(heartbeat.running)

        heartbeat.onConnected(service, 1_000L)
        assertTrue(heartbeat.running)

        heartbeat.onDisconnected(service)
        assertFalse(heartbeat.running)
    }

    @Test
    fun anOldInstanceShuttingDownLateDoesNotSilenceTheOneThatReplacedIt() {
        val heartbeat = ServiceHeartbeat()
        val stopping = Any()
        val started = Any()

        heartbeat.onConnected(stopping, 1_000L)
        heartbeat.onConnected(started, 2_000L)
        heartbeat.onDisconnected(stopping)

        assertTrue(heartbeat.running)
        assertEquals(2_000L, heartbeat.connectedAtMillis)
    }

    @Test
    fun anOverlayThatCouldNotBeDrawnIsRememberedUntilTheServiceReconnects() {
        val heartbeat = ServiceHeartbeat()
        val service = Any()
        heartbeat.onConnected(service, 1_000L)

        heartbeat.onOverlayFailed("BadTokenException")
        assertEquals("BadTokenException", heartbeat.lastOverlayFailure)

        heartbeat.onConnected(service, 2_000L)
        assertEquals(null, heartbeat.lastOverlayFailure)
    }
}
