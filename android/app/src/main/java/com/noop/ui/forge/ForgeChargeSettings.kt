package com.noop.ui.forge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.noop.R
import com.noop.analytics.Baselines
import com.noop.ui.NoopPrefs
import android.widget.Toast

/**
 * Fork (DX23876), Android parity plan: Settings › Calculations › Charge as iOS has it.
 * A manual reset for the personal Charge baseline: if a bad first week poisons it — worn while
 * sick, or the first few nights reading high — recalibrate re-learns it from tonight onward.
 * The values live in shared preferences, as Apple's UserDefaults do.
 */
@Composable
internal fun ForgeChargeSettingsSection(onBack: () -> Unit) {
    val context = LocalContext.current
    var showConfirm by remember { mutableStateOf(false) }

    SettingsColumn {
        PageTitle(stringResource(R.string.l10n_settings_screen_charge_d4e1aee4), onBack)
        Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space1)) {
            SettingsCard(
                title = stringResource(R.string.l10n_settings_screen_charge_d4e1aee4),
                blurb = stringResource(R.string.forge_settings_charge_detail),
            ) {
                Text(stringResource(R.string.l10n_settings_screen_recalibrate_charge_baseline_52a05a26),
                     style = Forge.type.body, color = Forge.colors.text)
                Text(stringResource(R.string.l10n_settings_screen_restarts_the_roughly_4_night_build_84f9f8d0),
                     style = Forge.type.footnote, color = Forge.colors.textSecondary)
            }
            Text(stringResource(R.string.forge_settings_charge_note),
                 style = Forge.type.caption, color = Forge.colors.textTertiary)
        }
        ForgeCard(Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.l10n_settings_screen_recalibrate_aaa989ea),
                 style = Forge.type.headline.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
                 color = Forge.colors.accent)
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = androidx.compose.ui.graphics.Color(0xFF2C2C2E),
            title = { Text(stringResource(R.string.l10n_settings_screen_recalibrate_your_charge_baseline_018e3846),
                           style = Forge.type.title2, color = androidx.compose.ui.graphics.Color.White) },
            text = {
                Text(stringResource(R.string.l10n_settings_screen_this_restarts_the_roughly_4_night_c610a93d),
                     style = Forge.type.subhead, color = androidx.compose.ui.graphics.Color(0xFF141419))
            },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    val nowSeconds = System.currentTimeMillis() / 1000L
                    val editor = NoopPrefs.of(context).edit()
                    Baselines.recalibrateRecoveryBaselines(editor, nowSeconds)
                    editor.apply()
                    Toast.makeText(context,
                        "Charge baseline reset. NOOP will re-learn it from tonight. Your history stays, and it takes a few nights to settle.",
                        Toast.LENGTH_LONG).show()
                }) {
                    Text(stringResource(R.string.l10n_settings_screen_recalibrate_aaa989ea),
                         style = Forge.type.body, color = Forge.colors.accent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(R.string.l10n_settings_screen_cancel_77dfd213),
                         style = Forge.type.body, color = androidx.compose.ui.graphics.Color(0xFF98989D))
                }
            },
        )
    }
}
