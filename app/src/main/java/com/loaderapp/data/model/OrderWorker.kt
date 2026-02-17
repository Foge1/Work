package com.loaderapp.data.model

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "order_workers",
    primaryKeys = ["orderId", "workerId"]
)
data class OrderWorker(
    val orderId: Long,
    val workerId: Long,
    val takenAt: Long = System.currentTimeMillis()
)
