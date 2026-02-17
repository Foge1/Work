package com.loaderapp.ui.dispatcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.repository.AppRepository
import com.loaderapp.notification.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DispatcherViewModel(
    application: Application,
    private val repository: AppRepository,
    private val dispatcherId: Long
) : AndroidViewModel(application) {
    
    private val notificationHelper = NotificationHelper(application)
    
    private val _orders = MutableStateFlow<List<Order>>(emptyList())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _isSearchActive = MutableStateFlow(false)
    val isSearchActive: StateFlow<Boolean> = _isSearchActive.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    // Statistics
    val completedCount = repository.getDispatcherCompletedCount(dispatcherId)
    val activeCount = repository.getDispatcherActiveCount(dispatcherId)
    
    init {
        loadOrders()
        observeSearch()
    }
    
    private fun loadOrders() {
        viewModelScope.launch {
            try {
                repository.getOrdersByDispatcher(dispatcherId).collect { ordersList ->
                    _orders.value = ordersList
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки заказов: ${e.message}"
            }
        }
    }
    
    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        repository.getOrdersByDispatcher(dispatcherId)
                    } else {
                        repository.searchOrdersByDispatcher(dispatcherId, query)
                    }
                }
                .collect { ordersList ->
                    _orders.value = ordersList
                }
        }
    }
    
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun setSearchActive(active: Boolean) {
        _isSearchActive.value = active
        if (!active) _searchQuery.value = ""
    }
    
    fun createOrder(
        address: String,
        dateTime: Long,
        cargoDescription: String,
        pricePerHour: Double,
        estimatedHours: Int = 1,
        comment: String = ""
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val order = Order(
                    address = address,
                    dateTime = dateTime,
                    cargoDescription = cargoDescription,
                    pricePerHour = pricePerHour,
                    estimatedHours = estimatedHours,
                    comment = comment,
                    status = OrderStatus.AVAILABLE,
                    dispatcherId = dispatcherId
                )
                repository.createOrder(order)
                notificationHelper.sendNewOrderNotification(address, pricePerHour)
                _errorMessage.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка создания заказа: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun cancelOrder(order: Order) {
        viewModelScope.launch {
            try {
                repository.cancelOrder(order.id)
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка отмены заказа: ${e.message}"
            }
        }
    }
    
    fun clearError() {
        _errorMessage.value = null
    }
}
