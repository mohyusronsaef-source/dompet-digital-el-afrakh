package com.elafrakh.dompet.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.elafrakh.dompet.data.local.TransactionType
import com.elafrakh.dompet.util.CurrencyFormatter
import com.elafrakh.dompet.util.DateTimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object OfflinePdfExporter {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 36f

    suspend fun exportToUri(
        context: Context,
        outputUri: Uri,
        report: ReportSummary
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val document = PdfDocument()
            try {
                var pageNumber = 1
                var pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                var page = document.startPage(pageInfo)
                var canvas = page.canvas

                val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0D47A1")
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#334155")
                    textSize = 11f
                }
                val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#0F172A")
                    textSize = 12f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#1E293B")
                    textSize = 10f
                }
                val incomePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#16A34A")
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val expensePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.parseColor("#DC2626")
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                }
                val linePaint = Paint().apply {
                    color = Color.parseColor("#CBD5E1")
                    strokeWidth = 1f
                }
                val headerBgPaint = Paint().apply {
                    color = Color.parseColor("#EFF6FF")
                }

                var y = MARGIN + 18f
                canvas.drawText("Dompet Digital El Afrakh", MARGIN, y, titlePaint)
                y += 18f
                canvas.drawText(
                    "Laporan Keuangan ${report.period.label} - ${report.periodTitle}",
                    MARGIN,
                    y,
                    subtitlePaint
                )
                y += 14f
                canvas.drawText(
                    "Dicetak: ${DateTimeHelper.formatDateTime(System.currentTimeMillis())} WIB (Offline)",
                    MARGIN,
                    y,
                    subtitlePaint
                )
                y += 14f
                canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
                y += 18f

                // Summary box
                canvas.drawRect(MARGIN, y, PAGE_WIDTH - MARGIN, y + 62f, headerBgPaint)
                canvas.drawText("Ringkasan Periode", MARGIN + 12f, y + 18f, sectionPaint)
                canvas.drawText(
                    "Total Pemasukan: ${CurrencyFormatter.formatRupiah(report.totalIncome)}",
                    MARGIN + 12f,
                    y + 36f,
                    incomePaint
                )
                canvas.drawText(
                    "Total Pengeluaran: ${CurrencyFormatter.formatRupiah(report.totalExpense)}",
                    MARGIN + 220f,
                    y + 36f,
                    expensePaint
                )
                canvas.drawText(
                    "Selisih: ${CurrencyFormatter.formatRupiah(report.netDifference)}",
                    MARGIN + 12f,
                    y + 52f,
                    sectionPaint
                )
                y += 82f

                fun drawTableHeader(targetY: Float): Float {
                    canvas.drawRect(MARGIN, targetY, PAGE_WIDTH - MARGIN, targetY + 22f, headerBgPaint)
                    val textY = targetY + 15f
                    canvas.drawText("No", MARGIN + 6f, textY, sectionPaint)
                    canvas.drawText("Tanggal & Jam", MARGIN + 34f, textY, sectionPaint)
                    canvas.drawText("Jenis", MARGIN + 148f, textY, sectionPaint)
                    canvas.drawText("Keterangan", MARGIN + 220f, textY, sectionPaint)
                    canvas.drawText("Nominal", PAGE_WIDTH - MARGIN - 90f, textY, sectionPaint)
                    return targetY + 26f
                }

                canvas.drawText("Daftar Transaksi (${report.transactions.size} catatan)", MARGIN, y, sectionPaint)
                y += 10f
                y = drawTableHeader(y)

                if (report.transactions.isEmpty()) {
                    canvas.drawText("Belum ada transaksi pada periode ini.", MARGIN + 6f, y + 14f, bodyPaint)
                } else {
                    report.transactions.forEachIndexed { index, tx ->
                        if (y > PAGE_HEIGHT - MARGIN - 28f) {
                            document.finishPage(page)
                            pageNumber += 1
                            pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                            page = document.startPage(pageInfo)
                            canvas = page.canvas
                            y = MARGIN
                            y = drawTableHeader(y)
                        }

                        val isIncome = tx.type == TransactionType.INCOME
                        val rowTextY = y + 14f
                        canvas.drawText("${index + 1}", MARGIN + 6f, rowTextY, bodyPaint)
                        canvas.drawText(
                            "${DateTimeHelper.formatDate(tx.timestampMillis)} ${DateTimeHelper.formatTime(tx.timestampMillis)}",
                            MARGIN + 34f,
                            rowTextY,
                            bodyPaint
                        )
                        canvas.drawText(
                            if (isIncome) "Pemasukan" else "Pengeluaran",
                            MARGIN + 148f,
                            rowTextY,
                            if (isIncome) incomePaint else expensePaint
                        )
                        val truncatedNote = if (tx.note.length > 26) tx.note.take(25) + "…" else tx.note
                        canvas.drawText(truncatedNote, MARGIN + 220f, rowTextY, bodyPaint)
                        canvas.drawText(
                            CurrencyFormatter.formatSignedRupiah(tx.amount, isIncome),
                            PAGE_WIDTH - MARGIN - 90f,
                            rowTextY,
                            if (isIncome) incomePaint else expensePaint
                        )
                        y += 20f
                        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, linePaint)
                    }
                }

                document.finishPage(page)

                context.contentResolver.openOutputStream(outputUri)?.use { out ->
                    document.writeTo(out)
                    out.flush()
                } ?: error("Tidak dapat membuka lokasi penyimpanan file PDF.")
            } finally {
                document.close()
            }
        }
    }
}
