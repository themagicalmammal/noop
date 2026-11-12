package com.noop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun BiscuitScreen() {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("noop_prefs", android.content.Context.MODE_PRIVATE) }
    var moodIndex by remember { mutableStateOf(0) }
    var outfit by remember {
        mutableStateOf(prefs.getString("noop.widget.face", null)?.removePrefix("face-")?.takeIf { it in setOf("coach", "glasses", "nightcap", "crown") })
    }
    val owned = remember(context) { GoalsStore.load(context).ownedRewards.toSet() }
    val moods = listOf("happy", "curious", "delighted", "sleepy", "focused", "calm")
    val outfits = listOf(
        "coach" to "face-coach",
        "glasses" to "face-glasses",
        "nightcap" to "face-nightcap",
        "crown" to "face-crown",
    )

    LazyScreenScaffold(
        title = uiString(com.noop.R.string.nav_biscuit),
        subtitle = uiString(com.noop.R.string.biscuit_subtitle),
    ) {
        item {
            NoopCard {
                Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
                    Text(uiString(com.noop.R.string.biscuit_prompt), style = NoopType.footnote, color = Palette.textSecondary)
                    BiscuitArt(mood = moods[moodIndex], outfit = outfit, modifier = Modifier.fillMaxWidth())
                    NoopButton(
                        text = uiString(com.noop.R.string.biscuit_next_mood),
                        onClick = { moodIndex = (moodIndex + 1) % moods.size },
                    )
                }
            }
        }
        item {
            NoopCard {
                Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
                    Text(uiString(com.noop.R.string.biscuit_outfits), style = NoopType.headline, color = Palette.textPrimary)
                    outfits.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            row.forEach { (name, rewardId) ->
                                val unlocked = rewardId in owned
                                FilterChip(
                                    selected = outfit == name,
                                    onClick = {
                                        if (unlocked) {
                                            outfit = name
                                            prefs.edit().putString("noop.widget.face", rewardId).apply()
                                        }
                                    },
                                    enabled = unlocked,
                                    label = { Text(uiString(biscuitOutfitName(name))) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun biscuitOutfitName(name: String): Int = when (name) {
    "glasses" -> com.noop.R.string.biscuit_outfit_glasses
    "nightcap" -> com.noop.R.string.biscuit_outfit_nightcap
    "crown" -> com.noop.R.string.biscuit_outfit_crown
    else -> com.noop.R.string.biscuit_outfit_coach
}
