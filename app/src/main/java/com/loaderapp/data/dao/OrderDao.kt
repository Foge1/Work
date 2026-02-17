package com.loaderapp.data.dao

import androidx.room.*
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {
    
    @Query("SELECT * FROM orders ORDER BY dateTime DESC")
    fun getAllOrders(): Flow<List<Order>>
    
    @Query("SELECT * FROM orders WHERE status = :status ORDER BY dateTime DESC")
    fun getOrdersByStatus(status: OrderStatus): Flow<List<Order>>
    
    @Query("SELECT * FROM orders WHERE workerId = :workerId ORDER BY dateTime DESC")
    fun getOrdersByWorker(workerId: Long): Flow<List<Order>>
    
    @Query("SELECT * FROM orders WHERE dispatcherId = :dispatcherId ORDER BY dateTime DESC")
    fun getOrdersByDispatcher(dispatcherId: Long): Flow<List<Order>>
    
    @Query("SELECT * FROM orders WHERE id = :orderId")
    suspend fun getOrderById(orderId: Long): Order?
    
    @Query("""
        SELECT * FROM orders 
        WHERE (address LIKE '%' || :query || '%' OR cargoDescription LIKE '%' || :query || '%')
        AND (:status IS NULL OR status = :status)
        ORDER BY dateTime DESC
    """)
    fun searchOrders(query: String, status: OrderStatus?): Flow<List<Order>>
    
    @Query("""
        SELECT * FROM orders 
        WHERE dispatcherId = :dispatcherId
        AND (address LIKE '%' || :query || '%' OR cargoDescription LIKE '%' || :query || '%')
        ORDER BY dateTime DESC
    """)
    fun searchOrdersByDispatcher(dispatcherId: Long, query: String): Flow<List<Order>>
    
    @Query("SELECT COUNT(*) FROM orders WHERE workerId = :workerId AND status = 'COMPLETED'")
    fun getCompletedOrdersCount(workerId: Long): Flow<Int>
    
    @Query("SELECT SUM(pricePerHour * estimatedHours) FROM orders WHERE workerId = :workerId AND status = 'COMPLETED'")
    fun getTotalEarnings(workerId: Long): Flow<Double?>
    
    @Query("SELECT AVG(workerRating) FROM orders WHERE workerId = :workerId AND workerRating IS NOT NULL")
    fun getAverageRating(workerId: Long): Flow<Float?>
    
    @Query("SELECT COUNT(*) FROM orders WHERE dispatcherId = :dispatcherId AND status = 'COMPLETED'")
    fun getDispatcherCompletedCount(dispatcherId: Long): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM orders WHERE dispatcherId = :dispatcherId AND status = 'AVAILABLE'")
    fun getDispatcherActiveCount(dispatcherId: Long): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order): Long
    
    @Update
    suspend fun updateOrder(order: Order)
    
    @Delete
    suspend fun deleteOrder(order: Order)
    
    @Query("UPDATE orders SET status = :status, workerId = :workerId WHERE id = :orderId")
    suspend fun takeOrder(orderId: Long, workerId: Long, status: OrderStatus)
    
    @Query("UPDATE orders SET status = :status, completedAt = :completedAt WHERE id = :orderId")
    suspend fun completeOrder(orderId: Long, status: OrderStatus, completedAt: Long)
    
    @Query("UPDATE orders SET status = :status WHERE id = :orderId")
    suspend fun updateOrderStatus(orderId: Long, status: OrderStatus)
    
    @Query("UPDATE orders SET workerRating = :rating WHERE id = :orderId")
    suspend fun rateOrder(orderId: Long, rating: Float)
}
