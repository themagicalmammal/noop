package com.noop.ui.forge

import java.text.NumberFormat
import java.util.Locale

/** Formats a whole number with the locale's grouping separators. */
fun whole(value: Double): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(Math.round(value))

/** Formats an integer with the locale's grouping separators. */
fun integer(value: Int): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)

/** Formats a long with the locale's grouping separators. */
fun integer(value: Long): String =
    NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)
