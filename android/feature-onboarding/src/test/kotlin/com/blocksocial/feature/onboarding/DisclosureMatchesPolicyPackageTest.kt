package com.blocksocial.feature.onboarding

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class DisclosureMatchesPolicyPackageTest {

    private val repositoryRoot: File = generateSequence(File(".").absoluteFile) { it.parentFile }
        .first { File(it, "docs/store/play/DISCLOSURE_AND_CONSENT.md").isFile }

    private val policyText: String = File(repositoryRoot, "docs/store/play/DISCLOSURE_AND_CONSENT.md")
        .readText()
        .normalise()

    private val strings: Map<String, String> = run {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(repositoryRoot, "android/feature-onboarding/src/main/res/values/strings.xml"))
        val nodes = document.getElementsByTagName("string")
        (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            val name = node.attributes.getNamedItem("name").nodeValue
            name to node.textContent.replace("\\'", "'").normalise()
        }
    }

    private fun String.normalise(): String = replace("**", "")
        .replace(Regex("(?m)^\\s*>\\s?"), "")
        .replace("`", "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun assertQuoted(key: String) {
        val shown = strings.getValue(key)
        assertTrue(
            "the disclosure package does not contain the shipped text for $key:\n$shown",
            policyText.contains(shown),
        )
    }

    @Test
    fun everyDisclosureParagraphIsQuotedFromThePolicyPackage() {
        listOf(
            "disclosure_title",
            "disclosure_intro",
            "disclosure_lead",
            "disclosure_reads",
            "disclosure_never_reads",
            "disclosure_does",
            "disclosure_where",
            "disclosure_continue",
            "disclosure_not_now",
            "disclosure_footnote",
        ).forEach(::assertQuoted)
    }

    @Test
    fun everyConsentParagraphIsQuotedFromThePolicyPackage() {
        listOf(
            "consent_title",
            "consent_body",
            "consent_detail",
            "consent_agree",
            "consent_cancel",
        ).forEach(::assertQuoted)
    }

    @Test
    fun theDisclosureNeverClaimsToBeTheSystem() {
        val forbidden = listOf("Android is requesting", "System", "OS is asking")
        val shipped = strings.filterKeys { it.startsWith("disclosure_") || it.startsWith("consent_") }

        shipped.forEach { (key, text) ->
            forbidden.forEach { phrase ->
                assertTrue("$key sounds like a system dialog", !text.contains(phrase))
            }
        }
    }

    @Test
    fun theHandOffToAndroidIsStatedOnBothScreens() {
        assertTrue(strings.getValue("disclosure_footnote").contains("Android"))
        assertTrue(policyText.contains(strings.getValue("disclosure_footnote")))
    }
}
