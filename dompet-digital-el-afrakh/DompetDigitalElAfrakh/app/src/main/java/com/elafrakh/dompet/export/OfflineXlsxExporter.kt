package com.elafrakh.dompet.export

import android.content.Context
import android.net.Uri
import com.elafrakh.dompet.data.local.TransactionType
import com.elafrakh.dompet.util.DateTimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Lightweight pure-Kotlin OOXML (.xlsx) generator without heavy external libraries.
 */
object OfflineXlsxExporter {

    suspend fun exportToUri(
        context: Context,
        outputUri: Uri,
        report: ReportSummary
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val out = context.contentResolver.openOutputStream(outputUri)
                ?: error("Tidak dapat membuka lokasi penyimpanan file Excel.")
            out.use { stream ->
                writeXlsx(stream, report)
            }
        }
    }

    fun writeXlsx(outputStream: OutputStream, report: ReportSummary) {
        val sheetXml = buildWorksheetXml(report)
        ZipOutputStream(outputStream).use { zip ->
            addZipEntry(zip, "[Content_Types].xml", CONTENT_TYPES_XML)
            addZipEntry(zip, "_rels/.rels", ROOT_RELS_XML)
            addZipEntry(zip, "xl/workbook.xml", WORKBOOK_XML)
            addZipEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS_XML)
            addZipEntry(zip, "xl/styles.xml", STYLES_XML)
            addZipEntry(zip, "xl/worksheets/sheet1.xml", sheetXml)
        }
    }

    fun escapeXml(value: String): String {
        val sb = StringBuilder(value.length)
        for (ch in value) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> if (ch.code >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') {
                    sb.append(ch)
                }
            }
        }
        return sb.toString()
    }

    fun columnLetter(indexZeroBased: Int): String {
        var idx = indexZeroBased
        val sb = StringBuilder()
        while (idx >= 0) {
            sb.insert(0, ('A' + (idx % 26)))
            idx = (idx / 26) - 1
        }
        return sb.toString()
    }

    private fun buildWorksheetXml(report: ReportSummary): String {
        val sb = StringBuilder(8192)
        sb.append(
            """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <cols>
    <col min="1" max="1" width="6" customWidth="1"/>
    <col min="2" max="2" width="14" customWidth="1"/>
    <col min="3" max="3" width="10" customWidth="1"/>
    <col min="4" max="4" width="16" customWidth="1"/>
    <col min="5" max="5" width="38" customWidth="1"/>
    <col min="6" max="6" width="18" customWidth="1"/>
    <col min="7" max="7" width="14" customWidth="1"/>
  </cols>
  <sheetData>
"""
        )

        var row = 1
        appendTextRow(sb, row++, listOf("Dompet Digital El Afrakh — Laporan Keuangan (${report.period.label})"), style = 1)
        appendTextRow(sb, row++, listOf("Periode: ${report.periodTitle}"))
        appendTextRow(sb, row++, listOf("Dicetak (WIB): ${DateTimeHelper.formatDateTime(System.currentTimeMillis())}"))
        row++ // blank spacer row

        appendSummaryRow(sb, row++, "Total Pemasukan", report.totalIncome)
        appendSummaryRow(sb, row++, "Total Pengeluaran", report.totalExpense)
        appendSummaryRow(sb, row++, "Selisih", report.netDifference)
        row++ // blank spacer row

        appendTextRow(
            sb,
            row++,
            listOf("No", "Tanggal", "Waktu", "Jenis", "Keterangan", "Nominal (Rp)", "Foto Nota"),
            style = 1
        )

        report.transactions.forEachIndexed { idx, tx ->
            val currentRow = row++
            val isIncome = tx.type == TransactionType.INCOME
            sb.append("""    <row r="$currentRow">""")
            sb.append(numCell(0, currentRow, (idx + 1).toLong(), style = 0))
            sb.append(strCell(1, currentRow, DateTimeHelper.formatDate(tx.timestampMillis), style = 0))
            sb.append(strCell(2, currentRow, DateTimeHelper.formatTime(tx.timestampMillis), style = 0))
            sb.append(strCell(3, currentRow, if (isIncome) "Pemasukan" else "Pengeluaran", style = 0))
            sb.append(strCell(4, currentRow, tx.note, style = 0))
            sb.append(numCell(5, currentRow, tx.amount, style = 2))
            sb.append(strCell(6, currentRow, if (tx.receiptFileName != null) "Ada" else "-", style = 0))
            sb.append("</row>\n")
        }

        sb.append("  </sheetData>\n</worksheet>")
        return sb.toString()
    }

    private fun appendTextRow(sb: StringBuilder, rowNum: Int, values: List<String>, style: Int = 0) {
        sb.append("""    <row r="$rowNum">""")
        values.forEachIndexed { colIdx, text ->
            sb.append(strCell(colIdx, rowNum, text, style))
        }
        sb.append("</row>\n")
    }

    private fun appendSummaryRow(sb: StringBuilder, rowNum: Int, label: String, amount: Long) {
        sb.append("""    <row r="$rowNum">""")
        sb.append(strCell(0, rowNum, label, style = 1))
        sb.append(numCell(1, rowNum, amount, style = 2))
        sb.append("</row>\n")
    }

    private fun strCell(colZeroBased: Int, rowNum: Int, text: String, style: Int): String {
        val ref = "${columnLetter(colZeroBased)}$rowNum"
        return """<c r="$ref" t="inlineStr" s="$style"><is><t>${escapeXml(text)}</t></is></c>"""
    }

    private fun numCell(colZeroBased: Int, rowNum: Int, number: Long, style: Int): String {
        val ref = "${columnLetter(colZeroBased)}$rowNum"
        return """<c r="$ref" t="n" s="$style"><v>$number</v></c>"""
    }

    private fun addZipEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(StandardCharsets.UTF_8))
        zip.closeEntry()
    }

    private const val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private const val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val WORKBOOK_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"
          xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Laporan Keuangan" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

    private const val WORKBOOK_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
  </fonts>
  <fills count="2">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  <cellXfs count="3">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
    <xf numFmtId="3" fontId="0" fillId="0" borderId="0" xfId="0" applyNumberFormat="1"/>
  </cellXfs>
</styleSheet>"""
}
