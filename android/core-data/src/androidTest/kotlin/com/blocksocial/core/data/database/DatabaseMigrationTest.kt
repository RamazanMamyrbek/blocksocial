package com.blocksocial.core.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BlockSocialDatabase::class.java,
    )

    @Test
    fun theExportedSchemaMatchesTheEntities() {
        helper.createDatabase(TEST_DATABASE, BlockSocialDatabase.VERSION).close()
        helper.runMigrationsAndValidate(
            TEST_DATABASE,
            BlockSocialDatabase.VERSION,
            true,
            *BlockSocialDatabase.MIGRATIONS,
        ).close()
    }

    private companion object {
        const val TEST_DATABASE = "migration-test.db"
    }
}
