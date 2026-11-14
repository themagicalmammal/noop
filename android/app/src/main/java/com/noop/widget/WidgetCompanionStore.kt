package com.noop.widget

import android.content.Context
import com.noop.analytics.WidgetCompanion
import org.json.JSONObject

/**
 * The one stored companion snapshot the three companion widgets read, mirrored from Aura's
 * `WidgetCompanion.load` / `saveIfChanged` (StrandiOSShared). Widgets never recompute the mood's
 * inputs; they read this value, so the three panels cannot disagree.
 */
object WidgetCompanionStore {
    private const val PREFS = "noop_widget"

    fun load(context: Context): WidgetCompanion? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(WidgetCompanion.storageKey, null) ?: return null
        return runCatching { WidgetCompanion.fromJson(JSONObject(raw)) }.getOrNull()
    }

    /** Writes only when the snapshot differs from what is stored, and says so: the publish
     *  path uses the result to decide whether the widgets are stale. */
    fun saveIfChanged(context: Context, snapshot: WidgetCompanion): Boolean {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val current = runCatching {
            prefs.getString(WidgetCompanion.storageKey, null)?.let { WidgetCompanion.fromJson(JSONObject(it)) }
        }.getOrNull()
        if (current == snapshot) return false
        prefs.edit().putString(WidgetCompanion.storageKey, snapshot.encode().toString()).apply()
        return true
    }
}
