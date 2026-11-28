package com.noop.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.noop.R
import com.noop.ingest.ImportDepth
import androidx.compose.ui.res.stringResource
import com.noop.ui.forge.Forge
import com.noop.ui.forge.Segmented

/**
 * How far back the next import reads, chosen where the import is started.
 *
 * Fork (DX23876), 2026-11-22. Shown in both import flows rather than buried in settings: the choice is
 * made at the moment somebody decides to import, and it is the same decision in both places, so it is
 * one composable reading and writing one preference.
 *
 * It is remembered, because the periodic auto-sync has no screen to ask from and must use the same answer
 * as the manual one — otherwise a wearer who picked a month would still have a decade read behind his
 * back every twelve hours.
 */
@Composable
internal fun ImportDepthChooser(modifier: Modifier = Modifier, enabled: Boolean = true) {
    val context = LocalContext.current
    val colors = Forge.colors
    var depth by remember { mutableStateOf(ImportDepth.ofMonths(NoopPrefs.importMonths(context))) }
    val steps = ImportDepth.entries

    Segmented(
        options = steps.toList(),
        selected = depth,
        label = { stringResource(importDepthLabel(it)) },
        onSelect = { step ->
            depth = step
            NoopPrefs.setImportMonths(context, step.months)
        },
    )
}

/** The label of one step. Separate so a test can assert every step has one. */
internal fun importDepthLabel(depth: ImportDepth): Int = when (depth) {
    ImportDepth.ONE_MONTH -> R.string.import_depth_one_month
    ImportDepth.ONE_YEAR -> R.string.import_depth_one_year
    ImportDepth.THREE_YEARS -> R.string.import_depth_three_years
    ImportDepth.TEN_YEARS -> R.string.import_depth_ten_years
}
