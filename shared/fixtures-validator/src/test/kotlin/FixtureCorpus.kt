import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object FixtureCorpus {

    val root: File = generateSequence(File(".").absoluteFile) { it.parentFile }
        .map { File(it, "shared/fixtures") }
        .firstOrNull { it.isDirectory }
        ?: error("shared/fixtures was not found above ${File(".").absolutePath}")

    val caseFileNames = listOf(
        "schedule-cases.json",
        "bypass-cases.json",
        "daily-limit-cases.json",
        "rule-priority-cases.json"
    )

    val allFileNames = caseFileNames + listOf("business-rules.json", "block-event-contract.json")

    fun read(name: String): JSONObject = JSONObject(File(root, name).readText())

    fun cases(name: String): List<JSONObject> {
        val array: JSONArray = read(name).getJSONArray("cases")
        return (0 until array.length()).map { array.getJSONObject(it) }
    }

    fun allCases(): List<Pair<String, JSONObject>> =
        caseFileNames.flatMap { file -> cases(file).map { file to it } }

    fun businessRules(): List<JSONObject> {
        val array = read("business-rules.json").getJSONArray("rules")
        return (0 until array.length()).map { array.getJSONObject(it) }
    }
}
