package com.noop.ui

import com.noop.R
import androidx.compose.ui.res.stringResource
import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

@Composable
internal fun ThreeDaySelectorBar(
    selectedOffset: Int,
    onSelect: (Int) -> Unit,
) {
    val base = LocalDate.now()
    val blockShape = RoundedCornerShape(Metrics.cornerSm)
    Row(horizontalArrangement = Arrangement.spacedBy(Metrics.selectorSpacing)) {
        listOf(2, 1, 0).forEach { offset ->
            val day = base.minusDays(offset.toLong())
            val selected = selectedOffset == offset
            val label = when (offset) {
                0 -> "Today"
                1 -> "Yesterday"
                else -> "2 days ago"
            }
            val date = day.format(DateTimeFormatter.ofPattern("d MMM", Locale.US))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(blockShape)
                    .background(
                        if (selected) Palette.accent.copy(alpha = StrandAlpha.selectedFill)
                        else Palette.surfaceInset,
                    )
                    .border(
                        width = Metrics.divider,
                        color = if (selected) {
                            Palette.accent.copy(alpha = StrandAlpha.selectedBorder)
                        } else {
                            Palette.hairline
                        },
                        shape = blockShape,
                    )
                    .clickable { onSelect(offset) }
                    .padding(vertical = Metrics.selectorPadding, horizontal = Metrics.selectorPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = label,
                    style = NoopType.caption,
                    color = if (selected) Palette.textPrimary else Palette.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = date,
                    style = NoopType.captionNumber,
                    // Selected date reads in bright gold-light; unselected sits muted.
                    color = if (selected) Palette.accentHover else Palette.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = Metrics.space2),
                )
            }
        }
    }
}

/**
 * Chevron-navigation day selector for the Today screen. The left chevron steps one day
 * older, the right one day newer (disabled at today so a future day can't be selected),
 * and tapping the centre accent block opens a [DatePickerDialog] capped at today for a
 * direct jump to any past date. Mirrors the SleepScreen night navigator, replacing the
 * fixed three-day strip with unbounded back-navigation while keeping the same tokens.
 */


@Composable
internal fun InsetChartPlaceholder(
    message: String,
    height: Dp = Metrics.compactChartHeight,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(Metrics.cornerSm))
            .background(Palette.surfaceInset),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, style = NoopType.subhead, color = Palette.textTertiary)
    }
}


