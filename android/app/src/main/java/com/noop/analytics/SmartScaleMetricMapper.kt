package com.noop.analytics

import com.noop.data.MetricSeriesRow

/** Maps passive scale readings to the metric keys already used by Aura's scale history. */
object SmartScaleMetricMapper {
    const val SOURCE_ID = "smart-scale"

    fun rows(day: String, measurement: ScaleMeasurement, heightCm: Double): List<MetricSeriesRow> {
        val values = linkedMapOf(
            "scale_weight" to measurement.weightKg,
            "scale_bmi" to if (heightCm > 0) measurement.weightKg / (heightCm / 100.0).let { it * it } else null,
            "scale_heart_rate" to measurement.heartRate?.toDouble(),
            "scale_body_fat" to measurement.bodyFatPercent,
            "scale_water_pct" to measurement.waterPercent,
            "scale_bone" to measurement.boneKg,
            "scale_metabolism" to measurement.basalMetabolismKcal,
            "scale_fat_free" to measurement.fatFreeMassKg,
            "scale_visceral" to measurement.visceralFat,
        )
        return values.mapNotNull { (key, value) ->
            value?.takeIf { it.isFinite() && it > 0.0 }?.let { MetricSeriesRow(SOURCE_ID, day, key, it) }
        }
    }
}
