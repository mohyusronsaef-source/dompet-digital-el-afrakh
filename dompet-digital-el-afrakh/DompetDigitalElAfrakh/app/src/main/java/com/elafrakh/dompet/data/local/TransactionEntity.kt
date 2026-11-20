package com.elafrakh.dompet.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["timestampMillis"]),
        Index(value = ["type"]),
        Index(value = ["receiptFileName"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: TransactionType,
    val amount: Long,
    val note: String,
    val timestampMillis: Long,
    val receiptFileName: String? = null
)
