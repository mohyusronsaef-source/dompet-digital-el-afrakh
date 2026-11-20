package com.elafrakh.dompet.data.repository

import android.net.Uri
import com.elafrakh.dompet.data.local.TransactionDao
import com.elafrakh.dompet.data.local.TransactionEntity
import com.elafrakh.dompet.data.local.TransactionType
import com.elafrakh.dompet.data.prefs.AppPreferences
import com.elafrakh.dompet.data.storage.ReceiptImageStore
import com.elafrakh.dompet.util.DateTimeHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class BalanceSummary(
    val initialBalance: Long = 0L,
    val totalIncome: Long = 0L,
    val totalExpense: Long = 0L,
    val currentBalance: Long = 0L,
    val todayIncome: Long = 0L,
    val todayExpense: Long = 0L,
    val todayDifference: Long = 0L
)

class FinanceRepository(
    private val dao: TransactionDao,
    val preferences: AppPreferences,
    val receiptStore: ReceiptImageStore
) {

    val transactionsFlow: Flow<List<TransactionEntity>> = dao.observeAll()

    val balanceSummaryFlow: Flow<BalanceSummary> = combine(
        preferences.initialBalanceFlow,
        dao.observeTotalByType(TransactionType.INCOME),
        dao.observeTotalByType(TransactionType.EXPENSE),
        dao.observeAll()
    ) { initial, totalIncome, totalExpense, allTransactions ->
        val (todayStart, todayEnd) = DateTimeHelper.todayRangeMillis()
        var todayIncome = 0L
        var todayExpense = 0L
        for (tx in allTransactions) {
            if (tx.timestampMillis in todayStart until todayEnd) {
                when (tx.type) {
                    TransactionType.INCOME -> todayIncome += tx.amount
                    TransactionType.EXPENSE -> todayExpense += tx.amount
                }
            }
        }
        BalanceSummary(
            initialBalance = initial,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            currentBalance = computeFinalBalance(initial, totalIncome, totalExpense),
            todayIncome = todayIncome,
            todayExpense = totalExpenseToday(todayExpense),
            todayDifference = todayIncome - todayExpense
        )
    }

    private fun totalExpenseToday(value: Long): Long = value

    suspend fun getCurrentBalanceSnapshot(): Long {
        val initial = preferences.getInitialBalanceSnapshot()
        val income = dao.getTotalByTypeSnapshot(TransactionType.INCOME)
        val expense = dao.getTotalByTypeSnapshot(TransactionType.EXPENSE)
        return computeFinalBalance(initial, income, expense)
    }

    suspend fun addIncome(amount: Long, note: String, timestampMillis: Long = System.currentTimeMillis()): Long {
        val entity = TransactionEntity(
            type = TransactionType.INCOME,
            amount = amount,
            note = note.trim(),
            timestampMillis = timestampMillis,
            receiptFileName = null
        )
        return dao.insert(entity)
    }

    suspend fun addExpense(
        amount: Long,
        note: String,
        receiptUri: Uri?,
        timestampMillis: Long = System.currentTimeMillis()
    ): Long {
        val savedFileName = if (receiptUri != null) {
            receiptStore.saveCompressedReceiptFromUri(receiptUri)
        } else {
            null
        }
        val entity = TransactionEntity(
            type = TransactionType.EXPENSE,
            amount = amount,
            note = note.trim(),
            timestampMillis = timestampMillis,
            receiptFileName = savedFileName
        )
        return dao.insert(entity)
    }

    suspend fun updateTransaction(
        existing: TransactionEntity,
        newAmount: Long,
        newNote: String,
        newReceiptUri: Uri?,
        removeExistingReceipt: Boolean
    ) {
        val oldFileName = existing.receiptFileName
        val finalFileName: String? = when {
            existing.type == TransactionType.INCOME -> null
            newReceiptUri != null -> receiptStore.saveCompressedReceiptFromUri(newReceiptUri)
            removeExistingReceipt -> null
            else -> oldFileName
        }

        val updated = existing.copy(
            amount = newAmount,
            note = newNote.trim(),
            receiptFileName = finalFileName
        )
        dao.update(updated)

        if (!oldFileName.isNullOrBlank() && oldFileName != finalFileName) {
            deleteReceiptIfUnreferenced(oldFileName)
        }
    }

    suspend fun deleteTransaction(entity: TransactionEntity) {
        dao.deleteById(entity.id)
        val fileName = entity.receiptFileName
        if (!fileName.isNullOrBlank()) {
            deleteReceiptIfUnreferenced(fileName)
        }
    }

    suspend fun deleteReceiptIfUnreferenced(fileName: String) {
        val refs = dao.countReceiptReferences(fileName)
        if (refs == 0) {
            receiptStore.deleteReceiptFile(fileName)
        }
    }

    suspend fun getAllTransactionsSnapshot(): List<TransactionEntity> = dao.getAllSnapshot()

    suspend fun restoreSnapshot(initialBalance: Long, transactions: List<TransactionEntity>) {
        dao.replaceAll(transactions)
        preferences.setInitialBalance(initialBalance)
    }

    suspend fun resetEverything() {
        dao.deleteAll()
        receiptStore.clearAllReceipts()
        preferences.resetAllDataPrefs()
    }

    companion object {
        fun computeFinalBalance(
            initialBalance: Long,
            totalIncome: Long,
            totalExpense: Long
        ): Long = initialBalance + totalIncome - totalExpense
    }
}
