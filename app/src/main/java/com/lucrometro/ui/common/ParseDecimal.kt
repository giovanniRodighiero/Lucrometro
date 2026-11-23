package com.lucrometro.ui.common

private val DECIMAL = Regex("""\d+([.,]\d*)?|[.,]\d+""")

/** A positive decimal typed with either a dot or a comma as the decimal mark (decision 0033); null if it is not one. */
fun parseDecimal(text: String): Double? =
    text.trim().takeIf { DECIMAL.matches(it) }?.replace(',', '.')?.toDoubleOrNull()
