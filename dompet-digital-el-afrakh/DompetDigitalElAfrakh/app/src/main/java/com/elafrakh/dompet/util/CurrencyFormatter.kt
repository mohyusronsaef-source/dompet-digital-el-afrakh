package com.elafrakh.dompet.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyFormatter {

    private val symbols = DecimalFormatSymbols(Locale("in", "ID")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    private fun newFormatter(): DecimalFormat = DecimalFormat("#,##0", symbols)

    fun formatRupiah(amount: Long): String {
        val isNegative = amount < 0
        val absValue = kotlin.math.abs(amount)
        val formatted = newFormatter().format(absValue)
        return if (isNegative) "-Rp$formatted" else "Rp$formatted"
    }

    fun formatSignedRupiah(amount: Long, isIncome: Boolean): String {
        val prefix = if (isIncome) "+ " else "- "
        return prefix + formatRupiah(kotlin.math.abs(amount))
    }

    fun formatDigitsOnly(rawInput: String): String {
        val digits = rawInput.filter { it.isDigit() }.take(12)
        if (digits.isEmpty()) return ""
        val value = digits.toLongOrNull() ?: return ""
        return newFormatter().format(value)
    }

    fun parseRupiahDigits(input: String): Long {
        val digits = input.filter { it.isDigit() }
        return digits.toLongOrNull() ?: 0L
    }

    fun formatCompactAxis(amount: Long): String {
        return when {
            amount >= 1_000_000L -> {
                val jt = amount / 1_000_000.0
                if (amount % 1_000_000L == 0L) "${amount / 1_000_000}jt"
                else String.format(Locale("in", "ID"), "%.1fjt", jt)
            }
            amount >= 1_000L -> "${amount / 1_000}k"
            else -> amount.toString()
        }
    }
}
