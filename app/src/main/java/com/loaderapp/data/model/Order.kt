package com.loaderapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val address: String,
    val dateTime: Long,
    val cargoDescription: String,
    val pricePerHour: Double,
    val estimatedHours: Int = 1,
    val requiredWorkers: Int = 1,       // сколько грузчиков нужно
    val minWorkerRating: Float = 0f,    // минимальный рейтинг грузчика
    val status: OrderStatus = OrderStatus.AVAILABLE,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val workerId: Long? = null,         // первый взявший (для обратной совместимости)
    val dispatcherId: Long = 0,
    val workerRating: Float? = null,
    val comment: String = ""
)

enum class OrderStatus {
    AVAILABLE,
    TAKEN,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
}
