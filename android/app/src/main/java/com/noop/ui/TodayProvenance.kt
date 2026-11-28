package com.noop.ui

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.noop.R
import com.noop.analytics.FusionSource
import com.noop.data.Vo2MaxEstimator

internal sealed interface DisplayText {
    data class Resource(@StringRes val id: Int, val args: List<Any> = emptyList()) : DisplayText
    data class Dynamic(val value: String) : DisplayText
}

/** The sensor/import provider whose inputs produced a Today score. */
internal data class ScoreInputProvider(val sourceId: String, val brand: String? = null)

internal fun provenanceBadgeLabel(owner: FusionSource?): DisplayText? = when (owner) {
    FusionSource.NOOP_COMPUTED -> DisplayText.Resource(R.string.today_source_on_device)
    FusionSource.WHOOP_IMPORT -> DisplayText.Resource(R.string.today_source_whoop)
    FusionSource.APPLE_HEALTH -> DisplayText.Resource(R.string.today_source_apple_health)
    FusionSource.HEALTH_CONNECT -> DisplayText.Resource(R.string.today_source_health_connect)
    FusionSource.XIAOMI_BAND -> DisplayText.Resource(R.string.today_source_mi_band)
    FusionSource.NUTRITION_CSV -> DisplayText.Resource(R.string.today_source_nutrition)
    FusionSource.LOCAL_CACHE -> DisplayText.Resource(R.string.today_source_cached)
    null -> null
}

/**
 * PURE mapper (unit-tested), a RAW resolver source id (as returned by [WhoopRepository.resolvedSeries]'s
 * winning point, e.g. "my-whoop", "my-whoop-noop", "apple-health") onto the spec's provenance labels,
 * given the strap's real [deviceId]. ANY NOOP-computed strap sibling (a "-noop"-suffixed id, not just the
 * active strap's) reads "On-device" - matching by suffix rather than "$deviceId-noop" so a computed row
 * from a non-active strap can't fall through to [FusionSource.NOOP_COMPUTED]'s raw "NOOP" displayName
 * (the internal id must never surface); the imported strap source ([deviceId], normally "my-whoop") reads
 * "Whoop"; the Apple-Health source reads "Apple Health". Any other real source (Health Connect, Mi Band,
 * nutrition) keeps its [FusionSource.displayName], still the genuine merge winner, never a blanket claim.
 * Mirrors the Swift `provenanceDisplayLabel` EXACTLY. This is the PER-METRIC mapper the Today rings use;
 * the day-level [dayOwnerSource]/[provenanceBadgeLabel] pair stays for the legacy By-Day vocabulary.
 */
internal fun provenanceDisplayLabel(
    rawSource: String,
    deviceId: String = com.noop.data.WhoopRepository.WHOOP_SOURCE,
): DisplayText {
    if (rawSource.startsWith(VO2_MAX_ATTRIBUTION_PREFIX)) {
        val raw = rawSource.removePrefix(VO2_MAX_ATTRIBUTION_PREFIX)
        return DisplayText.Resource(vo2MaxAttributionLabelRes(Vo2MaxEstimator.fromProvenanceId(raw)))
    }
    // #103/queue-11a follow-up: the spo2 candidate-fallback rows in the vital-detail readings table
    // (see [SPO2_CANDIDATE_ATTRIBUTION_SOURCE]) must read "strap estimate (unverified)" — the SAME
    // string every other candidate-fallback surface uses — never a device name, which would
    // misrepresent an unvalidated estimate as a calibrated reading in this table's Source column.
    if (rawSource == SPO2_CANDIDATE_ATTRIBUTION_SOURCE) {
        return DisplayText.Resource(R.string.spo2_strap_estimate_caption)
    }
    if (rawSource.endsWith("-noop")) return DisplayText.Resource(R.string.today_source_on_device)
    if (rawSource == deviceId || rawSource == com.noop.data.WhoopRepository.WHOOP_SOURCE) return DisplayText.Resource(R.string.today_source_whoop)
    if (rawSource == com.noop.data.WhoopRepository.APPLE_HEALTH_SOURCE) return DisplayText.Resource(R.string.today_source_apple_health)
    // Fall back to the FusionSource display name for any other known source; else the raw id verbatim.
    return FusionSource.entries.firstOrNull { it.id == rawSource }
        ?.let { source -> provenanceBadgeLabel(source) }
        ?: DisplayText.Dynamic(rawSource)
}

@StringRes
internal fun vo2MaxAttributionLabelRes(estimator: Vo2MaxEstimator?): Int = when (estimator) {
    Vo2MaxEstimator.NES -> R.string.vo2max_method_nes
    Vo2MaxEstimator.UTH -> R.string.vo2max_method_uth
    else -> R.string.vo2max_method_unknown
}

/** The tint for a per-metric provenance badge, keyed on the resolved LABEL, gold for Whoop, cyan for
 *  Apple Health, the positive status hue for on-device (and anything else). Matches the Data Sources
 *  footer + the Swift `provenanceTint` so the same source reads the same colour on Today. */
internal fun provenanceLabelTint(label: DisplayText): Color = when ((label as? DisplayText.Resource)?.id) {
    R.string.today_source_whoop -> Palette.accent
    R.string.today_source_apple_health -> Palette.metricCyan
    R.string.today_source_health_connect -> Palette.metricPurple
    else -> Palette.statusPositive
}
