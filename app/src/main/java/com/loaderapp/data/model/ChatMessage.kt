package com.loaderapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderId: Long,
    val senderId: Long,
    val senderName: String,
    val senderRole: UserRole,
    val text: String,
    val sentAt: Long = System.currentTimeMillis()
)
