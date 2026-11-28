package com.noop.ui.forge

import androidx.compose.foundation.layout.padding
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.noop.ui.AppViewModel
import kotlinx.coroutines.launch

/**
 * Fork (DX23876), Android plan E-3: the Forge app, shown by `NoopRoot` after the terms and onboarding gates with
 * the app's view model. They are the default; More switches to the classic screens for what they do not cover
 * yet. `--ez forge_ui true|false` on the launch intent sets the choice too.
 */
object ForgeUi {
    private const val PREFS = "forge"
    private const val KEY = "ui"
    const val EXTRA = "forge_ui"
    /** The Forge screens are the app (E-3f). */
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
    // Settings and pairing open over the tabs in the same window (`ForgeOverlayHost`).
    var settingsOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var batteryOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var searchOpen by androidx.compose.runtime.mutableStateOf(false)
    val moreNav = rememberMoreNavigator()
    var searchSettingsPage by androidx.compose.runtime.mutableStateOf<ForgeSettingsPage?>(null)
    // The coach sheet (owner, 2026-11-21): the new design's coach lives in each page's header, not in a tab.
    var coachOpen by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val coachScope = androidx.compose.runtime.rememberCoroutineScope()
    val openCoach: ((CoachPage?) -> Unit)? =
        if (ForgeDesign.preview && com.noop.ui.BottomBarStyleStore.coachEnabled) { page ->
            if (page != null) {
                val suggestions = coachPageSuggestions(context, page.kind)
                coachScope.launch(kotlinx.coroutines.Dispatchers.IO) { com.noop.core.CoreCoach.page(page.kind, page.day, suggestions) }
            }
            coachOpen = true
        } else null
    ForgeOverlayHost {
        androidx.compose.runtime.CompositionLocalProvider(
            LocalForgeOpenSettings provides (if (ForgeDesign.preview) ({ settingsOpen = true }) else null),
            LocalForgeOpenBattery provides (if (ForgeDesign.preview) ({ batteryOpen = true }) else null),
            LocalForgeOpenCoach provides openCoach,
            LocalForgeOpenSearch provides (if (ForgeDesign.preview) ({ searchOpen = true }) else null),
            LocalForgeCovered provides LocalForgeCovered.current) {
        ForgeShell { tab, padding, width ->
            // A weight widget's tap: Today pushes the weight page, so the shell moves there first.
            val openWeight by com.noop.widget.WeightNavigation.open.collectAsState()
            val selectForWeight = LocalForgeTabSelect.current
            androidx.compose.runtime.LaunchedEffect(openWeight, tab) {
                if (openWeight && tab != ForgeTab.Today) selectForWeight(ForgeTab.Today)
            }
            when (tab) {
                // The new design has one Today whatever the dashboard style, as iOS; the classic page jumps to tabs
                // the new design does not have.
                // The four tabs a connected source feeds carry the pull gesture (`ForgePullToRefresh`),
                // wrapped here rather than inside each screen so one place decides which tabs offer it
                // and no screen can quietly stop offering it.
                ForgeTab.Today ->
                    if (!ForgeDesign.preview && DashboardStylePrefs.style(context) == "classic") ForgeClassicToday(padding, viewModel)
                    else ForgePullToRefresh(viewModel) { ForgeTodayScreen(padding, width, viewModel) }
                ForgeTab.Trends -> ForgeTrendsScreen(padding, width, viewModel)
                ForgeTab.Health -> ForgePullToRefresh(viewModel) { ForgeHealthTab(padding, width, viewModel) }
                ForgeTab.Training -> ForgePullToRefresh(viewModel) { ForgeTrainingScreen(padding, viewModel) }
                ForgeTab.Sleep -> ForgePullToRefresh(viewModel) { ForgeSleepScreen(padding, width, viewModel) }
                // The food log in its own tab (owner, 2026-11-21); leaving it returns to Today, as the coach's tab.
                ForgeTab.Nutrition -> {
                    val select = LocalForgeTabSelect.current
                    androidx.activity.compose.BackHandler { select(ForgeTab.Today) }
                    androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.padding(padding)) {
                        ForgeFoodPage(onBack = { select(ForgeTab.Today) }, asTab = true, viewModel = viewModel)
                    }
                }
                // No bar shows the coach as a tab any more (owner, 2026-11-21); selecting it opens the coach sheet.
                ForgeTab.Coach -> Unit
            }
        }
        }
        // The profile photo's Settings; its search field opens the settings search.
        if (settingsOpen && ForgeDesign.preview) {
            ForgeOverlayPage(onDismiss = { settingsOpen = false }) {
                ForgeSettings(viewModel, onClose = { settingsOpen = false },
                              onSearch = { settingsOpen = false })
            }
        }
        // The search overlay opens over the tabs, as iOS pushes ForgeRoute.search.
        if (searchOpen && ForgeDesign.preview) {
            ForgeSearchOverlay(
                viewModel = viewModel,
                onDismiss = { searchOpen = false },
                onOpenDest = { dest ->
                    searchOpen = false
                    if (dest == MoreDest.Settings) settingsOpen = true
                    else moreNav.push(MoreRoute.Dest(dest))
                },
                onOpenSettingsPage = { page ->
                    searchOpen = false
                    searchSettingsPage = page
                },
            )
        }
        // A settings page opened from search: open settings directly to it.
        if (searchSettingsPage != null && ForgeDesign.preview) {
            val page = searchSettingsPage!!
            ForgeOverlayPage(onDismiss = { searchSettingsPage = null }) {
                ForgeSettings(viewModel, initialPage = page, onClose = { searchSettingsPage = null }, onSearch = null)
            }
        }
        // The MoreDest screens opened from search, shown over the tabs.
        MoreOverlay(moreNav, viewModel, onOpenSettings = { settingsOpen = true })
        // The battery screen opens over the tabs, as iOS pushes `TabRoute.battery`.
        if (batteryOpen && ForgeDesign.preview) {
            ForgeOverlayPage(onDismiss = { batteryOpen = false }) {
                ForgeBatteryScreen(viewModel, onClose = { batteryOpen = false }, onOpenDevices = { batteryOpen = false })
            }
        }
        // In the new design the coach sits in each main page's header and opens as a sheet, as on iOS; the floating
        // button is for the shipped tabs.
        if (coachOpen && openCoach != null) ForgeCoachSheet(viewModel, onDismiss = { coachOpen = false })
        if (!ForgeDesign.preview) ForgeCoachButton(viewModel)
    }
}
