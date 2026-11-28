package com.noop.ui

// MARK: - Weekly Digest formatting helpers (#208/#463)
//
// Pure formatting helpers for the weekly digest display. Kept separate from the UI
// layer because they are exercised by unit tests and must stay deterministic.

/**
 * The engine's Effort display factor for the user's scale (#268/#463): moverSentence's
 * "(avg X vs Y)" prints stored 0-100 Effort means, so the 0-21 toggle rescales them for
 * display only. 1.0 leaves every sentence byte-identical to the pre-toggle output.
 */
internal fun effortDisplayFactor(scale: EffortScale): Double =
    if (scale == EffortScale.WHOOP) UnitFormatter.EFFORT_SCALE_FACTOR else 1.0
