package com.noop.ui.forge

import com.noop.ui.NoopPrefs

/** Coach-note ratings, as iOS's `forge.coach.ratings`: "noteId:origin:+1|-1" lines. */
internal object CoachRatings {
    private const val KEY = "forge.coach.ratings"

    /** The reader's rating for [id], or 0 for none. */
    fun rating(context: android.content.Context, id: String): Int {
        val raw = NoopPrefs.of(context).getString(KEY, "") ?: ""
        return raw.split("\n").firstOrNull { it.startsWith("$id:") }
            ?.substringAfterLast(":")?.toIntOrNull() ?: 0
    }

    /** Record [value] (+1 / -1) for [id] from [origin]; keeps at most 300 lines. */
    fun record(context: android.content.Context, id: String, origin: String, value: Int) {
        val prefs = NoopPrefs.of(context)
        val raw = prefs.getString(KEY, "") ?: ""
        val lines = raw.split("\n").filter { it.isNotEmpty() && !it.startsWith("$id:") }
            .toMutableList()
        if (rating(context, id) != value) lines.add("$id:$origin:$value")
        prefs.edit().putString(KEY, lines.takeLast(300).joinToString("\n")).apply()
    }

    /** Count of thumbs down for the given origin, for the "less often?" prompt. */
    fun downCount(context: android.content.Context, origin: String): Int {
        val raw = NoopPrefs.of(context).getString(KEY, "") ?: ""
        return raw.split("\n").count { it.endsWith(":$origin:-1") }
    }
}
