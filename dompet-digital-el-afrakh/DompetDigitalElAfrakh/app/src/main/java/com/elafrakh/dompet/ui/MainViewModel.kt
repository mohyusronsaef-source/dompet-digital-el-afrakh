package com.elafrakh.dompet.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elafrakh.dompet.backup.RestoreResultSummary
import com.elafrakh.dompet.backup.ZipBackupManager
import com.elafrakh.dompet.data.local.AppDatabase
import com.elafrakh.dompet.data.local.TransactionEntity
import com.elafrakh.dompet.data.prefs.AppPreferences
import com.elafrakh.dompet.data.repository.BalanceSummary
import com.elafrakh.dompet.data.repository.FinanceRepository
import com.elafrakh.dompet.data.storage.ReceiptImageStore
import com.elafrakh.dompet.export.ChartBucket
import com.elafrakh.dompet.export.OfflinePdfExporter
import com.elafrakh.dompet.export.OfflineXlsxExporter
import com.elafrakh.dompet.export.ReportSummary
import com.elafrakh.dompet.util.DateTimeHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val preferences = AppPreferences(application)
    val receiptStore = ReceiptImageStore(application)
    val repository = FinanceRepository(database.transactionDao(), preferences, receiptStore)

    val balanceSummary: StateFlow<BalanceSummary> = repository.balanceSummaryFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BalanceSummary())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.transactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val last7DaysBuckets: StateFlow<List<ChartBucket>> = repository.transactionsFlow
        .map { list -> DateTimeHelper.buildLast7DaysBuckets(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isDarkMode: StateFlow<Boolean> = preferences.isDarkModeFlow
    val hasPinSetup: StateFlow<Boolean> = preferences.hasPinSetupFlow
    val hasInitialBalanceSetup: StateFlow<Boolean> = preferences.hasInitialBalanceSetupFlow

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    @Volatile
    private var suppressAutoLockForExternalPicker = false

    fun suppressNextAutoLock() {
        suppressAutoLockForExternalPicker = true
    }

    fun clearAutoLockSuppression() {
        suppressAutoLockForExternalPicker = false
    }

    fun lockOnBackground() {
        if (suppressAutoLockForExternalPicker) return
        if (preferences.isPinConfigured()) {
            _isUnlocked.value = false
        }
    }

    fun markUnlocked() {
        _isUnlocked.value = true
    }

    suspend fun setupFirstPin(pin: String): String = withContext(Dispatchers.Default) {
        val recoveryCode = preferences.saveNewPin(pin)
        _isUnlocked.value = true
        recoveryCode
    }

    suspend fun verifyPin(pin: String): Boolean = withContext(Dispatchers.Default) {
        val valid = preferences.verifyPin(pin)
        if (valid) {
            _isUnlocked.value = true
        }
        valid
    }

    suspend fun resetPinWithRecoveryCode(code: String, newPin: String): Boolean =
        withContext(Dispatchers.Default) {
            if (!preferences.verifyRecoveryCode(code)) {
                return@withContext false
            }
            preferences.updatePinOnly(newPin)
            _isUnlocked.value = true
            true
        }

    suspend fun changePin(oldPin: String, newPin: String): Boolean =
        withContext(Dispatchers.Default) {
            if (!preferences.verifyPin(oldPin)) {
                return@withContext false
            }
            preferences.updatePinOnly(newPin)
            true
        }

    fun saveInitialBalance(amount: Long) {
        preferences.setInitialBalance(amount)
    }

    fun setDarkMode(enabled: Boolean) {
        preferences.setDarkMode(enabled)
    }

    suspend fun addIncome(amount: Long, note: String) {
        repository.addIncome(amount = amount, note = note)
    }

    suspend fun addExpense(amount: Long, note: String, receiptUri: Uri?) {
        repository.addExpense(amount = amount, note = note, receiptUri = receiptUri)
    }

    suspend fun updateTransaction(
        existing: TransactionEntity,
        newAmount: Long,
        newNote: String,
        newReceiptUri: Uri?,
        removeExistingReceipt: Boolean
    ) {
        repository.updateTransaction(
            existing = existing,
            newAmount = newAmount,
            newNote = newNote,
            newReceiptUri = newReceiptUri,
            removeExistingReceipt = removeExistingReceipt
        )
    }

    fun deleteTransaction(entity: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(entity)
        }
    }

    suspend fun exportPdf(context: Context, uri: Uri, report: ReportSummary): Result<Unit> =
        OfflinePdfExporter.exportToUri(context, uri, report)

    suspend fun exportXlsx(context: Context, uri: Uri, report: ReportSummary): Result<Unit> =
        OfflineXlsxExporter.exportToUri(context, uri, report)

    suspend fun createBackupZip(context: Context, uri: Uri): Result<Int> =
        ZipBackupManager.createBackupZip(context, uri, repository)

    suspend fun restoreBackupZip(context: Context, uri: Uri): Result<RestoreResultSummary> =
        ZipBackupManager.restoreFromBackupZip(context, uri, repository)

    suspend fun resetAllData() {
        repository.resetEverything()
        _isUnlocked.value = false
    }
}
