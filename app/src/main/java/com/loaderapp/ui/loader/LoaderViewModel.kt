package com.loaderapp.ui.loader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.repository.AppRepository
import com.loaderapp.notification.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoaderViewModel(
    application: Application,
    private val repository: AppRepository,
    private val loaderId: Long
) : AndroidViewModel(application) {
    
    private val notificationHelper = NotificationHelper(application)
    
    private val _availableOrders = MutableStateFlow<List<Order>>(emptyList())
    val availableOrders: StateFlow<List<Order>> = _availableOrders.asStateFlow()
    
    private val _myOrders = MutableStateFlow<List<Order>>(emptyList())
    val myOrders: StateFlow<List<Order>> = _myOrders.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Statistics flows
    val completedCount = repository.getCompletedOrdersCount(loaderId)
    val totalEarnings = repository.getTotalEarnings(loaderId)
    val averageRating = repository.getAverageRating(loaderId)
    
    init {
        loadAvailableOrders()
        loadMyOrders()
    }
    
    private fun loadAvailableOrders() {
        viewModelScope.launch {
            try {
                repository.getAvailableOrders().collect { ordersList ->
                    _availableOrders.value = ordersList
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки доступных заказов: ${e.message}"
            }
        }
    }
    
    private fun loadMyOrders() {
        viewModelScope.launch {
            try {
                repository.getOrdersByWorker(loaderId).collect { ordersList ->
                    _myOrders.value = ordersList.filter { 
                        it.status == OrderStatus.TAKEN || it.status == OrderStatus.COMPLETED 
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки моих заказов: ${e.message}"
            }
        }
    }
    
    fun takeOrder(order: Order) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val currentOrder = repository.getOrderById(order.id)
                if (currentOrder?.status == OrderStatus.AVAILABLE) {
                    repository.takeOrder(order.id, loaderId)
                    val loader = repository.getUserById(loaderId)
                    if (loader != null) {
                        notificationHelper.sendOrderTakenNotification(order.address, loader.name)
                    }
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Заказ уже занят другим грузчиком"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка взятия заказа: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun completeOrder(order: Order) {
        viewModelScope.launch {
            try {
                repository.completeOrder(order.id)
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка завершения заказа: ${e.message}"
            }
        }
    }
    
    fun rateOrder(orderId: Long, rating: Float) {
        viewModelScope.launch {
            try {
                repository.rateOrder(orderId, rating)
                // Пересчитываем средний рейтинг пользователя
                val avgRating = repository.getAverageRating(loaderId)
                // Обновляем рейтинг пользователя в БД
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка выставления оценки: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
