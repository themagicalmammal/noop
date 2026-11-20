package com.noop.ui.forge

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonitorWeight
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Devices

import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PersonalInjury
import androidx.compose.foundation.clickable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.rotate
import com.noop.R
import com.noop.core.CoreRead
import com.noop.data.WorkoutRow
import com.noop.ui.AppViewModel
import com.noop.ui.forge.ForgeMetrics
import com.noop.ui.UnitFormatter
import com.noop.ui.UnitPrefs
import com.noop.ui.WorkoutEditing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
internal val NOTICES_DESTINATIONS = setOf("chargeBreakdown", "goalJourney", "plan")

/**
 * Fork (DX23876), Forge parity 2026-11: Today's timeline as the new iOS design draws it (`ForgeTimeline`,
 * `ForgeTodayView.dayList`): the day as a time rail, newest first, the time in a column on the left, a dot on a
 * thin line per entry, a "Now" marker on top and a divider where yesterday starts.
 *
 * It carries workouts (two or more detected or imported walks of a day as one entry, `TimelineFeed.walkBundles`),
 * waking up, naps, logged meals, ECG readings, the coach's notes, timed weigh-ins, the day's close and the data's
 * notices (`TodayNotices`) except the one the hint under the rings already says.
 */
internal object TodayTimeline {
    sealed interface Entry {
        val endTs: Long

        data class Workout(val row: WorkoutRow) : Entry {
            override val endTs: Long get() = row.endTs
        }

        data class Wake(override val endTs: Long, val startTs: Long, val asleepMinutes: Double,
                        val deep: Double, val rem: Double, val light: Double, val awake: Double,
                        val nap: Boolean) : Entry

        /** Two or more walks of one day the wearer did not start themselves, as one entry at the last one's end. */
        data class Walks(override val endTs: Long, val rows: List<WorkoutRow>) : Entry {
            val minutes: Double get() = rows.sumOf { ((it.durationS ?: (it.endTs - it.startTs).toDouble()) / 60).coerceAtLeast(0.0) }
        }

        data class Meal(override val endTs: Long, val meal: com.noop.core.CoreFood.Meal) : Entry

        data class Ecg(override val endTs: Long, val reading: com.noop.core.CoreEcg.Reading) : Entry

        data class Notice(override val endTs: Long, val notice: TodayNotices.Notice) : Entry

        /** One of the coach's own notes (iOS `.coach` entries), at the moment it was written. */
        data class Coach(override val endTs: Long, val note: com.noop.core.CoreCoach.Note) : Entry

        /** The evening report (iOS `dayEnd`): from the evening on, the day's sessions, tomorrow's plan, the bedtime. */
        data class DayEnd(override val endTs: Long, val sessions: Int, val tomorrow: String?, val bedtime: Long?) : Entry

        /** A weigh-in that records its time (only those reach the rail, as on iOS), with the 30-day change. */
        data class Weight(override val endTs: Long, val kg: Double, val change30: Double?) : Entry

        /** A day's log of one kind (iOS .dayLog): water drunk, food eaten, or journal written. */
        data class DayLog(override val endTs: Long, val kind: String, val value: Double) : Entry

        }

    /** The small events that share a card (iOS `isRow`). */
    fun isRow(entry: Entry): Boolean {
        return when (entry) {
            is Entry.Workout, is Entry.Walks, is Entry.Weight, is Entry.DayLog,
            is Entry.Meal, is Entry.Ecg -> true
            is Entry.Wake -> entry.nap
            else -> false
        }
    }

    /** A block of the rail: one large entry in its own card, or several small row events sharing one card. */
    sealed interface Block {
        data class Card(val entry: Entry) : Block
        data class Rows(val entries: List<Entry>) : Block
    }

    /** [entries] in order, with neighbouring small events joined into one block (iOS `blocks`). */
    fun blocks(entries: List<Entry>): List<Block> {
        val out = mutableListOf<Block>()
        var run = mutableListOf<Entry>()
        fun flush() {
            if (run.isNotEmpty()) {
                out.add(Block.Rows(run.toList()))
                run.clear()
            }
        }
        for (entry in entries) {
            if (isRow(entry)) {
                run.add(entry)
            } else {
                flush()
                out.add(Block.Card(entry))
            }
        }
        flush()
        return out
    }

    /** A walk that bundles: a walk from the strap, Health or detection, not one the wearer started or imported (iOS `bundlesAsWalk`). */
    fun bundlesAsWalk(row: WorkoutRow): Boolean =
        row.sport.contains("walk", ignoreCase = true) &&
            com.noop.ui.WorkoutEditing.classify(row.source) in setOf(
                com.noop.ui.WorkoutSource.WHOOP, com.noop.ui.WorkoutSource.APPLE, com.noop.ui.WorkoutSource.DETECTED)

    /**
     * Today's and yesterday's entries, newest first, by the day each one ended on. Nothing that ends after [now]
     * (a running workout belongs to the live screen, as on iOS). [nights] are the core's main nights and [naps] the
     * blocks off them (`SleepNight.naps`), so a short night stays the night. Pure, so the order and the split are
     * tested without a store.
     */
    fun entries(workouts: List<WorkoutRow>, nights: List<CoreRead.Night>, naps: List<CoreRead.Nap>, now: Long,
                zone: ZoneId, meals: List<com.noop.core.CoreFood.Meal> = emptyList(),
                ecgs: List<com.noop.core.CoreEcg.Reading> = emptyList(),
                notices: List<TodayNotices.Notice> = emptyList(),
                weighIns: List<Pair<Long, Double>> = emptyList(), weightChange30: Double? = null,
                dayEnd: Entry.DayEnd? = null, coachNotes: List<com.noop.core.CoreCoach.Note> = emptyList(),
                dayLogs: List<Entry.DayLog> = emptyList()): List<Entry> {
        val today = Instant.ofEpochSecond(now).atZone(zone).toLocalDate()
        fun inRange(ts: Long): Boolean {
            val day = Instant.ofEpochSecond(ts).atZone(zone).toLocalDate()
            return ts <= now && (day == today || day == today.minusDays(1))
        }
        val out = mutableListOf<Entry>()
        val shown = workouts.filter { inRange(it.endTs) }
        // Two or more bundling walks of a day are one entry (`TimelineFeed.walkBundles`); a single walk stays its own.
        val bundles = shown.filter(::bundlesAsWalk)
            .groupBy { Instant.ofEpochSecond(it.endTs).atZone(zone).toLocalDate() }.filterValues { it.size >= 2 }
        val inBundles = bundles.values.flatten().toSet()
        bundles.values.forEach { walks -> val sorted = walks.sortedBy { it.endTs }; out += Entry.Walks(sorted.last().endTs, sorted) }
        shown.filter { it !in inBundles }.forEach { out += Entry.Workout(it) }
        meals.filter { inRange(it.start) }.forEach { out += Entry.Meal(it.start, it) }
        ecgs.filter { inRange(it.startTs) }.forEach { out += Entry.Ecg(it.startTs, it) }
        notices.filter { inRange(it.at) }.forEach { out += Entry.Notice(it.at, it) }
        weighIns.filter { inRange(it.first) }.forEach { (at, kg) -> out += Entry.Weight(at, kg, weightChange30) }
        dayEnd?.takeIf { inRange(it.endTs) }?.let { out += it }
        coachNotes.filter { inRange(it.date) }.forEach { out += Entry.Coach(it.date, it) }
        nights.filter { inRange(it.endTs) }.forEach { n ->
            val m = n.minutes
            val awake = m?.get("wake") ?: 0.0
            val deep = m?.get("deep") ?: 0.0
            val rem = m?.get("rem") ?: 0.0
            val light = m?.get("light") ?: 0.0
            val staged = deep + rem + light
            val asleep = if (staged > 0) staged else (n.endTs - n.startTs) / 60.0 - awake
            if (asleep <= 0) return@forEach
            out += Entry.Wake(n.endTs, n.startTs, asleep, deep, rem, light, awake, nap = false)
        }
        naps.filter { inRange(it.endTs) && it.asleepMinutes > 0 }.forEach { n ->
            out += Entry.Wake(n.endTs, n.startTs, n.asleepMinutes, 0.0, 0.0, 0.0, 0.0, nap = true)
        }
        dayLogs.filter { inRange(it.endTs) }.forEach { out += it }
        return out.sortedByDescending { it.endTs }
    }

    /** "7:26": hours and minutes, as iOS's `hoursMinutes`. */
    fun hoursMinutes(minutes: Double): String {
        val total = Math.round(minutes).toInt()
        return "%d:%02d".format(Locale.ROOT, total / 60, total % 60)
    }
}

@Composable
internal fun ForgeTodayTimeline(viewModel: AppViewModel, onSleep: () -> Unit, onDestination: ((String) -> Unit)? = null, onDayLog: ((String) -> Unit)? = null) {
    val context = LocalContext.current
    val workouts by viewModel.workouts.collectAsState()
    var nights by remember { mutableStateOf<List<CoreRead.Night>>(emptyList()) }
    var naps by remember { mutableStateOf<List<CoreRead.Nap>>(emptyList()) }
    // "Now" moves on by the minute and the rail reads again when the core has new data (a night synced while
    // Today stays open), as the Sleep tab does; read once, both stayed where the page was opened.
    // Lifecycle-gated: a hidden timeline shouldn't be ticking the clock while its tab is away.
    val lifecycleOwner = LocalLifecycleOwner.current
    var now by remember { androidx.compose.runtime.mutableLongStateOf(System.currentTimeMillis() / 1000) }
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) { kotlinx.coroutines.delay(60_000); now = System.currentTimeMillis() / 1000 }
        }
    }
    var meals by remember { mutableStateOf<List<com.noop.core.CoreFood.Meal>>(emptyList()) }
    var ecgs by remember { mutableStateOf<List<com.noop.core.CoreEcg.Reading>>(emptyList()) }
    var notices by remember { mutableStateOf<List<TodayNotices.Notice>>(emptyList()) }
    var weighIns by remember { mutableStateOf<List<Pair<Long, Double>>>(emptyList()) }
    var weightChange by remember { mutableStateOf<Double?>(null) }
    var tomorrowPlan by remember { mutableStateOf<com.noop.core.CoreCoach.PlanRow?>(null) }
    var todayRow by remember { mutableStateOf<CoreRead.Day?>(null) }
    var catchUp by remember { mutableStateOf(false) }
    var coachNotes by remember { mutableStateOf<List<com.noop.core.CoreCoach.Note>>(emptyList()) }
    var dayLogs by remember { mutableStateOf<List<TodayTimeline.Entry.DayLog>>(emptyList()) }
    var why by remember { mutableStateOf<com.noop.core.CoreCoach.Note?>(null) }
    // New-dot tracking: entries after the last time the reader left Today carry a dot, like an unread mail.
    val timelinePrefs = context.getSharedPreferences(com.noop.ui.NoopPrefs.NAME, android.content.Context.MODE_PRIVATE)
    var seenBefore = timelinePrefs.getString("forge.timeline.seenAt", "0")?.toLong() ?: 0
    DisposableEffect(Unit) {
        onDispose {
            timelinePrefs.edit().putString("forge.timeline.seenAt", System.currentTimeMillis().toString()).apply()
        }
    }
    // The evening report from the evening on (iOS `ForgeToday.eveningStart`): 19:00, or two hours before the planned
    // bedtime when that is later; not in quiet hours (22:00 to 07:00), when the coach keeps quiet too.
    val wake = rememberTomorrowWake(viewModel)
    val needMin = remember { com.noop.alarm.WindDownStore.from(context).sleepNeedMinutes }
    val bedtime = wake.minusMinutes(needMin.toLong()).toEpochSecond()
    var open by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(false) }
    val live = rememberMomentumLive(viewModel)
    val coreRevision by com.noop.core.CoreShadow.dataRevision.collectAsState()
    val todayKey = com.noop.ui.logicalDayNow(ZoneId.systemDefault()).toString()
    val weighTitle = stringResource(R.string.forge_hint_weigh_title)
    val weighDetail = stringResource(R.string.forge_hint_weigh_detail)
    LaunchedEffect(coreRevision, live) {
        val end = System.currentTimeMillis() / 1000
        withContext(Dispatchers.IO) {
            CoreRead.open(context)
            nights = CoreRead.nights(end - 3 * 86_400, end)
            naps = CoreRead.naps(end - 3 * 86_400, end)
            val today = java.time.LocalDate.now()
            meals = listOf(today.minusDays(1), today).flatMap { com.noop.core.CoreFood.day(it.toString())?.meals.orEmpty() }
            ecgs = com.noop.core.CoreEcg.readings().orEmpty()
            val tomorrowKey = java.time.LocalDate.now().plusDays(1).toString()
            tomorrowPlan = runCatching { com.noop.core.CoreCoach.plan(com.noop.ui.UnitPrefs.effortScale(context).raw) }.getOrNull()
                ?.agreed?.firstOrNull { it.day == tomorrowKey }
            todayRow = CoreRead.day(com.noop.ui.logicalDayNow(ZoneId.systemDefault()).toString())
            catchUp = "sleepCatchUp" in TodayHints.lastMomentumKinds
            // The coach's notes while the coach is on, as iOS's rail reads them; the hint's own note is left out below.
            coachNotes = if (com.noop.ui.BottomBarStyleStore.coachEnabled) runCatching { com.noop.core.CoreCoach.notes() }.getOrDefault(emptyList())
                         else emptyList()
            com.noop.core.CoreBody.snapshot(context, days = 30)?.let { body ->
                weighIns = body.weighIns.mapNotNull { r -> r.takenAt?.let { it to r.value } }
                weightChange = body.weightChangeKg
            }
            // The hint under the rings says its notice; the rail leaves that one out rather than say it twice.
            val (all, weighIn) = TodayHints.load(context, live, todayKey)
            val dismissed = TodayHints.dismissed(com.noop.ui.NoopPrefs.of(context).getString(TodayHints.DISMISSED_KEY, "") ?: "", todayKey)
            val hintId = TodayHints.candidates(all, weighIn, weighTitle, weighDetail, dismissed, TodayHints.lastProposal, "",
                                               TodayHints.lastCoachNote).firstOrNull()?.id
            notices = all.filter { it.key != hintId }
            // Day logs: hydration, food (only days without logged meals), journal (iOS `dayLogs`).
            val twoDays = listOf(java.time.LocalDate.now().minusDays(1), java.time.LocalDate.now())
            val mealDays = meals.mapNotNull { forgeMealDay(it) }.toSet()
            val dayLogsOut = mutableListOf<TodayTimeline.Entry.DayLog>()
            for (d in twoDays) {
                val dayKey = d.toString()
                val endTs = forgeEndOfTimelineDay(d).toLong()
                // Hydration: the day's total water, as one row at the day's start.
                val dayStartTs = forgeEndOfTimelineDay(d)
                val totalMl = runCatching { com.noop.analytics.HydrationStore.total(viewModel.repo, dayStartTs) }.getOrDefault(0.0)
                if (totalMl > 0) {
                    dayLogsOut.add(TodayTimeline.Entry.DayLog(endTs, "drink", totalMl))
                }
                // Food: a day's kcal only when that day has no logged meals to show instead.
                if (dayKey !in mealDays) {
                    val kcal = runCatching { com.noop.core.CoreFood.day(dayKey)?.totals?.get("energy_kcal") ?: 0.0 }.getOrDefault(0.0)
                    if (kcal > 0) dayLogsOut.add(TodayTimeline.Entry.DayLog(endTs, "food", kcal))
                }
                // Journal: how many entries that day.
                val journalCount = runCatching {
                    val native = viewModel.repo.journal("noop-journal", dayKey, dayKey)
                    val imported = viewModel.repo.journal("my-whoop", dayKey, dayKey)
                    (native + imported).size
                }.getOrDefault(0)
                if (journalCount > 0) dayLogsOut.add(TodayTimeline.Entry.DayLog(endTs, "journal", journalCount.toDouble()))
            }
            dayLogs = dayLogsOut
        }
    }
    val zone = ZoneId.systemDefault()
    val nowDate = Instant.ofEpochSecond(now).atZone(zone)
    val seven = nowDate.toLocalDate().atTime(19, 0).atZone(zone).toEpochSecond()
    val eveningStart = maxOf(seven, bedtime - 2 * 3600)
    val eveningHour = Instant.ofEpochSecond(eveningStart).atZone(zone).hour
    val dayEnd = if (now >= eveningStart && eveningHour in 7 until 22) {
        val todayKey = com.noop.ui.logicalDayNow(zone)
        val sessions = workouts.count { Instant.ofEpochSecond(it.endTs).atZone(zone).toLocalDate() == todayKey }
        TodayTimeline.Entry.DayEnd(eveningStart, sessions, tomorrowPlan?.let { p ->
            val sport = com.noop.ui.WorkoutEditing.displaySport(p.sport.ifEmpty { p.summary })
            p.durationMin?.let { context.getString(R.string.forge_de_tomorrow_min, sport, it) } ?: context.getString(R.string.forge_de_tomorrow, sport)
        }, if (catchUp) bedtime else null)
    } else null
    val hintNoteId = TodayHints.lastCoachHintId
    val all = remember(workouts, nights, naps, now, meals, ecgs, notices, weighIns, weightChange, dayEnd, coachNotes, hintNoteId, dayLogs) {
        TodayTimeline.entries(workouts, nights, naps, now, zone, meals, ecgs, notices, weighIns, weightChange, dayEnd,
                              coachNotes.filter { it.id != hintNoteId }, dayLogs)
    }
    // Hidden entries: the reader swiped them away, like iOS's hiddenRaw.
    val hiddenRaw = timelinePrefs.getString("forge.timeline.hidden", "") ?: ""
    val hiddenIds = remember(hiddenRaw) { hiddenRaw.split("\n").filter { it.isNotEmpty() }.toSet() }
    // An entry is "new" if it appeared after the reader last left Today.
    val isNew: (TodayTimeline.Entry) -> Boolean = { entry -> seenBefore > 0 && entry.endTs > seenBefore }
    // "Yesterday" by the logical day, as iOS's rail (`logicalDayKey`): an evening workout seen after midnight
    // still belongs to today.
    val today = com.noop.ui.logicalDayNow(zone)
    // Closed, the rail shows today's latest three entries; "Show all" opens the whole day and yesterday (iOS `dayOpen`).
    val entries = if (open) all else all.filter { Instant.ofEpochSecond(it.endTs).atZone(zone).toLocalDate() == today }.take(3)
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = ForgeMetrics.space3, bottom = ForgeMetrics.space3),
            verticalAlignment = Alignment.Bottom) {
            Text(stringResource(R.string.forge_timeline_title), style = Forge.type.title2.copy(fontWeight = FontWeight.Bold),
                 // The canvas text colour, as iOS's `ForgeSectionTitle`: the rail sits below the scene, where scene ink
                 // (white over the picture) all but vanished on the light canvas.
                 color = Forge.colors.text, modifier = Modifier.weight(1f))
            Text(stringResource(if (open) R.string.forge_hint_show_less else R.string.forge_tl_show_all),
                 style = Forge.type.subhead, color = Forge.colors.accent,
                 modifier = Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button) { open = !open }.padding(4.dp))
        }
        RailRow(time = time(context, now), dot = Forge.colors.effort, last = entries.isEmpty()) {
            Text(stringResource(R.string.forge_timeline_now), style = Forge.type.footnote.copy(fontWeight = FontWeight.SemiBold),
                 color = Color.White,
                 modifier = Modifier.background(Forge.colors.effort, CircleShape).padding(horizontal = 10.dp, vertical = 3.dp))
        }
        var yesterdayShown = false
        val blocks = TodayTimeline.blocks(entries)
        blocks.forEachIndexed { bi, block ->
            // Determine the first entry's timestamp for the day divider check
            val firstEntry = when (block) {
                is TodayTimeline.Block.Card -> block.entry
                is TodayTimeline.Block.Rows -> block.entries.first()
            }
            val day = Instant.ofEpochSecond(firstEntry.endTs).atZone(zone).toLocalDate()
            if (day != today && !yesterdayShown) {
                yesterdayShown = true
                RailRow(time = null, dot = Forge.colors.text, last = false) {
                    Text(stringResource(R.string.today_day_yesterday), style = Forge.type.headline.copy(fontWeight = FontWeight.Bold),
                         color = Forge.colors.text)
                }
            }
            val last = bi == blocks.lastIndex
            when (block) {
                is TodayTimeline.Block.Card -> {
                    val e = block.entry
                    val cardNew = isNew(e)
                    when (e) {
                        is TodayTimeline.Entry.Workout -> RailRow(time(context, e.endTs), Forge.colors.effort, last, cardNew) { WorkoutEntry(e.row) }
                        is TodayTimeline.Entry.Wake -> RailRow(time(context, e.endTs), Forge.colors.rest, last, cardNew) { WakeEntry(e) }
                        is TodayTimeline.Entry.Walks -> RailRow(time(context, e.endTs), Forge.colors.effort, last, cardNew) {
                            FactRow(Icons.Rounded.DirectionsWalk, Forge.colors.charge, stringResource(R.string.forge_tl_walks, e.rows.size), "",
                                    TodayTimeline.hoursMinutes(e.minutes), stringResource(R.string.forge_tl_h_walked))
                        }
                        is TodayTimeline.Entry.Meal -> RailRow(time(context, e.endTs), Forge.colors.food, last, cardNew) {
                            FactRow(Icons.Rounded.Restaurant, Forge.colors.food, mealName(e.meal.kind),
                                    e.meal.entries.joinToString(", ") { it.name.substringBefore(", ") },
                                    ((e.meal.totals["energy_kcal"] ?: 0.0).toInt()).toString(), "kcal")
                        }
                        is TodayTimeline.Entry.Ecg -> RailRow(time(context, e.endTs), Forge.colors.accent, last, cardNew) {
                            FactRow(Icons.Rounded.MonitorHeart, Forge.colors.accent, stringResource(R.string.forge_tl_ecg), e.reading.title,
                                    e.reading.averageHr?.toString(), "bpm")
                        }
                        is TodayTimeline.Entry.Weight -> RailRow(time(context, e.endTs), Forge.colors.rest, last, cardNew) {
                            FactRow(Icons.Rounded.MonitorWeight, Forge.colors.rest, stringResource(R.string.forge_tl_weight),
                                    e.change30?.let { c -> stringResource(R.string.forge_tl_weight_30,
                                        (if (c >= 0) "+" else "−") + "%.1f kg".format(java.util.Locale.getDefault(), kotlin.math.abs(c))) } ?: "",
                                    "%.1f".format(java.util.Locale.getDefault(), e.kg), "kg")
                        }
                        is TodayTimeline.Entry.DayLog -> RailRow(time(context, e.endTs), Forge.colors.accent, last, cardNew) {
                            val nav = { kind: String -> onDayLog?.invoke(kind) }
                            when (e.kind) {
                                "drink" -> FactRow(Icons.Rounded.WaterDrop, ForgeTokens.StrandPalette.metricCyan.resolve(),
                                    stringResource(R.string.forge_tl_drink), "",
                                    "%.1f".format(java.util.Locale.getDefault(), e.value / 1000), "L",
                                    onClick = { nav("drink") })
                                "food" -> FactRow(Icons.Rounded.Restaurant, Forge.colors.effort,
                                    stringResource(R.string.forge_tl_food), "",
                                    e.value.toInt().toString(), "kcal",
                                    onClick = { nav("food") })
                                "journal" -> FactRow(Icons.Rounded.Book, Forge.colors.accent,
                                    stringResource(R.string.forge_tl_journal), "",
                                    e.value.toInt().toString(), stringResource(R.string.forge_tl_entries),
                                    onClick = { nav("journal") })
                                else -> FactRow(Icons.Rounded.Book, Forge.colors.accent,
                                    stringResource(R.string.forge_tl_journal), "",
                                    e.value.toInt().toString(), stringResource(R.string.forge_tl_entries),
                                    onClick = { nav("journal") })
                            }
                        }
                        is TodayTimeline.Entry.Coach -> RailRow(time(context, e.endTs), Forge.colors.accent, last, cardNew) {
                            CoachNoteCard(e.note, onWhy = { why = e.note })
                        }
                        is TodayTimeline.Entry.DayEnd -> RailRow(time(context, e.endTs), Forge.colors.rest, last, cardNew) { DayEndEntry(e, todayRow) }
                        is TodayTimeline.Entry.Notice -> RailRow(time(context, e.endTs), Forge.colors.textTertiary, last, cardNew) {
                            val (icon, tint) = noticeLook(e.notice)
                            ForgeCard(Modifier.fillMaxWidth()) {
                                Row(horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.Top) {
                                    Box(Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
                                        contentAlignment = Alignment.Center) {
                                        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
                                    }
                                    Text(e.notice.headline, style = Forge.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = Forge.colors.text,
                                         modifier = Modifier.weight(1f).padding(start = 8.dp))
                                }
                                if (e.notice.detail.isNotEmpty()) Text(e.notice.detail, style = Forge.type.caption, color = Forge.colors.textSecondary,
                                                                       modifier = Modifier.padding(top = 4.dp))
                                if (e.notice.actionTitle != null && e.notice.destination != null &&
                                    e.notice.destination in NOTICES_DESTINATIONS && onDestination != null) {
                                    TextButton(onClick = { onDestination(e.notice.destination!!) },
                                               modifier = Modifier.padding(top = 4.dp).heightIn(min = ForgeMetrics.minHitTarget)) {
                                        Text(e.notice.actionTitle, style = Forge.type.subhead, color = Forge.colors.accent)
                                    }
                                }
                            }
                        }
                    }
                }
                is TodayTimeline.Block.Rows -> RailRow(time = null, dot = Forge.colors.textTertiary, last = last) {
                    RowsCard(block.entries, context, isNew)
                }
            }
        }
    }
    why?.let { CoachWhySheet(it) { why = null } }
}

/** The coach's note on the rail, as iOS's `coachCard`: its kind, the headline, the body (Read more past four lines), what
 * the wearer asked after it with the answers, the replies it offers, each sent to the coach, and the thumbs. */
@Composable
internal fun CoachNoteCard(note: com.noop.core.CoreCoach.Note, onWhy: () -> Unit) {
    val context = LocalContext.current
    val colors = Forge.colors
    val select = LocalForgeTabSelect.current
    var opened by remember { mutableStateOf(note.body.length <= 220) }
    val rating = remember { mutableStateOf(CoachRatings.rating(context, note.id)) }
    ForgeCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.MonitorHeart, contentDescription = null, tint = colors.accent, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(coachOriginLabel(note.origin)), style = Forge.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = colors.accent, modifier = Modifier.weight(1f), maxLines = 1)
            Icon(Icons.Rounded.Info, stringResource(R.string.forge_cn_why), tint = colors.textTertiary,
                 modifier = Modifier.size(20.dp).clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onWhy))
        }
        note.kicker?.let { Text(it, style = Forge.type.caption, color = colors.textSecondary, modifier = Modifier.padding(top = 4.dp)) }
        Text(note.headline, style = Forge.type.title2.copy(fontWeight = FontWeight.Bold), color = colors.text, modifier = Modifier.padding(top = 4.dp))
        if (note.body.isNotEmpty()) {
            Text(note.body.replace("**", ""), style = Forge.type.body, color = colors.textSecondary, maxLines = if (opened) Int.MAX_VALUE else 4,
                 overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
            if (!opened) Text(stringResource(R.string.forge_cn_more), style = Forge.type.subhead.copy(fontWeight = FontWeight.SemiBold),
                              color = colors.accent, modifier = Modifier.clickable { opened = true }.padding(vertical = 8.dp))
        }
        note.followUps.forEach { (q, a) ->
            Text(q, style = Forge.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = colors.text, modifier = Modifier.padding(top = 8.dp))
            Text(a.replace("**", ""), style = Forge.type.body, color = colors.textSecondary)
        }
        if (note.replies.isNotEmpty()) Row(Modifier.padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                                           horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            note.replies.forEach { r ->
                Text(r, style = Forge.type.subhead, color = colors.accent,
                     modifier = Modifier.background(colors.accent.copy(alpha = 0.12f), CircleShape)
                         .clickable { com.noop.ui.CoachHandoff.pendingPrompt = r; select(ForgeTab.Coach) }.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
        Row(Modifier.padding(top = 8.dp)) {
            val rated = rating.value
            IconButton(onClick = { rating.value = 1; CoachRatings.record(context, note.id, note.origin, 1) },
                       modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.ThumbUp, stringResource(R.string.forge_cn_helpful),
                     tint = if (rated > 0) colors.accent else colors.textTertiary)
            }
            IconButton(onClick = { rating.value = -1; CoachRatings.record(context, note.id, note.origin, -1) },
                       modifier = Modifier.size(36.dp)) {
                Icon(Icons.Rounded.ThumbUp, stringResource(R.string.forge_cn_not_helpful),
                     tint = if (rated < 0) colors.accent else colors.textTertiary,
                     modifier = Modifier.rotate(180f))
            }
        }
    }
}

/** "Why am I seeing this?" for a coach note, as iOS's `ForgeCoachWhySheet`: why now, what it read, where the figures come from. */
@Composable
internal fun CoachWhySheet(note: com.noop.core.CoreCoach.Note, onDismiss: () -> Unit) {
    val colors = Forge.colors
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(Modifier.background(colors.surface, RoundedCornerShape(20.dp)).padding(20.dp).verticalScroll(rememberScrollState()),
               verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space3)) {
            Text(stringResource(R.string.forge_cn_why), style = Forge.type.headline, color = colors.text)
            Text(stringResource(R.string.forge_cn_why_now), style = Forge.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = colors.text)
            Text(stringResource(coachOriginWhy(note.origin)), style = Forge.type.body, color = colors.textSecondary)
            if (note.read.isNotEmpty()) {
                Text(stringResource(R.string.forge_cn_read), style = Forge.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = colors.text)
                Text(note.read.joinToString(" · "), style = Forge.type.footnote, color = colors.text)
            }
            Text(stringResource(R.string.forge_cn_where), style = Forge.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = colors.text)
            Text(stringResource(R.string.forge_cn_where_text), style = Forge.type.body, color = colors.textSecondary)
            Text(stringResource(R.string.forge_hv_done), style = Forge.type.subhead.copy(fontWeight = FontWeight.SemiBold), color = colors.accent,
                 modifier = Modifier.align(Alignment.End).clickable(onClick = onDismiss).padding(8.dp))
        }
    }
}

/** A notice's symbol from its topic and its colour from its tone, for its card (iOS `noticeLook`). */
@Composable
private fun noticeLook(notice: TodayNotices.Notice): Pair<ImageVector, Color> {
    val tint = when (notice.tone) {
        "positive" -> Forge.colors.charge
        "neutral" -> Forge.colors.accent
        "caution" -> Forge.colors.effort
        "critical" -> if (notice.key == "healthAlert") Forge.colors.effort else Forge.colors.effort
        else -> Forge.colors.accent
    }
    val symbol = when (notice.topic) {
        "sleep" -> Icons.Rounded.Bedtime
        "load" -> Icons.Rounded.FitnessCenter
        "bodySignals" -> Icons.Rounded.MonitorHeart
        "movement" -> Icons.Rounded.DirectionsWalk
        "device" -> Icons.Rounded.Devices
        "goals" -> Icons.Rounded.Flag
        else -> Icons.Rounded.Info
    }
    return symbol to tint
}

/** One row of the rail: the time in its column, a dot on the line, the entry beside them. */
@Composable
private fun RailRow(time: String?, dot: Color, last: Boolean, isNew: Boolean = false, content: @Composable () -> Unit) {
    val line = Forge.colors.divider
    val canvas = Forge.colors.canvas
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Box(Modifier.width(56.dp).padding(top = 14.dp)) {
            if (time != null) {
                Text(time, style = Forge.type.captionNumber, color = Forge.colors.textSecondary, maxLines = 1)
            }
        }
        Box(Modifier.width(20.dp).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.width(1.dp).fillMaxHeight(if (last) 0.2f else 1f).background(line))
            // As iOS's rail dot: an 8pt filled circle with a 2pt canvas ring around it.
            Box(Modifier.padding(top = 18.dp).size(12.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(12.dp).border(2.dp, canvas, CircleShape))
                Box(Modifier.size(8.dp).background(dot, CircleShape))
            }
        }
        Box(Modifier.weight(1f).padding(start = ForgeMetrics.space2, top = 6.dp, bottom = ForgeMetrics.space3)) {
            Box(Modifier.fillMaxWidth()) {
                content()
                if (isNew) {
                    Box(Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 5.dp)
                        .size(8.dp).background(Forge.colors.accent, CircleShape))
                }
            }
        }
    }
}

/**
 * Several small events sharing one card, divided by hairlines, so a busy day reads as a few blocks
 * rather than a dozen equal cards (iOS `rowsCard`).
 */
@Composable
private fun RowsCard(entries: List<TodayTimeline.Entry>, context: Context, isNew: (TodayTimeline.Entry) -> Boolean) {
    ForgeCard(Modifier.fillMaxWidth(), contentPadding = 0.dp) {
        Column(Modifier.fillMaxWidth()) {
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    Box(Modifier.fillMaxWidth().height(0.5.dp).padding(start = 60.dp).background(Forge.colors.divider))
                }
                Box(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Box(Modifier.width(56.dp).padding(top = 2.dp)) {
                            Text(time(context, entry.endTs), style = Forge.type.footnote, color = Forge.colors.textSecondary, maxLines = 1)
                        }
                        Box(Modifier.weight(1f).padding(start = 8.dp)) {
                            when (entry) {
                            is TodayTimeline.Entry.Workout -> WorkoutEntry(entry.row)
                            is TodayTimeline.Entry.Wake -> WakeEntry(entry)
                            is TodayTimeline.Entry.Walks -> FactRow(Icons.Rounded.DirectionsWalk, Forge.colors.charge,
                                stringResource(R.string.forge_tl_walks, entry.rows.size), "",
                                TodayTimeline.hoursMinutes(entry.minutes), stringResource(R.string.forge_tl_h_walked))
                            is TodayTimeline.Entry.Meal -> FactRow(Icons.Rounded.Restaurant, Forge.colors.food, mealName(entry.meal.kind),
                                entry.meal.entries.joinToString(", ") { it.name.substringBefore(", ") },
                                ((entry.meal.totals["energy_kcal"] ?: 0.0).toInt()).toString(), "kcal")
                            is TodayTimeline.Entry.Ecg -> FactRow(Icons.Rounded.MonitorHeart, Forge.colors.accent,
                                stringResource(R.string.forge_tl_ecg), entry.reading.title,
                                entry.reading.averageHr?.toString(), "bpm")
                            is TodayTimeline.Entry.Weight -> FactRow(Icons.Rounded.MonitorWeight, Forge.colors.rest,
                                stringResource(R.string.forge_tl_weight),
                                entry.change30?.let { c -> stringResource(R.string.forge_tl_weight_30,
                                    (if (c >= 0) "+" else "−") + "%.1f kg".format(java.util.Locale.getDefault(), kotlin.math.abs(c))) } ?: "",
                                "%.1f".format(java.util.Locale.getDefault(), entry.kg), "kg")
                            is TodayTimeline.Entry.DayLog -> {
                                when (entry.kind) {
                                    "drink" -> FactRow(Icons.Rounded.WaterDrop, ForgeTokens.StrandPalette.metricCyan.resolve(),
                                        stringResource(R.string.forge_tl_drink), "",
                                        "%.1f".format(java.util.Locale.getDefault(), entry.value / 1000), "L")
                                    "food" -> FactRow(Icons.Rounded.Restaurant, Forge.colors.effort,
                                        stringResource(R.string.forge_tl_food), "",
                                        entry.value.toInt().toString(), "kcal")
                                    "journal" -> FactRow(Icons.Rounded.Book, Forge.colors.accent,
                                        stringResource(R.string.forge_tl_journal), "",
                                        entry.value.toInt().toString(), stringResource(R.string.forge_tl_entries))
                                    else -> FactRow(Icons.Rounded.Book, Forge.colors.accent,
                                        stringResource(R.string.forge_tl_journal), "",
                                        entry.value.toInt().toString(), stringResource(R.string.forge_tl_entries))
                                }
                            }
                            else -> {}
                            }
                        }
                    }
                    if (isNew(entry)) {
                        Box(Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 10.dp)
                            .size(8.dp).background(Forge.colors.accent, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutEntry(row: WorkoutRow) {
    val context = LocalContext.current
    val scale = remember { UnitPrefs.effortScale(context) }
    val system = remember { UnitPrefs.distanceSystem(context) }
    val minutes = ((row.durationS ?: (row.endTs - row.startTs).toDouble()) / 60).toInt()
    val detail = buildList {
        add(stringResource(R.string.forge_timeline_minutes, minutes))
        row.distanceM?.takeIf { it > 0 }?.let { add(UnitFormatter.distanceFromMeters(it, system)) }
    }.joinToString(" · ")
    val minutesDouble = row.durationS ?: (row.endTs - row.startTs).toDouble()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Its sport's own symbol and colour, as iOS's `workoutCard`.
        FactRow(com.noop.ui.sportIcon(row.sport), forgeSportTint(row.sport), WorkoutEditing.displaySport(row.sport), detail,
                row.strain?.let { UnitFormatter.effortDisplay(it, scale).removeSuffix(".0") }, stringResource(R.string.forge_effort))
        // Where the effort went: the heart-rate zones of the session, when it stored them.
        com.noop.ui.parseZonePercents(row.zonesJSON)?.let { zones ->
            ForgeZoneBar(zones, minutesDouble / 60.0)
        }
    }
}

@Composable
private fun WakeEntry(e: TodayTimeline.Entry.Wake) {
    val context = LocalContext.current
    val colors = Forge.colors
    Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
        FactRow(if (e.nap) Icons.Rounded.Bedtime else Icons.Rounded.WbTwilight, colors.rest,
                stringResource(if (e.nap) R.string.forge_timeline_nap else R.string.forge_timeline_woke_up),
                "${time(context, e.startTs)} – ${time(context, e.endTs)}",
                TodayTimeline.hoursMinutes(e.asleepMinutes), stringResource(R.string.forge_timeline_h_asleep))
        val total = e.deep + e.rem + e.light + e.awake
        if (total > 0 && !e.nap) {
            Row(Modifier.fillMaxWidth().height(8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                listOf(e.deep to colors.sleepDeep, e.light to colors.sleepLight, e.rem to colors.sleepRem,
                       e.awake to colors.sleepAwake).filter { it.first > 0 }.forEach { (m, c) ->
                    Box(Modifier.weight(m.toFloat()).fillMaxHeight().background(c, RoundedCornerShape(4.dp)))
                }
            }
        }
    }
}

/** The entry's icon on its wash, its title and detail, and the figure large with its unit small (iOS `factRow`). */
@Composable
private fun FactRow(icon: ImageVector, tint: Color, title: String, detail: String, value: String?, unit: String, onClick: (() -> Unit)? = null) {
    val colors = Forge.colors
    Row(verticalAlignment = Alignment.CenterVertically, modifier = if (onClick != null) Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick) else Modifier) {
        Box(Modifier.size(34.dp).background(tint.copy(alpha = 0.16f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(ForgeMetrics.space3))
        Column(Modifier.weight(1f)) {
            Text(title, style = Forge.type.body.copy(fontWeight = FontWeight.SemiBold), color = colors.text,
                 maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(detail, style = Forge.type.caption, color = colors.textSecondary, maxLines = 2)
        }
        if (value != null) {
            Spacer(Modifier.width(ForgeMetrics.space2))
            Text(buildAnnotatedString {
                append(value)
                withStyle(SpanStyle(fontSize = Forge.type.caption.fontSize, color = colors.textSecondary)) { append(" $unit") }
            }, style = Forge.type.bodyNumber.copy(fontSize = Forge.type.title2.fontSize), color = colors.text, maxLines = 1)
        }
    }
}

private fun time(context: Context, ts: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "H:mm" else "h:mm a"
    return Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(pattern, Locale.getDefault()))
}

/** The evening report, as iOS's `dayEndCard`: Effort against today's target, the sessions, tomorrow, the bedtime and what helps. */
@Composable
private fun DayEndEntry(e: TodayTimeline.Entry.DayEnd, today: CoreRead.Day?) {
    val context = LocalContext.current
    val colors = Forge.colors
    val format = rememberEffortFormat()
    val effort = today?.effort
    val target = effortTargetStored(today?.charge)
    Column(verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2)) {
        Text(stringResource(R.string.forge_de_title), style = Forge.type.footnote.copy(fontWeight = FontWeight.SemiBold), color = colors.textSecondary)
        if (effort != null && target != null) {
            Column(Modifier.fillMaxWidth().background(colors.inset, RoundedCornerShape(12.dp)).padding(12.dp)) {
                androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(12.dp)) {
                    val w = size.width
                    drawRoundRect(colors.divider, size = size, cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()))
                    drawRoundRect(colors.effort.copy(alpha = 0.3f), androidx.compose.ui.geometry.Offset((w * target.start / 100).toFloat(), 0f),
                                  androidx.compose.ui.geometry.Size((w * (target.endInclusive - target.start) / 100).toFloat(), size.height),
                                  androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx()))
                    drawCircle(colors.effort, 6.dp.toPx(), androidx.compose.ui.geometry.Offset((w * (effort / 100)).toFloat().coerceIn(6.dp.toPx(), w - 6.dp.toPx()), size.height / 2))
                }
                Text(stringResource(R.string.forge_de_target, format(effort), format(target.start), format(target.endInclusive)),
                     style = Forge.type.caption, color = colors.textSecondary, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Text(when (e.sessions) { 0 -> stringResource(R.string.forge_de_none); 1 -> stringResource(R.string.forge_de_one)
                                  else -> stringResource(R.string.forge_de_many, e.sessions) }, style = Forge.type.body, color = colors.textSecondary)
        e.tomorrow?.let { Text(it, style = Forge.type.body, color = colors.textSecondary) }
        e.bedtime?.let { Text(stringResource(R.string.forge_de_bed, time(context, it)), style = Forge.type.body, color = colors.textSecondary) }
        val help = when {
            e.bedtime != null -> stringResource(R.string.forge_de_help_sleep)
            effort != null && target != null && effort > target.endInclusive -> stringResource(R.string.forge_de_help_easy)
            else -> null
        }
        help?.let { Text(it, style = Forge.type.body.copy(fontWeight = FontWeight.SemiBold), color = colors.text) }
    }
}

/** The kicker over a coach note on the rail, by the note's origin (iOS `ForgeCoachNoteCard`). */
private fun coachOriginLabel(origin: String): Int = when (origin) {
    "brief" -> R.string.forge_cn_morning; "checkIn" -> R.string.forge_cn_checkin; "weeklyReview" -> R.string.forge_cn_weekly
    "afterWorkout" -> R.string.forge_cn_workout; "evening" -> R.string.forge_cn_evening; else -> R.string.forge_cn_coach
}

/** Why the coach wrote now, by the note's origin, for the "Why now" sheet. */
private fun coachOriginWhy(origin: String): Int = when (origin) {
    "brief" -> R.string.forge_cn_t_brief; "checkIn" -> R.string.forge_cn_t_checkin; "weeklyReview" -> R.string.forge_cn_t_weekly
    "afterWorkout" -> R.string.forge_cn_t_workout; "evening" -> R.string.forge_cn_t_evening; else -> R.string.forge_cn_t_nudge
}

/** The local day key for a meal (which day it belongs to on the rail). */
internal fun forgeMealDay(meal: com.noop.core.CoreFood.Meal): String? {
    val ts = meal.start
    if (ts <= 0) return null
    return Instant.ofEpochSecond(ts).atZone(ZoneId.systemDefault()).toLocalDate().toString()
}

/** Where a figure that only knows its day sits: start of that logical day plus 4 hours (iOS `endOfTimelineDay`). */
internal fun forgeEndOfTimelineDay(day: java.time.LocalDate): Long {
    return day.atTime(4, 0).atZone(ZoneId.systemDefault()).toInstant().epochSecond
}
