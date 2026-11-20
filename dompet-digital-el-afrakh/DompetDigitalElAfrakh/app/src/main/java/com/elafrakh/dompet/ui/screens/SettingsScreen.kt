package com.elafrakh.dompet.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.elafrakh.dompet.BuildConfig
import com.elafrakh.dompet.backup.RestoreResultSummary
import com.elafrakh.dompet.ui.components.ConfirmActionDialog
import com.elafrakh.dompet.ui.components.ElAfrakhLogoBadge
import com.elafrakh.dompet.ui.theme.BluePrimary
import com.elafrakh.dompet.ui.theme.BluePrimaryDark
import com.elafrakh.dompet.ui.theme.ExpenseRed
import com.elafrakh.dompet.util.CurrencyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialBalance: Long,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onChangePin: suspend (oldPin: String, newPin: String) -> Boolean,
    onUpdateInitialBalance: (Long) -> Unit,
    onSuppressAutoLock: () -> Unit,
    onCreateBackup: suspend (Uri) -> Result<Int>,
    onRestoreBackup: suspend (Uri) -> Result<RestoreResultSummary>,
    onShowMessage: (String) -> Unit
) {
    val scope = rememberCoroutineScope()

    var showChangePinDialog by remember { mutableStateOf(false) }
    var showEditInitialBalanceDialog by remember { mutableStateOf(false) }
    var pendingInitialBalanceConfirm by remember { mutableStateOf<Long?>(null) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showAppInfoDialog by remember { mutableStateOf(false) }
    var isBusy by remember { mutableStateOf(false) }

    val backupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            isBusy = true
            scope.launch {
                val res = onCreateBackup(uri)
                isBusy = false
                if (res.isSuccess) {
                    onShowMessage("Backup berhasil (${res.getOrDefault(0)} transaksi & foto nota tersimpan).")
                } else {
                    onShowMessage("Gagal membuat backup: ${res.exceptionOrNull()?.message ?: "Kesalahan"}")
                }
            }
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingRestoreUri = uri
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan", color = Color.White) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // App Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ElAfrakhLogoBadge(
                        size = 56.dp,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconColor = BluePrimary
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Dompet Digital El Afrakh",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Versi ${BuildConfig.VERSION_NAME} • Mode Offline 100%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Security & Balance Settings
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsMenuRow(
                        icon = Icons.Rounded.Lock,
                        title = "Ubah PIN Keamanan",
                        subtitle = "Ganti 6 digit PIN masuk aplikasi",
                        onClick = { showChangePinDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    SettingsMenuRow(
                        icon = Icons.Rounded.AccountBalanceWallet,
                        title = "Ubah Saldo Awal",
                        subtitle = "Saat ini: ${CurrencyFormatter.formatRupiah(initialBalance)}",
                        onClick = { showEditInitialBalanceDialog = true }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleDarkMode(!isDarkTheme) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DarkMode,
                            contentDescription = "Mode Gelap",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Mode Gelap (Dark Mode)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = if (isDarkTheme) "Aktif — tampilan gelap" else "Nonaktif — tampilan terang",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isDarkTheme,
                            onCheckedChange = onToggleDarkMode
                        )
                    }
                }
            }

            // Backup & Restore & Info
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsMenuRow(
                        icon = Icons.Rounded.Backup,
                        title = "Backup Data (.zip)",
                        subtitle = "Simpan database transaksi, saldo awal & foto nota",
                        enabled = !isBusy,
                        onClick = {
                            onSuppressAutoLock()
                            backupLauncher.launch("Backup_ElAfrakh_${System.currentTimeMillis()}.zip")
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    SettingsMenuRow(
                        icon = Icons.Rounded.Restore,
                        title = "Restore Data (.zip)",
                        subtitle = "Pulihkan catatan & foto nota dari file backup",
                        enabled = !isBusy,
                        onClick = {
                            onSuppressAutoLock()
                            restoreLauncher.launch(arrayOf("application/zip", "application/octet-stream"))
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))
                    SettingsMenuRow(
                        icon = Icons.Rounded.Info,
                        title = "Informasi Aplikasi",
                        subtitle = "Tentang Dompet Digital El Afrakh & penyimpanan offline",
                        onClick = { showAppInfoDialog = true }
                    )
                }
            }
        }
    }

    // Change PIN Dialog
    if (showChangePinDialog) {
        var oldPin by remember { mutableStateOf("") }
        var newPin by remember { mutableStateOf("") }
        var confirmPin by remember { mutableStateOf("") }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showChangePinDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Ubah PIN 6 Digit") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = oldPin,
                        onValueChange = {
                            oldPin = it.filter { c -> c.isDigit() }.take(6)
                            errorMsg = null
                        },
                        label = { Text("PIN Lama") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = {
                            newPin = it.filter { c -> c.isDigit() }.take(6)
                            errorMsg = null
                        },
                        label = { Text("PIN Baru (6 digit)") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmPin,
                        onValueChange = {
                            confirmPin = it.filter { c -> c.isDigit() }.take(6)
                            errorMsg = null
                        },
                        label = { Text("Konfirmasi PIN Baru") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg != null) {
                        Text(errorMsg ?: "", color = ExpenseRed, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (oldPin.length != 6 || newPin.length != 6) {
                            errorMsg = "PIN lama dan baru harus 6 digit angka."
                            return@Button
                        }
                        if (newPin != confirmPin) {
                            errorMsg = "Konfirmasi PIN baru tidak cocok."
                            return@Button
                        }
                        scope.launch {
                            val ok = onChangePin(oldPin, newPin)
                            if (ok) {
                                showChangePinDialog = false
                                onShowMessage("PIN berhasil diperbarui.")
                            } else {
                                errorMsg = "PIN lama tidak sesuai."
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simpan PIN")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showChangePinDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("Batal")
                }
            }
        )
    }

    // Edit Initial Balance Dialog
    if (showEditInitialBalanceDialog) {
        var digits by remember { mutableStateOf(initialBalance.toString()) }
        var errorMsg by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showEditInitialBalanceDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Ubah Saldo Awal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Mengubah saldo awal akan otomatis menghitung ulang saldo akhir saat ini.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = digits,
                        onValueChange = {
                            digits = it.filter { c -> c.isDigit() }.take(12)
                            errorMsg = null
                        },
                        label = { Text("Nominal Saldo Awal (Rp)") },
                        prefix = { Text("Rp ") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (errorMsg != null) {
                        Text(errorMsg ?: "", color = ExpenseRed, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (digits.isBlank()) {
                            errorMsg = "Masukkan nominal saldo awal."
                            return@Button
                        }
                        val amount = CurrencyFormatter.parseRupiahDigits(digits)
                        showEditInitialBalanceDialog = false
                        pendingInitialBalanceConfirm = amount
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Lanjut")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showEditInitialBalanceDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    val confirmNewInitBalance = pendingInitialBalanceConfirm
    if (confirmNewInitBalance != null) {
        ConfirmActionDialog(
            title = "Konfirmasi Ubah Saldo Awal",
            message = "Ubah saldo awal menjadi ${CurrencyFormatter.formatRupiah(confirmNewInitBalance)}? Seluruh perhitungan saldo akhir akan diperbarui otomatis.",
            confirmText = "Ya, Ubah",
            onConfirm = {
                onUpdateInitialBalance(confirmNewInitBalance)
                pendingInitialBalanceConfirm = null
                onShowMessage("Saldo awal berhasil diperbarui.")
            },
            onDismiss = { pendingInitialBalanceConfirm = null }
        )
    }

    // Restore Overwrite Confirmation
    val restoreUri = pendingRestoreUri
    if (restoreUri != null) {
        ConfirmActionDialog(
            title = "Timpa Data dengan Backup?",
            message = "Proses restore akan mengganti seluruh catatan transaksi, saldo awal, dan foto nota saat ini dengan data dari file backup. Lanjutkan?",
            confirmText = "Ya, Restore Data",
            isDestructive = true,
            onConfirm = {
                pendingRestoreUri = null
                isBusy = true
                scope.launch {
                    val res = onRestoreBackup(restoreUri)
                    isBusy = false
                    if (res.isSuccess) {
                        val info = res.getOrThrow()
                        onShowMessage(
                            "Restore berhasil: ${info.transactionCount} transaksi & ${info.restoredPhotosCount} foto nota dipulihkan."
                        )
                    } else {
                        onShowMessage("Restore gagal: ${res.exceptionOrNull()?.message ?: "File tidak valid"}")
                    }
                }
            },
            onDismiss = { pendingRestoreUri = null }
        )
    }

    if (showAppInfoDialog) {
        AlertDialog(
            onDismissRequest = { showAppInfoDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Tentang Dompet Digital El Afrakh") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Dompet Digital El Afrakh adalah aplikasi pencatatan keuangan kantin yang berjalan 100% offline di perangkat Android Anda.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Versi Aplikasi: ${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• Penyimpanan: Room Database (SQLite) & Penyimpanan Internal HP",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Zona Waktu Pencatatan: Asia/Jakarta (WIB)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "• Catatan: Aplikasi ini bukan layanan pembayaran maupun transfer bank, dan tidak mengirimkan data ke internet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showAppInfoDialog = false }, shape = RoundedCornerShape(12.dp)) {
                    Text("Tutup")
                }
            }
        )
    }
}

@Composable
private fun SettingsMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
