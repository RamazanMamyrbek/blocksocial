package com.blocksocial.feature.onboarding

import com.blocksocial.core.domain.ProtectionRequirement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class PermissionCardSlotsTest {

    private val repositoryRoot: File = generateSequence(File(".").absoluteFile) { it.parentFile }
        .first { File(it, "docs/store/play/DISCLOSURE_AND_CONSENT.md").isFile }

    private val shippedText: Map<String, String> = run {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(File(repositoryRoot, "android/feature-onboarding/src/main/res/values/strings.xml"))
        val nodes = document.getElementsByTagName("string")
        (0 until nodes.length).associate { index ->
            val node = nodes.item(index)
            node.attributes.getNamedItem("name").nodeValue to node.textContent.replace("\\'", "'")
        }
    }

    private val resourceNames: Map<Int, String> = R.string::class.java.fields
        .associate { it.getInt(null) to it.name }

    private fun slotText(requirement: ProtectionRequirement, slot: PermissionSlot): String {
        val name = resourceNames.getValue(permissionSlotText(requirement, slot))
        return shippedText.getValue(name)
    }

    @Test
    fun everyPermissionHasTheSameFourSlots() {
        assertEquals(4, PermissionSlot.entries.size)

        val ids = ProtectionRequirement.entries.flatMap { requirement ->
            PermissionSlot.entries.map { slot -> permissionSlotText(requirement, slot) }
        }

        assertEquals("no slot may be shared between permissions", ids.size, ids.distinct().size)
    }

    @Test
    fun theSlotsAlwaysAppearInTheSameOrder() {
        val heading = mapOf(
            PermissionSlot.WHAT_IT_ENABLES to "What it enables.",
            PermissionSlot.WHAT_IS_READ to "What is read.",
            PermissionSlot.WHAT_IS_NEVER_READ to "What is never read.",
            PermissionSlot.HOW_TO_REVOKE to "How to turn it off.",
        )

        assertEquals(PermissionSlot.entries.toList(), heading.keys.toList())

        ProtectionRequirement.entries.forEach { requirement ->
            PermissionSlot.entries.forEach { slot ->
                val text = slotText(requirement, slot)
                assertTrue(
                    "$requirement $slot reads \"$text\"",
                    text.startsWith(heading.getValue(slot)),
                )
            }
        }
    }

    @Test
    fun noSlotIsLeftEmpty() {
        ProtectionRequirement.entries.forEach { requirement ->
            PermissionSlot.entries.forEach { slot ->
                val text = slotText(requirement, slot)
                assertTrue(
                    "$requirement $slot says nothing beyond its heading",
                    text.length > heading(slot).length + MINIMUM_SENTENCE,
                )
            }
        }
    }

    @Test
    fun everyRevokeSlotNamesWhereToGo() {
        ProtectionRequirement.entries.forEach { requirement ->
            assertTrue(
                "$requirement does not say where to turn it off",
                slotText(requirement, PermissionSlot.HOW_TO_REVOKE).contains("Settings"),
            )
        }
    }

    @Test
    fun theAccessibilityCardRepeatsTheFourPromisesOfThePolicyPackage() {
        val neverRead = slotText(
            ProtectionRequirement.ACCESSIBILITY_SERVICE,
            PermissionSlot.WHAT_IS_NEVER_READ,
        )

        listOf("text on your screen", "messages", "what you type", "passwords").forEach { promise ->
            assertTrue("the card drops \"$promise\"", neverRead.contains(promise))
        }
    }

    @Test
    fun noCardClaimsToBeTheSystem() {
        val forbidden = listOf("Android is requesting", "OS is asking")

        ProtectionRequirement.entries.forEach { requirement ->
            PermissionSlot.entries.forEach { slot ->
                val text = slotText(requirement, slot)
                forbidden.forEach { phrase ->
                    assertTrue("$requirement $slot sounds like a system dialog", !text.contains(phrase))
                }
            }
        }
    }

    private fun heading(slot: PermissionSlot): String = when (slot) {
        PermissionSlot.WHAT_IT_ENABLES -> "What it enables."
        PermissionSlot.WHAT_IS_READ -> "What is read."
        PermissionSlot.WHAT_IS_NEVER_READ -> "What is never read."
        PermissionSlot.HOW_TO_REVOKE -> "How to turn it off."
    }

    private companion object {
        const val MINIMUM_SENTENCE = 20
    }
}
