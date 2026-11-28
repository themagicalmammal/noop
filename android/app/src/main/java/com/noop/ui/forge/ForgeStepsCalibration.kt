package com.noop.ui.forge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.noop.R
import com.noop.data.WhoopRepository
import com.noop.ui.AppViewModel
import com.noop.ui.ProfileStore
import com.noop.ui.StepsCalibrationFormat
import com.noop.analytics.StepsEstimateEngine
import com.noop.ui.uiString
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private const val STEPS_COEFFICIENT_STEP = 0.1

private data class ForgeStepsComparisonRow(val day: String, val estimated: Int, val actual: Int) {
    val errorPct: Double get() = if (actual > 0) (estimated - actual).toDouble() / actual * 100 else 0.0
}

@Composable
internal fun ForgeStepsCalibrationSection(
    vm: AppViewModel,
    profile: ProfileStore,
    onProfileChanged: () -> Unit,
    onBack: () -> Unit,
) {
    val colors = Forge.colors
    var comparison by remember { mutableStateOf<List<ForgeStepsComparisonRow>>(emptyList()) }
    var sampleMotion by remember { mutableStateOf<Double?>(null) }
    var loaded by remember { mutableStateOf(false) }

    val stepperMax = maxOf(profile.stepsCalibrationCoefficient, profile.stepsManualCoefficient, 50.0) * 2

    LaunchedEffect(Unit) {
        loaded = true
        val coeff = if (profile.stepsManualCoefficient > 0) {
            profile.stepsManualCoefficient
        } else {
            profile.stepsCalibrationCoefficient
        }
        if (coeff <= 0) return@LaunchedEffect

        val stepsByDay = LinkedHashMap<String, Int>()
        for (row in vm.repo.appleDaily(WhoopRepository.APPLE_HEALTH_SOURCE, "0000-01-01", "9999-12-31")) {
            row.steps?.takeIf { it > 0 }?.let { stepsByDay[row.day] = it }
        }
        for (row in vm.repo.appleDaily(WhoopRepository.HEALTH_CONNECT_SOURCE, "0000-01-01", "9999-12-31")) {
            row.steps?.takeIf { it > 0 }?.let { stepsByDay.putIfAbsent(row.day, it) }
        }
        val phoneDays = stepsByDay.entries
            .map { it.key to it.value }
            .sortedByDescending { it.first }

        val cal = StepsEstimateEngine.Calibration(
            coefficient = coeff,
            sampleDays = profile.stepsCalibrationSampleDays,
            confidence = profile.stepsCalibrationConfidence,
            manual = profile.stepsManualCoefficient > 0,
        )
        val rows = ArrayList<ForgeStepsComparisonRow>()
        val motions = ArrayList<Double>()
        val activeStrapId = vm.activeStrapId
        for ((day, phone) in phoneDays.take(10)) {
            val mid = runCatching {
                LocalDate.parse(day).atStartOfDay(ZoneId.systemDefault()).toEpochSecond()
            }.getOrNull() ?: continue
            val grav = vm.repo.gravitySamplesUnion(activeStrapId, mid, mid + 86_400 - 1)
            val motion = StepsEstimateEngine.dayMotionIntensity(grav)
            val est = StepsEstimateEngine.estimate(motion, cal) ?: continue
            motions.add(motion)
            rows.add(ForgeStepsComparisonRow(day, est, phone))
            if (rows.size >= 7) break
        }
        comparison = rows
        if (motions.isNotEmpty()) sampleMotion = motions.sorted()[motions.size / 2]
    }

    SettingsColumn {
        PageTitle(uiString(R.string.l10n_steps_calibration_screen_calibrate_your_steps_38b4e814), onBack)
        Text(uiString(R.string.l10n_steps_calibration_screen_whoop_4_0_motion_steps_a63239dc),
             style = Forge.type.subhead, color = colors.textSecondary)

        ForgeStepsExplainerCard()
        if (loaded && sampleMotion == null) ForgeStepsNoMotionNote()
        ForgeStepsCurrentFitCard(profile, matchedDays = comparison.size)
        ForgeStepsComparisonCard(comparison)
        ForgeStepsManualAdjustCard(
            profile = profile,
            stepperMax = stepperMax,
            sampleMotion = sampleMotion,
            onProfileChanged = onProfileChanged,
        )
    }
}

@Composable
private fun ForgeStepsExplainerCard() {
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
            Text(uiString(R.string.l10n_steps_calibration_screen_how_this_works_b895a8c3),
                 style = Forge.type.title2, color = Forge.colors.text)
            Text(uiString(R.string.l10n_steps_calibration_screen_noop_estimates_your_steps_from_your_d569bc31),
                 style = Forge.type.subhead, color = Forge.colors.textSecondary)
            Text(uiString(R.string.l10n_steps_calibration_screen_on_the_days_your_phone_also_2f65a14c),
                 style = Forge.type.footnote, color = Forge.colors.textTertiary)
        }
    }
}

@Composable
private fun ForgeStepsNoMotionNote() {
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
            Text(uiString(R.string.l10n_steps_calibration_screen_no_motion_synced_yet_65106670),
                 style = Forge.type.title2, color = Forge.colors.text)
            Text(uiString(R.string.l10n_steps_calibration_screen_we_re_not_seeing_any_motion_6ac8e092),
                 style = Forge.type.subhead, color = Forge.colors.textSecondary)
            Text(uiString(R.string.l10n_steps_calibration_screen_open_noop_near_your_strap_and_e08ddd6d),
                 style = Forge.type.footnote, color = Forge.colors.textTertiary)
        }
    }
}

@Composable
private fun ForgeStepsCurrentFitCard(profile: ProfileStore, matchedDays: Int) {
    val colors = Forge.colors
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space3)) {
            Text("Current calibration", style = Forge.type.overline, color = colors.textTertiary)
            if (profile.stepsCalibrationCoefficient > 0 || profile.stepsManualCoefficient > 0) {
                val coeff = if (profile.stepsManualCoefficient > 0) {
                    profile.stepsManualCoefficient
                } else {
                    profile.stepsCalibrationCoefficient
                }
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(String.format(Locale.US, "%.1f", coeff), style = Forge.type.heroNumber, color = colors.accent)
                    Text(uiString(R.string.l10n_steps_calibration_screen_steps_per_motion_unit_a2c2ac56),
                         style = Forge.type.footnote, color = colors.textTertiary)
                }
                if (profile.stepsManualCoefficient > 0) {
                    ForgeStepsStatLine("Source", "Manual (you set this by hand)")
                } else {
                    val days = profile.stepsCalibrationSampleDays
                    ForgeStepsStatLine("Fitted from", "$days day${if (days == 1) "" else "s"} your phone also counted")
                    ForgeStepsStatLine(
                        "Confidence",
                        "${StepsCalibrationFormat.confidenceLabel(profile.stepsCalibrationConfidence)} · " +
                            "${(profile.stepsCalibrationConfidence * 100).roundToInt()}%",
                    )
                }
            } else {
                Text(uiString(R.string.l10n_steps_calibration_screen_not_calibrated_yet_30abe0d0),
                     style = Forge.type.body, color = colors.text)
                Text(
                    forgeStepsCalibrationHeadline(
                        StepsEstimateEngine.CalibrationStatus
                            .NeedsMoreDays(have = matchedDays, need = StepsEstimateEngine.MIN_CALIBRATION_DAYS)
                            .headline,
                    ),
                    style = Forge.type.body, color = colors.accent,
                )
                Text(uiString(R.string.l10n_steps_calibration_screen_these_are_the_days_where_your_ae5c9c2c),
                     style = Forge.type.footnote, color = colors.textTertiary)
            }
        }
    }
}

@Composable
private fun ForgeStepsComparisonCard(rows: List<ForgeStepsComparisonRow>) {
    val colors = Forge.colors
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space3)) {
            Text("Estimated vs your phone", style = Forge.type.overline, color = colors.textTertiary)
            if (rows.isEmpty()) {
                Text(uiString(R.string.l10n_steps_calibration_screen_no_days_yet_where_both_noop_71d6005b),
                     style = Forge.type.footnote, color = colors.textTertiary)
            } else {
                Row(Modifier.fillMaxWidth()) {
                    Text(uiString(R.string.l10n_steps_calibration_screen_day_987b9ced), style = Forge.type.caption, color = colors.textTertiary, modifier = Modifier.weight(1f))
                    Text(uiString(R.string.l10n_steps_calibration_screen_est_18b405fb), style = Forge.type.caption, color = colors.textTertiary, textAlign = TextAlign.End, modifier = Modifier.width(64.dp))
                    Text(uiString(R.string.l10n_steps_calibration_screen_phone_77064d52), style = Forge.type.caption, color = colors.textTertiary, textAlign = TextAlign.End, modifier = Modifier.width(64.dp))
                    Text("Δ", style = Forge.type.caption, color = colors.textTertiary, textAlign = TextAlign.End, modifier = Modifier.width(52.dp))
                }
                for (row in rows) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(shortDay(row.day), style = Forge.type.footnote, color = colors.textSecondary, modifier = Modifier.weight(1f))
                        Text(grouped(row.estimated), style = Forge.type.caption, color = colors.text, textAlign = TextAlign.End, modifier = Modifier.width(64.dp))
                        Text(grouped(row.actual), style = Forge.type.caption, color = colors.text, textAlign = TextAlign.End, modifier = Modifier.width(64.dp))
                        Text(
                            String.format(Locale.US, "%+.0f%%", row.errorPct),
                            style = Forge.type.caption,
                            color = if (abs(row.errorPct) <= 15) colors.charge else androidx.compose.ui.graphics.Color(0xFFCC8A00),
                            textAlign = TextAlign.End,
                            modifier = Modifier.width(52.dp),
                        )
                    }
                }
                Text(uiString(R.string.l10n_steps_calibration_screen_these_days_are_excluded_from_the_6bedabbf),
                     style = Forge.type.caption, color = colors.textTertiary)
            }
        }
    }
}

@Composable
private fun ForgeStepsManualAdjustCard(
    profile: ProfileStore,
    stepperMax: Double,
    sampleMotion: Double?,
    onProfileChanged: () -> Unit,
) {
    val colors = Forge.colors
    val manual = profile.stepsManualCoefficient
    val effective = if (manual > 0) manual else profile.stepsCalibrationCoefficient

    fun step(delta: Double) {
        val next = (Math.round((effective + delta) * 10) / 10.0).coerceIn(0.0, stepperMax)
        profile.stepsManualCoefficient = next
        onProfileChanged()
    }

    ForgeCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space3)) {
            Text("Adjust manually", style = Forge.type.overline, color = colors.textTertiary)
            Text(uiString(R.string.l10n_steps_calibration_screen_override_the_automatic_fit_with_your_36a7b6fa),
                 style = Forge.type.footnote, color = colors.textTertiary)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    if (manual > 0) String.format(Locale.US, "%.1f", manual) else "Auto",
                    style = Forge.type.heroNumber,
                    color = if (manual > 0) colors.accent else colors.textSecondary,
                )
                Text(
                    if (manual > 0) "steps / motion unit" else "fit from your phone",
                    style = Forge.type.footnote, color = colors.textTertiary,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                com.noop.ui.StepperField(
                    value = if (manual > 0) String.format(Locale.US, "%.1f", manual) else "Auto",
                    accessibility = if (manual > 0) {
                        String.format(Locale.US, "Manual steps coefficient, %.1f steps per motion unit", manual)
                    } else {
                        "Manual steps coefficient, automatic"
                    },
                    onMinus = { step(-STEPS_COEFFICIENT_STEP) },
                    onPlus = { step(STEPS_COEFFICIENT_STEP) },
                )
            }
            if (sampleMotion != null && effective > 0) {
                val preview = (sampleMotion * effective).roundToInt()
                ForgeStepsStatLine(
                    "A typical recent day",
                    "≈ ${grouped(preview)} steps${if (manual > 0) " at this setting" else " (auto)"}",
                )
            }
            if (manual > 0) {
                Text(uiString(R.string.l10n_steps_calibration_screen_takes_effect_on_the_next_analytics_13205327),
                     style = Forge.type.caption, color = colors.textTertiary)
            }
        }
    }
}

@Composable
private fun ForgeStepsStatLine(label: String, value: String) {
    val colors = Forge.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(label, style = Forge.type.footnote, color = colors.textTertiary, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, style = Forge.type.footnote, color = colors.textSecondary, textAlign = TextAlign.End)
    }
}

private fun grouped(n: Int): String =
    if (abs(n) >= 1000) String.format(Locale.US, "%,d", n) else "$n"

private fun shortDay(key: String): String = runCatching {
    LocalDate.parse(key).format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.US))
}.getOrDefault(key)

private fun forgeStepsCalibrationHeadline(headline: StepsEstimateEngine.CalibrationStatus.Headline): String = when (headline) {
    StepsEstimateEngine.CalibrationStatus.Headline.Manual -> uiString(R.string.today_steps_headline_manual)
    is StepsEstimateEngine.CalibrationStatus.Headline.Calibrated ->
        uiString(R.string.today_steps_headline_calibrated, headline.sampleDays)
    StepsEstimateEngine.CalibrationStatus.Headline.ConnectPhoneSteps ->
        uiString(R.string.today_steps_headline_connect_phone)
    is StepsEstimateEngine.CalibrationStatus.Headline.NeedMoreDays ->
        uiString(R.string.today_steps_headline_more_days, headline.remaining)
}
