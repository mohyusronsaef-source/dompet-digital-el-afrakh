package com.elafrakh.dompet.util

import com.elafrakh.dompet.data.local.TransactionEntity
import com.elafrakh.dompet.data.local.TransactionType
import com.elafrakh.dompet.export.ChartBucket
import com.elafrakh.dompet.export.ReportPeriod
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

object DateTimeHelper {

    val JAKARTA_ZONE: ZoneId = ZoneId.of("Asia/Jakarta")
    private val ID_LOCALE = Locale("id", "ID")

    private val DATE_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy", ID_LOCALE)
    private val TIME_FORMATTER: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm", ID_LOCALE)
    private val SHORT_DAY_MONTH: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM", ID_LOCALE)
    private val FULL_DATE_ID: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMMM yyyy", ID_LOCALE)
    private val MONTH_YEAR_ID: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MMMM yyyy", ID_LOCALE)

    fun formatDate(timestampMillis: Long): String {
        val zdt = Instant.ofEpochMilli(timestampMillis).atZone(JAKARTA_ZONE)
        return zdt.format(DATE_FORMATTER)
    }

    fun formatTime(timestampMillis: Long): String {
        val zdt = Instant.ofEpochMilli(timestampMillis).atZone(JAKARTA_ZONE)
        return zdt.format(TIME_FORMATTER)
    }

    fun formatDateTime(timestampMillis: Long): String =
        "${formatDate(timestampMillis)} • ${formatTime(timestampMillis)}"

    fun toLocalDate(timestampMillis: Long): LocalDate =
        Instant.ofEpochMilli(timestampMillis).atZone(JAKARTA_ZONE).toLocalDate()

    fun todayLocalDate(): LocalDate = LocalDate.now(JAKARTA_ZONE)

    fun dayRangeMillis(date: LocalDate): Pair<Long, Long> {
        val start = date.atStartOfDay(JAKARTA_ZONE).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(JAKARTA_ZONE).toInstant().toEpochMilli()
        return start to end
    }

    fun todayRangeMillis(): Pair<Long, Long> = dayRangeMillis(todayLocalDate())

    fun buildLast7DaysBuckets(transactions: List<TransactionEntity>): List<ChartBucket> {
        val today = todayLocalDate()
        val days = (6 downTo 0).map { offset -> today.minusDays(offset.toLong()) }
        val incomeMap = HashMap<LocalDate, Long>()
        val expenseMap = HashMap<LocalDate, Long>()

        val minMillis = days.first().atStartOfDay(JAKARTA_ZONE).toInstant().toEpochMilli()
        for (tx in transactions) {
            if (tx.timestampMillis < minMillis) continue
            val txDate = toLocalDate(tx.timestampMillis)
            if (tx.type == TransactionType.INCOME) {
                incomeMap[txDate] = (incomeMap[txDate] ?: 0L) + tx.amount
            } else {
                expenseMap[txDate] = (expenseMap[txDate] ?: 0L) + tx.amount
            }
        }

        return days.map { day ->
            ChartBucket(
                label = day.format(SHORT_DAY_MONTH),
                income = incomeMap[day] ?: 0L,
                expense = expenseMap[day] ?: 0L
            )
        }
    }

    data class PeriodRange(
        val startMillis: Long,
        val endMillis: Long,
        val title: String,
        val buckets: List<ChartBucket>
    )

    fun computePeriodData(
        period: ReportPeriod,
        offset: Int,
        transactions: List<TransactionEntity>
    ): PeriodRange {
        val today = todayLocalDate()
        return when (period) {
            ReportPeriod.DAILY -> {
                val targetDate = today.plusDays(offset.toLong())
                val (start, end) = dayRangeMillis(targetDate)
                val filtered = transactions.filter { it.timestampMillis in start until end }
                val buckets = listOf("00-06", "06-12", "12-18", "18-24").mapIndexed { idx, label ->
                    val bStart = idx * 6
                    val bEnd = (idx + 1) * 6
                    var inc = 0L
                    var exp = 0L
                    for (tx in filtered) {
                        val hour = Instant.ofEpochMilli(tx.timestampMillis).atZone(JAKARTA_ZONE).hour
                        if (hour in bStart until bEnd) {
                            if (tx.type == TransactionType.INCOME) inc += tx.amount else exp += tx.amount
                        }
                    }
                    ChartBucket(label = label, income = inc, expense = exp)
                }
                PeriodRange(start, end, targetDate.format(FULL_DATE_ID), buckets)
            }
            ReportPeriod.WEEKLY -> {
                val baseMonday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                    .plusWeeks(offset.toLong())
                val sunday = baseMonday.plusDays(6)
                val start = baseMonday.atStartOfDay(JAKARTA_ZONE).toInstant().toEpochMilli()
                val end = sunday.plusDays(1).atStartOfDay(JAKARTA_ZONE).toInstant().toEpochMilli()
                val dayLabels = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                val filtered = transactions.filter { it.timestampMillis in start until end }
                val buckets = (0..6).map { dayIdx ->
                    val d = baseMonday.plusDays(dayIdx.toLong())
                    var inc = 0L
                    var exp = 0L
                    for (tx in filtered) {
                        if (toLocalDate(tx.timestampMillis) == d) {
                            if (tx.type == TransactionType.INCOME) inc += tx.amount else exp += tx.amount
                        }
                    }
                    ChartBucket(label = dayLabels[dayIdx], income = inc, expense = exp)
                }
                val title = "${baseMonday.format(SHORT_DAY_MONTH)} - ${sunday.format(FULL_DATE_ID)}"
                PeriodRange(start, end, title, buckets)
            }
            ReportPeriod.MONTHLY -> {
                val ym = YearMonth.from(today).plusMonths(offset.toLong())
                val firstDay = ym.atDay(1)
                val lastDay = ym.atEndOfMonth()
                val start = firstDay.atStartOfDay(JAKARTA_ZONE).toInstant().toEpochMilli()
                val end = lastDay.plusDays(1).atStartOfDay(JAKARTA_ZONE).toInstant().toEpochMilli()
                val filtered = transactions.filter { it.timestampMillis in start until end }
                val buckets = (1..4).map { weekNum ->
                    val minD = (weekNum - 1) * 7 + 1
                    val maxD = if (weekNum == 4) lastDay.dayOfMonth else weekNum * 7
                    var inc = 0L
                    var exp = 0L
                    for (tx in filtered) {
                        val dom = toLocalDate(tx.timestampMillis).dayOfMonth
                        if (dom in minD..maxD) {
                            if (tx.type == TransactionType.INCOME) inc += tx.amount else exp += tx.amount
                        }
                    }
                    ChartBucket(label = "Mgg $weekNum", income = inc, expense = exp)
                }
                val title = ym.atDay(1).format(MONTH_YEAR_ID).replaceFirstChar { it.uppercase() }
                PeriodRange(start, end, title, buckets)
            }
            ReportPeriod.YEARLY -> {
                val year = today.year + offset
                val firstDay = LocalDate.of(year, 1, 1)
                val lastDay = LocalDate.of(year, 12, 31)
                val start = firstDay.atStartOfDay(JAKARTA_ZONE).toInstant().toEpochMilli()
                val end = lastDay.plusDays(1).atStartOfDay(JAKARTA_ZONE).toInstant().toEpochMilli()
                val filtered = transactions.filter { it.timestampMillis in start until end }
                val monthLabels = listOf(
                    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
                    "Jul", "Agu", "Sep", "Okt", "Nov", "Des"
                )
                val buckets = (1..12).map { m ->
                    var inc = 0L
                    var exp = 0L
                    for (tx in filtered) {
                        if (toLocalDate(tx.timestampMillis).monthValue == m) {
                            if (tx.type == TransactionType.INCOME) inc += tx.amount else exp += tx.amount
                        }
                    }
                    ChartBucket(label = monthLabels[m - 1], income = inc, expense = exp)
                }
                PeriodRange(start, end, "Tahun $year", buckets)
            }
        }
    }
}
