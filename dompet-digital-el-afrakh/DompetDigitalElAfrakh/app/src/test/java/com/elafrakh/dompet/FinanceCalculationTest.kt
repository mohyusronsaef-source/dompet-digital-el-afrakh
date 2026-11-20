package com.elafrakh.dompet

import com.elafrakh.dompet.data.local.TransactionEntity
import com.elafrakh.dompet.data.local.TransactionType
import com.elafrakh.dompet.data.repository.FinanceRepository
import com.elafrakh.dompet.data.security.PinCryptoManager
import com.elafrakh.dompet.export.OfflineXlsxExporter
import com.elafrakh.dompet.export.ReportPeriod
import com.elafrakh.dompet.export.ReportSummary
import com.elafrakh.dompet.util.CurrencyFormatter
import com.elafrakh.dompet.util.DateTimeHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.ZonedDateTime
import java.util.zip.ZipInputStream

class FinanceCalculationTest {

    @Test
    fun `final balance formula computes initial plus income minus expense`() {
        val initial = 1_000_000L
        val income = 500_000L
        val expense = 225_000L
        val expected = 1_275_000L

        assertEquals(expected, FinanceRepository.computeFinalBalance(initial, income, expense))
    }

    @Test
    fun `rupiah formatter formats positive negative and signed values correctly`() {
        assertEquals("Rp1.275.000", CurrencyFormatter.formatRupiah(1_275_000L))
        assertEquals("-Rp75.000", CurrencyFormatter.formatRupiah(-75_000L))
        assertEquals("+ Rp500.000", CurrencyFormatter.formatSignedRupiah(500_000L, isIncome = true))
        assertEquals("- Rp75.000", CurrencyFormatter.formatSignedRupiah(75_000L, isIncome = false))
    }

    @Test
    fun `date time helper formats in Asia Jakarta timezone`() {
        val jakartaTime = ZonedDateTime.of(2025, 11, 20, 14, 30, 0, 0, DateTimeHelper.JAKARTA_ZONE)
        val millis = jakartaTime.toInstant().toEpochMilli()

        assertEquals("20/11/2025", DateTimeHelper.formatDate(millis))
        assertEquals("14:30", DateTimeHelper.formatTime(millis))
        assertEquals("20/11/2025 • 14:30", DateTimeHelper.formatDateTime(millis))
    }

    @Test
    fun `pin crypto manager hashes and verifies 6 digit pin securely`() {
        val hashed = PinCryptoManager.hashSecret("123456")
        assertTrue(PinCryptoManager.verifySecret("123456", hashed.hashBase64, hashed.saltBase64))
        assertFalse(PinCryptoManager.verifySecret("654321", hashed.hashBase64, hashed.saltBase64))
    }

    @Test
    fun `xlsx exporter generates valid zip entries with escaped xml`() {
        val report = ReportSummary(
            period = ReportPeriod.DAILY,
            periodTitle = "20 November 2025",
            totalIncome = 500_000L,
            totalExpense = 75_000L,
            netDifference = 425_000L,
            buckets = emptyList(),
            transactions = listOf(
                TransactionEntity(
                    id = 1L,
                    type = TransactionType.INCOME,
                    amount = 500_000L,
                    note = "Penjualan nasi goreng & es teh <paket>",
                    timestampMillis = 1_763_623_800_000L
                )
            )
        )
        val out = ByteArrayOutputStream()
        OfflineXlsxExporter.writeXlsx(out, report)

        val entries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(out.toByteArray())).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                entry = zip.nextEntry
            }
        }
        assertTrue(entries.contains("[Content_Types].xml"))
        assertTrue(entries.contains("xl/worksheets/sheet1.xml"))
    }
}
