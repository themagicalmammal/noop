package com.noop.core

import com.noop.data.DailyMetric
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

/**
 * Fork (DX23876), Android performance: a process-level cache for core reads so tab switches do not
 * re-run every JNI call. Entries are keyed by operation and parameters and stamped with the data
 * revision they were fetched at; a mismatch (the core's [CoreShadow.dataRevision] moved) refetches.
 * The cache lives for the process: switching tabs recomposes the screen but the data stays here.
 */
object CoreCache {
    private data class Entry<T>(val revision: Long, val value: T)

    private val map = ConcurrentHashMap<String, Entry<*>>()
    private val locks = ConcurrentHashMap<String, ReentrantLock>()

    /**
     * A cache entry fetched within the last [GRACE_PERIOD] revisions is still considered fresh: a
     * revision bump (a sync row, an analysis pass) does not invalidate every tile at once. This
     * prevents the cascade of JNI re-reads when any data changes while the user is browsing.
     */
    private const val GRACE_PERIOD = 100L

    /**
     * Returns the cached value when the entry for [key] was fetched within the last [GRACE_PERIOD]
     * revisions of [rev]; otherwise runs [fetch] and stores the result. The fetch runs at most
     * once per key even when several screens ask at the same time.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, rev: Long, fetch: () -> T): T {
        val hit = map[key] as? Entry<T>
        if (hit != null && rev - hit.revision < GRACE_PERIOD) return hit.value
        val lock = locks.computeIfAbsent(key) { ReentrantLock() }
        lock.lock()
        try {
            val fresh = map[key] as? Entry<T>
            if (fresh != null && rev - fresh.revision < GRACE_PERIOD) return fresh.value
            val value = fetch()
            map[key] = Entry(rev, value)
            return value
        } finally {
            lock.unlock()
        }
    }

    /** Drops everything: call when the whole core resets rather than a single day or series changing. */
    fun clear() { map.clear() }

    // --- Typed helpers for the operations the Forge screens make ---

    fun day(date: String, rev: Long): CoreRead.Day? =
        get("day:$date", rev) { CoreRead.day(date) }

    fun nights(from: Long, to: Long, rev: Long): List<CoreRead.Night> =
        get("nights:$from:$to", rev) { CoreRead.nights(from, to) }

    fun naps(from: Long, to: Long, rev: Long): List<CoreRead.Nap> =
        get("naps:$from:$to", rev) { CoreRead.naps(from, to) }

    fun series(metric: CoreRead.Metric, from: String, to: String, rev: Long): List<CoreRead.Point> =
        get("series:${metric.key}:$from:$to", rev) { CoreRead.series(metric, from, to) }

    fun heartRate(from: Long, to: Long, rev: Long): List<CoreRead.HeartPoint> =
        get("hr:$from:$to", rev) { CoreRead.heartRate(from, to) }

    fun trainingSnapshot(dismissed: List<String>, days: Int, weekStartsOn: String, rev: Long): CoreTraining.Snapshot? =
        get("training:${days}:${weekStartsOn}:${dismissed.hashCode()}", rev) {
            CoreTraining.snapshot(dismissed, days, weekStartsOn)
        }

    fun dailyRows(from: String, to: String, deviceId: String, rev: Long): List<DailyMetric>? =
        get("rows:$from:$to:$deviceId", rev) { CoreDailyRows.rows(from, to, deviceId) }

    /** Today's live Effort (the JNI score over raw HR); keyed by day, resting HR and stored Effort. */
    fun liveEffort(context: android.content.Context, storedEffort: Double?, rev: Long): Double? =
        get("liveEffort:${storedEffort}", rev) {
            CoreLiveEffort.today(context, storedEffort)
        }


    /** A measure's tile history (days, values, skin kind); keyed by key and skin preference. */
    fun tileHistory(key: String, skinTempPrefer: String?, rev: Long): CoreTile.History =
        get("tile:$key:${skinTempPrefer ?: ""}", rev) {
            CoreTile.history(key, skinTempPrefer)
        }

    /** A tile's facts, from its history. */
    fun tileFacts(catalogKey: String, days: List<String>, values: List<Double>, today: String, target: Double?, rev: Long): CoreTile.Facts? =
        get("facts:$catalogKey:$today:${target ?: ""}", rev) {
            CoreTile.facts(catalogKey, days, values, today, target)
        }

    /** A tile's delta from its values. */
    fun tileDelta(key: String, values: List<Double>, rev: Long): CoreTile.Delta? =
        get("delta:$key:${values.hashCode()}", rev) {
            CoreTile.delta(key, values)
        }

    /** Weight/body snapshot, read only while a weight tile is placed. */
    fun bodySnapshot(context: android.content.Context, rev: Long): CoreBody.Snapshot? =
        get("bodySnapshot", rev) { CoreBody.snapshot(context) }

    /** Today's energy total, keyed by the dismissed-workout list. */
    fun energyToday(context: android.content.Context, dismissed: List<String>, rev: Long): CoreEnergyDetail.Summary? =
        get("energy:${dismissed.hashCode()}", rev) { CoreEnergyDetail.today(context, dismissed) }

    /** The day's stress timeline, keyed by whether a personal baseline is used. */
    fun stressDay(personal: Boolean, rev: Long): CoreStress.Day? =
        get("stressDay:${personal}", rev) { CoreStress.day(personal) }

    /** AgeLess snapshot, keyed by revision. */
    fun ageLess(context: android.content.Context, rev: Long): CoreAgeLess.Output? =
        get("ageLess", rev) { CoreAgeLess.snapshot(context) }

    /** Coach notes, keyed by revision. */
    fun coachNotes(rev: Long): List<CoreCoach.Note> =
        get("coachNotes", rev) { CoreCoach.notes() }

}