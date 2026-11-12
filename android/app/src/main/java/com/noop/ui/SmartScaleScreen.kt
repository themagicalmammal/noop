package com.noop.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.noop.R
import com.noop.analytics.ScaleUser
import com.noop.analytics.SmartScaleMetricMapper
import com.noop.ble.SmartScaleScanner
import com.noop.data.MetricSeriesRow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SmartScaleScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scanner = remember { SmartScaleScanner(context.applicationContext) }
    val scan by scanner.state.collectAsStateWithLifecycle()
    var history by remember { androidx.compose.runtime.mutableStateOf<List<MetricSeriesRow>>(emptyList()) }
    val profile = remember { ProfileStore.from(context.applicationContext) }
    val requestScan = rememberRequestScan {
        scanner.start(ScaleUser(profile.age, profile.sex == "male", profile.heightCm))
    }
    DisposableEffect(scanner) { onDispose { scanner.stop() } }
    LaunchedEffect(scan.status, scan.measurement, scan.capturedAtMs) {
        if (scan.status != SmartScaleScanner.Status.IDLE && scan.measurement == null) return@LaunchedEffect
        runCatching {
            scan.measurement?.let { measurement ->
                val day = Instant.ofEpochMilli(scan.capturedAtMs ?: System.currentTimeMillis())
                    .atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                viewModel.repo.upsertMetricSeries(SmartScaleMetricMapper.rows(day, measurement, profile.heightCm))
            }
            viewModel.repo.metricSeries(SmartScaleMetricMapper.SOURCE_ID, "scale_weight", "0000-01-01", "9999-12-31")
                .takeLast(30).asReversed()
        }.onSuccess { history = it }
    }

    LazyScreenScaffold(title = uiString(R.string.smart_scale_title), subtitle = uiString(R.string.smart_scale_subtitle)) {
        item {
            NoopCard {
                Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
                    Text(uiString(R.string.smart_scale_intro), style = NoopType.body, color = Palette.textSecondary)
                    Text(uiString(R.string.smart_scale_receive_only), style = NoopType.caption, color = Palette.textSecondary)
                    NoopButton(
                        text = uiString(if (scan.status == SmartScaleScanner.Status.SCANNING || scan.status == SmartScaleScanner.Status.STEPPED_ON) R.string.smart_scale_scanning else R.string.smart_scale_start),
                        leadingIcon = Icons.Filled.FitnessCenter,
                        onClick = requestScan,
                    )
                }
            }
        }
        if (scan.status != SmartScaleScanner.Status.IDLE) item {
            NoopCard(tint = Palette.accent) {
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = Metrics.gap), verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
                    val statusRes = when (scan.status) {
                        SmartScaleScanner.Status.SCANNING -> R.string.smart_scale_status_scanning
                        SmartScaleScanner.Status.STEPPED_ON -> R.string.smart_scale_status_stepped_on
                        SmartScaleScanner.Status.COMPLETE -> R.string.smart_scale_status_complete
                        SmartScaleScanner.Status.TIMED_OUT -> R.string.smart_scale_status_timeout
                        SmartScaleScanner.Status.UNAVAILABLE -> R.string.smart_scale_status_unavailable
                        SmartScaleScanner.Status.FAILED -> R.string.smart_scale_status_failed
                        SmartScaleScanner.Status.IDLE -> R.string.smart_scale_status_idle
                    }
                    Text(uiString(statusRes), style = NoopType.headline, color = Palette.textPrimary)
                    scan.scaleName?.let { Text(it, style = NoopType.body, color = Palette.textSecondary) }
                    (scan.measurement?.weightKg ?: scan.liveKg)?.let {
                        Text(uiString(R.string.smart_scale_weight, it), style = NoopType.title2, color = Palette.textPrimary)
                    }
                    scan.measurement?.let { m ->
                        Text(uiString(R.string.smart_scale_saved_metrics), style = NoopType.body, color = Palette.textSecondary)
                        m.bodyFatPercent?.let { Text(uiString(R.string.smart_scale_body_fat, it), style = NoopType.body, color = Palette.textSecondary) }
                    }
                }
            }
        }
        item {
            NoopCard {
                Column(verticalArrangement = Arrangement.spacedBy(Metrics.gap)) {
                    Text(uiString(R.string.smart_scale_history), style = NoopType.headline, color = Palette.textPrimary)
                    if (history.isEmpty()) {
                        Text(uiString(R.string.smart_scale_no_history), style = NoopType.body, color = Palette.textSecondary)
                    } else {
                        history.forEach { row ->
                            val formattedDay = runCatching {
                                java.time.LocalDate.parse(row.day).format(DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM))
                            }.getOrDefault(row.day)
                            androidx.compose.foundation.layout.Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(formattedDay, style = NoopType.body, color = Palette.textSecondary)
                                Text(uiString(R.string.smart_scale_weight, row.value), style = NoopType.body, color = Palette.textPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}
