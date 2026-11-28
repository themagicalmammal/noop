package com.noop.ui.forge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.noop.R
import com.noop.ui.AppViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DeviceHub
import androidx.compose.material.icons.rounded.Sync

/**
 * The forge skin's battery screen, mirroring iOS `BatteryDetailView`:
 * a horizontal battery glyph filled to the charge level, the percentage beneath it,
 * status text, the runtime estimate, the battery history chart, the connection details,
 * and the refresh and manage-devices actions.
 */
@Composable
fun ForgeBatteryScreen(viewModel: AppViewModel, onClose: () -> Unit, onOpenDevices: () -> Unit) {
    val live by viewModel.live.collectAsState()
    val colors = Forge.colors

    var samples by remember { mutableStateOf<List<com.noop.data.BatterySample>?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis() / 1000
            samples = viewModel.repo.batterySamples(viewModel.activeStrapId, now - 14L * 86_400, now, limit = 2_000)
        }
    }

    val connected = live.connected
    val pct = live.batteryPct
    val charging = live.charging
    val mv = live.batteryMv

    val levelTint = when {
        pct == null -> colors.textTertiary
        pct <= 15 -> Color(0xFFFF3B30)
        pct <= 35 -> Color(0xFFFF9500)
        else -> Color(0xFF34C75A)
    }

    val statusText = when {
        !connected -> stringResource(R.string.forge_battery_status_not_connected)
        pct == null && charging == true -> stringResource(R.string.forge_battery_charging_waiting)
        pct == null -> stringResource(R.string.forge_battery_waiting_reading)
        charging == true -> stringResource(R.string.forge_battery_charging)
        else -> stringResource(R.string.forge_battery_on_battery)
    }

    val detailText = remember(pct, charging, connected) {
        if (pct == null || charging == true || !connected) return@remember null
        val estimatedHours = when {
            pct <= 15 -> 12.0
            pct <= 30 -> 48.0
            pct <= 50 -> 96.0
            pct <= 70 -> 144.0
            pct <= 85 -> 168.0
            else -> 192.0
        }
        if (estimatedHours < 48) {
            "About ${estimatedHours.toInt()} hours remaining"
        } else {
            "About ${(estimatedHours / 24).toInt()} days remaining"
        }
    }

    ForgeSceneBackdrop(ForgeBackdrop.Sky) {
        SettingsColumn {
            Row(
                Modifier.fillMaxWidth().padding(top = ForgeMetrics.space2, start = ForgeMetrics.space1, end = ForgeMetrics.space1),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.AutoMirrored.Rounded.ArrowBack,
                        null,
                        tint = Forge.sceneInk.text
                    )
                }
                Spacer(Modifier.width(ForgeMetrics.space2))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.forge_battery_strap_battery),
                        style = Forge.type.title1,
                        color = Forge.sceneInk.text
                    )
                    Text(
                        stringResource(R.string.forge_battery_subtitle),
                        style = Forge.type.subhead,
                        color = colors.textSecondary
                    )
                }
            }

            ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius) {
                Column(
                    Modifier.fillMaxWidth().padding(vertical = ForgeMetrics.space3),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BatteryGlyph(
                        fraction = pct?.let { max(0.0, min(100.0, it)) / 100.0 } ?: 0.0,
                        charging = charging == true,
                        tint = levelTint
                    )
                    if (pct != null) {
                        Text(
                            "${pct.toInt()}%",
                            style = androidx.compose.ui.text.TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Forge.sceneInk.text,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        )
                    }
                    Text(
                        statusText,
                        style = Forge.type.headline,
                        color = Forge.sceneInk.text
                    )
                    detailText?.let {
                        Text(
                            it,
                            style = Forge.type.subhead,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            val sampleList = samples
            if (connected && sampleList != null && sampleList.size > 1) {
                ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius) {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = ForgeMetrics.space3),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.forge_battery_history),
                            style = Forge.type.headline,
                            color = Forge.sceneInk.text
                        )
                        BatteryHistoryChart(sampleList.map { it.soc ?: 0.0 }, levelTint)
                    }
                }
            }

            ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius) {
                Column(Modifier.fillMaxWidth().padding(vertical = ForgeMetrics.space3)) {
                    DetailRow(
                        stringResource(R.string.forge_battery_connection),
                        if (connected) stringResource(R.string.forge_battery_connected)
                        else stringResource(R.string.forge_battery_value_not_connected)
                    )
                    if (connected && mv != null) {
                        ForgeDivider(
                            Modifier.padding(horizontal = ForgeMetrics.space1),
                            startPadding = 0.dp, endPadding = 0.dp,
                            topSpacing = ForgeMetrics.space2, bottomSpacing = ForgeMetrics.space2
                        )
                        DetailRow(
                            stringResource(R.string.forge_battery_voltage),
                            "%.2f V".format(mv / 1000.0)
                        )
                    }
                    val lastTs = sampleList?.lastOrNull()?.ts
                    if (connected && lastTs != null) {
                        ForgeDivider(
                            Modifier.padding(horizontal = ForgeMetrics.space1),
                            startPadding = 0.dp, endPadding = 0.dp,
                            topSpacing = ForgeMetrics.space2, bottomSpacing = ForgeMetrics.space2
                        )
                        DetailRow(
                            stringResource(R.string.forge_battery_last_reading),
                            relativeTime(lastTs)
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)
            ) {
                BatteryButton(
                    text = stringResource(R.string.forge_battery_refresh),
                    enabled = connected,
                    primary = true,
                    icon = androidx.compose.material.icons.Icons.Rounded.Sync,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onClick = { viewModel.getBattery() }
                )
                BatteryButton(
                    text = stringResource(R.string.forge_battery_manage_devices),
                    enabled = true,
                    primary = false,
                    icon = androidx.compose.material.icons.Icons.Rounded.DeviceHub,
                    colors = colors,
                    modifier = Modifier.weight(1f),
                    onClick = onOpenDevices
                )
            }
        }
    }
}

/**
 * The horizontal battery glyph from iOS `StrapBatteryGlyph`: a rounded rectangle filled to
 * the charge level with the level tint, a small terminal bump on the right, and a lightning
 * bolt overlay while charging.
 */
@Composable
private fun BatteryGlyph(fraction: Double, charging: Boolean, tint: Color) {
    val bodyWidth = 160.dp
    val bodyHeight = 70.dp
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(bodyWidth).height(bodyHeight)
                .clip(RoundedCornerShape(16.dp))
                .drawBehind {
                    val cr = CornerRadius(16.dp.toPx())
                    drawRoundRect(
                        color = if (fraction > 0) tint.copy(alpha = 0.55f) else Color(0xFF8E8E93).copy(alpha = 0.6f),
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height),
                        cornerRadius = cr
                    )
                }
                .drawBehind {
                    if (fraction > 0) {
                        val fillWidth = (size.width - 12.dp.toPx()) * fraction.toFloat()
                        val cr = CornerRadius(10.dp.toPx())
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                listOf(tint.copy(alpha = 0.75f), tint),
                                Offset.Zero, Offset(size.width, size.height)
                            ),
                            topLeft = Offset(6.dp.toPx(), 6.dp.toPx()),
                            size = Size(fillWidth, size.height - 12.dp.toPx()),
                            cornerRadius = cr
                        )
                    }
                }
        ) {
            if (charging) {
                Icon(
                    androidx.compose.material.icons.Icons.Rounded.Bolt,
                    null,
                    tint = Color.White,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                )
            }
        }
        Box(
            Modifier.width(6.dp).height(24.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(if (fraction > 0) tint.copy(alpha = 0.7f) else Color(0xFF8E8E93).copy(alpha = 0.6f))
        )
    }
}

/**
 * Battery history chart: a line chart of SOC samples over time with the level tint, matching
 * iOS `BatteryHistoryCard`.
 */
@Composable
private fun BatteryHistoryChart(values: List<Double>, tint: Color) {
    if (values.isEmpty()) return
    val gridColor = Color(0xFFAEAEB2).copy(alpha = 0.3f)
    Canvas(Modifier.fillMaxWidth().height(140.dp)) {
        val padLeft = 16f
        val padRight = 8f
        val padTop = 8f
        val padBottom = 24f
        val w = size.width
        val h = size.height
        val chartW = w - padLeft - padRight
        val chartH = h - padTop - padBottom
        val minV = 0.0f
        val maxV = 100.0f

        for (frac in listOf(0f, 0.5f, 1f)) {
            val y = padTop + chartH * (1 - frac)
            drawLine(gridColor, Offset(padLeft, y), Offset(w - padRight, y), 1f)
        }

        if (values.size > 1) {
            val path = Path()
            for (i in values.indices) {
                val x = padLeft + chartW * (i.toFloat() / (values.size - 1))
                val v = values[i].toFloat().coerceIn(minV, maxV)
                val y = padTop + chartH * (1 - (v - minV) / (maxV - minV))
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path, tint,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                )
            )

            val areaPath = Path()
            areaPath.moveTo(padLeft, padTop + chartH)
            for (i in values.indices) {
                val x = padLeft + chartW * (i.toFloat() / (values.size - 1))
                val v = values[i].toFloat().coerceIn(minV, maxV)
                val y = padTop + chartH * (1 - (v - minV) / (maxV - minV))
                areaPath.lineTo(x, y)
            }
            areaPath.lineTo(w - padRight, padTop + chartH)
            areaPath.close()
            drawPath(
                areaPath,
                Brush.linearGradient(
                    listOf(tint.copy(alpha = 0.3f), tint.copy(alpha = 0.05f)),
                    Offset(0f, 0f), Offset(0f, size.height)
                )
            )
        }
    }
}

/** A single detail row: label on the left, value on the right. */
@Composable
private fun DetailRow(label: String, value: String) {
    val colors = Forge.colors
    Row(
        Modifier.fillMaxWidth().padding(horizontal = ForgeMetrics.space1, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = Forge.type.subhead, color = colors.textSecondary)
        Spacer(Modifier.weight(1f))
        Text(value, style = Forge.type.subhead, color = Forge.sceneInk.text)
    }
}

/** A relative time string like "12 minutes ago". */
private fun relativeTime(ts: Long): String {
    val now = System.currentTimeMillis() / 1000
    val diff = now - ts
    return when {
        diff < 60 -> "Just now"
        diff < 3600 -> "${(diff / 60).toInt()} minutes ago"
        diff < 86400 -> "${(diff / 3600).toInt()} hours ago"
        else -> "${(diff / 86400).toInt()} days ago"
    }
}

/**
 * A rounded button with an icon and label, matching iOS `NoopButton`. Primary uses an accent
 * fill with white text; secondary uses the inset surface with accent text.
 */
@Composable
private fun BatteryButton(
    text: String,
    enabled: Boolean,
    primary: Boolean,
    icon: ImageVector,
    colors: ForgeColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bg = if (primary) colors.accent else colors.inset
    val fg = if (primary) Color.White else colors.accent
    Box(
        modifier.fillMaxWidth()
            .clickable(enabled = enabled) { onClick() }
            .clip(RoundedCornerShape(24.dp))
            .background(bg.copy(alpha = if (enabled) 1f else 0.4f))
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(16.dp))
            Text(
                text,
                style = Forge.type.headline,
                color = fg.copy(alpha = if (enabled) 1f else 0.5f)
            )
        }
    }
}
