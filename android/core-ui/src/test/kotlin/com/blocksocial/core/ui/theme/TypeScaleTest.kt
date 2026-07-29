package com.blocksocial.core.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

class TypeScaleTest {

    @Test
    fun `the type scale matches the documented sizes and weights`() {
        assertEquals(32.sp, DisplaySmall.fontSize)
        assertEquals(FontWeight.Bold, DisplaySmall.fontWeight)

        assertEquals(22.sp, TitleLarge.fontSize)
        assertEquals(FontWeight.SemiBold, TitleLarge.fontWeight)

        assertEquals(16.sp, LabelLarge.fontSize)
        assertEquals(FontWeight.SemiBold, LabelLarge.fontWeight)

        assertEquals(15.sp, BodyLarge.fontSize)
        assertEquals(FontWeight.Normal, BodyLarge.fontWeight)

        assertEquals(13.sp, Numeric.fontSize)
        assertEquals(FontWeight.Medium, Numeric.fontWeight)
    }

    @Test
    fun `measured values use a monospace family and prose does not`() {
        assertEquals(FontFamily.Monospace, Numeric.fontFamily)
        listOf(DisplaySmall, TitleLarge, LabelLarge, BodyLarge).forEach {
            assertEquals(FontFamily.Default, it.fontFamily)
        }
    }

    @Test
    fun `the material roles carry the documented scale`() {
        assertEquals(DisplaySmall, BlockSocialTypography.headlineMedium)
        assertEquals(TitleLarge, BlockSocialTypography.titleLarge)
        assertEquals(LabelLarge, BlockSocialTypography.labelLarge)
        assertEquals(BodyLarge, BlockSocialTypography.bodyLarge)
    }
}
