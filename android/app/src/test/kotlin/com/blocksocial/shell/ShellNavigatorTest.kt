package com.blocksocial.shell

import com.blocksocial.core.model.AppRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShellNavigatorTest {

    private val instagram = Destination.Rules(AppRef("instagram"), "Instagram")

    @Test
    fun theApplicationOpensOnTheDashboard() {
        assertEquals(Destination.Dashboard, ShellNavigator().current)
    }

    @Test
    fun thereIsNowhereToGoBackToFromTheFirstScreen() {
        val navigator = ShellNavigator()

        assertFalse(navigator.canGoBack)
        assertFalse(navigator.back())
        assertEquals(Destination.Dashboard, navigator.current)
    }

    @Test
    fun openingAScreenAndComingBackReturnsWhereYouWere() {
        val navigator = ShellNavigator()

        navigator.open(Destination.Protection)
        assertEquals(Destination.Protection, navigator.current)
        assertTrue(navigator.back())
        assertEquals(Destination.Dashboard, navigator.current)
    }

    @Test
    fun switchingTabsDoesNotPileUpAHistoryToWalkBackThrough() {
        val navigator = ShellNavigator()

        navigator.selectTab(Destination.Apps)
        navigator.selectTab(Destination.History)
        navigator.selectTab(Destination.Dashboard)

        assertFalse(navigator.canGoBack)
        assertEquals(listOf(Destination.Dashboard), navigator.history())
    }

    @Test
    fun aScreenOpenedFromATabKeepsThatTabSelected() {
        val navigator = ShellNavigator()

        navigator.selectTab(Destination.Apps)
        navigator.open(instagram)

        assertEquals(instagram, navigator.current)
        assertEquals(Destination.Apps, navigator.selectedTab)
    }

    @Test
    fun protectionBelongsToTheDashboardTab() {
        val navigator = ShellNavigator()

        navigator.open(Destination.Protection)

        assertEquals(Destination.Dashboard, navigator.selectedTab)
    }

    @Test
    fun openingWhereYouAlreadyAreDoesNotAddAStepToWalkBack() {
        val navigator = ShellNavigator()

        navigator.open(Destination.Protection)
        navigator.open(Destination.Protection)

        assertTrue(navigator.back())
        assertFalse(navigator.canGoBack)
    }

    @Test
    fun everyDestinationSurvivesBeingWrittenDownAndReadBack() {
        listOf(
            Destination.Dashboard,
            Destination.Apps,
            Destination.History,
            Destination.Protection,
            instagram,
            Destination.Rules(AppRef("twitter"), "X (Twitter)"),
        ).forEach { destination ->
            assertEquals(destination, decodeDestination(destination.encode()))
        }
    }

    @Test
    fun aHistoryThatCannotBeReadFallsBackToTheDashboardRatherThanCrashing() {
        assertEquals(Destination.Dashboard, decodeDestination("nonsense"))
        assertEquals(Destination.Dashboard, ShellNavigator(emptyList()).current)
    }

    @Test
    fun everyTabIsReachableAndIsItsOwnTab() {
        Destination.tabs.forEach { tab ->
            assertEquals(tab, tab.tab)
        }
        assertEquals(Destination.tabs.size, Destination.tabs.distinct().size)
    }
}
