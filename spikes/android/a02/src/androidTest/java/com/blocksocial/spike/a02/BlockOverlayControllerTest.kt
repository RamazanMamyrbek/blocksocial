package com.blocksocial.spike.a02

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.ContextThemeWrapper
import androidx.compose.material3.Text
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class BlockOverlayControllerTest {

    private lateinit var context: Context
    private lateinit var windowManager: FakeWindowManager

    private val youtube = "com.google.android.youtube"
    private val chrome = "com.android.chrome"

    @Before
    fun setUp() {
        context = ContextThemeWrapper(
            InstrumentationRegistry.getInstrumentation().targetContext,
            android.R.style.Theme_Material_NoActionBar
        )
        windowManager = FakeWindowManager()
    }

    private fun controller(
        watchdogTimeoutMillis: Long = 30_000L,
        onDismissed: (String, OverlayDismissReason) -> Unit = { _, _ -> }
    ) = BlockOverlayController(
        context = context,
        windowManager = windowManager,
        handler = Handler(Looper.getMainLooper()),
        watchdogTimeoutMillis = watchdogTimeoutMillis,
        onDismissed = onDismissed
    )

    private fun onMainThread(block: () -> Unit) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    }

    @Test
    fun overlayAttachesAndDetaches() {
        val controller = controller()

        onMainThread {
            controller.show(youtube) { Text("block") }
        }
        assertTrue(controller.isShowing)
        assertEquals(1, windowManager.added.size)
        assertEquals(0, windowManager.removed.size)

        onMainThread {
            controller.dismiss(OverlayDismissReason.STAY_FOCUSED)
        }
        assertFalse(controller.isShowing)
        assertEquals(1, windowManager.removed.size)
        assertEquals(0, windowManager.attachedCount)
    }

    @Test
    fun showingTwiceForTheSamePackageDoesNotStack() {
        val controller = controller()

        onMainThread {
            controller.show(youtube) { Text("block") }
            controller.show(youtube) { Text("block") }
            controller.show(youtube) { Text("block") }
        }

        assertEquals(1, windowManager.added.size)
        assertEquals(1, windowManager.attachedCount)
    }

    @Test
    fun leavingTheBlockedPackageRemovesTheOverlay() {
        val controller = controller()

        onMainThread {
            controller.show(youtube) { Text("block") }
            controller.dismissIfForegroundLeft(chrome)
        }

        assertFalse(controller.isShowing)
        assertEquals(0, windowManager.attachedCount)
    }

    @Test
    fun stayingInTheBlockedPackageKeepsTheOverlay() {
        val controller = controller()

        onMainThread {
            controller.show(youtube) { Text("block") }
            controller.dismissIfForegroundLeft(youtube)
        }

        assertTrue(controller.isShowing)
        assertEquals(1, windowManager.attachedCount)

        onMainThread { controller.dismiss(OverlayDismissReason.SERVICE_STOPPED) }
    }

    @Test
    fun aStuckOverlayIsRemovedByTheWatchdog() {
        val dismissed = CountDownLatch(1)
        var reason: OverlayDismissReason? = null
        val controller = controller(watchdogTimeoutMillis = 500L) { _, dismissReason ->
            reason = dismissReason
            dismissed.countDown()
        }

        onMainThread {
            controller.show(youtube) { Text("block") }
        }

        assertTrue(dismissed.await(10, TimeUnit.SECONDS))
        assertEquals(OverlayDismissReason.WATCHDOG_TIMEOUT, reason)
        assertFalse(controller.isShowing)
        assertEquals(0, windowManager.attachedCount)
    }

    @Test
    fun switchingBlockedPackageReplacesRatherThanStacks() {
        val controller = controller()

        onMainThread {
            controller.show(youtube) { Text("block") }
            controller.show(chrome) { Text("block") }
        }

        assertEquals(2, windowManager.added.size)
        assertEquals(1, windowManager.removed.size)
        assertEquals(1, windowManager.attachedCount)
        assertEquals(chrome, controller.currentPackage)

        onMainThread { controller.dismiss(OverlayDismissReason.SERVICE_STOPPED) }
    }
}
