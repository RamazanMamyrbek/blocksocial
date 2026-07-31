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
        assertEquals(Destination.Home, ShellNavigator().current)
    }

    @Test
    fun thereIsNowhereToGoBackToFromTheFirstScreen() {
        val navigator = ShellNavigator()

        assertFalse(navigator.canGoBack)
        assertFalse(navigator.back())
        assertEquals(Destination.Home, navigator.current)
    }

    @Test
    fun openingAScreenAndComingBackReturnsWhereYouWere() {
        val navigator = ShellNavigator()

        navigator.open(Destination.Protection)
        assertEquals(Destination.Protection, navigator.current)
        assertTrue(navigator.back())
        assertEquals(Destination.Home, navigator.current)
    }

    @Test
    fun switchingTabsDoesNotPileUpAHistoryToWalkBackThrough() {
        val navigator = ShellNavigator()

        navigator.selectTab(Destination.Today)
        navigator.selectTab(Destination.History)
        navigator.selectTab(Destination.Home)

        assertFalse(navigator.canGoBack)
        assertEquals(listOf(Destination.Home), navigator.history())
    }

    @Test
    fun aScreenOpenedFromATabKeepsThatTabSelected() {
        val navigator = ShellNavigator()

        navigator.selectTab(Destination.Today)
        navigator.open(Destination.History)

        assertEquals(Destination.History, navigator.current)
        assertEquals(Destination.History, navigator.selectedTab)
    }

    @Test
    fun aRuleScreenBelongsToTheHomeTabWhereverItWasOpenedFrom() {
        val navigator = ShellNavigator()

        navigator.selectTab(Destination.Today)
        navigator.open(instagram)

        assertEquals(instagram, navigator.current)
        assertEquals(Destination.Home, navigator.selectedTab)
    }

    @Test
    fun protectionBelongsToTheHomeTab() {
        val navigator = ShellNavigator()

        navigator.open(Destination.Protection)

        assertEquals(Destination.Home, navigator.selectedTab)
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
            Destination.Home,
            Destination.Today,
            Destination.History,
            Destination.Protection,
            Destination.AddApp,
            instagram,
            Destination.Rules(AppRef("twitter"), "X (Twitter)"),
        ).forEach { destination ->
            assertEquals(destination, decodeDestination(destination.encode()))
        }
    }

    @Test
    fun aHistoryThatCannotBeReadFallsBackToHomeRatherThanCrashing() {
        assertEquals(Destination.Home, decodeDestination("nonsense"))
        assertEquals(Destination.Home, ShellNavigator(emptyList()).current)
    }

    @Test
    fun everyTabIsReachableAndIsItsOwnTab() {
        Destination.tabs.forEach { tab ->
            assertEquals(tab, tab.tab)
        }
        assertEquals(Destination.tabs.size, Destination.tabs.distinct().size)
    }
}
