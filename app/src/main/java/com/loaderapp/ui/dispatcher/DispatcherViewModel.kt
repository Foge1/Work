package com.loaderapp.ui.dispatcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.User
import com.loaderapp.data.repository.AppRepository
import com.loaderapp.notification.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
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

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val completedCount = repository.getDispatcherCompletedCount(dispatcherId)
    val activeCount = repository.getDispatcherActiveCount(dispatcherId)

    private val _workerCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val workerCounts: StateFlow<Map<Long, Int>> = _workerCounts.asStateFlow()

    init {
        loadOrders()
        observeSearch()
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            repository.getUserByIdFlow(dispatcherId).collect { _currentUser.value = it }
        }
    }

    private fun loadOrders() {
        viewModelScope.launch {
            try {
                repository.getOrdersByDispatcher(dispatcherId).collect { orders ->
                    _orders.value = orders
                    val counts = mutableMapOf<Long, Int>()
                    orders.forEach { counts[it.id] = repository.getWorkerCountSync(it.id) }
                    _workerCounts.value = counts
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки заказов: ${e.message}"
            }
        }
    }

    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery.debounce(300).flatMapLatest { query ->
                if (query.isBlank()) repository.getOrdersByDispatcher(dispatcherId)
                else repository.searchOrdersByDispatcher(dispatcherId, query)
            }.collect { _orders.value = it }
        }
    }

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSearchActive(active: Boolean) { _isSearchActive.value = active; if (!active) _searchQuery.value = "" }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(600)
            _isRefreshing.value = false
        }
    }

    suspend fun getUserById(id: Long): User? = repository.getUserById(id)

    fun createOrder(
        address: String, dateTime: Long, cargoDescription: String,
        pricePerHour: Double, estimatedHours: Int = 1, comment: String = "",
        requiredWorkers: Int = 1, minWorkerRating: Float = 0f
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val order = Order(
                    address = address, dateTime = dateTime, cargoDescription = cargoDescription,
                    pricePerHour = pricePerHour, estimatedHours = estimatedHours, comment = comment,
                    requiredWorkers = requiredWorkers, minWorkerRating = minWorkerRating,
                    status = OrderStatus.AVAILABLE, dispatcherId = dispatcherId
                )
                repository.createOrder(order)
                notificationHelper.sendNewOrderNotification(address, pricePerHour)
                _snackbarMessage.value = "✅ Заказ создан"
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
                _snackbarMessage.value = "Заказ отменён"
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка отмены заказа: ${e.message}"
            }
        }
    }

    fun saveProfile(name: String, phone: String, birthDate: Long?) {
        viewModelScope.launch {
            try {
                val user = repository.getUserById(dispatcherId) ?: return@launch
                repository.updateUser(user.copy(name = name, phone = phone, birthDate = birthDate))
                _snackbarMessage.value = "✅ Профиль сохранён"
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка сохранения: ${e.message}"
            }
        }
    }

    fun clearSnackbar() { _snackbarMessage.value = null }
    fun clearError() { _errorMessage.value = null }
}
