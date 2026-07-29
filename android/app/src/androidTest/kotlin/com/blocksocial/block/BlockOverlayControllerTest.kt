package com.blocksocial.block

import android.os.Handler
import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.UserAction
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

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val instagram = AppRef("instagram")
    private val tiktok = AppRef("tiktok")

    private lateinit var windowManager: FakeWindowManager
    private lateinit var dismissals: MutableList<Pair<AppRef, BlockOutcome>>

    @Before
    fun reset() {
        windowManager = FakeWindowManager()
        dismissals = mutableListOf()
    }

    private fun controller(watchdogTimeoutMillis: Long = 60_000L): BlockOverlayController {
        lateinit var created: BlockOverlayController
        instrumentation.runOnMainSync {
            created = BlockOverlayController(
                context = instrumentation.targetContext,
                windowManager = windowManager,
                handler = Handler(Looper.getMainLooper()),
                watchdogTimeoutMillis = watchdogTimeoutMillis,
                onDismissed = { app, outcome -> dismissals += app to outcome },
            )
        }
        return created
    }

    private fun BlockOverlayController.showOnMain(app: AppRef) {
        instrumentation.runOnMainSync { show(app) { } }
    }

    private fun BlockOverlayController.dismissOnMain(outcome: BlockOutcome) {
        instrumentation.runOnMainSync { dismiss(outcome) }
    }

    @Test
    fun everyDismissalPathDetachesTheOverlay() {
        BlockOutcome.entries.forEach { outcome ->
            reset()
            val controller = controller()
            controller.showOnMain(instagram)
            assertEquals("$outcome did not attach", 1, windowManager.attachedCount)

            controller.dismissOnMain(outcome)

            assertEquals("$outcome left a window behind", 0, windowManager.attachedCount)
            assertFalse(controller.isShowing)
            assertEquals(listOf(instagram to outcome), dismissals)
        }
    }

    @Test
    fun showingTwiceForTheSameApplicationDoesNotStackWindows() {
        val controller = controller()

        controller.showOnMain(instagram)
        controller.showOnMain(instagram)

        assertEquals(1, windowManager.attachedCount)
        assertEquals(emptyList<Pair<AppRef, BlockOutcome>>(), dismissals)
    }

    @Test
    fun aDifferentApplicationReplacesTheWindowRatherThanStacking() {
        val controller = controller()

        controller.showOnMain(instagram)
        controller.showOnMain(tiktok)

        assertEquals(1, windowManager.attachedCount)
        assertEquals(listOf(instagram to BlockOutcome.FOREGROUND_MOVED_ON), dismissals)
        assertEquals(tiktok, controller.currentApp)
    }

    @Test
    fun theForegroundMovingElsewhereRemovesTheOverlay() {
        val controller = controller()
        controller.showOnMain(instagram)

        instrumentation.runOnMainSync { controller.dismissIfForegroundLeft(tiktok) }

        assertEquals(0, windowManager.attachedCount)
        assertEquals(listOf(instagram to BlockOutcome.FOREGROUND_MOVED_ON), dismissals)
    }

    @Test
    fun theForegroundStayingPutKeepsTheOverlay() {
        val controller = controller()
        controller.showOnMain(instagram)

        instrumentation.runOnMainSync { controller.dismissIfForegroundLeft(instagram) }

        assertEquals(1, windowManager.attachedCount)
        assertTrue(controller.isShowing)
    }

    @Test
    fun theWatchdogRemovesAnOverlayNobodyDismissed() {
        val controller = controller(watchdogTimeoutMillis = 250L)
        controller.showOnMain(instagram)

        val settled = CountDownLatch(1)
        Handler(Looper.getMainLooper()).postDelayed({ settled.countDown() }, 1_500L)
        assertTrue(settled.await(5, TimeUnit.SECONDS))

        assertEquals(0, windowManager.attachedCount)
        assertEquals(listOf(instagram to BlockOutcome.WATCHDOG_TIMEOUT), dismissals)
    }

    @Test
    fun dismissingWhenNothingIsShowingIsHarmless() {
        val controller = controller()

        controller.dismissOnMain(BlockOutcome.STAYED_FOCUSED)

        assertEquals(0, windowManager.attachedCount)
        assertEquals(emptyList<Pair<AppRef, BlockOutcome>>(), dismissals)
    }

    @Test
    fun backIsRecordedAsADecisionRatherThanASilentEscape() {
        assertEquals(UserAction.STAYED_FOCUSED, BlockOutcome.BACK_PRESSED.userAction)
        assertEquals(UserAction.BYPASSED, BlockOutcome.OPENED_TEMPORARILY.userAction)
        assertEquals(UserAction.DISMISSED_BY_SYSTEM, BlockOutcome.WATCHDOG_TIMEOUT.userAction)
    }
}
