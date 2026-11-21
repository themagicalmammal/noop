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
     * Returns [fetch] if the cache holds no entry for [key] at revision [rev]; otherwise the cached
     * value. The fetch runs at most once per key even when several screens ask at the same time.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, rev: Long, fetch: () -> T): T {
        val hit = map[key] as? Entry<T>
        if (hit != null && hit.revision == rev) return hit.value
        val lock = locks.computeIfAbsent(key) { ReentrantLock() }
        lock.lock()
        try {
            val fresh = map[key] as? Entry<T>
            if (fresh != null && fresh.revision == rev) return fresh.value
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
    fun liveEffort(context: android.content.Context, restingHr: Int?, storedEffort: Double?, rev: Long): Double? =
        get("liveEffort:${restingHr}:${storedEffort}", rev) {
            CoreLiveEffort.today(context, restingHr, storedEffort)
        }

}
