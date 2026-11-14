package com.noop.widget

import android.content.Context
import com.noop.analytics.WidgetCompanion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class WidgetCompanionStoreTest {

    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val sample = WidgetCompanion(
        streaksOn = true, streakDays = 12, legStart = 7, legTarget = 14,
        trainedOn = Instant.ofEpochSecond(1_760_000_000L), ownsFire = true, biscuitOn = false,
    )

    @Test fun anUnsetStoreReadsNull() {
        assertNull(WidgetCompanionStore.load(context))
        // Nothing was written yet, so nothing is stale.
        assertTrue(WidgetCompanionStore.saveIfChanged(context, WidgetCompanion()))
        assertEquals(WidgetCompanion(), WidgetCompanionStore.load(context))
    }

    @Test fun theSnapshotRoundTrips() {
        WidgetCompanionStore.saveIfChanged(context, sample)
        assertEquals(sample, WidgetCompanionStore.load(context))
    }

    @Test fun anUnchangedSnapshotIsNotWritten() {
        WidgetCompanionStore.saveIfChanged(context, sample)
        assertFalse(WidgetCompanionStore.saveIfChanged(context, sample))
        assertEquals(sample, WidgetCompanionStore.load(context))
        // A different snapshot still writes.
        assertTrue(WidgetCompanionStore.saveIfChanged(context, sample.copy(streakDays = 13)))
    }

    @Test fun aCorruptStoreReadsNullRatherThanCrashing() {
        context.getSharedPreferences("noop_widget", Context.MODE_PRIVATE)
            .edit().putString("noop.widget.companion.v1", "{not json").apply()
        assertNull(WidgetCompanionStore.load(context))
    }
}
