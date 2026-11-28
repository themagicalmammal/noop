package com.noop.ui

import kotlin.math.roundToInt

// The pure helpers of the former Coupled view (task #43), kept for the screens and the view model that still
// read them: the slept-vs-needed duration format, and the OPTIMAL strain band.

/** "6h 42m" from a minutes count, for the slept-vs-needed read. Mirrors CoupledView.hoursMinutes EXACTLY. */
internal fun hoursMinutes(minutes: Double): String {
    val total = minutes.roundToInt().coerceAtLeast(0)
    return "${total / 60}h ${total % 60}m"
}

// MARK: - OPTIMAL strain range (task #43) — pure display-only recovery->strain mapping
//
// The classic coupled read suggests a Day-Strain target BAND from today's recovery: a green day earns a
// higher optimal band, a red day a lower one. PRESENTATION ONLY, never fed back into any score. These are
// the APPROVED bands and MUST stay byte-identical to the Swift CoupledView.optimalStrainRange:
//   recovery >= 67 (green)        -> 14-18 of 21
//   34 <= recovery <= 66 (yellow) -> 10-14
//   recovery < 34 (red)           -> 4-10
// null recovery (calibrating / unscored day) -> null, the caller renders the no-data token, never a
// guessed band.

internal data class OptimalStrainRange(val low: Int, val high: Int)

/** The pure recovery->optimal-strain band, or null when recovery is unknown. */
internal fun optimalStrainRange(recovery: Double?): OptimalStrainRange? {
    val r = recovery ?: return null
    return when {
        r >= 67 -> OptimalStrainRange(14, 18)
        r >= 34 -> OptimalStrainRange(10, 14)
        else -> OptimalStrainRange(4, 10)
    }
}
