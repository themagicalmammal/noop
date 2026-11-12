package com.noop.analytics

/** BLE advertisement data handed to the passive scale decoders. */
data class ScaleAdvertisement(
    val name: String = "",
    val manufacturerData: Map<Int, ByteArray> = emptyMap(),
    val serviceUuids: Set<String> = emptySet(),
)

data class ScaleUser(val age: Int, val male: Boolean, val heightCm: Double)

data class ScaleMeasurement(
    val weightKg: Double,
    val resistanceOhm: Double? = null,
    val heartRate: Int? = null,
    val bodyFatPercent: Double? = null,
    val waterPercent: Double? = null,
    val musclePercent: Double? = null,
    val boneKg: Double? = null,
    val basalMetabolismKcal: Double? = null,
    val fatFreeMassKg: Double? = null,
    val visceralFat: Double? = null,
)

sealed interface ScaleBroadcastResult {
    data object Ignored : ScaleBroadcastResult
    data class Measuring(val liveKg: Double?) : ScaleBroadcastResult
    data class Finished(val measurement: ScaleMeasurement) : ScaleBroadcastResult
}

/** Receive-only decoder contract. A driver cannot emit a BLE write or connect command. */
interface ScaleBroadcastDriver {
    val id: String
    val displayName: String
    fun matches(ad: ScaleAdvertisement): Boolean
    fun handle(ad: ScaleAdvertisement, user: ScaleUser): ScaleBroadcastResult
    fun reset()
}

/** Port of Aura's `StandardImpedance`; used only for the Fit 8S broadcast's measured impedance. */
object StandardScaleImpedance {
    fun apply(m: ScaleMeasurement, resistance: Double, user: ScaleUser): ScaleMeasurement {
        if (resistance <= 0 || user.heightCm <= 0 || m.weightKg <= 0) return m
        val w = m.weightKg
        val index = user.heightCm * user.heightCm / resistance
        val ffm = if (user.male) -10.68 + 0.65 * index + 0.26 * w + 0.02 * resistance
        else -9.53 + 0.69 * index + 0.17 * w + 0.02 * resistance
        val liters = if (user.male) 1.2 + 0.45 * index + 0.18 * w else 3.75 + 0.45 * index + 0.11 * w
        val skeletal = 0.401 * index + (if (user.male) 3.825 else 0.0) - 0.071 * user.age + 5.102
        return m.copy(
            resistanceOhm = resistance,
            fatFreeMassKg = ffm,
            bodyFatPercent = (1 - ffm / w) * 100,
            waterPercent = 0.99513 * liters / w * 100,
            musclePercent = skeletal / w * 100,
            boneKg = (if (user.male) 0.057 else 0.05) * ffm,
            basalMetabolismKcal = ffm * 21.6 + 370,
        )
    }
}

class EtekcityFit8SDriver : ScaleBroadcastDriver {
    override val id = "etekcity-fit8s"
    override val displayName = "Etekcity Fit 8S"
    override fun matches(ad: ScaleAdvertisement): Boolean =
        0x06D0 in ad.manufacturerData && ad.serviceUuids.any { it.equals("FFD0", ignoreCase = true) }

    override fun handle(ad: ScaleAdvertisement, user: ScaleUser): ScaleBroadcastResult {
        val payload = ad.manufacturerData[0x06D0] ?: return ScaleBroadcastResult.Ignored
        if (payload.size != 20) return ScaleBroadcastResult.Ignored
        val grams = u24le(payload, 10)
        if (payload[15].toInt() and 0xff != 1) {
            return ScaleBroadcastResult.Measuring(grams.takeIf { it > 0 }?.div(1000.0))
        }
        if (grams <= 0) return ScaleBroadcastResult.Measuring(null)
        var result = ScaleMeasurement(weightKg = grams / 1000.0)
        val resistance = u16le(payload, 13)
        if (resistance != 0) result = StandardScaleImpedance.apply(result, resistance.toDouble(), user)
        return ScaleBroadcastResult.Finished(result)
    }

    override fun reset() = Unit
}

class AaaxScaleDriver : ScaleBroadcastDriver {
    override val id = "aaax"
    override val displayName = "AAA-series scale"
    private var published = false
    private val names = setOf("AAA002", "AAA007", "AAA013", "AAA07A")

    override fun matches(ad: ScaleAdvertisement): Boolean = ad.name.trim() in names

    override fun handle(ad: ScaleAdvertisement, user: ScaleUser): ScaleBroadcastResult {
        for ((company, data) in ad.manufacturerData) {
            val p = decode(company, data) ?: continue
            if ((p[4].toInt() and 0xff) == 0xA6) return ScaleBroadcastResult.Measuring(null)
            if ((p[4].toInt() and 0xff) != 0xAD) continue
            val value = p.take(4).fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xff) }
            val grams = (value and 0x3ffff).toInt()
            if ((value ushr 31) != 1L || grams <= 0 || published) {
                return ScaleBroadcastResult.Measuring(grams.takeIf { it > 0 }?.div(1000.0))
            }
            published = true
            return ScaleBroadcastResult.Finished(ScaleMeasurement(weightKg = grams / 1000.0))
        }
        return ScaleBroadcastResult.Ignored
    }

    override fun reset() { published = false }

    companion object {
        fun decode(company: Int, data: ByteArray): ByteArray? {
            if (data.size < 12) return null
            val key = (company ushr 8).toByte()
            val p = ByteArray(6) { i -> (data[6 + i].toInt() xor key.toInt()).toByte() }
            val sum = (0..4).sumOf { p[it].toInt() and 0xff }
            if ((sum and 0x1f) != (p[5].toInt() and 0x1f)) return null
            return p
        }
    }
}

class ScaleUpDriver : ScaleBroadcastDriver {
    override val id = "scaleup"
    override val displayName = "Scale Up"
    private var published = false

    override fun matches(ad: ScaleAdvertisement): Boolean =
        ad.name.trim().equals("da", ignoreCase = true) || ad.manufacturerData.keys.any { it and 0xff in setOf(0xd0, 0xe0) }

    override fun handle(ad: ScaleAdvertisement, user: ScaleUser): ScaleBroadcastResult {
        for ((key, data) in ad.manufacturerData) {
            if (data.size < 9) continue
            val flag = key and 0xff
            if (flag != 0xd0 && flag != 0xe0) continue
            if (flag == 0xd0) published = false
            val kg = (((key ushr 8) shl 8) or (data[0].toInt() and 0xff)) / 100.0
            if (kg !in 0.5..300.0) continue
            if (flag == 0xd0 || published) return ScaleBroadcastResult.Measuring(kg)
            published = true
            return ScaleBroadcastResult.Finished(ScaleMeasurement(weightKg = kg))
        }
        return ScaleBroadcastResult.Ignored
    }

    override fun reset() { published = false }
}

/** Passive QN broadcast scales use an AA BB marker and advertise hundredths of a kilogram. */
class QnBroadcastDriver : ScaleBroadcastDriver {
    override val id = "qn-broadcast"
    override val displayName = "QN Scale"
    private var published = false

    override fun matches(ad: ScaleAdvertisement): Boolean =
        ad.manufacturerData[COMPANY_ID]?.let(::hasMagic) == true

    override fun handle(ad: ScaleAdvertisement, user: ScaleUser): ScaleBroadcastResult {
        if (published) return ScaleBroadcastResult.Ignored
        val payload = ad.manufacturerData[COMPANY_ID] ?: return ScaleBroadcastResult.Ignored
        if (payload.size < 19 || !hasMagic(payload)) return ScaleBroadcastResult.Ignored
        val kg = u16le(payload, 17) / 100.0
        if (kg !in 0.5..300.0) return ScaleBroadcastResult.Ignored
        val status = payload[15].toInt() and 0xff
        val ft26r = payload.size >= 22 && payload[19] == 0x51.toByte() &&
            payload[20] == 0x0e.toByte() && payload[21] == 0x03.toByte()
        val stableMask = if (ft26r) 0x21 else 0x20
        if (status and stableMask == 0) return ScaleBroadcastResult.Measuring(kg)
        published = true
        return ScaleBroadcastResult.Finished(ScaleMeasurement(weightKg = kg))
    }

    override fun reset() { published = false }

    private fun hasMagic(data: ByteArray): Boolean =
        data.size >= 2 && data[0] == 0xaa.toByte() && data[1] == 0xbb.toByte()

    private companion object { const val COMPANY_ID = 0xffff }
}

/** Yunmai X passive advertisement format, including its XOR integrity byte and impedance estimate. */
class YunmaiXDriver : ScaleBroadcastDriver {
    override val id = "yunmai-x"
    override val displayName = "Yunmai X"
    private var armed = true

    override fun matches(ad: ScaleAdvertisement): Boolean =
        frame(ad) != null || ad.serviceUuids.any { it.equals("1320", ignoreCase = true) }

    override fun handle(ad: ScaleAdvertisement, user: ScaleUser): ScaleBroadcastResult {
        val frame = frame(ad) ?: return ScaleBroadcastResult.Ignored
        return when (frame.state) {
            1, 2 -> {
                armed = true
                ScaleBroadcastResult.Measuring(frame.weightKg.takeIf { it > 0 })
            }
            3 -> {
                if (!armed) return ScaleBroadcastResult.Measuring(null)
                if (frame.weightKg <= 0) return ScaleBroadcastResult.Ignored
                armed = false
                ScaleBroadcastResult.Finished(measurement(frame, user))
            }
            else -> ScaleBroadcastResult.Ignored
        }
    }

    override fun reset() { armed = true }

    private fun frame(ad: ScaleAdvertisement): Frame? {
        for ((_, payload) in ad.manufacturerData.toSortedMap()) {
            if (payload.size != 14) continue
            val signature = payload.sliceArray(4..6)
            if (!signature.contentEquals(byteArrayOf(0x0b, 0x74, 0x17)) &&
                !signature.contentEquals(byteArrayOf(0x06, 0x52, 0x11))) continue
            val checksum = (7..12).fold(0) { acc, i -> acc xor (payload[i].toInt() and 0xff) }
            if (checksum != (payload[13].toInt() and 0xff)) continue
            return Frame(
                state = payload[8].toInt() and 0xff,
                weightKg = u16be(payload, 9) / 100.0,
                impedanceOhm = u16be(payload, 11),
            )
        }
        return null
    }

    private fun measurement(frame: Frame, user: ScaleUser): ScaleMeasurement {
        val result = ScaleMeasurement(weightKg = frame.weightKg)
        val resistance = frame.impedanceOhm
        if (resistance <= 0 || user.heightCm <= 0 || user.age <= 0) return result
        var normalizedResistance = (resistance.toFloat() - 100f) / 100f
        if (normalizedResistance >= 1f) normalizedResistance = kotlin.math.sqrt(normalizedResistance.toDouble()).toFloat()
        val weight = frame.weightKg.toFloat()
        val height = user.heightCm.toFloat()
        val h = height / 100f
        var fat = (weight * 1.5f / h / h) + (user.age.toFloat() * 0.08f)
        if (user.male) fat -= 10.8f
        fat = (fat - 7.4f) + normalizedResistance
        if (fat < 5f || fat > 75f || !fat.isFinite()) return result
        val muscle = (100f - fat) * 0.67f
        val roundedMuscle = ((muscle * 100f) + 0.5f) / 100f
        val heightDelta = height - 170f
        val bone = if (user.male) {
            ((weight * (roundedMuscle / 100f) * 4f) / 7f * 0.22f * 0.6f) + (heightDelta / 100f)
        } else {
            ((weight * (roundedMuscle / 100f) * 4f) / 7f * 0.34f * 0.45f) + (heightDelta / 100f)
        }
        return result.copy(
            resistanceOhm = resistance.toDouble(),
            bodyFatPercent = fat.toDouble(),
            waterPercent = (((100f - fat) * 0.726f * 100f + 0.5f) / 100f).toDouble(),
            musclePercent = roundedMuscle.toDouble(),
            boneKg = (((bone * 10f) + 0.5f) / 10f).toDouble(),
            fatFreeMassKg = (weight * (100f - fat) / 100f).toDouble(),
            visceralFat = yunmaiVisceralFat(fat, user),
        )
    }

    private fun yunmaiVisceralFat(bodyFat: Float, user: ScaleUser): Double {
        val age = if (user.age < 18 || user.age > 120) 18 else user.age
        val adjusted = bodyFat - if (user.male) {
            if (age < 40) 21 else if (age < 60) 22 else 24
        } else {
            if (age < 40) 34 else if (age < 60) 35 else 36
        }.toFloat()
        val divisor = if (adjusted > 0f) 1.1f else if (user.male) 1.4f else 1.8f
        return ((adjusted / divisor) + 9.5f).coerceIn(1f, 30f).toDouble()
    }

    private data class Frame(val state: Int, val weightKg: Double, val impedanceOhm: Int)
}

object SmartScaleBroadcastCatalog {
    fun create(): List<ScaleBroadcastDriver> = listOf(
        EtekcityFit8SDriver(), AaaxScaleDriver(), ScaleUpDriver(), QnBroadcastDriver(), YunmaiXDriver(),
    )
}

private fun u16le(data: ByteArray, offset: Int): Int =
    (data[offset].toInt() and 0xff) or ((data[offset + 1].toInt() and 0xff) shl 8)

private fun u24le(data: ByteArray, offset: Int): Int =
    u16le(data, offset) or ((data[offset + 2].toInt() and 0xff) shl 16)

private fun u16be(data: ByteArray, offset: Int): Int =
    ((data[offset].toInt() and 0xff) shl 8) or (data[offset + 1].toInt() and 0xff)
