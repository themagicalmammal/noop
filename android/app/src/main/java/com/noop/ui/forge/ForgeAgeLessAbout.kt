package com.noop.ui.forge

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.noop.R

/**
 * Fork (DX23876), Android parity plan 2.3: the AgeLess about sheet as iOS's `AgeLessAboutSheet`.
 * What AgeLess is, the studies it rests on (linked), and the calculation one level down.
 * Opened from the info button on the AgeLess screen.
 */
internal data class AgeLessStudy(val component: String, val citation: String, val doi: String)

internal val AGELESS_STUDIES = listOf(
    AgeLessStudy("sleepConsistency", "Windred et al., Sleep 2024", "10.1093/sleep/zsad253"),
    AgeLessStudy("sleepDuration", "Saint-Maurice et al., Sleep 2024", "10.1093/sleep/zsad312"),
    AgeLessStudy("dailySteps", "Paluch et al., Lancet Public Health 2022", "10.1016/S2468-2667(21)00302-9"),
    AgeLessStudy("zone13", "Lee et al., Circulation 2022", "10.1161/CIRCULATIONAHA.121.058162"),
    AgeLessStudy("zone45", "Ahmadi et al., Eur Heart J 2022", "10.1093/eurheartj/ehac572"),
    AgeLessStudy("strength", "Momma et al., Br J Sports Med 2022", "10.1136/bjsports-2021-105061"),
    AgeLessStudy("vo2max", "Kodama et al., JAMA 2009", "10.1001/jama.2009.681"),
    AgeLessStudy("restingHR", "Zhang et al., CMAJ 2016", "10.1503/cmaj.150535"),
    AgeLessStudy("leanMass", "Jayedi et al., Int J Obes 2022", "10.1038/s41366-022-01165-5"),
)

@Composable
internal fun ForgeAgeLessAboutSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = Forge.colors

    SettingsColumn {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start,
        ) {
            // Header illustration
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF8ED8C8),
                                Color(0xFF6BA3A0),
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(ForgeMetrics.space4),
                horizontalAlignment = Alignment.Start,
            ) {
                Text("AgeLess", style = Forge.type.title1, color = colors.text)
                Text(
                    "How young does your everyday life keep you?",
                    style = Forge.type.headline,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = ForgeMetrics.space2),
                )
            }

            Text(
                "AgeLess shows how old your body seems, measured by sleep, movement and fitness. " +
                    "Sleep well, move a lot, stay fit, and the number gets younger.",
                style = Forge.type.body,
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = ForgeMetrics.space4),
            )

            aboutSection(
                title = "Getting younger is possible",
                text = "Small steps count. AgeLess shows you which one helps you most.",
            )
            aboutSection(
                title = "Your pace",
                text = "Aging speed shows where your last month is taking you.",
            )

            aboutSection(
                title = "Built on science",
                text = "Each of the nine measures is linked to a long, healthy life in large studies with " +
                    "tens of thousands of people. Tap a study to read it.",
            )

            AGELESS_STUDIES.forEach { study ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("https://doi.org/${study.doi}"))
                            )
                        }
                        .padding(ForgeMetrics.space3),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        study.component,
                        style = Forge.type.subhead.copy(fontWeight = FontWeight.Medium),
                        color = colors.text,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        study.citation,
                        style = Forge.type.caption,
                        color = colors.textSecondary,
                    )
                }
            }

            aboutSection(
                title = "The method",
                text = "Nine measures of sleep, activity and fitness over the last six months, each compared " +
                    "with a recommendation for good long-term health. Large studies link every measure to " +
                    "all-cause mortality; the difference in risk is turned into years.",
            )

            Text(
                "Beta. Not a medical value.",
                style = Forge.type.caption,
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = ForgeMetrics.space4),
            )
        }
    }
}

@Composable
private fun aboutSection(title: String, text: String) {
    val colors = Forge.colors
    Column(
        modifier = Modifier.padding(horizontal = ForgeMetrics.space4),
        horizontalAlignment = Alignment.Start,
    ) {
        Text(title, style = Forge.type.headline, color = colors.text)
        Text(text, style = Forge.type.body, color = colors.textSecondary)
    }
}
