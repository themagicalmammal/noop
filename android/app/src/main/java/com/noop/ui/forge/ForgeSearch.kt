package com.noop.ui.forge

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.noop.R
import com.noop.ui.AppViewModel
import com.noop.ui.exploreMetricHits

/**
 * The redesign's one search, as iOS draws it (`ForgeSearchView`): metrics, screens and settings in one list,
 * with the last searches kept on this phone. A dedicated overlay over the tabs, opened from a tab header's
 * magnifier and the Settings search button.
 */
@Composable
internal fun ForgeSearchOverlay(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onOpenDest: (MoreDest) -> Unit,
    onOpenSettingsPage: (ForgeSettingsPage) -> Unit,
) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    Box(
        Modifier.fillMaxSize()
            // Takes every touch, so nothing reaches the tab underneath.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } }
    ) {
        ForgeSceneBackdrop(ForgeBackdrop.Canvas) {
            BackHandler(onBack = onDismiss)

            Column(
                Modifier.fillMaxSize().statusBarsPadding()
                    .padding(horizontal = ForgeMetrics.screenPadding)
                    .padding(top = ForgeMetrics.space4),
            ) {
                // Header: the back arrow and the title, as iOS's detail bar shows while scrolling.
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Rounded.Search, contentDescription = stringResource(R.string.forge_search_title),
                             tint = Forge.sceneInk.text, modifier = Modifier.size(22.dp))
                    }
                    Text(stringResource(R.string.forge_search_title), style = Forge.type.title2, color = Forge.sceneInk.text)
                }

                Spacer(Modifier.height(ForgeMetrics.space2))

                // The search field, focused on appearance as iOS's `.onAppear { focused = true }`.
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, null, tint = Forge.sceneInk.secondary, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    Icons.Rounded.Clear,
                                    null,
                                    tint = Forge.sceneInk.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    } else null,
                    shape = RoundedCornerShape(ForgeMetrics.groupedRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Forge.colors.surfaceTop,
                        unfocusedContainerColor = Forge.colors.surfaceTop,
                        focusedBorderColor = Forge.colors.accent.copy(alpha = 0.4f),
                        unfocusedBorderColor = Forge.colors.heroBorder,
                    ),
                )

                Spacer(Modifier.height(ForgeMetrics.space2))

                if (query.isBlank()) {
                    // Recent searches or the empty state.
                    val recents = SearchRecents.load(context)
                    if (recents.isNotEmpty()) {
                        SectionHeader(stringResource(R.string.forge_search_recent), Modifier.fillMaxWidth())
                        Spacer(Modifier.height(ForgeMetrics.space2))
                        @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                        androidx.compose.foundation.layout.FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(ForgeMetrics.space2),
                            verticalArrangement = Arrangement.spacedBy(ForgeMetrics.space2),
                        ) {
                            recents.forEach { r ->
                                Text(
                                    r,
                                    style = Forge.type.subhead,
                                    color = Forge.colors.text,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Forge.colors.surfaceTop)
                                        .clickable(role = Role.Button) { query = r }
                                        .padding(horizontal = 14.dp, vertical = ForgeMetrics.space2)
                                )
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxWidth().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CoachPicture(
                                mainIcon = Icons.Rounded.Search,
                                accentAIcon = Icons.Rounded.AutoAwesome,
                                accentBIcon = Icons.Rounded.Star,
                                tint = Forge.colors.accent,
                                size = 96,
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                stringResource(R.string.forge_search_hint),
                                style = Forge.type.body,
                                color = Forge.sceneInk.secondary,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    // Results: metrics first, then screens, then settings pages, as iOS lists them.
                    val q = query.trim()
                    val metrics = exploreMetricHits(q)
                    val screens = MoreDest.entries.filter { it.shown(context) }.filter { d ->
                        (listOf(context.getString(d.titleRes)) + d.keywords).any { it.contains(q, ignoreCase = true) }
                    }
                    val pages = HUB_GROUPS.flatMap { it.second }.filter { p ->
                        (listOf(context.getString(p.titleRes), context.getString(p.subtitleRes)) + SETTINGS_SEARCH_WORDS[p].orEmpty())
                            .any { it.contains(q, ignoreCase = true) }
                    }

                    if (metrics.isEmpty() && screens.isEmpty() && pages.isEmpty()) {
                        Text(
                            stringResource(R.string.forge_search_nothing_found, q),
                            style = Forge.type.body,
                            color = Forge.sceneInk.secondary,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
                    }

                    if (metrics.isNotEmpty()) {
                        SectionHeader(stringResource(R.string.forge_search_metrics))
                        ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius, contentPadding = 0.dp) {
                            metrics.forEachIndexed { i, (key, title, category) ->
                                HubRow(Icons.Rounded.ShowChart, Forge.colors.charge, title, category) {
                                    SearchRecents.remember(context, q)
                                    com.noop.ui.ExploreLaunch.pendingKey = key
                                    onOpenDest(MoreDest.Explore)
                                }
                                if (i < metrics.lastIndex) HubDivider()
                            }
                        }
                        Spacer(Modifier.height(ForgeMetrics.space2))
                    }

                    if (screens.isNotEmpty()) {
                        SectionHeader(stringResource(R.string.forge_search_screens))
                        ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius, contentPadding = 0.dp) {
                            screens.forEachIndexed { i, d ->
                                HubRow(d.icon, d.category?.let { categoryTint(it) } ?: Forge.colors.textSecondary,
                                       context.getString(d.titleRes), context.getString(d.titleRes)) {
                                    SearchRecents.remember(context, q)
                                    onOpenDest(d)
                                }
                                if (i < screens.lastIndex) HubDivider()
                            }
                        }
                        Spacer(Modifier.height(ForgeMetrics.space2))
                    }

                    if (pages.isNotEmpty()) {
                        SectionHeader(stringResource(R.string.nav_settings))
                        ForgeCard(Modifier.fillMaxWidth(), radius = ForgeMetrics.groupedRadius, contentPadding = 0.dp) {
                            pages.forEachIndexed { i, p ->
                                HubRow(p.icon, p.tint(), context.getString(p.titleRes), context.getString(p.subtitleRes)) {
                                    SearchRecents.remember(context, q)
                                    onOpenSettingsPage(p)
                                }
                                if (i < pages.lastIndex) HubDivider()
                            }
                        }
                        Spacer(Modifier.height(ForgeMetrics.space2))
                    }
                }
            }
        }
    }
}
