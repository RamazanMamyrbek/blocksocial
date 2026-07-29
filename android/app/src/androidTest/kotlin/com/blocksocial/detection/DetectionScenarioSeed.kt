package com.blocksocial.detection

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.blocksocial.core.data.database.BlockSocialDatabase
import com.blocksocial.core.data.mapper.toEntity
import com.blocksocial.core.model.AppRef
import com.blocksocial.core.model.RestrictedApp
import com.blocksocial.core.model.RestrictionRule
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class DetectionScenarioSeed {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun database(): BlockSocialDatabase = Room.databaseBuilder(
        context,
        BlockSocialDatabase::class.java,
        BlockSocialDatabase.NAME,
    ).addMigrations(*BlockSocialDatabase.MIGRATIONS).build()

    @Test
    fun seedAnAlwaysOnRuleForYoutube() = runTest {
        val database = database()
        try {
            database.clearAllTables()
            database.restrictedAppDao().upsert(
                RestrictedApp(ref = YOUTUBE, displayName = "YouTube", selected = true)
                    .toEntity(Instant.now()),
            )
            database.restrictionRuleDao().upsert(
                RestrictionRule.AlwaysOn(id = "scenario-always-on", enabled = true)
                    .toEntity(YOUTUBE, Instant.now(), Instant.now()),
            )

            assertEquals(1, database.restrictionRuleDao().enabledRulesFor(YOUTUBE.value).size)
        } finally {
            database.close()
        }
    }

    private companion object {
        val YOUTUBE = AppRef("youtube")
    }
}
