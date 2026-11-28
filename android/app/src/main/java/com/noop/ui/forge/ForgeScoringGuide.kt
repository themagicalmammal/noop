package com.noop.ui.forge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.noop.R
import com.noop.ui.ScoreSection
import com.noop.ui.uiString

@Composable
internal fun ForgeScoringGuideSection(onBack: () -> Unit) {
    val colors = Forge.colors
    SettingsColumn {
        PageTitle(stringResource(R.string.l10n_scoring_guide_screen_how_your_scores_work_21a0e2be), onBack)
        Text(stringResource(R.string.l10n_scoring_guide_screen_charge_effort_rest_f396a530),
             style = Forge.type.subhead, color = colors.textSecondary)

        // Intro card.
        ForgeCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
                Text(stringResource(R.string.scoring_guide_three_scores),
                     style = Forge.type.overline, color = colors.textTertiary)
                Text(stringResource(R.string.scoring_guide_intro_body),
                     style = Forge.type.subhead, color = colors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space4)) {
                    ForgeLegendDot(ScoreSection.CHARGE, colors)
                    ForgeLegendDot(ScoreSection.EFFORT, colors)
                    ForgeLegendDot(ScoreSection.REST, colors)
                }
            }
        }

        // Score cards.
        ForgeScoreCard(ScoreSection.CHARGE, R.string.scoring_guide_charge_headline,
                        R.string.scoring_guide_charge_body, R.string.scoring_guide_charge_vs_whoop, colors)
        ForgeScoreCard(ScoreSection.EFFORT, R.string.scoring_guide_effort_headline,
                        R.string.scoring_guide_effort_body, R.string.scoring_guide_effort_vs_whoop, colors)
        ForgeScoreCard(ScoreSection.REST, R.string.scoring_guide_rest_headline,
                        R.string.scoring_guide_rest_body, R.string.scoring_guide_rest_vs_whoop, colors)

        // Confidence card.
        ForgeCard(Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space3)) {
                Text(stringResource(R.string.l10n_scoring_guide_screen_how_sure_is_noop_solid_building_c21d86d7),
                     style = Forge.type.headline, color = colors.text)
                Text(stringResource(R.string.scoring_guide_confidence_body),
                     style = Forge.type.subhead, color = colors.textSecondary)
            }
        }

        // Footer note.
        Text(stringResource(R.string.l10n_scoring_guide_screen_these_are_independent_approximations_from_a_301457ed),
             style = Forge.type.footnote, color = colors.textTertiary)
    }
}

@Composable
private fun ForgeLegendDot(section: ScoreSection, colors: ForgeColors) {
    Row(verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        androidx.compose.foundation.layout.Box(
            Modifier.size(8.dp).clip(CircleShape)
                .background(section.accent))
        Text(stringResource(section.labelRes), style = Forge.type.caption, color = colors.textSecondary)
    }
}

@Composable
private fun ForgeScoreCard(section: ScoreSection, headlineRes: Int, bodyRes: Int, vsWhoopRes: Int, colors: ForgeColors) {
    ForgeCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space3)) {
            Row(horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space3),
                verticalAlignment = Alignment.CenterVertically) {
                Icon(section.icon, contentDescription = null, tint = section.accent,
                     modifier = Modifier.size(16.dp))
                Text(stringResource(section.labelRes).uppercase(),
                     style = Forge.type.overline, color = section.accent)
            }
            Text(stringResource(headlineRes), style = Forge.type.headline, color = colors.text)
            Text(stringResource(bodyRes), style = Forge.type.subhead, color = colors.textSecondary)
            Text(stringResource(vsWhoopRes),
                 style = Forge.type.footnote.copy(fontStyle = FontStyle.Italic),
                 color = colors.textTertiary)
        }
    }
}
