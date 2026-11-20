package com.noop.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.unit.ColorProvider

/**
 * Widget colours resolved from the same Forge colour profile the app reads, so the home-screen
 * widgets wear the same skin as the app — Colourful, Stone, Slate or Sage — rather than the old
 * Titanium & Gold. Glance composes outside our theme, so these read the profile straight from
 * SharedPreferences (the same key [com.noop.ui.forge.ForgeColorProfile.KEY] uses).
 *
 * Dark/light falls back to the widget's own resolution (a separate process cannot see the app's
 * in-process state). The profile's accent is used where the app would use its accent, and the
 * recovery/effort bands keep the app's own hues rather than hardcoded gold.
 */
internal object ForgeWidgetColors {

    private const val PREFS = "noop_prefs"
    private const val KEY_PROFILE = "noop.forgeDesign.colorProfile"
    private const val KEY_ACCENT = "accent.color"

    private val profiles = mapOf(
        "colorful" to Profile(
            surfaceLight = 0xFFFFFFFF, surfaceDark = 0xFF1C1C1E,
            accentLight = 0xFF007AFF, accentDark = 0xFF0A84FF),
        "stone" to Profile(
            surfaceLight = 0xFFFFFDFA, surfaceDark = 0xFF161A20,
            accentLight = 0xFF9A3E1C, accentDark = 0xFFE8B64A),
        "slate" to Profile(
            surfaceLight = 0xFFFFFFFF, surfaceDark = 0xFF161B22,
            accentLight = 0xFF3F5F85, accentDark = 0xFF8FB3DC),
        "sage" to Profile(
            surfaceLight = 0xFFFFFFFF, surfaceDark = 0xFF151B17,
            accentLight = 0xFF4A6B52, accentDark = 0xFF9CC7A8),
    )

    private data class Profile(val surfaceLight: Long, val surfaceDark: Long,
                               val accentLight: Long, val accentDark: Long)

    private fun profileName(context: Context): String {
        return runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_PROFILE, "colorful") ?: "colorful"
        }.getOrDefault("colorful")
    }

    private fun accentName(context: Context): String {
        return runCatching {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_ACCENT, "systemBlue") ?: "systemBlue"
        }.getOrDefault("systemBlue")
    }

    fun surface(context: Context, dark: Boolean): ColorProvider {
        val p = profiles[profileName(context)] ?: profiles["colorful"]!!
        return ColorProvider(if (dark) Color(p.surfaceDark) else Color(p.surfaceLight))
    }

    fun textPrimary(dark: Boolean): ColorProvider =
        ColorProvider(if (dark) Color(0xFFFFFFFF) else Color(0xFF000000))

    fun textSecondary(dark: Boolean): ColorProvider =
        ColorProvider(if (dark) Color(0xB8EBEBF5) else Color(0xD83C3C43))

    /** The app's accent (profile accent, or the wearer's chosen colour). */
    fun accent(context: Context, dark: Boolean): ColorProvider {
        val profile = profileName(context)
        val accent = accentName(context)
        val p = profiles[profile] ?: profiles["colorful"]!!

        // Colourful profile: the accent follows the wearer's choice, as the app does.
        if (profile == "colorful") {
            return when (accent) {
                "mint" -> ColorProvider(if (dark) Color(0xFF69DDB8) else Color(0xFF149A78))
                "whoopBlue" -> ColorProvider(if (dark) Color(0xFF60A0E0) else Color(0xFF234F9E))
                "custom" -> {
                    val hex = runCatching {
                        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            .getString("accent.customHex", null) ?: "#149A78"
                    }.getOrDefault("#149A78")
                    val c = androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(
                        if (hex.startsWith("#")) hex else "#$hex"))
                    ColorProvider(c)
                }
                else -> ColorProvider(if (dark) Color(0xFF0A84FF) else Color(0xFF007AFF))
            }
        }
        // The calm profiles keep their own accent.
        return ColorProvider(if (dark) Color(p.accentDark) else Color(p.accentLight))
    }

    /** Recovery-band colour (the app's 67 / 34 cuts). */
    fun band(recovery: Int, dark: Boolean): ColorProvider = ColorProvider(
        when {
            recovery >= 67 -> if (dark) Color(0xFF30D158) else Color(0xFF34C759)
            recovery >= 34 -> if (dark) Color(0xFFD98A3D) else Color(0xFFC2792E)
            else -> if (dark) Color(0xFFE0662F) else Color(0xFFC84E1E)
        },
    )

    /** Effort tint — the app's strain hue, distinct from the recovery bands. */
    fun effort(dark: Boolean): ColorProvider =
        ColorProvider(if (dark) Color(0xFF4FB6A8) else Color(0xFF2E7D74))

    /** The heart-rate trace tint (the app's liquid heart). */
    fun heart(dark: Boolean): ColorProvider =
        ColorProvider(if (dark) Color(0xFFE0662F) else Color(0xFFC84E1E))

    /** The raw surface colour value, for bitmap rendering and other non-Glance use. */
    fun surfaceColor(context: Context, dark: Boolean): Color {
        val p = profiles[profileName(context)] ?: profiles["colorful"]!!
        return if (dark) Color(p.surfaceDark) else Color(p.surfaceLight)
    }
}