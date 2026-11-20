package com.noop.ui.forge

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.noop.ui.AppViewModel

/**
 * Fork (DX23876), Android plan E-3: the Forge app, shown by `NoopRoot` after the terms and onboarding gates with
 * the app's view model. They are the default; More switches to the classic screens for what they do not cover
 * yet, and the classic Settings switch back. `--ez forge_ui true|false` on the launch intent sets the choice too.
 */
object ForgeUi {
    private const val PREFS = "forge"
    private const val KEY = "ui"
    const val EXTRA = "forge_ui"
    /** The Forge screens are the app (E-3f); More switches to the classic screens and their Settings back. */
    const val DEFAULT = true

    fun enabled(context: Context, intent: Intent?): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (intent?.hasExtra(EXTRA) == true) prefs.edit().putBoolean(KEY, intent.getBooleanExtra(EXTRA, false)).apply()
        return prefs.getBoolean(KEY, DEFAULT)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, enabled).commit()
    }

    /** Starts the app afresh, so a restored store or the other screen set is what it opens with. */
    fun restart(context: Context) {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        Runtime.getRuntime().exit(0)
    }
}

@Composable
fun ForgeRoot(viewModel: AppViewModel) {
    val context = LocalContext.current
    remember { ForgeBackground.load(context) }
    remember { ForgeDesign.load(context, (context as? android.app.Activity)?.intent) }
    // A dashboard style this build dropped hands its arrangement to the style it resolves to, once.
    remember { DashboardStylePrefs.migrateRetiredStyle(context) }
    // The workout list Today's timeline, its latest-workout card, the dashboards and Training's count read: the view
    // model filled it only when the Workouts page, Insights or Data Sources opened, so those stayed empty until then.
    // Loaded at start and again after each analysis pass, when a synced workout can have arrived.
    val coreRevision by com.noop.core.CoreShadow.dataRevision.collectAsState()
    androidx.compose.runtime.LaunchedEffect(coreRevision) { viewModel.loadWorkouts() }
    // Prefetch Today's data on app start so the first tab opens instantly.
    // The cache is keyed by revision, so this only fetches when data has changed.
    androidx.compose.runtime.LaunchedEffect(coreRevision) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            com.noop.core.CoreRead.open(context)
            val d = java.time.LocalDate.now()
            val from = d.minusDays(7).toString()
            val to = d.minusDays(1).toString()
            com.noop.core.CoreCache.day(d.toString(), coreRevision)
            com.noop.core.CoreCache.series(com.noop.core.CoreRead.Metric.Charge, from, to, coreRevision)
            com.noop.core.CoreCache.series(com.noop.core.CoreRead.Metric.Rest, from, to, coreRevision)
            com.noop.core.CoreCache.series(com.noop.core.CoreRead.Metric.Hrv, from, to, coreRevision)
            com.noop.core.CoreCache.series(com.noop.core.CoreRead.Metric.RestingHr, from, to, coreRevision)
            com.noop.core.CoreCache.series(com.noop.core.CoreRead.Metric.RespRate, from, to, coreRevision)
        }
    }
    // Settings and pairing open over the tabs in the same window (`ForgeOverlayHost`).
    var moreOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var settingsOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var searchOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var settingsPageStack by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<ForgeSettingsPage?>(null) }
    var moreNavStack by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }

    // Opening a destination from the search overlay: close the search and open the More page with the destination pushed.
    fun openFromSearch(dest: MoreDest) {
        searchOpen = false
        if (dest == MoreDest.Settings) {
            settingsOpen = true
        } else {
            // Push the destination onto the More nav stack before opening More.
            val routes = if (moreNavStack.isEmpty()) "" else moreNavStack + ","
            moreNavStack = "$routes d:${dest.name}"
            moreOpen = true
        }
    }

    fun openSettingsPageFromSearch(page: ForgeSettingsPage) {
        searchOpen = false
        settingsOpen = true
        settingsPageStack = page
    }

    ForgeOverlayHost {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalForgeOpenMore provides (if (ForgeDesign.preview) ({ moreOpen = true }) else null),
            LocalForgeOpenSettings provides (if (ForgeDesign.preview) ({ settingsOpen = true }) else null),
            LocalForgeOpenSearch provides (if (ForgeDesign.preview) ({ searchOpen = true }) else null),
            // The tabs lie under the More page while it is open.
            LocalForgeCovered provides (LocalForgeCovered.current || (moreOpen && ForgeDesign.preview))) {
        ForgeShell { tab, padding, width ->
            // All tabs stay composed so their state survives switches, as iOS keeps all
            // tab view controllers alive. Hidden tabs are offset off-screen so their
            // scroll areas do not overlap the visible tab and cannot steal gestures.
            val visibleTabs = ForgeDesign.tabs(ForgeDesign.preview, com.noop.ui.BottomBarStyleStore.coachEnabled)
            androidx.compose.foundation.layout.Box(modifier = androidx.compose.ui.Modifier.fillMaxSize()) {
                for (t in visibleTabs) {
                    val isVisible = (t == tab)
                    val offset = if (isVisible) 0 else (if (visibleTabs.indexOf(t) < visibleTabs.indexOf(tab)) -1 else 1)
                    androidx.compose.foundation.layout.Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .offset(x = (offset * 10000).dp)
                    ) {
                        when (t) {
                            ForgeTab.Today ->
                                if (!ForgeDesign.preview && DashboardStylePrefs.style(context) == "classic") ForgeClassicToday(padding, viewModel)
                                else ForgeTodayScreen(padding, width, viewModel)
                            ForgeTab.Trends -> ForgeTrendsScreen(padding, width, viewModel)
                            ForgeTab.Health -> ForgeHealthTab(padding, width, viewModel)
                            ForgeTab.Training -> ForgeTrainingScreen(padding, viewModel)
                            ForgeTab.Sleep -> ForgeSleepScreen(padding, width, viewModel)
                            ForgeTab.More -> ForgeMoreScreen(padding, viewModel)
                            ForgeTab.Coach -> {
                                val select = LocalForgeTabSelect.current
                                if (isVisible) androidx.activity.compose.BackHandler { select(ForgeTab.Today) }
                                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.padding(padding)) {
                                    ForgeCoachChatPages(viewModel, androidx.compose.ui.res.stringResource(com.noop.R.string.nav_coach),
                                                        onBack = { select(ForgeTab.Today) })
                                }
                            }
                        }
                    }
                }
            }
        }
        }
        androidx.compose.animation.AnimatedVisibility(moreOpen && ForgeDesign.preview,
            enter = androidx.compose.animation.slideInHorizontally { it } + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutHorizontally { it } + androidx.compose.animation.fadeOut()) {
            ForgeMoreOverlay(viewModel, onDismiss = { moreOpen = false })
        }
        // The profile photo's Settings; its search field opens the one search, as iOS's does.
        if (settingsOpen && ForgeDesign.preview) {
            ForgeOverlayPage(onDismiss = { settingsOpen = false }) {
                ForgeSettings(viewModel, onClose = { settingsOpen = false },
                              initialPage = settingsPageStack,
                              onSearch = { settingsOpen = false; ForgeDesign.focusSearch = true; searchOpen = true })
            }
        }
        // The dedicated search overlay, opened from a tab header's magnifier.
        androidx.compose.animation.AnimatedVisibility(searchOpen && ForgeDesign.preview,
            enter = androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.fadeOut()) {
            ForgeSearchOverlay(
                viewModel = viewModel,
                onDismiss = { searchOpen = false },
                onOpenDest = { openFromSearch(it) },
                onOpenSettingsPage = { openSettingsPageFromSearch(it) },
            )
        }
        // In the new design the coach has its own place while it is on (`ForgeTab.Coach`: a button beside the bar, a
        // rail entry on wide windows), as on iOS; the floating button is for the shipped tabs and a coach that is off.
        if (!(ForgeDesign.preview && com.noop.ui.BottomBarStyleStore.coachEnabled)) ForgeCoachButton(viewModel)
    }
}


