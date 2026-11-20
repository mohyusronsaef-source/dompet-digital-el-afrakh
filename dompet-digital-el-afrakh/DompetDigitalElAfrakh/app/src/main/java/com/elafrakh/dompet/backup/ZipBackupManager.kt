package com.elafrakh.dompet.backup

import android.content.Context
import android.net.Uri
import com.elafrakh.dompet.data.local.TransactionEntity
import com.elafrakh.dompet.data.local.TransactionType
import com.elafrakh.dompet.data.repository.FinanceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class RestoreResultSummary(
    val transactionCount: Int,
    val restoredPhotosCount: Int,
    val initialBalance: Long
)

object ZipBackupManager {

    private const val BACKUP_JSON_NAME = "backup.json"
    private const val RECEIPTS_PREFIX = "receipts/"
    private const val SCHEMA_VERSION = 1

    suspend fun createBackupZip(
        context: Context,
        outputUri: Uri,
        repository: FinanceRepository
    ): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val initialBalance = repository.preferences.getInitialBalanceSnapshot()
            val transactions = repository.getAllTransactionsSnapshot()

            val json = JSONObject().apply {
                put("app", "Dompet Digital El Afrakh")
                put("schemaVersion", SCHEMA_VERSION)
                put("exportedAtMillis", System.currentTimeMillis())
                put("initialBalance", initialBalance)
                val txArray = JSONArray()
                for (tx in transactions) {
                    txArray.put(
                        JSONObject().apply {
                            put("id", tx.id)
                            put("type", tx.type.name)
                            put("amount", tx.amount)
                            put("note", tx.note)
                            put("timestampMillis", tx.timestampMillis)
                            put("receiptFileName", tx.receiptFileName ?: JSONObject.NULL)
                        }
                    )
                }
                put("transactions", txArray)
            }

            val outStream = context.contentResolver.openOutputStream(outputUri)
                ?: error("Tidak dapat membuka file tujuan backup.")

            outStream.use { rawOut ->
                ZipOutputStream(rawOut).use { zip ->
                    zip.putNextEntry(ZipEntry(BACKUP_JSON_NAME))
                    zip.write(json.toString(2).toByteArray(StandardCharsets.UTF_8))
                    zip.closeEntry()

                    val includedFiles = HashSet<String>()
                    for (tx in transactions) {
                        val fileName = tx.receiptFileName
                        if (!fileName.isNullOrBlank() && includedFiles.add(fileName)) {
                            val file = repository.receiptStore.getReceiptFile(fileName)
                            if (file.exists() && file.isFile) {
                                zip.putNextEntry(ZipEntry("$RECEIPTS_PREFIX$fileName"))
                                file.inputStream().use { input -> input.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                }
            }
            transactions.size
        }
    }

    suspend fun restoreFromBackupZip(
        context: Context,
        inputUri: Uri,
        repository: FinanceRepository
    ): Result<RestoreResultSummary> = withContext(Dispatchers.IO) {
        runCatching {
            val tempRestoreDir = File(context.cacheDir, "restore_stage_${System.currentTimeMillis()}").apply {
                mkdirs()
            }
            try {
                var backupJsonText: String? = null
                val stagedReceipts = ArrayList<File>()

                val inStream = context.contentResolver.openInputStream(inputUri)
                    ?: error("Tidak dapat membaca file backup yang dipilih.")

                inStream.use { rawIn ->
                    ZipInputStream(rawIn).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            val entryName = entry.name
                            if (!entry.isDirectory) {
                                if (entryName == BACKUP_JSON_NAME) {
                                    backupJsonText = zip.readBytes().toString(StandardCharsets.UTF_8)
                                } else if (entryName.startsWith(RECEIPTS_PREFIX)) {
                                    val safeFileName = File(entryName).name
                                    if (safeFileName.isNotBlank() && !safeFileName.contains("..")) {
                                        val destTemp = File(tempRestoreDir, safeFileName)
                                        FileOutputStream(destTemp).use { out ->
                                            zip.copyTo(out)
                                        }
                                        stagedReceipts.add(destTemp)
                                    }
                                }
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }

                val jsonRaw = backupJsonText
                    ?: error("File backup tidak valid (backup.json tidak ditemukan).")

                val root = JSONObject(jsonRaw)
                if (!root.has("initialBalance") || !root.has("transactions")) {
                    error("Format data backup tidak dikenali.")
                }

                val initialBalance = root.getLong("initialBalance").coerceAtLeast(0L)
                val txArray = root.getJSONArray("transactions")
                val restoredList = ArrayList<TransactionEntity>(txArray.length())

                for (i in 0 until txArray.length()) {
                    val item = txArray.getJSONObject(i)
                    val typeStr = item.getString("type")
                    val type = runCatching { TransactionType.valueOf(typeStr) }
                        .getOrElse { error("Jenis transaksi tidak valid pada indeks $i.") }
                    val receiptName = if (item.isNull("receiptFileName")) {
                        null
                    } else {
                        item.optString("receiptFileName").takeIf { it.isNotBlank() }?.let { File(it).name }
                    }
                    restoredList.add(
                        TransactionEntity(
                            id = 0L, // Biarkan Room menghasilkan ID baru yang konsisten
                            type = type,
                            amount = item.getLong("amount").coerceAtLeast(0L),
                            note = item.getString("note"),
                            timestampMillis = item.getLong("timestampMillis"),
                            receiptFileName = receiptName
                        )
                    )
                }

                // Ganti foto nota setelah validasi sukses
                repository.receiptStore.clearAllReceipts()
                var restoredPhotos = 0
                for (staged in stagedReceipts) {
                    val targetFile = repository.receiptStore.getReceiptFile(staged.name)
                    staged.copyTo(targetFile, overwrite = true)
                    restoredPhotos++
                }

                repository.restoreSnapshot(initialBalance, restoredList)

                RestoreResultSummary(
                    transactionCount = restoredList.size,
                    restoredPhotosCount = restoredPhotos,
                    initialBalance = initialBalance
                )
            } finally {
                tempRestoreDir.deleteRecursively()
            }
        }
    }
}
