package com.loaderapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "orders")
data class Order(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val address: String,
    val dateTime: Long, // timestamp в миллисекундах
    val cargoDescription: String,
    val pricePerHour: Double,
    val estimatedHours: Int = 1, // Ожидаемое количество часов
    val status: OrderStatus = OrderStatus.AVAILABLE,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null, // Время завершения
    val workerId: Long? = null, // ID грузчика, который взял заказ
    val dispatcherId: Long = 0, // ID диспетчера, который создал заказ
    val workerRating: Float? = null, // Оценка грузчика за этот заказ (1-5)
    val comment: String = "" // Комментарий к заказу
)

enum class OrderStatus {
    AVAILABLE,    // Доступен для взятия
    TAKEN,        // Взят грузчиком
    IN_PROGRESS,  // В процессе выполнения
    COMPLETED,    // Выполнен
    CANCELLED     // Отменен
}
