package com.noop.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

// MARK: - PaletteTokens — the per-scheme colour set behind `object Palette`
//
// Compose has no OS-dynamic colour (unlike iOS UIColor(light:dark:)), so the light theme is built
// the same way conceptually: ONE set of colour tokens, swapped wholesale per scheme. `Palette.active`
// is snapshot state, so every `Palette.X` read (in a composable OR a Canvas DrawScope) re-resolves
// automatically when the theme flips — ZERO call-site changes across the ~1,740 references.
//
// Dark values mirror StrandPalette.swift's dark; light values are the approved "Warm Paper" set
// (docs/superpowers/specs/2026-06-16-light-theme-design.md). Names/order match the Swift palette.

data class PaletteTokens(
    val surfaceBase: Color,
    val surfaceRaised: Color,
    val surfaceOverlay: Color,
    val surfaceInset: Color,
    val hairline: Color,
    val hairlineStrong: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val glowAmbient: Color,
    val accent: Color,
    val accentHover: Color,
    val accentMuted: Color,
    val focusRing: Color,
    val recovery000: Color,
    val recovery030: Color,
    val recovery055: Color,
    val recovery078: Color,
    val recovery100: Color,
    val strain000: Color,
    val strain033: Color,
    val strain066: Color,
    val strain100: Color,
    val sleepAwake: Color,
    val sleepLight: Color,
    val sleepDeep: Color,
    val sleepREM: Color,
    val zone1: Color,
    val zone2: Color,
    val zone3: Color,
    val zone4: Color,
    val zone5: Color,
    val statusPositive: Color,
    val statusWarning: Color,
    val statusCritical: Color,
    val metricCyan: Color,
    val metricPurple: Color,
    val metricAmber: Color,
    val metricRose: Color,
    val chargeColor: Color,
    val chargeDeep: Color,
    val chargeBright: Color,
    val chargeGlow: Color,
    val effortColor: Color,
    val effortDeep: Color,
    val effortBright: Color,
    val effortGlow: Color,
    val restColor: Color,
    val restDeep: Color,
    val restBright: Color,
    val restGlow: Color,
    val stressColor: Color,
    val stressDeep: Color,
    val stressBright: Color,
    val stressGlow: Color,
    val todayColor: Color,
    val heartRateColor: Color,
    val activityColor: Color,
    val scenicCenter: Color,
    val scenicEdge: Color,
    val scenicStar: Color,
    val cardFillTop: Color,
    val cardFillBottom: Color,
    val gold: Color,
    val goldLight: Color,
    val goldDeep: Color,
    val goldDeepText: Color,
    val signalYellow: Color,
    val titaniumTop: Color,
    val titaniumMid: Color,
    val titaniumLow: Color,
    val titaniumDeep: Color,
    // The bright gauge-tip / sparkline-head core: white reads as a highlight on dark; on light it
    // would vanish into the white card, so it flips to a deep ink (crisp centre on the coloured bead).
    val tipCore: Color,
    // #1160/#1161: the Liquid hero card surface (Today's Charge/Effort/Rest vessel + every screen's hero).
    // Pinned near-black in DARK; in LIGHT it flips to a frosted white so the hero fits in with the other
    // cards instead of reading as a broken dark block (was a raw literal inlined in 11 screens). heroBorder
    // + heroLabel flip with it so the edge stays subtle and the source-badge text stays readable in both.
    val heroFill: Color,
    val heroBorder: Color,
)

// Dark palette: black-and-white recovery-dashboard foundation (near-black canvas, soft cards,
// hairline borders) with a fixed red chrome accent; the recovery, strain, sleep, charge/effort/
// rest and stress DATA colour worlds stay domain-specific.
val DarkTokens = PaletteTokens(
    surfaceBase = Color(0xFF000000), surfaceRaised = Color(0xFF0D0D0F), surfaceOverlay = Color(0xFF121212),
    surfaceInset = Color(0xFF0A0B0D), hairline = Color(0xFF26262A), hairlineStrong = Color(0xFF333333),
    textPrimary = Color(0xFFFFFFFF), textSecondary = Color(0xFF9A9AA0), textTertiary = Color(0xFF5A5C5E),
    glowAmbient = Color(0xFF3A2D0A),
    // Fixed red interface accent; health metric colors remain domain-specific.
    accent = Color(0xFFE45555), accentHover = Color(0xFFF07B7B), accentMuted = Color(0xFF311819), focusRing = Color(0xFFE45555),
    recovery000 = Color(0xFFE0463C), recovery030 = Color(0xFFE8743C), recovery055 = Color(0xFFF9DF4A),
    recovery078 = Color(0xFF8FD86A), recovery100 = Color(0xFF03E095),
    strain000 = Color(0xFF9C5A14), strain033 = Color(0xFFC2762A), strain066 = Color(0xFFD98A3D), strain100 = Color(0xFFF0A85A),
    sleepAwake = Color(0xFFF2B84B), sleepLight = Color(0xFF9BA8E8), sleepDeep = Color(0xFF4653C4), sleepREM = Color(0xFF8B7BE8),
    zone1 = Color(0xFF8C8C8C), zone2 = Color(0xFF46B45A), zone3 = Color(0xFFE8B84B), zone4 = Color(0xFFD98A3D), zone5 = Color(0xFFE0662F),
    statusPositive = Color(0xFF03E095), statusWarning = Color(0xFFF0A020), statusCritical = Color(0xFFE0662F),
    metricCyan = Color(0xFF8C8C8C), metricPurple = Color(0xFF8E6FD6), metricAmber = Color(0xFFD98A3D), metricRose = Color(0xFFE0662F),
    chargeColor = Color(0xFF4FE3A9), chargeDeep = Color(0xFF0B3B2B), chargeBright = Color(0xFF8CF5CD), chargeGlow = Color(0xFF4FE3A9),
    effortColor = Color(0xFFF26A21), effortDeep = Color(0xFF5A240A), effortBright = Color(0xFFFF9A5C), effortGlow = Color(0xFFF26A21),
    restColor = Color(0xFF1E2E8C), restDeep = Color(0xFF101A52), restBright = Color(0xFF4653C4), restGlow = Color(0xFF1E2E8C),
    stressColor = Color(0xFFF0A020), stressDeep = Color(0xFF46B45A), stressBright = Color(0xFFE0662F), stressGlow = Color(0xFFF0A020),
    todayColor = Color(0xFF6A4BD6), heartRateColor = Color(0xFFF0595A), activityColor = Color(0xFFA8E05A),
    scenicCenter = Color(0xFF141419), scenicEdge = Color(0xFF000000), scenicStar = Color(0xFFCCCCCC),
    cardFillTop = Color(0xFF161616), cardFillBottom = Color(0xFF0C0C0C),
    gold = Color(0xFFE45555), goldLight = Color(0xFFF07B7B), goldDeep = Color(0xFFB03A3A),
    goldDeepText = Color(0xFFFFFFFF), signalYellow = Color(0xFFFFD63D),
    titaniumTop = Color(0xFFF2F2F2), titaniumMid = Color(0xFFC9C9C9), titaniumLow = Color(0xFF969696), titaniumDeep = Color(0xFF6B6B6B),
    tipCore = Color(0xFFFFFFFF),
    heroFill = Color(0xCC0D0D0D), heroBorder = Color(0x1CFFFFFF),
)

val LightTokens = PaletteTokens(
    surfaceBase = Color(0xFFEAE3D4), surfaceRaised = Color(0xFFFFFFFF), surfaceOverlay = Color(0xFFFFFFFF),
    surfaceInset = Color(0xFFDFD8C8), hairline = Color(0xFFD8D0BD), hairlineStrong = Color(0xFFC7BCA4),
    textPrimary = Color(0xFF1A2230), textSecondary = Color(0xFF4C5564), textTertiary = Color(0xFF7C8696),
    glowAmbient = Color(0xFFF0E4C0),
    // Fixed red interface accent (deepened for contrast on the light WARM paper canvas).
    accent = Color(0xFFC62828), accentHover = Color(0xFFE45555), accentMuted = Color(0xFFF8E3E3), focusRing = Color(0xFFC62828),
    recovery000 = Color(0xFF8F6212), recovery030 = Color(0xFFA87718), recovery055 = Color(0xFFC28E26),
    recovery078 = Color(0xFFD2A23A), recovery100 = Color(0xFFE0B44C),
    strain000 = Color(0xFF7E460E), strain033 = Color(0xFFA4621B), strain066 = Color(0xFFC2792E), strain100 = Color(0xFFD89240),
    sleepAwake = Color(0xFFB07D17), sleepLight = Color(0xFF6A74C8), sleepDeep = Color(0xFF2A38A8), sleepREM = Color(0xFF6A4FC0),
    zone1 = Color(0xFF828282), zone2 = Color(0xFF2E9E4F), zone3 = Color(0xFFC28E26), zone4 = Color(0xFFC2792E), zone5 = Color(0xFFC84E1E),
    statusPositive = Color(0xFFB07D17), statusWarning = Color(0xFFC2792E), statusCritical = Color(0xFFC84E1E),
    metricCyan = Color(0xFF828282), metricPurple = Color(0xFF6A4FC0), metricAmber = Color(0xFFC2792E), metricRose = Color(0xFFC84E1E),
    chargeColor = Color(0xFFB88421), chargeDeep = Color(0xFF8F6212), chargeBright = Color(0xFFE0B44C), chargeGlow = Color(0xFFC8902F),
    effortColor = Color(0xFFB4540F), effortDeep = Color(0xFF7A3406), effortBright = Color(0xFFD97B2E), effortGlow = Color(0xFFB4540F),
    restColor = Color(0xFF1E2E8C), restDeep = Color(0xFF101A52), restBright = Color(0xFF33409E), restGlow = Color(0xFF1E2E8C),
    stressColor = Color(0xFFB88421), stressDeep = Color(0xFF2E9E4F), stressBright = Color(0xFFC84E1E), stressGlow = Color(0xFFB88421),
    todayColor = Color(0xFF3B1FA8), heartRateColor = Color(0xFFC8403F), activityColor = Color(0xFF6E8F1F),
    scenicCenter = Color(0xFFFBF6EA), scenicEdge = Color(0xFFEDE6D6), scenicStar = Color(0xFFD8CDB6),
    cardFillTop = Color(0xFFFFFFFF), cardFillBottom = Color(0xFFFAF7F0),
    gold = Color(0xFFC62828), goldLight = Color(0xFFE45555), goldDeep = Color(0xFF8F1F1F),
    goldDeepText = Color(0xFFFFFFFF), signalYellow = Color(0xFFE8A800),
    titaniumTop = Color(0xFFDDE1E6), titaniumMid = Color(0xFFBBC2C9), titaniumLow = Color(0xFF98A0A8), titaniumDeep = Color(0xFF6B6B6B),
    tipCore = Color(0xFF241B06),
    // Frosted WHITE hero in light mode (#1160), subtle dark edge. (Hero text uses the flip-able text*
    // tokens now, so no separate label token is needed.)
    heroFill = Color(0xD9FFFFFF), heroBorder = Color(0x1A000000),
)

// MARK: - Chart style (data-viz colour mode) + the Classic throwback ramps

enum class ChartStyle(val storageValue: String, val label: String) {
    TITANIUM("titanium", "Titanium"),
    CLASSIC("classic", "Classic");

    companion object {
        fun fromStorage(raw: String?): ChartStyle = entries.firstOrNull { it.storageValue == raw } ?: TITANIUM
    }
}

/** Chart-colour preference, persisted in `noop_prefs` and mirrored in snapshot state so a flip
 *  re-colours every gauge/chart live (the Palette ramp accessors read [ChartStylePrefs.style]). */
object ChartStylePrefs {
    private const val FILE = "noop_prefs"
    private const val KEY = "chart.style"
    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    var style by mutableStateOf(ChartStyle.TITANIUM)
        private set

    fun load(ctx: Context) {
        style = ChartStyle.fromStorage(prefs(ctx).getString(KEY, ChartStyle.TITANIUM.storageValue))
    }

    fun set(ctx: Context, value: ChartStyle) {
        style = value
        prefs(ctx).edit().putString(KEY, value.storageValue).apply()
    }
}

/** The Classic (throwback) data ramps — light/dark tuned. Picked by the Palette accessors when
 *  ChartStylePrefs.style == CLASSIC. Surfaces/text/accent are never classic — only data encodings. */
data class ClassicRamp(
    val recovery: List<Pair<Float, Color>>,
    val strain: List<Pair<Float, Color>>,
    val stress: List<Pair<Float, Color>>,
    val sleepAwake: Color, val sleepLight: Color, val sleepDeep: Color, val sleepREM: Color,
    val zone1: Color, val zone2: Color, val zone3: Color, val zone4: Color, val zone5: Color,
    val statusPositive: Color, val statusWarning: Color, val statusCritical: Color,
    val metricCyan: Color, val metricPurple: Color, val metricAmber: Color, val metricRose: Color,
    val chargeColor: Color, val chargeDeep: Color, val chargeBright: Color,
    val effortColor: Color, val effortDeep: Color, val effortBright: Color,
    val restColor: Color, val restDeep: Color, val restBright: Color,
    val stressColor: Color, val stressDeep: Color, val stressBright: Color,
)

val ClassicDark = ClassicRamp(
    recovery = listOf(0.0f to Color(0xFFE5483B), 0.30f to Color(0xFFEE8B3C), 0.55f to Color(0xFFF2C53D), 0.78f to Color(0xFFA6D04E), 1.0f to Color(0xFF46B45A)),
    strain = listOf(0.0f to Color(0xFFB3B3B3), 0.33f to Color(0xFF969696), 0.66f to Color(0xFF6E6E6E), 1.0f to Color(0xFF525252)),
    stress = listOf(0.0f to Color(0xFF46B45A), 0.5f to Color(0xFFF2C53D), 1.0f to Color(0xFFE5483B)),
    sleepAwake = Color(0xFFC9C9C9), sleepLight = Color(0xFFA6A6A6), sleepDeep = Color(0xFF5A5A5A), sleepREM = Color(0xFF8C8C8C),
    zone1 = Color(0xFF8C8C8C), zone2 = Color(0xFF46B45A), zone3 = Color(0xFFF2C53D), zone4 = Color(0xFFEE8B3C), zone5 = Color(0xFFE5483B),
    statusPositive = Color(0xFF46B45A), statusWarning = Color(0xFFF2C53D), statusCritical = Color(0xFFE5483B),
    metricCyan = Color(0xFF8C8C8C), metricPurple = Color(0xFF8E6FD6), metricAmber = Color(0xFFF2C53D), metricRose = Color(0xFFE5483B),
    chargeColor = Color(0xFF46B45A), chargeDeep = Color(0xFF2E9E4F), chargeBright = Color(0xFF86D98E),
    effortColor = Color(0xFFB3B3B3), effortDeep = Color(0xFF8C8C8C), effortBright = Color(0xFFD6D6D6),
    restColor = Color(0xFF8C8C8C), restDeep = Color(0xFF5A5A5A), restBright = Color(0xFFB3B3B3),
    stressColor = Color(0xFFF2C53D), stressDeep = Color(0xFF46B45A), stressBright = Color(0xFFE5483B),
)

val ClassicLight = ClassicRamp(
    recovery = listOf(0.0f to Color(0xFFCB3A2F), 0.30f to Color(0xFFD87328), 0.55f to Color(0xFFCFA528), 0.78f to Color(0xFF74A53A), 1.0f to Color(0xFF2E9E4F)),
    strain = listOf(0.0f to Color(0xFF5E5E5E), 0.33f to Color(0xFF4A4A4A), 0.66f to Color(0xFF383838), 1.0f to Color(0xFF262626)),
    stress = listOf(0.0f to Color(0xFF2E9E4F), 0.5f to Color(0xFFCFA528), 1.0f to Color(0xFFCB3A2F)),
    sleepAwake = Color(0xFF8C8C8C), sleepLight = Color(0xFF7A7A7A), sleepDeep = Color(0xFF4A4A4A), sleepREM = Color(0xFF6E6E6E),
    zone1 = Color(0xFF828282), zone2 = Color(0xFF2E9E4F), zone3 = Color(0xFFCFA528), zone4 = Color(0xFFD87328), zone5 = Color(0xFFCB3A2F),
    statusPositive = Color(0xFF2E9E4F), statusWarning = Color(0xFFCFA528), statusCritical = Color(0xFFCB3A2F),
    metricCyan = Color(0xFF828282), metricPurple = Color(0xFF6A4FC0), metricAmber = Color(0xFFCFA528), metricRose = Color(0xFFCB3A2F),
    chargeColor = Color(0xFF2E9E4F), chargeDeep = Color(0xFF207A3C), chargeBright = Color(0xFF5FBE6E),
    effortColor = Color(0xFF4A4A4A), effortDeep = Color(0xFF333333), effortBright = Color(0xFF6B6B6B),
    restColor = Color(0xFF6E6E6E), restDeep = Color(0xFF4A4A4A), restBright = Color(0xFF969696),
    stressColor = Color(0xFFCFA528), stressDeep = Color(0xFF2E9E4F), stressBright = Color(0xFFCB3A2F),
)

// MARK: - Appearance preference (System / Light / Dark)

enum class AppearanceMode(val storageValue: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromStorage(raw: String?): AppearanceMode =
            entries.firstOrNull { it.storageValue == raw } ?: SYSTEM
    }
}

/**
 * How heavy the numeral over a gauge is drawn (#2346).
 *
 * `BOLD` is the shipped look and stays the default, so nothing moves for anyone who does not ask. `SOFT`
 * drops to `NoopType.number`'s own default weight, which is what every other number in the app uses.
 *
 * Named for the effect rather than a font weight: the reporter asked for "softer", and the two platforms
 * reach it through different weight vocabularies. Twin of the Apple `GaugeNumeralStyle`.
 */
enum class GaugeNumeralStyle(val storageValue: String) {
    BOLD("bold"),
    SOFT("soft");

    companion object {
        fun fromStorage(raw: String?): GaugeNumeralStyle =
            entries.firstOrNull { it.storageValue == raw } ?: BOLD
    }
}

/** Theme preference, persisted in `noop_prefs` and mirrored in snapshot state so the toggle is live.
 *  [load] is called once from MainActivity before first composition (no flash); [set] writes + flips. */
object AppearancePrefs {
    private const val FILE = "noop_prefs"
    private const val KEY = "theme.appearance"
    private const val GAUGE_KEY = "appearance.gaugeNumerals"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    /** Live appearance mode read by NoopTheme; defaults to System until [load] runs. */
    var mode by mutableStateOf(AppearanceMode.SYSTEM)
        private set

    /**
     * Gauge numeral weight (#2346). Deliberately NOT in the `.noopbak` whitelist, matching [mode] beside
     * it: that contract carries profile, units and anything that changes a NUMBER, while a display choice
     * is device-local. Adding it would touch a byte-identical cross-platform contract for a preference
     * that means nothing on another device.
     */
    var gaugeNumerals by mutableStateOf(GaugeNumeralStyle.BOLD)
        private set

    fun load(ctx: Context) {
        mode = AppearanceMode.fromStorage(prefs(ctx).getString(KEY, AppearanceMode.SYSTEM.storageValue))
        gaugeNumerals = GaugeNumeralStyle.fromStorage(
            prefs(ctx).getString(GAUGE_KEY, GaugeNumeralStyle.BOLD.storageValue))
    }

    fun setGaugeNumerals(ctx: Context, value: GaugeNumeralStyle) {
        gaugeNumerals = value
        prefs(ctx).edit().putString(GAUGE_KEY, value.storageValue).apply()
    }

    fun set(ctx: Context, value: AppearanceMode) {
        mode = value
        prefs(ctx).edit().putString(KEY, value.storageValue).apply()
    }
}
