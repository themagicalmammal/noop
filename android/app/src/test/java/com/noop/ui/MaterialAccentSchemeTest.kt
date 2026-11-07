package com.noop.ui

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MaterialAccentSchemeTest {
    private val originalTokens = Palette.active

    @After
    fun restorePreferences() {
        Palette.active = originalTokens
    }

    @Test
    fun `Material controls follow the fixed chrome accent in both appearances`() {
        for (dark in listOf(false, true)) {
            val tokens = if (dark) DarkTokens else LightTokens
            Palette.active = tokens
            val scheme = noopColorScheme(tokens, dark)
            assertEquals("primary: dark=$dark", Palette.accent, scheme.primary)
            assertEquals(Palette.accentMuted, scheme.primaryContainer)
            assertEquals(if (dark) Palette.accentHover else Palette.accent, scheme.onPrimaryContainer)
            // The chrome accent must not recolor metric or warning semantics.
            assertEquals(tokens.metricPurple, scheme.secondary)
            assertEquals(tokens.statusCritical, scheme.error)
        }
    }
}
