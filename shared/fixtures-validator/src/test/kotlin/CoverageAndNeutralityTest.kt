import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CoverageAndNeutralityTest {

    @Test
    fun `every business rule the corpus owns is covered by at least one case`() {
        val owned = FixtureCorpus.businessRules()
            .filter { it.getString("scope") == "fixtures" }
            .map { it.getString("id") }
        val covered = FixtureCorpus.allCases().flatMap { (_, case) ->
            val array = case.getJSONArray("covers")
            (0 until array.length()).map { array.getString(it) }
        }.toSet()

        val gaps = owned - covered
        assertTrue(gaps.isEmpty(), "business rules with no case: ${gaps.sorted()}")
    }

    @Test
    fun `a business rule the corpus does not own says where it is enforced instead`() {
        FixtureCorpus.businessRules()
            .filter { it.getString("scope") != "fixtures" }
            .forEach {
                assertTrue(
                    it.optString("enforcedBy").length > 40,
                    "${it.getString("id")} is out of scope without saying where it is enforced"
                )
            }
    }

    @Test
    fun `every rule a case claims to cover actually exists`() {
        val known = FixtureCorpus.businessRules().map { it.getString("id") }.toSet()
        FixtureCorpus.allCases().forEach { (file, case) ->
            val array = case.getJSONArray("covers")
            (0 until array.length()).map { array.getString(it) }.forEach { rule ->
                assertTrue(
                    rule in known,
                    "$file: ${case.getString("id")} claims to cover unknown rule '$rule'"
                )
            }
        }
    }

    @Test
    fun `every business rule identifier is unique`() {
        val ids = FixtureCorpus.businessRules().map { it.getString("id") }
        assertEquals(ids.distinct(), ids, "business-rules.json repeats an identifier")
    }

    private val platformNames = listOf("android", "ios", "swift", "kotlin", "java", "xcode", "gradle")

    private val mechanismTerms = listOf(
        "accessibility service", "accessibilityservice", "usagestats", "usage stats manager",
        "elapsedrealtime", "elapsed realtime", "systemuptime", "system uptime",
        "shield", "familycontrols", "family controls", "deviceactivity", "device activity",
        "managedsettings", "managed settings", "sharedpreferences", "datastore",
        "com.", "org.telegram", "packagename", "bundleid"
    )

    private fun scan(name: String, terms: List<String>, allowedContext: List<String>): List<String> =
        File(FixtureCorpus.root, name).readLines().flatMapIndexed { index, line ->
            if (allowedContext.any { line.contains(it) }) {
                emptyList()
            } else {
                val lower = line.lowercase()
                terms.filter { lower.contains(it) }
                    .map { "$name:${index + 1} contains '$it': ${line.trim()}" }
            }
        }

    private fun textOf(node: Any?, skipKey: String): List<String> = when (node) {
        is JSONObject -> node.keys().asSequence()
            .filter { it != skipKey }
            .flatMap { key -> (listOf(key) + textOf(node.get(key), skipKey)).asSequence() }
            .toList()

        is JSONArray -> (0 until node.length()).flatMap { textOf(node.get(it), skipKey) }
        is String -> listOf(node)
        else -> emptyList()
    }

    @Test
    fun `a case names no platform and no platform mechanism`() {
        val terms = platformNames + mechanismTerms + listOf("package name", "bundle identifier")
        val offences = FixtureCorpus.allCases().flatMap { (file, case) ->
            val id = case.getString("id")
            textOf(case, skipKey = "atRisk").flatMap { text ->
                val lower = text.lowercase()
                terms.filter { lower.contains(it) }.map { "$file:$id contains '$it': $text" }
            }
        }
        assertTrue(offences.isEmpty(), "the corpus leaks platform detail:\n" + offences.joinToString("\n"))
    }

    @Test
    fun `an at-risk note is the one place a platform may be named, and it names only a limit`() {
        val atRiskCases = FixtureCorpus.allCases().filter { (_, case) -> case.has("atRisk") }
        atRiskCases.forEach { (file, case) ->
            val platform = case.getJSONObject("atRisk").getString("platform")
            assertTrue(
                platform in setOf("ios", "android"),
                "$file: ${case.getString("id")} marks an unknown platform '$platform'"
            )
        }
        val offences = atRiskCases.flatMap { (file, case) ->
            val reason = case.getJSONObject("atRisk").getString("reason").lowercase()
            mechanismTerms
                .filter { reason.contains(it) }
                .map { "$file:${case.getString("id")} at-risk reason names a mechanism: '$it'" }
        }
        assertTrue(offences.isEmpty(), offences.joinToString("\n"))
    }

    @Test
    fun `the event contract names no mechanism, though it may name the platform that wrote the event`() {
        val offences = scan(
            "block-event-contract.json",
            mechanismTerms,
            listOf("never a package name or a bundle identifier")
        )
        assertTrue(offences.isEmpty(), "the event contract leaks mechanism detail:\n" + offences.joinToString("\n"))
    }

    @Test
    fun `application handles are opaque`() {
        val allowed = setOf("app-a", "app-b")
        FixtureCorpus.allCases().forEach { (file, case) ->
            val handles = mutableListOf<String>()
            case.getJSONObject("when").optString("forApp").takeIf { it.isNotEmpty() }
                ?.let { handles += it }
            val given = case.getJSONObject("given")
            given.optJSONObject("grant")?.optString("forApp")?.takeIf { it.isNotEmpty() }
                ?.let { handles += it }
            given.optJSONArray("usageSessions")?.let { sessions ->
                (0 until sessions.length()).forEach {
                    handles += sessions.getJSONObject(it).getString("forApp")
                }
            }
            handles.forEach {
                assertTrue(
                    it in allowed,
                    "$file: ${case.getString("id")} uses handle '$it'; cases use only $allowed"
                )
            }
        }
    }

    @Test
    fun `each kind carries the coverage the plan asked for`() {
        val ids = FixtureCorpus.allCases().associate { (file, case) -> case.getString("id") to file }

        val required = listOf(
            "schedule-inside-normal-interval",
            "schedule-overnight-after-midnight-belongs-to-start-day",
            "schedule-weekday-not-selected",
            "schedule-disabled-rule-never-active",
            "schedule-spring-forward-after-the-gap",
            "schedule-autumn-back-second-pass",
            "schedule-zone-change-same-instant-now-outside",
            "bypass-active-inside-window",
            "bypass-after-expiry-blocks-again",
            "bypass-does-not-leak-to-another-app",
            "bypass-clock-moved-back-does-not-extend-the-grant",
            "bypass-does-not-resurrect-after-a-reboot",
            "limit-below-the-limit",
            "limit-exactly-reached",
            "limit-resets-at-local-midnight",
            "limit-on-a-twenty-three-hour-day",
            "limit-on-a-twenty-five-hour-day",
            "limit-reached-while-the-application-is-open"
        )
        val missing = required.filterNot { ids.containsKey(it) }
        assertTrue(missing.isEmpty(), "the corpus lost a case the plan requires: $missing")
    }

    @Test
    fun `at-risk cases exist and are a short list rather than the whole corpus`() {
        val atRisk = FixtureCorpus.allCases().filter { (_, case) -> case.has("atRisk") }
        assertTrue(atRisk.isNotEmpty(), "no case is marked at risk, which cannot be right")
        val total = FixtureCorpus.allCases().size
        assertTrue(
            atRisk.size * 2 < total,
            "${atRisk.size} of $total cases are at risk; that is a redesign, not a shortlist"
        )
    }
}
