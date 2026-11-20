package com.elafrakh.dompet.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elafrakh.dompet.ui.components.ConfirmActionDialog
import com.elafrakh.dompet.ui.theme.BluePrimary
import com.elafrakh.dompet.ui.theme.BluePrimaryDark
import com.elafrakh.dompet.ui.theme.ExpenseRed
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class PinStep {
    CREATE_NEW,
    CONFIRM_NEW,
    LOGIN
}

@Composable
fun PinScreen(
    isPinConfigured: Boolean,
    onSetupPin: suspend (String) -> String,
    onVerifyPin: suspend (String) -> Boolean,
    onResetPinWithRecovery: suspend (code: String, newPin: String) -> Boolean,
    onResetAppData: suspend () -> Unit,
    onPinSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val clipboard = LocalClipboardManager.current

    var step by remember(isPinConfigured) {
        mutableStateOf(if (isPinConfigured) PinStep.LOGIN else PinStep.CREATE_NEW)
    }
    var enteredPin by remember { mutableStateOf("") }
    var firstPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var generatedRecoveryCode by remember { mutableStateOf<String?>(null) }
    var copiedRecovery by remember { mutableStateOf(false) }

    var showForgotPinDialog by remember { mutableStateOf(false) }
    var showConfirmWipeDialog by remember { mutableStateOf(false) }

    val shakeOffset = remember { Animatable(0f) }

    suspend fun triggerShake() {
        val offsets = listOf(-18f, 18f, -12f, 12f, -6f, 0f)
        for (target in offsets) {
            shakeOffset.animateTo(target, animationSpec = tween(40))
        }
    }

    fun handleSubmitPin(pinToProcess: String) {
        if (pinToProcess.length != 6 || isProcessing) return
        when (step) {
            PinStep.CREATE_NEW -> {
                firstPin = pinToProcess
                enteredPin = ""
                errorMessage = null
                step = PinStep.CONFIRM_NEW
            }
            PinStep.CONFIRM_NEW -> {
                if (pinToProcess != firstPin) {
                    errorMessage = "PIN konfirmasi tidak cocok. Silakan ulangi."
                    enteredPin = ""
                    firstPin = ""
                    step = PinStep.CREATE_NEW
                    scope.launch { triggerShake() }
                } else {
                    isProcessing = true
                    scope.launch {
                        val recovery = onSetupPin(pinToProcess)
                        isProcessing = false
                        generatedRecoveryCode = recovery
                    }
                }
            }
            PinStep.LOGIN -> {
                isProcessing = true
                scope.launch {
                    val ok = onVerifyPin(pinToProcess)
                    isProcessing = false
                    if (ok) {
                        enteredPin = ""
                        errorMessage = null
                        onPinSuccess()
                    } else {
                        enteredPin = ""
                        errorMessage = "PIN salah. Periksa kembali 6 digit PIN Anda."
                        triggerShake()
                    }
                }
            }
        }
    }

    fun onDigitPressed(digit: String) {
        if (isProcessing || enteredPin.length >= 6) return
        errorMessage = null
        val updated = enteredPin + digit
        enteredPin = updated
        if (updated.length == 6) {
            handleSubmitPin(updated)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top Blue Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BluePrimaryDark,
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                )
                .padding(top = 48.dp, bottom = 36.dp, start = 24.dp, end = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Keamanan PIN",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (step) {
                        PinStep.CREATE_NEW -> "Buat PIN Baru"
                        PinStep.CONFIRM_NEW -> "Konfirmasi PIN"
                        PinStep.LOGIN -> "Masukkan PIN"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (step) {
                        PinStep.CREATE_NEW -> "Atur 6 digit PIN untuk mengamankan catatan kantin"
                        PinStep.CONFIRM_NEW -> "Masukkan ulang 6 digit PIN yang sama"
                        PinStep.LOGIN -> "Masukkan 6 digit PIN Anda untuk melanjutkan"
                    },
                    color = Color(0xFFDBEAFE),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // PIN dots + Error message
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Surface(
                    modifier = Modifier.offset { IntOffset(shakeOffset.value.roundToInt(), 0) },
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(6) { index ->
                            val filled = index < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (filled) BluePrimary else Color.Transparent
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (filled) BluePrimary else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = errorMessage ?: " ",
                    color = ExpenseRed,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            // Keypad 1-9, Backspace, 0, Check
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9")
                )
                rows.forEach { rowDigits ->
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        rowDigits.forEach { digit ->
                            KeypadDigitButton(
                                label = digit,
                                onClick = { onDigitPressed(digit) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    // Backspace
                    Surface(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable(enabled = enteredPin.isNotEmpty() && !isProcessing) {
                                enteredPin = enteredPin.dropLast(1)
                                errorMessage = null
                            },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Backspace,
                                contentDescription = "Hapus angka",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    KeypadDigitButton(
                        label = "0",
                        onClick = { onDigitPressed("0") }
                    )

                    // Confirm check
                    Surface(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable(enabled = enteredPin.length == 6 && !isProcessing) {
                                handleSubmitPin(enteredPin)
                            },
                        shape = CircleShape,
                        color = if (enteredPin.length == 6) BluePrimary else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Konfirmasi PIN",
                                tint = if (enteredPin.length == 6) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Bottom action (Forgot PIN or Cancel confirm)
            Box(
                modifier = Modifier.padding(bottom = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when (step) {
                    PinStep.LOGIN -> {
                        TextButton(onClick = { showForgotPinDialog = true }) {
                            Text(
                                text = "Lupa PIN?",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                    PinStep.CONFIRM_NEW -> {
                        TextButton(
                            onClick = {
                                firstPin = ""
                                enteredPin = ""
                                errorMessage = null
                                step = PinStep.CREATE_NEW
                            }
                        ) {
                            Text("Ulangi Buat PIN")
                        }
                    }
                    PinStep.CREATE_NEW -> {
                        Text(
                            text = "PIN disimpan aman secara lokal (offline)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Recovery code modal shown right after first-time PIN setup
    val recovery = generatedRecoveryCode
    if (recovery != null) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(20.dp),
            title = { Text("Simpan Kode Pemulihan PIN") },
            text = {
                Column {
                    Text(
                        text = "Karena aplikasi berjalan 100% offline tanpa akun email, simpan kode pemulihan ini untuk mereset PIN jika suatu saat lupa:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = recovery,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    letterSpacing = 2.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            TextButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(recovery))
                                    copiedRecovery = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Salin kode",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (copiedRecovery) "Disalin" else "Salin")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        generatedRecoveryCode = null
                        onPinSuccess()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Saya Sudah Simpan")
                }
            }
        )
    }

    // Offline Forgot PIN dialog
    if (showForgotPinDialog) {
        var recoveryInput by remember { mutableStateOf("") }
        var newPinInput by remember { mutableStateOf("") }
        var recoveryError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = { Text("Pemulihan PIN Offline") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Masukkan kode pemulihan lokal 8 karakter dan PIN baru 6 digit.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = recoveryInput,
                        onValueChange = {
                            recoveryInput = it.uppercase().take(10)
                            recoveryError = null
                        },
                        label = { Text("Kode Pemulihan (contoh: ABCD-1234)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = { v ->
                            newPinInput = v.filter { it.isDigit() }.take(6)
                            recoveryError = null
                        },
                        label = { Text("PIN Baru (6 digit)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (recoveryError != null) {
                        Text(
                            text = recoveryError ?: "",
                            color = ExpenseRed,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    TextButton(
                        onClick = {
                            showForgotPinDialog = false
                            showConfirmWipeDialog = true
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = ExpenseRed)
                    ) {
                        Text("Lupa kode pemulihan? Reset Aplikasi")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (recoveryInput.isBlank() || newPinInput.length != 6) {
                            recoveryError = "Isi kode pemulihan dan PIN baru 6 digit."
                            return@Button
                        }
                        scope.launch {
                            val ok = onResetPinWithRecovery(recoveryInput, newPinInput)
                            if (ok) {
                                showForgotPinDialog = false
                                onPinSuccess()
                            } else {
                                recoveryError = "Kode pemulihan tidak sesuai."
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Atur Ulang PIN")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showForgotPinDialog = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Batal")
                }
            }
        )
    }

    if (showConfirmWipeDialog) {
        ConfirmActionDialog(
            title = "Reset Seluruh Data Aplikasi?",
            message = "Tindakan ini akan menghapus PIN, saldo awal, semua catatan transaksi, dan seluruh foto nota secara permanen. Lanjutkan?",
            confirmText = "Ya, Reset Semua",
            isDestructive = true,
            onConfirm = {
                showConfirmWipeDialog = false
                scope.launch {
                    onResetAppData()
                    enteredPin = ""
                    firstPin = ""
                    step = PinStep.CREATE_NEW
                }
            },
            onDismiss = { showConfirmWipeDialog = false }
        )
    }
}

@Composable
private fun KeypadDigitButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
