package com.blocksocial.core.domain

import com.blocksocial.core.domain.fixtures.FixtureCorpus
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.BlockEvent
import com.blocksocial.core.model.Platform
import com.blocksocial.core.model.RuleMode
import com.blocksocial.core.model.UserAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.lang.reflect.Modifier
import java.time.Instant
import java.time.ZoneId

class BlockEventContractTest {

    private val contract = FixtureCorpus.envelope("block-event-contract.json")

    private fun contractFieldNames(): List<String> {
        val fields = contract.getJSONArray("fields")
        return (0 until fields.length()).map { fields.getJSONObject(it).getString("name") }
    }

    private fun contractValues(fieldName: String): List<String> {
        val fields = contract.getJSONArray("fields")
        val field = (0 until fields.length())
            .map { fields.getJSONObject(it) }
            .single { it.getString("name") == fieldName }
        val values = field.getJSONArray("values")
        return (0 until values.length()).map { values.getString(it) }
    }

    private fun event(
        userAction: UserAction = UserAction.STAYED_FOCUSED,
        bypassDurationMinutes: Int? = null,
        primaryReason: RuleMode = RuleMode.SCHEDULE,
        allReasons: List<RuleMode> = listOf(RuleMode.SCHEDULE),
    ) = BlockEvent(
        id = "event-1",
        restrictedAppRef = AppRef("app-a"),
        occurredAt = Instant.parse("2026-07-27T10:30:00Z"),
        zone = ZoneId.of("Asia/Tokyo"),
        primaryReason = primaryReason,
        allReasons = allReasons,
        userAction = userAction,
        bypassDurationMinutes = bypassDurationMinutes,
        platform = Platform.ANDROID,
    )

    @Test
    fun theStoredShapeIsExactlyTheFrozenFieldList() {
        val declared = BlockEvent::class.java.declaredFields
            .filterNot { Modifier.isStatic(it.modifiers) }
            .map { it.name }
        assertEquals(contractFieldNames().toSet(), declared.toSet())
    }

    @Test
    fun theSchemaVersionMatchesTheContract() {
        assertEquals(contract.getInt("eventSchemaVersion"), BlockEvent.EVENT_SCHEMA_VERSION)
        assertEquals(BlockEvent.EVENT_SCHEMA_VERSION, event().eventSchemaVersion)
    }

    @Test
    fun theEnumeratedValuesMatchTheContract() {
        assertEquals(contractValues("userAction"), UserAction.entries.map { it.name })
        assertEquals(contractValues("platform"), Platform.entries.map { it.name })
        assertEquals(contractValues("primaryReason"), RuleMode.entries.map { it.name })
        assertEquals(contractValues("allReasons"), RuleMode.entries.map { it.name })
    }

    @Test
    fun theReasonOrderIsThePriorityOrder() {
        assertEquals(
            RuleMode.entries.sortedBy { it.priority },
            RuleMode.entries.toList(),
        )
    }

    @Test
    fun primaryReasonMustAppearInAllReasons() {
        assertThrows(IllegalArgumentException::class.java) {
            event(primaryReason = RuleMode.ALWAYS, allReasons = listOf(RuleMode.SCHEDULE))
        }
    }

    @Test
    fun allReasonsMustBeInPriorityOrder() {
        assertThrows(IllegalArgumentException::class.java) {
            event(
                primaryReason = RuleMode.ALWAYS,
                allReasons = listOf(RuleMode.SCHEDULE, RuleMode.ALWAYS),
            )
        }
    }

    @Test
    fun bypassDurationIsPresentExactlyWhenTheUserBypassed() {
        assertThrows(IllegalArgumentException::class.java) {
            event(userAction = UserAction.BYPASSED, bypassDurationMinutes = null)
        }
        assertThrows(IllegalArgumentException::class.java) {
            event(userAction = UserAction.STAYED_FOCUSED, bypassDurationMinutes = 5)
        }
        assertEquals(5, event(UserAction.BYPASSED, 5).bypassDurationMinutes)
    }
}
