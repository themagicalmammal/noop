package com.noop.analytics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartScaleBroadcastTest {
    private val user = ScaleUser(age = 35, male = true, heightCm = 178.0)

    @Test fun etekcityFit8SDecodesFinalReadingAndComposition() {
        val payload = ByteArray(20)
        payload[10] = 0x3e
        payload[11] = 0x2a
        payload[12] = 0x01
        payload[13] = 0xBC.toByte()
        payload[14] = 0x02
        payload[15] = 1
        val driver = EtekcityFit8SDriver()
        val ad = ScaleAdvertisement(manufacturerData = mapOf(0x06D0 to payload), serviceUuids = setOf("ffd0"))
        assertTrue(driver.matches(ad))
        val result = driver.handle(ad, user) as ScaleBroadcastResult.Finished
        assertEquals(76.35, result.measurement.weightKg, 0.001)
        assertEquals(700.0, result.measurement.resistanceOhm!!, 0.001)
        assertTrue(result.measurement.bodyFatPercent!! > 0)
    }

    @Test fun etekcityRejectsMalformedPayload() {
        val driver = EtekcityFit8SDriver()
        val ad = ScaleAdvertisement(manufacturerData = mapOf(0x06D0 to ByteArray(19)), serviceUuids = setOf("ffd0"))
        assertEquals(ScaleBroadcastResult.Ignored, driver.handle(ad, user))
    }

    @Test fun aaaxChecksChecksumAndPublishesCompletedWeightOnce() {
        val plain = byteArrayOf(0x80.toByte(), 0, 0x03, 0xE8.toByte(), 0xAD.toByte(), 24)
        val data = ByteArray(12)
        val xorKey = 0x12
        plain.forEachIndexed { i, b -> data[6 + i] = (b.toInt() xor xorKey).toByte() }
        val driver = AaaxScaleDriver()
        val ad = ScaleAdvertisement(name = "AAA002", manufacturerData = mapOf(0x1234 to data))
        assertTrue(driver.matches(ad))
        val finished = driver.handle(ad, user) as ScaleBroadcastResult.Finished
        assertEquals(1.0, finished.measurement.weightKg, 0.0001)
        assertTrue(driver.handle(ad, user) is ScaleBroadcastResult.Measuring)
        driver.reset()
        assertTrue(driver.handle(ad, user) is ScaleBroadcastResult.Finished)
        data[11] = (data[11].toInt() xor 1).toByte()
        assertFalse(AaaxScaleDriver.decode(0x1234, data) != null)
    }

    @Test fun scaleUpSeparatesLiveAndFinalPackets() {
        val driver = ScaleUpDriver()
        val live = ScaleAdvertisement(name = "da", manufacturerData = mapOf(0x12D0 to byteArrayOf(0x34) + ByteArray(8)))
        assertTrue(driver.matches(live))
        assertEquals(ScaleBroadcastResult.Measuring(46.6), driver.handle(live, user))
        val done = live.copy(manufacturerData = mapOf(0x12E0 to byteArrayOf(0x34) + ByteArray(8)))
        val result = driver.handle(done, user) as ScaleBroadcastResult.Finished
        assertEquals(46.6, result.measurement.weightKg, 0.001)
    }

    @Test fun qnBroadcastWaitsForStableFlagAndAcceptsFt26rFlag() {
        val payload = ByteArray(22)
        payload[0] = 0xaa.toByte(); payload[1] = 0xbb.toByte()
        payload[15] = 0x00
        payload[17] = 0x2c
        payload[18] = 0x01 // 0x012c = 300 hundredths = 3 kg
        payload[19] = 0x51; payload[20] = 0x0e; payload[21] = 0x03
        val driver = QnBroadcastDriver()
        val ad = ScaleAdvertisement(manufacturerData = mapOf(0xffff to payload))
        assertTrue(driver.matches(ad))
        assertEquals(ScaleBroadcastResult.Measuring(3.0), driver.handle(ad, user))
        payload[15] = 0x21
        val result = driver.handle(ad, user) as ScaleBroadcastResult.Finished
        assertEquals(3.0, result.measurement.weightKg, 0.001)
        assertEquals(ScaleBroadcastResult.Ignored, driver.handle(ad, user))
    }

    @Test fun yunmaiXValidatesXorAndPublishesFinalCompositionOnce() {
        val payload = ByteArray(14)
        payload[4] = 0x0b; payload[5] = 0x74; payload[6] = 0x17
        payload[8] = 3
        payload[9] = 0x1d; payload[10] = 0xd3.toByte() // 76.35 kg
        payload[11] = 0x02; payload[12] = 0xbc.toByte() // 700 ohm
        payload[13] = (7..12).fold(0) { acc, i -> acc xor (payload[i].toInt() and 0xff) }.toByte()
        val driver = YunmaiXDriver()
        val ad = ScaleAdvertisement(manufacturerData = mapOf(0x1234 to payload))
        assertTrue(driver.matches(ad))
        val completed = driver.handle(ad, user) as ScaleBroadcastResult.Finished
        assertEquals(76.35, completed.measurement.weightKg, 0.001)
        assertEquals(700.0, completed.measurement.resistanceOhm!!, 0.001)
        assertTrue(completed.measurement.bodyFatPercent!! in 5.0..75.0)
        assertTrue(driver.handle(ad, user) is ScaleBroadcastResult.Measuring)
        driver.reset()
        assertTrue(driver.handle(ad, user) is ScaleBroadcastResult.Finished)
        payload[13] = (payload[13].toInt() xor 1).toByte()
        assertEquals(ScaleBroadcastResult.Ignored, driver.handle(ad, user))
    }

    @Test fun metricMapperUsesAuraKeysAndProfileHeightForBmi() {
        val rows = SmartScaleMetricMapper.rows("2026-11-12", ScaleMeasurement(weightKg = 80.0, bodyFatPercent = 20.0), 200.0)
        assertEquals(setOf("scale_weight", "scale_bmi", "scale_body_fat"), rows.map { it.key }.toSet())
        assertEquals(20.0, rows.first { it.key == "scale_bmi" }.value, 0.0001)
        assertTrue(rows.all { it.deviceId == "smart-scale" && it.day == "2026-11-12" })
    }
}
