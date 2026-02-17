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

    val completedCount = repository.getCompletedOrdersCount(loaderId)
    val totalEarnings = repository.getTotalEarnings(loaderId)
    val averageRating = repository.getAverageRating(loaderId)

    init {
        loadAvailableOrders()
        loadMyOrders()
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            repository.getUserByIdFlow(loaderId).collect { _currentUser.value = it }
        }
    }

    private fun loadAvailableOrders() {
        viewModelScope.launch {
            try {
                repository.getAvailableOrders().collect { _availableOrders.value = it }
            } catch (e: Exception) {
                _errorMessage.value = "Ошибка загрузки заказов: ${e.message}"
            }
        }
    }

    private fun loadMyOrders() {
        viewModelScope.launch {
            try {
                repository.getOrdersByWorker(loaderId).collect { list ->
                    _myOrders.value = list.filter { it.status == OrderStatus.TAKEN || it.status == OrderStatus.COMPLETED }
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
                val current = repository.getOrderById(order.id)
                if (current?.status == OrderStatus.AVAILABLE) {
                    repository.takeOrder(order.id, loaderId)
                    val loader = repository.getUserById(loaderId)
                    if (loader != null) notificationHelper.sendOrderTakenNotification(order.address, loader.name)
                    _snackbarMessage.value = "✅ Заказ взят!"
                } else {
                    _snackbarMessage.value = "⚠️ Заказ уже занят другим грузчиком"
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
