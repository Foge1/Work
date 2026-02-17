package com.loaderapp.data.dao

import androidx.room.*
import com.loaderapp.data.model.OrderWorker
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderWorkerDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addWorkerToOrder(orderWorker: OrderWorker)

    @Query("SELECT COUNT(*) FROM order_workers WHERE orderId = :orderId")
    fun getWorkerCount(orderId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM order_workers WHERE orderId = :orderId")
    suspend fun getWorkerCountSync(orderId: Long): Int

    @Query("SELECT workerId FROM order_workers WHERE orderId = :orderId")
    suspend fun getWorkerIds(orderId: Long): List<Long>

    @Query("SELECT EXISTS(SELECT 1 FROM order_workers WHERE orderId = :orderId AND workerId = :workerId)")
    suspend fun hasWorker(orderId: Long, workerId: Long): Boolean

    @Query("SELECT orderId FROM order_workers WHERE workerId = :workerId")
    fun getOrderIdsByWorker(workerId: Long): Flow<List<Long>>
}
