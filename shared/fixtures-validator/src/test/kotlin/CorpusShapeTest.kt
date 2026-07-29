import org.json.JSONArray
import org.json.JSONObject
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.fail

class CorpusShapeTest {

    private val expectedKinds = mapOf(
        "schedule-cases.json" to "schedule",
        "bypass-cases.json" to "bypass",
        "daily-limit-cases.json" to "daily-limit",
        "rule-priority-cases.json" to "rule-priority"
    )

    @Test
    fun `every file parses and declares the same contract version`() {
        val versions = FixtureCorpus.allFileNames.associateWith {
            FixtureCorpus.read(it).getInt("contractVersion")
        }
        assertEquals(1, versions.values.distinct().size, "contract versions disagree: $versions")
    }

    @Test
    fun `every case file declares its kind`() {
        expectedKinds.forEach { (file, kind) ->
            assertEquals(kind, FixtureCorpus.read(file).getString("kind"), "wrong kind in $file")
        }
    }

    @Test
    fun `every case has the required fields`() {
        FixtureCorpus.allCases().forEach { (file, case) ->
            listOf("id", "title", "covers", "given", "when", "expect").forEach { field ->
                assertTrue(case.has(field), "$file: a case is missing '$field': ${case.opt("id")}")
            }
            assertTrue(
                case.getJSONArray("covers").length() > 0,
                "$file: ${case.getString("id")} covers no business rule"
            )
            assertTrue(
                case.getString("title").isNotBlank(),
                "$file: ${case.getString("id")} has a blank title"
            )
        }
    }

    @Test
    fun `no two cases share an identifier`() {
        val seen = mutableMapOf<String, String>()
        FixtureCorpus.allCases().forEach { (file, case) ->
            val id = case.getString("id")
            val previous = seen.put(id, file)
            if (previous != null) {
                fail("duplicate case id '$id' in $file and $previous")
            }
        }
    }

    @Test
    fun `case identifiers are lower kebab case`() {
        val pattern = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")
        FixtureCorpus.allCases().forEach { (file, case) ->
            val id = case.getString("id")
            assertTrue(pattern.matches(id), "$file: '$id' is not lower-kebab-case")
        }
    }

    @Test
    fun `every case names a zone that exists and an instant that parses`() {
        FixtureCorpus.allCases().forEach { (file, case) ->
            val id = case.getString("id")
            val whenBlock = case.getJSONObject("when")
            val zone = whenBlock.getString("zone")
            assertTrue(
                ZoneId.getAvailableZoneIds().contains(zone),
                "$file: $id names an unknown zone '$zone'"
            )
            runCatching { OffsetDateTime.parse(whenBlock.getString("instant")) }
                .onFailure { fail("$file: $id has an instant that does not parse: ${it.message}") }
        }
    }

    @Test
    fun `an instant always carries an explicit offset`() {
        val withOffset = Regex(".*([+-]\\d{2}:\\d{2}|Z)$")
        fun check(where: String, value: String) {
            assertTrue(withOffset.matches(value), "$where: '$value' has no explicit offset")
        }
        FixtureCorpus.allCases().forEach { (file, case) ->
            val id = case.getString("id")
            check("$file:$id when.instant", case.getJSONObject("when").getString("instant"))
            val given = case.getJSONObject("given")
            given.optJSONObject("grant")?.let {
                check("$file:$id grant", it.getString("grantedAtWallClock"))
            }
            given.optJSONArray("usageSessions")?.let { sessions ->
                (0 until sessions.length()).map { sessions.getJSONObject(it) }.forEach { session ->
                    check("$file:$id session.from", session.getString("from"))
                    session.optString("to").takeIf { it.isNotEmpty() }
                        ?.let { check("$file:$id session.to", it) }
                }
            }
        }
    }

    @Test
    fun `a local time note agrees with the instant it describes`() {
        val leadingLocalTime = Regex("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2})")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        var checked = 0
        FixtureCorpus.allCases().forEach { (file, case) ->
            val whenBlock = case.getJSONObject("when")
            val note = whenBlock.optString("localTimeNote").takeIf { it.isNotEmpty() } ?: return@forEach
            val claimed = leadingLocalTime.find(note)?.groupValues?.get(1) ?: return@forEach
            val actual = OffsetDateTime.parse(whenBlock.getString("instant"))
                .atZoneSameInstant(ZoneId.of(whenBlock.getString("zone")))
                .format(formatter)
            assertEquals(
                claimed,
                actual,
                "$file: ${case.getString("id")} claims local time '$claimed' but the instant reads '$actual' in ${whenBlock.getString("zone")}"
            )
            checked++
        }
        assertTrue(checked >= 30, "only $checked notes were verifiable; the corpus should carry more")
    }

    @Test
    fun `a weekday named in a note is the weekday of the instant`() {
        val weekdays = listOf(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        )
        FixtureCorpus.allCases().forEach { (file, case) ->
            val whenBlock = case.getJSONObject("when")
            val note = whenBlock.optString("localTimeNote").takeIf { it.isNotEmpty() } ?: return@forEach
            val named = weekdays
                .mapNotNull { day -> note.indexOf(day).takeIf { it >= 0 }?.let { it to day } }
                .minByOrNull { it.first }
                ?.second
                ?: return@forEach
            val actual = OffsetDateTime.parse(whenBlock.getString("instant"))
                .atZoneSameInstant(ZoneId.of(whenBlock.getString("zone")))
                .dayOfWeek
                .name
                .lowercase()
                .replaceFirstChar { it.uppercase() }
            assertEquals(
                named,
                actual,
                "$file: ${case.getString("id")} names $named but the instant falls on $actual"
            )
        }
    }

    @Test
    fun `an at-risk marking names a platform and a reason`() {
        FixtureCorpus.allCases().forEach { (file, case) ->
            val atRisk = case.optJSONObject("atRisk") ?: return@forEach
            val id = case.getString("id")
            assertTrue(atRisk.optString("platform").isNotBlank(), "$file: $id atRisk has no platform")
            assertTrue(
                atRisk.optString("reason").length > 40,
                "$file: $id atRisk has no usable reason"
            )
        }
    }

    @Test
    fun `a case excluded from the MVP explains why`() {
        FixtureCorpus.allCases().forEach { (file, case) ->
            if (case.optBoolean("mvpApplicable", true)) return@forEach
            assertTrue(
                case.optString("mvpApplicableReason").length > 40,
                "$file: ${case.getString("id")} is excluded from the MVP without a stated reason"
            )
        }
    }

    @Test
    fun `the priority order reserves the focus session slot`() {
        val order: JSONArray = FixtureCorpus.read("rule-priority-cases.json").getJSONArray("priorityOrder")
        val values = (0 until order.length()).map { order.getString(it) }
        assertEquals(listOf("ALWAYS", "FOCUS_SESSION", "SCHEDULE", "DAILY_LIMIT"), values)
    }

    @Test
    fun `every reason named by a case is part of the declared priority order`() {
        val order = FixtureCorpus.read("rule-priority-cases.json").getJSONArray("priorityOrder")
        val allowed = (0 until order.length()).map { order.getString(it) }.toSet()
        FixtureCorpus.cases("rule-priority-cases.json").forEach { case ->
            val expect = case.getJSONObject("expect")
            expect.optString("primaryReason").takeIf { it.isNotEmpty() }?.let {
                assertTrue(it in allowed, "${case.getString("id")}: unknown reason '$it'")
            }
            val all = expect.getJSONArray("allReasons")
            (0 until all.length()).map { all.getString(it) }.forEach {
                assertTrue(it in allowed, "${case.getString("id")}: unknown reason '$it'")
            }
        }
    }

    @Test
    fun `all reasons are listed in priority order and contain the primary one`() {
        val order = FixtureCorpus.read("rule-priority-cases.json").getJSONArray("priorityOrder")
        val rank = (0 until order.length()).associate { order.getString(it) to it }
        FixtureCorpus.cases("rule-priority-cases.json").forEach { case ->
            val expect = case.getJSONObject("expect")
            val all = expect.getJSONArray("allReasons")
            val reasons = (0 until all.length()).map { all.getString(it) }
            assertEquals(
                reasons.sortedBy { rank.getValue(it) },
                reasons,
                "${case.getString("id")}: allReasons is not in priority order"
            )
            val primary = expect.optString("primaryReason").takeIf { it.isNotEmpty() }
            if (primary != null) {
                assertTrue(
                    primary in reasons,
                    "${case.getString("id")}: primaryReason '$primary' is absent from allReasons"
                )
                assertEquals(
                    reasons.firstOrNull(),
                    primary,
                    "${case.getString("id")}: primaryReason is not the highest-ranked reason"
                )
            } else {
                assertTrue(
                    reasons.isEmpty(),
                    "${case.getString("id")}: no primary reason but allReasons is not empty"
                )
            }
        }
    }

    @Test
    fun `a bypass case always states whether the block is suppressed`() {
        val evaluations = setOf(
            "ACTIVE", "EXPIRED", "ACTIVE_AFTER_REBOOT",
            "EXPIRED_AFTER_REBOOT", "DISCARDED_CLOCK_MOVED_BEFORE_GRANT"
        )
        FixtureCorpus.cases("bypass-cases.json").forEach { case ->
            val expect = case.getJSONObject("expect")
            assertTrue(expect.has("suppressesBlock"), "${case.getString("id")}: no suppressesBlock")
            if (!expect.isNull("evaluation")) {
                val evaluation = expect.getString("evaluation")
                assertTrue(
                    evaluation in evaluations,
                    "${case.getString("id")}: unknown evaluation '$evaluation'"
                )
                val suppresses = expect.getBoolean("suppressesBlock")
                val shouldSuppress = evaluation == "ACTIVE" || evaluation == "ACTIVE_AFTER_REBOOT"
                assertEquals(
                    shouldSuppress,
                    suppresses,
                    "${case.getString("id")}: evaluation '$evaluation' disagrees with suppressesBlock"
                )
            }
        }
    }

    @Test
    fun `the block event contract is internally consistent`() {
        val contract: JSONObject = FixtureCorpus.read("block-event-contract.json")
        val fields = contract.getJSONArray("fields")
        val names = (0 until fields.length()).map { fields.getJSONObject(it).getString("name") }
        listOf(
            "id", "restrictedAppRef", "occurredAt", "zone", "primaryReason",
            "allReasons", "userAction", "bypassDurationMinutes", "platform", "eventSchemaVersion"
        ).forEach { required ->
            assertTrue(required in names, "the event contract is missing '$required'")
        }
        assertEquals(names.distinct(), names, "the event contract repeats a field name")
        assertNotNull(contract.optJSONObject("changesFromTheSpecification"))
        (0 until fields.length()).map { fields.getJSONObject(it) }.forEach {
            assertTrue(
                it.getString("description").length > 20,
                "field '${it.getString("name")}' has no usable description"
            )
        }
    }
}
