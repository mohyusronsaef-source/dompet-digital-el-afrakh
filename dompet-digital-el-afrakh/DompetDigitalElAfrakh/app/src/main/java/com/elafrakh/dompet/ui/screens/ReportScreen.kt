package com.elafrakh.dompet.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Balance
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elafrakh.dompet.data.local.TransactionEntity
import com.elafrakh.dompet.data.local.TransactionType
import com.elafrakh.dompet.data.storage.ReceiptImageStore
import com.elafrakh.dompet.export.ReportPeriod
import com.elafrakh.dompet.export.ReportSummary
import com.elafrakh.dompet.ui.components.EmptyTransactionState
import com.elafrakh.dompet.ui.components.GroupedFinanceBarChart
import com.elafrakh.dompet.ui.components.TransactionItemCard
import com.elafrakh.dompet.ui.theme.BluePrimary
import com.elafrakh.dompet.ui.theme.BluePrimaryDark
import com.elafrakh.dompet.ui.theme.BlueSoftDarkTint
import com.elafrakh.dompet.ui.theme.BlueSoftTint
import com.elafrakh.dompet.ui.theme.ExpenseRed
import com.elafrakh.dompet.ui.theme.ExpenseRedDarkSoft
import com.elafrakh.dompet.ui.theme.ExpenseRedSoft
import com.elafrakh.dompet.ui.theme.IncomeGreen
import com.elafrakh.dompet.ui.theme.IncomeGreenDarkSoft
import com.elafrakh.dompet.ui.theme.IncomeGreenSoft
import com.elafrakh.dompet.util.CurrencyFormatter
import com.elafrakh.dompet.util.DateTimeHelper
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    transactions: List<TransactionEntity>,
    isDarkTheme: Boolean,
    receiptStore: ReceiptImageStore,
    onSuppressAutoLock: () -> Unit,
    onBack: () -> Unit,
    onExportPdf: suspend (Uri, ReportSummary) -> Result<Unit>,
    onExportXlsx: suspend (Uri, ReportSummary) -> Result<Unit>,
    onTransactionClick: (TransactionEntity) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var selectedPeriod by remember { mutableStateOf(ReportPeriod.MONTHLY) }
    var periodOffset by remember { mutableIntStateOf(0) }
    var isExporting by remember { mutableStateOf(false) }

    val report = remember(transactions, selectedPeriod, periodOffset) {
        val range = DateTimeHelper.computePeriodData(selectedPeriod, periodOffset, transactions)
        val periodTx = transactions.filter { it.timestampMillis in range.startMillis until range.endMillis }
        var totalInc = 0L
        var totalExp = 0L
        for (tx in periodTx) {
            if (tx.type == TransactionType.INCOME) totalInc += tx.amount else totalExp += tx.amount
        }
        ReportSummary(
            period = selectedPeriod,
            periodTitle = range.title,
            totalIncome = totalInc,
            totalExpense = totalExp,
            netDifference = totalInc - totalExp,
            buckets = range.buckets,
            transactions = periodTx
        )
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null) {
            isExporting = true
            scope.launch {
                val res = onExportPdf(uri, report)
                isExporting = false
                if (res.isSuccess) {
                    onShowMessage("Laporan PDF berhasil disimpan.")
                } else {
                    onShowMessage("Gagal mengekspor PDF: ${res.exceptionOrNull()?.message ?: "Kesalahan tidak diketahui"}")
                }
            }
        }
    }

    val xlsxLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri ->
        if (uri != null) {
            isExporting = true
            scope.launch {
                val res = onExportXlsx(uri, report)
                isExporting = false
                if (res.isSuccess) {
                    onShowMessage("Laporan Excel (.xlsx) berhasil disimpan.")
                } else {
                    onShowMessage("Gagal mengekspor Excel: ${res.exceptionOrNull()?.message ?: "Kesalahan tidak diketahui"}")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Keuangan", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Kembali",
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
            TabRow(
                selectedTabIndex = selectedPeriod.ordinal,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                ReportPeriod.entries.forEach { period ->
                    Tab(
                        selected = selectedPeriod == period,
                        onClick = {
                            selectedPeriod = period
                            periodOffset = 0
                        },
                        text = {
                            Text(
                                text = period.label,
                                fontWeight = if (selectedPeriod == period) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Period Navigator
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { periodOffset -= 1 }) {
                                Icon(Icons.Rounded.ChevronLeft, contentDescription = "Periode sebelumnya")
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Laporan ${selectedPeriod.label}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = report.periodTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = { periodOffset += 1 },
                                enabled = periodOffset < 0
                            ) {
                                Icon(Icons.Rounded.ChevronRight, contentDescription = "Periode berikutnya")
                            }
                        }
                    }
                }

                // Summary Cards
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportSummaryRowCard(
                            label = "Total Pemasukan",
                            amount = report.totalIncome,
                            accentColor = IncomeGreen,
                            bgColor = if (isDarkTheme) IncomeGreenDarkSoft else IncomeGreenSoft,
                            icon = Icons.Rounded.ArrowUpward
                        )
                        ReportSummaryRowCard(
                            label = "Total Pengeluaran",
                            amount = report.totalExpense,
                            accentColor = ExpenseRed,
                            bgColor = if (isDarkTheme) ExpenseRedDarkSoft else ExpenseRedSoft,
                            icon = Icons.Rounded.ArrowDownward
                        )
                        ReportSummaryRowCard(
                            label = "Selisih",
                            amount = report.netDifference,
                            accentColor = if (report.netDifference >= 0) BluePrimary else ExpenseRed,
                            bgColor = if (isDarkTheme) BlueSoftDarkTint else BlueSoftTint,
                            icon = Icons.Rounded.Balance
                        )
                    }
                }

                // Chart
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Text(
                                text = "Grafik Pemasukan & Pengeluaran",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            GroupedFinanceBarChart(buckets = report.buckets)
                        }
                    }
                }

                // Export Buttons (PDF & Excel .xlsx)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSuppressAutoLock()
                                val safeName = "Laporan_${selectedPeriod.label}_${System.currentTimeMillis()}.pdf"
                                pdfLauncher.launch(safeName)
                            },
                            enabled = !isExporting,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, ExpenseRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExpenseRed)
                        ) {
                            Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export PDF")
                        }

                        Button(
                            onClick = {
                                onSuppressAutoLock()
                                val safeName = "Laporan_${selectedPeriod.label}_${System.currentTimeMillis()}.xlsx"
                                xlsxLauncher.launch(safeName)
                            },
                            enabled = !isExporting,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen)
                        ) {
                            Icon(Icons.Rounded.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export Excel")
                        }
                    }
                }

                // Period Transactions
                item {
                    Text(
                        text = "Ringkasan Transaksi (${report.transactions.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                if (report.transactions.isEmpty()) {
                    item {
                        EmptyTransactionState(
                            title = "Belum Ada Data di Periode Ini",
                            subtitle = "Catat pemasukan atau pengeluaran untuk melihat ringkasan laporan pada periode ${report.periodTitle}."
                        )
                    }
                } else {
                    items(items = report.transactions, key = { it.id }) { tx ->
                        TransactionItemCard(
                            transaction = tx,
                            isDarkTheme = isDarkTheme,
                            receiptStore = receiptStore,
                            onClick = { onTransactionClick(tx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportSummaryRowCard(
    label: String,
    amount: Long,
    accentColor: Color,
    bgColor: Color,
    icon: ImageVector
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyFormatter.formatRupiah(amount),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = accentColor
                )
            }
        }
    }
}
