package com.elafrakh.dompet.export

import com.elafrakh.dompet.data.local.TransactionEntity

enum class ReportPeriod(val label: String) {
    DAILY("Harian"),
    WEEKLY("Mingguan"),
    MONTHLY("Bulanan"),
    YEARLY("Tahunan")
}

data class ChartBucket(
    val label: String,
    val income: Long,
    val expense: Long
)

data class ReportSummary(
    val period: ReportPeriod,
    val periodTitle: String,
    val totalIncome: Long,
    val totalExpense: Long,
    val netDifference: Long,
    val buckets: List<ChartBucket>,
    val transactions: List<TransactionEntity>
)
