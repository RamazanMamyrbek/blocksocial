package com.blocksocial.lite.detection

import org.junit.Assert.assertEquals
import org.junit.Test

class DecisionLogTest {

    @Test
    fun theMostRecentScreenChangeIsReadFirst() {
        val log = DecisionLog()
        log.record(record(atMillis = 1_000L))
        log.record(record(atMillis = 2_000L))

        assertEquals(listOf(2_000L, 1_000L), log.newestFirst().map { it.atMillis })
    }

    @Test
    fun onlyTheLastThirtyScreenChangesAreKept() {
        val log = DecisionLog()
        (1L..40L).forEach { index -> log.record(record(atMillis = index)) }

        val kept = log.newestFirst()
        assertEquals(30, kept.size)
        assertEquals(40L, kept.first().atMillis)
        assertEquals(11L, kept.last().atMillis)
    }

    @Test
    fun anApplicationOutsideTheCatalogIsRememberedWithoutItsName() {
        val log = DecisionLog()
        log.record(record(atMillis = 1_000L, app = null))

        assertEquals(listOf<String?>(null), log.newestFirst().map { it.app })
    }

    private fun record(atMillis: Long, app: String? = "youtube") = DecisionRecord(
        atMillis = atMillis,
        app = app,
        transition = TransitionOutcome.TARGET_ENTERED,
        usedMinutes = 5,
        limitMinutes = 3,
        warned = true,
    )
}
