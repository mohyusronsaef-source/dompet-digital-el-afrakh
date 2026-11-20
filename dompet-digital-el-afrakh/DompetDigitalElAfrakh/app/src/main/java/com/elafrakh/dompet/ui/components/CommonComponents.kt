package com.elafrakh.dompet.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.elafrakh.dompet.data.local.TransactionEntity
import com.elafrakh.dompet.data.local.TransactionType
import com.elafrakh.dompet.data.storage.ReceiptImageStore
import com.elafrakh.dompet.export.ChartBucket
import com.elafrakh.dompet.ui.theme.BluePrimary
import com.elafrakh.dompet.ui.theme.ExpenseRed
import com.elafrakh.dompet.ui.theme.ExpenseRedDarkSoft
import com.elafrakh.dompet.ui.theme.ExpenseRedSoft
import com.elafrakh.dompet.ui.theme.IncomeGreen
import com.elafrakh.dompet.ui.theme.IncomeGreenDarkSoft
import com.elafrakh.dompet.ui.theme.IncomeGreenSoft
import com.elafrakh.dompet.util.CurrencyFormatter
import com.elafrakh.dompet.util.DateTimeHelper

@Composable
fun ElAfrakhLogoBadge(
    size: Dp = 64.dp,
    containerColor: Color = Color.White,
    iconColor: Color = BluePrimary
) {
    Surface(
        modifier = Modifier.size(size),
        shape = RoundedCornerShape(size * 0.28f),
        color = containerColor,
        shadowElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Storefront,
                contentDescription = "Logo Dompet Digital El Afrakh",
                tint = iconColor,
                modifier = Modifier.size(size * 0.58f)
            )
        }
    }
}

@Composable
fun TransactionItemCard(
    transaction: TransactionEntity,
    isDarkTheme: Boolean,
    receiptStore: ReceiptImageStore?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIncome = transaction.type == TransactionType.INCOME
    val accentColor = if (isIncome) IncomeGreen else ExpenseRed
    val iconBg = when {
        isIncome && isDarkTheme -> IncomeGreenDarkSoft
        isIncome -> IncomeGreenSoft
        isDarkTheme -> ExpenseRedDarkSoft
        else -> ExpenseRedSoft
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Rounded.ArrowUpward else Icons.Rounded.ArrowDownward,
                    contentDescription = if (isIncome) "Pemasukan" else "Pengeluaran",
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.note,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = DateTimeHelper.formatDateTime(transaction.timestampMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!transaction.receiptFileName.isNullOrBlank() && receiptStore != null) {
                Spacer(modifier = Modifier.width(8.dp))
                ReceiptThumbnailSmall(
                    fileName = transaction.receiptFileName,
                    receiptStore = receiptStore
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = CurrencyFormatter.formatSignedRupiah(transaction.amount, isIncome),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
        }
    }
}

@Composable
fun ReceiptThumbnailSmall(
    fileName: String,
    receiptStore: ReceiptImageStore
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = fileName) {
        value = receiptStore.loadBitmap(fileName, maxSizePx = 160)
    }

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        val loaded = bitmap
        if (loaded != null) {
            Image(
                bitmap = loaded.asImageBitmap(),
                contentDescription = "Foto nota",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.ReceiptLong,
                contentDescription = "Nota terlampir",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun GroupedFinanceBarChart(
    buckets: List<ChartBucket>,
    modifier: Modifier = Modifier
) {
    val maxVal = buckets.maxOfOrNull { maxOf(it.income, it.expense) }?.coerceAtLeast(1L) ?: 1L
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(IncomeGreen)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Pemasukan",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.width(18.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(ExpenseRed)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Pengeluaran",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(152.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Y-Axis labels
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .height(132.dp)
                    .padding(bottom = 4.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = CurrencyFormatter.formatCompactAxis(maxVal),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    maxLines = 1
                )
                Text(
                    text = CurrencyFormatter.formatCompactAxis(maxVal / 2),
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    maxLines = 1
                )
                Text(
                    text = "0",
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(132.dp)
                ) {
                    val chartHeight = size.height - 6.dp.toPx()
                    // Horizontal guide lines
                    for (i in 0..2) {
                        val y = (chartHeight / 2f) * i + 4.dp.toPx()
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    if (buckets.isNotEmpty()) {
                        val groupWidth = size.width / buckets.size
                        val barWidth = (groupWidth * 0.28f).coerceIn(5.dp.toPx(), 16.dp.toPx())
                        val barGap = (groupWidth * 0.08f).coerceAtLeast(2.dp.toPx())
                        val corner = CornerRadius(4.dp.toPx(), 4.dp.toPx())

                        buckets.forEachIndexed { index, bucket ->
                            val centerX = groupWidth * index + groupWidth / 2f
                            val incomeRatio = bucket.income.toFloat() / maxVal.toFloat()
                            val expenseRatio = bucket.expense.toFloat() / maxVal.toFloat()

                            val incHeight = (incomeRatio * (chartHeight - 4.dp.toPx())).let {
                                if (bucket.income > 0L) maxOf(it, 4.dp.toPx()) else 0f
                            }
                            val expHeight = (expenseRatio * (chartHeight - 4.dp.toPx())).let {
                                if (bucket.expense > 0L) maxOf(it, 4.dp.toPx()) else 0f
                            }

                            val baseBottom = chartHeight + 4.dp.toPx()
                            if (incHeight > 0f) {
                                drawRoundRect(
                                    color = IncomeGreen,
                                    topLeft = Offset(centerX - barWidth - barGap / 2f, baseBottom - incHeight),
                                    size = Size(barWidth, incHeight),
                                    cornerRadius = corner
                                )
                            }
                            if (expHeight > 0f) {
                                drawRoundRect(
                                    color = ExpenseRed,
                                    topLeft = Offset(centerX + barGap / 2f, baseBottom - expHeight),
                                    size = Size(barWidth, expHeight),
                                    cornerRadius = corner
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    buckets.forEach { bucket ->
                        Text(
                            text = bucket.label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 10.sp,
                            color = labelColor,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTransactionState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmText: String,
    cancelText: String = "Batal",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) ExpenseRed else MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = cancelText)
            }
        }
    )
}

@Composable
fun ReceiptFullScreenDialog(
    fileName: String,
    receiptStore: ReceiptImageStore,
    onDismiss: () -> Unit
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = fileName) {
        value = receiptStore.loadBitmap(fileName, maxSizePx = 1400)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f)),
            color = Color.Black.copy(alpha = 0.92f)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                val loaded = bitmap
                if (loaded != null) {
                    Image(
                        bitmap = loaded.asImageBitmap(),
                        contentDescription = "Pratinjau foto nota",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                    )
                } else {
                    Text(
                        text = "Memuat foto nota...",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup foto nota",
                        tint = Color.White
                    )
                }
            }
        }
    }
}
