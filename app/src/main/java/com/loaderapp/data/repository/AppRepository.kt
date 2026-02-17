package com.loaderapp.data.repository

import com.loaderapp.data.dao.OrderDao
import com.loaderapp.data.dao.UserDao
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.User
import com.loaderapp.data.model.UserRole
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val orderDao: OrderDao,
    private val userDao: UserDao
) {
    
    // Order operations
    fun getAllOrders(): Flow<List<Order>> = orderDao.getAllOrders()
    
    fun getAvailableOrders(): Flow<List<Order>> = orderDao.getOrdersByStatus(OrderStatus.AVAILABLE)
    
    fun getOrdersByWorker(workerId: Long): Flow<List<Order>> = orderDao.getOrdersByWorker(workerId)
    
    fun getOrdersByDispatcher(dispatcherId: Long): Flow<List<Order>> = orderDao.getOrdersByDispatcher(dispatcherId)
    
    suspend fun getOrderById(orderId: Long): Order? = orderDao.getOrderById(orderId)
    
    fun searchOrders(query: String, status: OrderStatus? = null): Flow<List<Order>> =
        orderDao.searchOrders(query, status)
    
    fun searchOrdersByDispatcher(dispatcherId: Long, query: String): Flow<List<Order>> =
        orderDao.searchOrdersByDispatcher(dispatcherId, query)
    
    // Statistics
    fun getCompletedOrdersCount(workerId: Long): Flow<Int> = orderDao.getCompletedOrdersCount(workerId)
    fun getTotalEarnings(workerId: Long): Flow<Double?> = orderDao.getTotalEarnings(workerId)
    fun getAverageRating(workerId: Long): Flow<Float?> = orderDao.getAverageRating(workerId)
    fun getDispatcherCompletedCount(dispatcherId: Long): Flow<Int> = orderDao.getDispatcherCompletedCount(dispatcherId)
    fun getDispatcherActiveCount(dispatcherId: Long): Flow<Int> = orderDao.getDispatcherActiveCount(dispatcherId)
    
    suspend fun createOrder(order: Order): Long = orderDao.insertOrder(order)
    
    suspend fun updateOrder(order: Order) = orderDao.updateOrder(order)
    
    suspend fun deleteOrder(order: Order) = orderDao.deleteOrder(order)
    
    suspend fun takeOrder(orderId: Long, workerId: Long) {
        orderDao.takeOrder(orderId, workerId, OrderStatus.TAKEN)
    }
    
    suspend fun completeOrder(orderId: Long) {
        orderDao.completeOrder(orderId, OrderStatus.COMPLETED, System.currentTimeMillis())
    }
    
    suspend fun cancelOrder(orderId: Long) {
        orderDao.updateOrderStatus(orderId, OrderStatus.CANCELLED)
    }
    
    suspend fun rateOrder(orderId: Long, rating: Float) {
        orderDao.rateOrder(orderId, rating)
    }
    
    // User operations
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()
    
    fun getLoaders(): Flow<List<User>> = userDao.getUsersByRole(UserRole.LOADER)
    
    fun getDispatchers(): Flow<List<User>> = userDao.getUsersByRole(UserRole.DISPATCHER)
    
    suspend fun getUserById(userId: Long): User? = userDao.getUserById(userId)
    
    fun getUserByIdFlow(userId: Long): Flow<User?> = userDao.getUserByIdFlow(userId)
    
    suspend fun createUser(user: User): Long = userDao.insertUser(user)
    
    suspend fun updateUser(user: User) = userDao.updateUser(user)
    
    suspend fun deleteUser(user: User) = userDao.deleteUser(user)
    
    suspend fun updateUserRating(userId: Long, rating: Double) = userDao.updateUserRating(userId, rating)
}
