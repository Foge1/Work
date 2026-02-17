package com.loaderapp.data.repository

import com.loaderapp.data.dao.ChatDao
import com.loaderapp.data.dao.OrderDao
import com.loaderapp.data.dao.OrderWorkerDao
import com.loaderapp.data.dao.UserDao
import com.loaderapp.data.model.ChatMessage
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.OrderWorker
import com.loaderapp.data.model.User
import com.loaderapp.data.model.UserRole
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val orderDao: OrderDao,
    private val userDao: UserDao,
    private val orderWorkerDao: OrderWorkerDao,
    private val chatDao: ChatDao
) {

    // Order operations
    fun getAllOrders(): Flow<List<Order>> = orderDao.getAllOrders()
    fun getAvailableOrders(): Flow<List<Order>> = orderDao.getOrdersByStatus(OrderStatus.AVAILABLE)
    fun getOrdersByWorker(workerId: Long): Flow<List<Order>> = orderDao.getOrdersByWorker(workerId)
    fun getOrdersByDispatcher(dispatcherId: Long): Flow<List<Order>> = orderDao.getOrdersByDispatcher(dispatcherId)
    suspend fun getOrderById(orderId: Long): Order? = orderDao.getOrderById(orderId)
    fun searchOrders(query: String, status: OrderStatus? = null): Flow<List<Order>> = orderDao.searchOrders(query, status)
    fun searchOrdersByDispatcher(dispatcherId: Long, query: String): Flow<List<Order>> = orderDao.searchOrdersByDispatcher(dispatcherId, query)

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
        // Добавляем грузчика в таблицу order_workers
        orderWorkerDao.addWorkerToOrder(OrderWorker(orderId = orderId, workerId = workerId))
        // Обновляем workerId в заказе (первый взявший / для обратной совместимости)
        val order = orderDao.getOrderById(orderId)
        if (order != null && order.workerId == null) {
            orderDao.updateOrder(order.copy(workerId = workerId, status = OrderStatus.TAKEN))
        } else if (order != null) {
            // Уже есть грузчики — проверяем набрали ли нужное количество
            val count = orderWorkerDao.getWorkerCountSync(orderId)
            if (count >= order.requiredWorkers) {
                orderDao.updateOrder(order.copy(status = OrderStatus.TAKEN))
            }
        }
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

    // OrderWorker operations
    fun getWorkerCountForOrder(orderId: Long): Flow<Int> = orderWorkerDao.getWorkerCount(orderId)
    suspend fun getWorkerCountSync(orderId: Long): Int = orderWorkerDao.getWorkerCountSync(orderId)
    suspend fun hasWorkerTakenOrder(orderId: Long, workerId: Long): Boolean = orderWorkerDao.hasWorker(orderId, workerId)
    fun getOrderIdsByWorker(workerId: Long): Flow<List<Long>> = orderWorkerDao.getOrderIdsByWorker(workerId)

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

    // Chat operations
    fun getMessagesForOrder(orderId: Long) = chatDao.getMessagesForOrder(orderId)
    suspend fun sendMessage(message: ChatMessage): Long = chatDao.insertMessage(message)
    fun getMessageCount(orderId: Long) = chatDao.getMessageCount(orderId)
}
