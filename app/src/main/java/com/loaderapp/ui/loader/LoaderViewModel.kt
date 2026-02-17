package com.loaderapp.ui.loader

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.User
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

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // workerCount для каждого заказа: orderId -> count
    private val _workerCounts = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val workerCounts: StateFlow<Map<Long, Int>> = _workerCounts.asStateFlow()

    val completedCount = repository.getCompletedOrdersCount(loaderId)
    val totalEarnings = repository.getTotalEarnings(loaderId)
    val averageRating = repository.getAverageRating(loaderId)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadAvailableOrders()
        loadMyOrders()
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            repository.getUserByIdFlow(loaderId).collect { user ->
                _currentUser.value = user
                // При получении пользователя обновляем доступные заказы с учётом рейтинга
                if (user != null) reloadAvailableWithRating(user.rating.toFloat())
            }
        }
    }

    private fun reloadAvailableWithRating(myRating: Float) {
        viewModelScope.launch {
            repository.getAvailableOrders().collect { orders ->
                val filtered = orders.filter { order ->
                    // Фильтр по мин. рейтингу
                    myRating >= order.minWorkerRating
                }
                _availableOrders.value = filtered
                // Загружаем счётчики грузчиков для видимых заказов
                updateWorkerCounts(filtered)
            }
        }
    }

    private fun loadAvailableOrders() {
        viewModelScope.launch {
            try {
                repository.getAvailableOrders().collect { orders ->
                    val myRating = _currentUser.value?.rating?.toFloat() ?: 5f
                    val filtered = orders.filter { it.minWorkerRating <= myRating }
                    _availableOrders.value = filtered
                    updateWorkerCounts(filtered)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки заказов: ${e.message}"
            }
        }
    }

    private fun loadMyOrders() {
        // Источник 1: заказы где workerId = loaderId (реагирует на изменения статуса)
        viewModelScope.launch {
            try {
                repository.getOrdersByWorker(loaderId).collect { directOrders ->
                    mergeMyOrders(directOrders)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки моих заказов: ${e.message}"
            }
        }
        // Источник 2: junction-таблица order_workers (для мультигрузчиков)
        viewModelScope.launch {
            try {
                repository.getOrderIdsByWorker(loaderId).collect { workerOrderIds ->
                    val directOrders = _myOrders.value
                    val directIds = directOrders.map { it.id }.toSet()
                    val extraIds = workerOrderIds.filter { it !in directIds }
                    val extraOrders = extraIds.mapNotNull { repository.getOrderById(it) }
                    mergeMyOrders(directOrders + extraOrders)
                }
            } catch (e: Exception) {
                // не критично — основной источник выше
            }
        }
    }

    private suspend fun mergeMyOrders(orders: List<Order>) {
        val result = orders
            .filter { it.status == OrderStatus.TAKEN || it.status == OrderStatus.COMPLETED }
            .sortedByDescending { it.dateTime }
        _myOrders.value = result
        updateWorkerCounts(result)
    }

    private suspend fun updateWorkerCounts(orders: List<Order>) {
        val counts = mutableMapOf<Long, Int>()
        orders.forEach { order ->
            counts[order.id] = repository.getWorkerCountSync(order.id)
        }
        _workerCounts.value = counts
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val myRating = _currentUser.value?.rating?.toFloat() ?: 5f
            val orders = repository.getAvailableOrders()
            updateWorkerCounts(_availableOrders.value + _myOrders.value)
            kotlinx.coroutines.delay(600)
            _isRefreshing.value = false
        }
    }

    suspend fun getUserById(id: Long): User? = repository.getUserById(id)

    fun takeOrder(order: Order) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val current = repository.getOrderById(order.id)
                if (current != null && current.status == OrderStatus.AVAILABLE) {
                    // Проверяем что грузчик ещё не взял этот заказ
                    val alreadyTaken = repository.hasWorkerTakenOrder(order.id, loaderId)
                    if (alreadyTaken) {
                        _snackbarMessage.value = "⚠️ Вы уже взяли этот заказ"
                        return@launch
                    }
                    repository.takeOrder(order.id, loaderId)
                    val loader = repository.getUserById(loaderId)
                    if (loader != null) notificationHelper.sendOrderTakenNotification(order.address, loader.name)
                    // Обновляем счётчик
                    val newCount = repository.getWorkerCountSync(order.id)
                    _workerCounts.value = _workerCounts.value + (order.id to newCount)
                    _snackbarMessage.value = "✅ Заказ взят!"
                } else {
                    _snackbarMessage.value = "⚠️ Заказ больше недоступен"
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
                _snackbarMessage.value = "🎉 Заказ завершён!"
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка завершения заказа: ${e.message}"
            }
        }
    }

    fun rateOrder(orderId: Long, rating: Float) {
        viewModelScope.launch {
            try {
                repository.rateOrder(orderId, rating)
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка выставления оценки: ${e.message}"
            }
        }
    }

    fun saveProfile(name: String, phone: String, birthDate: Long?) {
        viewModelScope.launch {
            try {
                val user = repository.getUserById(loaderId) ?: return@launch
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
