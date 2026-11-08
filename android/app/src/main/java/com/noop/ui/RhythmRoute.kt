package com.noop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.noop.analytics.RhythmEmptyState
import com.noop.analytics.RhythmScreener
import com.noop.data.GravitySample
import com.noop.data.RrInterval
import com.noop.protocol.RrInterval as ProtocolRrInterval
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.sqrt

/**
 * The data-loading route for the experimental Rhythm visualization. Loads the most recent
 * banked sleep session, pulls its R-R + gravity samples, windows the night into 5-minute slices,
 * gates each on stillness, and runs the pure [RhythmScreener] engine. Self-gates on consent
 * (handled inside [RhythmScreen]).
 *
 * Mirrors macOS `RhythmHost.load()` logic for cross-platform parity.
 */
@Composable
fun RhythmRoute(viewModel: AppViewModel) {
    // State: the three outputs RhythmScreen takes
    var night by remember {
        mutableStateOf<RhythmScreener.NightRhythmSummary?>(null)
    }
    var windows by remember {
        mutableStateOf<List<RhythmScreener.WindowResult>>(emptyList())
    }
    var emptyReason by remember {
        mutableStateOf(RhythmEmptyState.GATHERING_DATA)
    }

    // Load data once on composition (compute-once-per-host-lifetime pattern)
    LaunchedEffect(Unit) {
        loadRhythmData(viewModel, onLoaded = { n, w, e ->
            night = n
            windows = w
            emptyReason = e
        })
    }

    // Feed the screen (consent gate is inside RhythmScreen)
    RhythmScreen(
        night = night,
        windows = windows,
        emptyReason = emptyReason,
        onClose = null,  // No close callback needed when navigated to
    )
}

/**
 * Load the most recent banked night's R-R windows, run the pure [RhythmScreener] over each
 * still, resting window. All math is on-device; nothing is computed until the user passes
 * the consent gate (handled by [RhythmScreen]).
 */
private suspend fun loadRhythmData(
    viewModel: AppViewModel,
    onLoaded: (
        night: RhythmScreener.NightRhythmSummary?,
        windows: List<RhythmScreener.WindowResult>,
        emptyReason: RhythmEmptyState
    ) -> Unit
) {
    // allSleepSessionsUnion reads across EVERY registered WHOOP regardless of which id is "active",
    // so passing the (possibly stale, possibly never-connected) active strap id is safe here.
    val deviceId = viewModel.activeStrapId
    val repo = viewModel.repo

    // Steps 1-5 run off the main thread: a night can carry tens of thousands of R-R and
    // gravity rows, and the windowing pass must not block UI composition.
    val outcome = withContext(Dispatchers.Default) {
        // Step 1: Load the most recent sleep session (last 14 days), imported UNION computed across every
        // registered strap, matching Swift `allSleepSessions(days: 14)` exactly.
        val sessions = runCatching {
            repo.allSleepSessionsUnion(deviceId, days = 14)
        }.getOrDefault(emptyList())

        // Sessions are sorted by effectiveStartTs ascending, so the last entry is the most recent night.
        val lastSleep = sessions.lastOrNull() ?: run {
            // No sleep session found — stay in GATHERING_DATA state
            return@withContext RhythmLoadOutcome(null, emptyList(), RhythmEmptyState.GATHERING_DATA)
        }

        // Step 2: Extract time bounds
        val lo = lastSleep.effectiveStartTs
        val hi = lastSleep.endTs
        if (hi <= lo) {
            return@withContext RhythmLoadOutcome(null, emptyList(), RhythmEmptyState.GATHERING_DATA)
        }

        // Step 3: Load R-R intervals and gravity samples for the night
        val rrRows = runCatching {
            repo.rrIntervalsUnion(deviceId, lo, hi, limit = 200_000)
        }.getOrDefault(emptyList())

        if (rrRows.isEmpty()) {
            // No R-R data — diagnose honestly
            val grav = runCatching {
                repo.gravitySamplesUnion(deviceId, lo, hi, limit = 200_000)
            }.getOrDefault(emptyList())

            val emptyReason = RhythmScreener.classifyEmptyState(
                windows = emptyList(),
                hadMotionSignal = grav.isNotEmpty(),
                beatsAreBanked = false
            )
            return@withContext RhythmLoadOutcome(null, emptyList(), emptyReason)
        }

        val grav = runCatching {
            repo.gravitySamplesUnion(deviceId, lo, hi, limit = 200_000)
        }.getOrDefault(emptyList())

        // Step 4: Window the night into 5-minute slices. Single pass: each row is
        // bucketed by its slice index instead of rescanning the full lists per
        // window, so the pass is O(rows + windows) rather than O(rows * windows).
        val windowSec = 5 * 60L
        val windowCount = ((hi - lo + windowSec - 1) / windowSec).toInt()

        fun bucketIndex(ts: Long): Int = ((ts - lo) / windowSec).toInt()

        val rrByWindow = List(windowCount) { mutableListOf<RrInterval>() }
        for (row in rrRows) {
            val idx = bucketIndex(row.ts)
            // The last slice may end before `hi`-rounded-up; keep only rows truly
            // inside its actual span (mirrors the minOf(t + windowSec, hi) bound).
            if (idx in 0 until windowCount && (idx < windowCount - 1 || row.ts < hi)) {
                rrByWindow[idx].add(row)
            }
        }

        val gravByWindow = List(windowCount) { mutableListOf<GravitySample>() }
        for (row in grav) {
            val idx = bucketIndex(row.ts)
            if (idx in 0 until windowCount && (idx < windowCount - 1 || row.ts < hi)) {
                gravByWindow[idx].add(row)
            }
        }

        val results = mutableListOf<RhythmScreener.WindowResult>()
        for (i in rrByWindow.indices) {
            val wRR = rrByWindow[i]
            if (wRR.size >= RhythmScreener.WINDOW_MIN_BEATS) {
                val still = isStill(gravByWindow[i])
                // Convert Room entities to protocol objects for the analytics engine
                val protocolRR = wRR.map { ProtocolRrInterval(ts = it.ts.toInt(), rrMs = it.rrMs) }
                val input = RhythmScreener.WindowInput.fromRr(protocolRR, motionStill = still)
                results.add(RhythmScreener.screenWindow(input))
            }
        }

        // Step 5: Compute night summary and empty state
        val night = RhythmScreener.summarizeNight(results)
        val emptyReason = RhythmScreener.classifyEmptyState(
            windows = results,
            hadMotionSignal = grav.isNotEmpty(),
            beatsAreBanked = RhythmScreener.nightBeatsAreBanked(
                rrMs = rrRows.map { it.rrMs.toDouble() },
                tsSec = rrRows.map { it.ts.toInt() }
            )
        )

        RhythmLoadOutcome(night, results, emptyReason)
    }
    onLoaded(outcome.night, outcome.windows, outcome.emptyReason)
}

/** The three outputs of [loadRhythmData], carried out of the background pass. */
private data class RhythmLoadOutcome(
    val night: RhythmScreener.NightRhythmSummary?,
    val windows: List<RhythmScreener.WindowResult>,
    val emptyReason: RhythmEmptyState,
)

/**
 * A window is "still" when its accelerometer magnitude varies little (a resting wrist).
 * A coarse, conservative gate — movement is the single biggest false signal for a regularity
 * read, so we err toward NOT reading a window rather than describing a moving one.
 *
 * Requires at least 4 samples; normalised standard deviation below 3% of the mean magnitude
 * reads as a still wrist. Mirrors macOS `RhythmHost.isStill()`.
 */
private fun isStill(grav: List<GravitySample>): Boolean {
    if (grav.size < 4) return false

    val mags = grav.map { g ->
        sqrt(g.x * g.x + g.y * g.y + g.z * g.z)
    }

    val mean = mags.sum() / mags.size
    if (mean <= 0.0) return false

    val variance = mags.map { (it - mean) * (it - mean) }.sum() / mags.size
    val normalizedStd = sqrt(variance) / mean

    return normalizedStd < 0.03
}
