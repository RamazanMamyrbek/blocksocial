package com.blocksocial.core.domain.fixtures

import org.json.JSONObject
import java.io.File

class FixtureCase(val id: String, val json: JSONObject) {
    override fun toString(): String = id
}

object FixtureCorpus {

    const val CONTRACT_VERSION = 1

    val directory: File = generateSequence(File(".").absoluteFile) { it.parentFile }
        .map { File(it, "shared/fixtures") }
        .first { it.isDirectory }

    fun envelope(fileName: String): JSONObject = JSONObject(File(directory, fileName).readText())

    fun cases(fileName: String): List<FixtureCase> {
        val array = envelope(fileName).getJSONArray("cases")
        return (0 until array.length())
            .map { array.getJSONObject(it) }
            .map { FixtureCase(it.getString("id"), it) }
    }
}
