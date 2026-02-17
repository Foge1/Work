package com.loaderapp.data.dao

import androidx.room.*
import com.loaderapp.data.model.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chat_messages WHERE orderId = :orderId ORDER BY sentAt ASC")
    fun getMessagesForOrder(orderId: Long): Flow<List<ChatMessage>>

    @Insert
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("SELECT COUNT(*) FROM chat_messages WHERE orderId = :orderId")
    fun getMessageCount(orderId: Long): Flow<Int>
}
