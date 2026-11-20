package com.elafrakh.dompet.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elafrakh.dompet.data.local.TransactionEntity
import com.elafrakh.dompet.data.local.TransactionType
import com.elafrakh.dompet.data.storage.ReceiptImageStore
import com.elafrakh.dompet.ui.components.ConfirmActionDialog
import com.elafrakh.dompet.ui.components.EmptyTransactionState
import com.elafrakh.dompet.ui.components.ReceiptFullScreenDialog
import com.elafrakh.dompet.ui.components.TransactionItemCard
import com.elafrakh.dompet.ui.theme.BluePrimaryDark
import com.elafrakh.dompet.ui.theme.ExpenseRed
import com.elafrakh.dompet.ui.theme.IncomeGreen
import com.elafrakh.dompet.util.CurrencyFormatter
import com.elafrakh.dompet.util.DateTimeHelper
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

private enum class HistoryFilter(val label: String) {
    ALL("Semua"),
    INCOME("Pemasukan"),
    EXPENSE("Pengeluaran")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    transactions: List<TransactionEntity>,
    isDarkTheme: Boolean,
    receiptStore: ReceiptImageStore,
    initialSelectedTransaction: TransactionEntity?,
    onClearInitialSelected: () -> Unit,
    onSuppressAutoLock: () -> Unit,
    onBack: () -> Unit,
    onUpdateTransaction: suspend (
        existing: TransactionEntity,
        newAmount: Long,
        newNote: String,
        newReceiptUri: Uri?,
        removeExistingReceipt: Boolean
    ) -> Unit,
    onDeleteTransaction: (TransactionEntity) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    var detailTransaction by remember(initialSelectedTransaction) {
        mutableStateOf(initialSelectedTransaction)
    }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var deletingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var fullScreenReceiptFile by remember { mutableStateOf<String?>(null) }

    val filteredList = remember(transactions, searchQuery, selectedFilter, selectedDate) {
        transactions.filter { tx ->
            val typeOk = when (selectedFilter) {
                HistoryFilter.ALL -> true
                HistoryFilter.INCOME -> tx.type == TransactionType.INCOME
                HistoryFilter.EXPENSE -> tx.type == TransactionType.EXPENSE
            }
            val queryOk = searchQuery.isBlank() ||
                tx.note.contains(searchQuery.trim(), ignoreCase = true)
            val dateOk = selectedDate == null ||
                DateTimeHelper.toLocalDate(tx.timestampMillis) == selectedDate
            typeOk && queryOk && dateOk
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riwayat Transaksi", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(
                            imageVector = Icons.Rounded.CalendarMonth,
                            contentDescription = "Filter tanggal",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimaryDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari transaksi berdasarkan keterangan...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Rounded.Search, contentDescription = null)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(imageVector = Icons.Rounded.Close, contentDescription = "Bersihkan")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HistoryFilter.entries.forEach { filter ->
                        val selected = selectedFilter == filter
                        FilterChip(
                            selected = selected,
                            onClick = { selectedFilter = filter },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }

                val activeDate = selectedDate
                if (activeDate != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tanggal: ${DateTimeHelper.formatDate(DateTimeHelper.dayRangeMillis(activeDate).first)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Hapus filter tanggal",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { selectedDate = null }
                            )
                        }
                    }
                }
            }

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    EmptyTransactionState(
                        title = "Tidak Ada Transaksi Ditemukan",
                        subtitle = if (transactions.isEmpty()) {
                            "Riwayat masih kosong. Tambahkan pemasukan atau pengeluaran terlebih dahulu."
                        } else {
                            "Coba ubah kata kunci pencarian, jenis transaksi, atau filter tanggal."
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(
                        items = filteredList,
                        key = { it.id }
                    ) { tx ->
                        TransactionItemCard(
                            transaction = tx,
                            isDarkTheme = isDarkTheme,
                            receiptStore = receiptStore,
                            onClick = { detailTransaction = tx }
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Pilih")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        selectedDate = null
                        showDatePicker = false
                    }
                ) {
                    Text("Reset")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Detail Transaction Dialog
    val detailTx = detailTransaction
    if (detailTx != null) {
        val isIncome = detailTx.type == TransactionType.INCOME
        val receiptFile = detailTx.receiptFileName
        val detailBitmap by produceState<Bitmap?>(initialValue = null, key1 = receiptFile) {
            value = if (receiptFile != null) receiptStore.loadBitmap(receiptFile, maxSizePx = 900) else null
        }

        AlertDialog(
            onDismissRequest = {
                detailTransaction = null
                onClearInitialSelected()
            },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = if (isIncome) "Detail Pemasukan" else "Detail Pengeluaran",
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = CurrencyFormatter.formatSignedRupiah(detailTx.amount, isIncome),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = if (isIncome) IncomeGreen else ExpenseRed
                    )
                    Text(
                        text = "Keterangan: ${detailTx.note}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Waktu: ${DateTimeHelper.formatDateTime(detailTx.timestampMillis)} WIB",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!receiptFile.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Foto Nota (ketuk untuk perbesar)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        val loaded = detailBitmap
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { fullScreenReceiptFile = receiptFile },
                            contentAlignment = Alignment.Center
                        ) {
                            if (loaded != null) {
                                Image(
                                    bitmap = loaded.asImageBitmap(),
                                    contentDescription = "Foto nota transaksi",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text("Memuat foto nota...")
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                editingTransaction = detailTx
                                detailTransaction = null
                                onClearInitialSelected()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit")
                        }

                        Button(
                            onClick = {
                                deletingTransaction = detailTx
                                detailTransaction = null
                                onClearInitialSelected()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Hapus")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        detailTransaction = null
                        onClearInitialSelected()
                    }
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    // Edit Transaction Dialog
    val editTx = editingTransaction
    if (editTx != null) {
        EditTransactionDialog(
            transaction = editTx,
            receiptStore = receiptStore,
            onSuppressAutoLock = onSuppressAutoLock,
            onDismiss = { editingTransaction = null },
            onSave = { newAmount, newNote, newReceiptUri, removeExisting ->
                scope.launch {
                    onUpdateTransaction(editTx, newAmount, newNote, newReceiptUri, removeExisting)
                    editingTransaction = null
                    onShowMessage("Transaksi diperbarui dan saldo dihitung ulang.")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    val deleteTx = deletingTransaction
    if (deleteTx != null) {
        ConfirmActionDialog(
            title = "Hapus Transaksi?",
            message = "Transaksi \"${deleteTx.note}\" (${CurrencyFormatter.formatRupiah(deleteTx.amount)}) akan dihapus dan saldo akan dihitung ulang otomatis.",
            confirmText = "Hapus",
            isDestructive = true,
            onConfirm = {
                onDeleteTransaction(deleteTx)
                deletingTransaction = null
                onShowMessage("Transaksi berhasil dihapus.")
            },
            onDismiss = { deletingTransaction = null }
        )
    }

    val fullFile = fullScreenReceiptFile
    if (fullFile != null) {
        ReceiptFullScreenDialog(
            fileName = fullFile,
            receiptStore = receiptStore,
            onDismiss = { fullScreenReceiptFile = null }
        )
    }
}

@Composable
private fun EditTransactionDialog(
    transaction: TransactionEntity,
    receiptStore: ReceiptImageStore,
    onSuppressAutoLock: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (
        newAmount: Long,
        newNote: String,
        newReceiptUri: Uri?,
        removeExistingReceipt: Boolean
    ) -> Unit
) {
    val isExpense = transaction.type == TransactionType.EXPENSE
    var amountDigits by remember { mutableStateOf(transaction.amount.toString()) }
    var note by remember { mutableStateOf(transaction.note) }
    var newReceiptUri by remember { mutableStateOf<Uri?>(null) }
    var removeExistingReceipt by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { ok ->
        if (ok) {
            newReceiptUri = pendingCameraUri
            removeExistingReceipt = false
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            newReceiptUri = uri
            removeExistingReceipt = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(if (isExpense) "Edit Pengeluaran" else "Edit Pemasukan")
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountDigits,
                    onValueChange = { input ->
                        amountDigits = input.filter { it.isDigit() }.take(12)
                        errorMessage = null
                    },
                    label = { Text("Nominal (Rp)") },
                    prefix = { Text("Rp ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = {
                        note = it.take(160)
                        errorMessage = null
                    },
                    label = { Text("Keterangan") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    minLines = 2,
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                if (isExpense) {
                    Text(
                        text = "Foto Nota",
                        style = MaterialTheme.typography.labelLarge
                    )
                    val statusText = when {
                        newReceiptUri != null -> "Foto nota baru dipilih"
                        !transaction.receiptFileName.isNullOrBlank() && !removeExistingReceipt -> "Foto nota tersimpan"
                        else -> "Tidak ada foto nota"
                    }
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                onSuppressAutoLock()
                                val (uri, _) = receiptStore.createTempCameraUri()
                                pendingCameraUri = uri
                                cameraLauncher.launch(uri)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Kamera")
                        }
                        OutlinedButton(
                            onClick = {
                                onSuppressAutoLock()
                                galleryLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Rounded.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Galeri")
                        }
                    }
                    if (newReceiptUri != null || (!transaction.receiptFileName.isNullOrBlank() && !removeExistingReceipt)) {
                        TextButton(
                            onClick = {
                                newReceiptUri = null
                                removeExistingReceipt = true
                            },
                            colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                        ) {
                            Text("Hapus Foto Nota")
                        }
                    }
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = ExpenseRed,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = CurrencyFormatter.parseRupiahDigits(amountDigits)
                    if (amount <= 0L) {
                        errorMessage = "Nominal harus lebih dari Rp0."
                        return@Button
                    }
                    if (note.isBlank()) {
                        errorMessage = "Keterangan tidak boleh kosong."
                        return@Button
                    }
                    onSave(amount, note.trim(), newReceiptUri, removeExistingReceipt)
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simpan")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, shape = RoundedCornerShape(12.dp)) {
                Text("Batal")
            }
        }
    )
}
