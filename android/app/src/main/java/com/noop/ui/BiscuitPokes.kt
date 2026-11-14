package com.noop.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import com.noop.analytics.BiscuitColor

/**
 * Poked-count state for the Biscuit face (parity twin of `NoopBiscuitPokes`).
 *
 * [count] is the all-time poke total; at [BiscuitPokes.TOA…ED_AT] the toasted coat
 * unlocks. [line] is a milestone caption (string resource) shown after a poked poke,
 * cleared by the caller after its display window. Persisted in the shared `noop_prefs`
 * store, keys `noop.biscuit.pokes` / `noop.biscuit.toasted`.
 */
class BiscuitPokes internal constructor(private val prefs: SharedPreferences) {

    var count: Int = prefs.getInt(KEY_COUNT, 0)
        private set

    var wearsToasted: Boolean = prefs.getBoolean(KEY_TOASTED, false)
        set(value) {
            field = value
            prefs.edit().putBoolean(KEY_TOASTED, value).apply()
        }

    @StringRes var line: Int? = null

    val toastedUnlocked: Boolean
        get() = count >= TOASTED_AT

    /**
     * Record one poke. Returns the milestone caption to show when this poke hit a
     * milestone, otherwise null.
     */
    fun poked(): Int? {
        count += 1
        prefs.edit().putInt(KEY_COUNT, count).apply()
        val said = MILESTONES[count]
        if (said != null) line = said
        return said
    }

    fun clearLine() {
        line = null
    }

    /** The body color to draw: the toasted coat once earned and worn, else the recovery tint. */
    fun coat(recovery: Double?): BiscuitColor =
        if (wearsToasted && toastedUnlocked) BiscuitColor.toasted else BiscuitColor.recovery(recovery)

    companion object {
        const val TOASTED_AT = 500
        private const val KEY_COUNT = "noop.biscuit.pokes"
        private const val KEY_TOASTED = "noop.biscuit.toasted"

        private val MILESTONES: Map<Int, Int> = mapOf(
            10 to com.noop.R.string.biscuit_poke_10,
            25 to com.noop.R.string.biscuit_poke_25,
            50 to com.noop.R.string.biscuit_poke_50,
            100 to com.noop.R.string.biscuit_poke_100,
            250 to com.noop.R.string.biscuit_poke_250,
            TOASTED_AT to com.noop.R.string.biscuit_poke_500,
            1000 to com.noop.R.string.biscuit_poke_1000,
        )

        fun load(context: Context): BiscuitPokes =
            BiscuitPokes(context.getSharedPreferences(NoopPrefs.NAME, Context.MODE_PRIVATE))
    }
}
