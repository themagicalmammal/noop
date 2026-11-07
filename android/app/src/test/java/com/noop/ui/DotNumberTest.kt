package com.noop.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The dot-matrix glyph layout is a cross-platform byte-identical contract (twin:
 * `DotMatrixTests`, StrandDesign). The same shared literal is pasted verbatim into both
 * tests, so a drift in either glyph table fails on both platforms at once.
 */
class DotNumberTest {
    /** `grid("26,580")` — 7 rows x 23 columns; each group of 4 columns is one glyph + gap. */
    private val expected = listOf(
        listOf(1,1,1,0, 1,1,1,0, 0,0,0,0, 1,1,1,0, 1,1,1,0, 1,1,1),
        listOf(0,0,1,0, 1,0,0,0, 0,0,0,0, 1,0,0,0, 1,0,1,0, 1,0,1),
        listOf(1,1,1,0, 1,1,1,0, 0,0,0,0, 1,1,1,0, 1,0,1,0, 1,0,1),
        listOf(1,1,0,0, 1,0,1,0, 0,0,0,0, 0,0,1,0, 1,1,1,0, 1,0,1),
        listOf(1,0,0,0, 1,0,1,0, 0,0,0,0, 0,0,1,0, 1,0,1,0, 1,0,1),
        listOf(1,0,0,0, 1,0,1,0, 1,1,1,0, 0,0,1,0, 1,0,1,0, 1,0,1),
        listOf(1,1,1,0, 1,1,1,0, 1,1,1,0, 1,1,1,0, 1,1,1,0, 1,1,1),
    )

    @Test
    fun `grid layout matches the shared literal`() {
        assertEquals(expected, DotMatrix.grid("26,580"))
    }

    @Test
    fun `empty text yields empty rows`() {
        val grid = DotMatrix.grid("")
        assertEquals(DotMatrix.ROWS, grid.size)
        assertTrue(grid.all { it.isEmpty() })
    }

    @Test
    fun `unknown characters render blank`() {
        val grid = DotMatrix.grid("?")
        assertEquals(DotMatrix.ROWS, grid.size)
        assertEquals(DotMatrix.GLYPH_COLUMNS, grid.first().size)
        assertTrue(grid.all { row -> row.all { it == 0 } })
    }
}
