package com.noop.ui

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parity pin for [BiscuitPokes] (Aura `NoopBiscuitPokes`). */
class BiscuitPokesTest {

    /** Minimal in-memory [SharedPreferences] double so this stays a pure-JVM test. */
    private class FakePrefs : SharedPreferences {
        val values = HashMap<String, Any?>()

        override fun getAll(): Map<String, *> = values.filterValues { it != null }
        override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
        override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String> =
            values[key] as? MutableSet<String> ?: defValues ?: mutableSetOf()
        override fun contains(key: String?): Boolean = key != null && values[key] != null
        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            override fun putString(key: String?, value: String?) = apply { values[key!!] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { this@FakePrefs.values[key!!] = values }
            override fun putInt(key: String?, value: Int) = apply { this@FakePrefs.values[key!!] = value }
            override fun putLong(key: String?, value: Long) = apply { this@FakePrefs.values[key!!] = value }
            override fun putFloat(key: String?, value: Float) = apply { this@FakePrefs.values[key!!] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { this@FakePrefs.values[key!!] = value }
            override fun remove(key: String?) = apply { values.remove(key) }
            override fun clear() = apply { values.clear() }
            override fun commit(): Boolean = true
            override fun apply() {}
        }
    }

    private fun pokes() = BiscuitPokes(FakePrefs())

    @Test
    fun pokedCountsAndPersists() {
        val store = FakePrefs()
        val p = BiscuitPokes(store)
        assertEquals(0, p.count)
        assertNull(p.poked())
        assertEquals(1, p.count)
        assertEquals(1, BiscuitPokes(store).count)
    }

    @Test
    fun milestonesFireExactlyOnce() {
        val p = pokes()
        val seen = mutableMapOf<Int, Int>()
        for (i in 1..1000) {
            val line = p.poked()
            if (line != null) seen[p.count] = line
        }
        assertEquals(
            setOf(10, 25, 50, 100, 250, 500, 1000),
            seen.keys,
        )
        assertEquals(1000, p.count)
        assertEquals(7, seen.size)
        p.clearLine()
        assertNull(p.line)
    }

    @Test
    fun toastedCoatUnlocksAt500() {
        val p = pokes()
        assertFalse(p.toastedUnlocked)
        assertEquals(0, p.count)
        repeat(499) { p.poked() }
        assertFalse(p.toastedUnlocked)
        p.poked()
        assertTrue(p.toastedUnlocked)
    }

    @Test
    fun coatFollowsToastedOnlyWhenUnlocked() {
        val p = pokes()
        p.wearsToasted = true
        assertEquals(com.noop.analytics.BiscuitColor.resting, p.coat(null))
        assertEquals(com.noop.analytics.BiscuitColor.lowRecovery, p.coat(20.0))
        repeat(500) { p.poked() }
        assertEquals(com.noop.analytics.BiscuitColor.toasted, p.coat(20.0))
        p.wearsToasted = false
        assertEquals(com.noop.analytics.BiscuitColor.lowRecovery, p.coat(20.0))
        p.wearsToasted = true
        assertEquals(com.noop.analytics.BiscuitColor.toasted, p.coat(90.0))
    }
}
