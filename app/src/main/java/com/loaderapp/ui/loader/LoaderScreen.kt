package com.loaderapp.ui.loader

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.User
import com.loaderapp.ui.history.HistoryScreen
import com.loaderapp.ui.profile.ProfileScreen
import com.loaderapp.ui.rating.RatingScreen
import com.loaderapp.ui.settings.SettingsScreen
import com.loaderapp.ui.theme.GoldStar
import com.loaderapp.ui.theme.ShimmerDark
import com.loaderapp.ui.theme.ShimmerLight
import com.loaderapp.ui.theme.StatusOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class LoaderDestination { ORDERS, SETTINGS, RATING, HISTORY, PROFILE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoaderScreen(
    viewModel: LoaderViewModel,
    userName: String,
    onSwitchRole: () -> Unit,
    onDarkThemeChanged: ((Boolean) -> Unit)? = null
) {
    val availableOrders by viewModel.availableOrders.collectAsState()
    val myOrders by viewModel.myOrders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState(initial = 0)
    val totalEarnings by viewModel.totalEarnings.collectAsState(initial = null)
    val averageRating by viewModel.averageRating.collectAsState(initial = null)
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var selectedOrder by remember { mutableStateOf<Order?>(null) }

    var selectedTab by remember { mutableStateOf(0) }
    var showSwitchDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf<Order?>(null) }
    var orderToTake by remember { mutableStateOf<Order?>(null) }
    var currentDestination by remember { mutableStateOf(LoaderDestination.ORDERS) }
    val activeOrder = myOrders.firstOrNull { it.status == OrderStatus.TAKEN }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.width(240.dp),
                drawerShape = RectangleShape
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Default.Menu, contentDescription = "Закрыть меню", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(text = userName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Грузчик", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if ((averageRating ?: 0f) > 0f) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = GoldStar, modifier = Modifier.size(12.dp))
                                Text(" ${"%.1f".format(averageRating)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                HorizontalDivider()
                val primary = MaterialTheme.colorScheme.primary

                @Composable
                fun DrawerItem(label: String, selected: Boolean, badge: Int = 0, onClick: () -> Unit) {
                    val textColor = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp).background(
                            if (selected) Brush.horizontalGradient(listOf(primary.copy(alpha = 0.15f), Color.Transparent))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(primary.copy(alpha = if (selected) 1f else 0f)))
                        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent, onClick = onClick) {
                            Row(modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = textColor)
                                if (badge > 0) Badge(containerColor = primary) { Text("$badge", fontSize = 11.sp, color = Color.White) }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                DrawerItem("Заказы", currentDestination == LoaderDestination.ORDERS, badge = availableOrders.size) { currentDestination = LoaderDestination.ORDERS; scope.launch { drawerState.close() } }
                DrawerItem("Профиль", currentDestination == LoaderDestination.PROFILE) { currentDestination = LoaderDestination.PROFILE; scope.launch { drawerState.close() } }
                DrawerItem("Рейтинг", currentDestination == LoaderDestination.RATING) { currentDestination = LoaderDestination.RATING; scope.launch { drawerState.close() } }
                DrawerItem("История", currentDestination == LoaderDestination.HISTORY) { currentDestination = LoaderDestination.HISTORY; scope.launch { drawerState.close() } }
                DrawerItem("Настройки", currentDestination == LoaderDestination.SETTINGS) { currentDestination = LoaderDestination.SETTINGS; scope.launch { drawerState.close() } }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DrawerItem("Сменить роль", false) { showSwitchDialog = true; scope.launch { drawerState.close() } }
            }
        }
    ) {
        AnimatedContent(targetState = currentDestination, transitionSpec = { fadeIn(animationSpec = tween(220)) + slideInHorizontally(animationSpec = tween(220), initialOffsetX = { it / 12 }) togetherWith fadeOut(animationSpec = tween(150)) }, label = "loader_nav") { destination ->
            when (destination) {
                LoaderDestination.ORDERS -> LoaderOrdersContent(availableOrders = availableOrders, myOrders = myOrders, isLoading = isLoading, isRefreshing = isRefreshing, userName = userName, selectedTab = selectedTab, activeOrder = activeOrder, onTabSelected = { selectedTab = it }, onMenuClick = { scope.launch { drawerState.open() } }, onTakeOrder = { orderToTake = it }, onCompleteOrder = { order -> viewModel.completeOrder(order); showRatingDialog = order }, onOrderClick = { selectedOrder = it }, onRefresh = { viewModel.refresh() })
                LoaderDestination.SETTINGS -> SettingsScreen(onMenuClick = { scope.launch { drawerState.open() } }, onBackClick = { currentDestination = LoaderDestination.ORDERS }, onDarkThemeChanged = onDarkThemeChanged)
                LoaderDestination.RATING -> RatingScreen(userName = userName, userRating = averageRating?.toDouble() ?: 5.0, onMenuClick = { scope.launch { drawerState.open() } }, onBackClick = { currentDestination = LoaderDestination.ORDERS }, completedCount = completedCount, totalEarnings = totalEarnings ?: 0.0, averageRating = averageRating ?: 0f, isDispatcher = false)
                LoaderDestination.HISTORY -> HistoryScreen(orders = myOrders, onMenuClick = { scope.launch { drawerState.open() } }, onBackClick = { currentDestination = LoaderDestination.ORDERS })
                LoaderDestination.PROFILE -> {
                    currentUser?.let { user ->
                        ProfileScreen(
                            user = user,
                            completedCount = completedCount,
                            totalEarnings = totalEarnings ?: 0.0,
                            averageRating = averageRating ?: 0f,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onSaveProfile = { name, phone, birthDate -> viewModel.saveProfile(name, phone, birthDate) }
                        )
                    }
                }
            }
        }
    }

    orderToTake?.let { order -> TakeOrderBottomSheet(order = order, onDismiss = { orderToTake = null }, onConfirm = { viewModel.takeOrder(order); orderToTake = null }) }
    showRatingDialog?.let { order -> RateOrderDialog(onDismiss = { showRatingDialog = null }, onRate = { rating -> viewModel.rateOrder(order.id, rating); showRatingDialog = null }) }

    snackbarMessage?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearSnackbar()
        }
    }

    // Детальный bottom sheet для заказа
    selectedOrder?.let { order ->
        val dispatcher by produceState<User?>(null, order.dispatcherId) {
            value = viewModel.getUserById(order.dispatcherId)
        }
        val worker by produceState<User?>(null, order.workerId) {
            value = order.workerId?.let { viewModel.getUserById(it) }
        }
        OrderDetailBottomSheet(
            order = order,
            dispatcher = dispatcher,
            worker = worker,
            onDismiss = { selectedOrder = null }
        )
    }

    if (showSwitchDialog) {
        AlertDialog(onDismissRequest = { showSwitchDialog = false }, title = { Text("Сменить роль?") }, text = { Text("Вы хотите выйти из режима грузчика?") },
            confirmButton = { TextButton(onClick = { showSwitchDialog = false; onSwitchRole() }) { Text("Да") } },
            dismissButton = { TextButton(onClick = { showSwitchDialog = false }) { Text("Отмена") } })
    }
    errorMessage?.let { LaunchedEffect(it) { viewModel.clearError() } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeOrderBottomSheet(order: Order, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Взять заказ?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("Адрес", order.address)
                    InfoRow("Дата", dateFormat.format(Date(order.dateTime)))
                    InfoRow("Груз", order.cargoDescription)
                    InfoRow("Оплата", "${order.pricePerHour.toInt()} ₽/час" + if (order.estimatedHours > 1) " · ~${(order.pricePerHour * order.estimatedHours).toInt()} ₽" else "")
                    if (order.comment.isNotBlank()) InfoRow("Комментарий", order.comment)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onConfirm() }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Подтвердить", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) { Text("Отмена") }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(text = label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.35f))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.65f), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun RateOrderDialog(onDismiss: () -> Unit, onRate: (Float) -> Unit) {
    var selectedRating by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Оцените заказ") }, text = {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Как прошёл заказ?", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 1..5) {
                    val isSelected = i <= selectedRating
                    IconButton(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); selectedRating = i }, modifier = Modifier.size(44.dp)) {
                        Icon(imageVector = if (isSelected) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "$i звёзд", tint = if (isSelected) GoldStar else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(36.dp))
                    }
                }
            }
            AnimatedVisibility(visible = selectedRating > 0) {
                Text(text = when (selectedRating) { 1 -> "Плохо"; 2 -> "Неплохо"; 3 -> "Хорошо"; 4 -> "Очень хорошо"; 5 -> "Отлично! 🎉"; else -> "" }, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }, confirmButton = { Button(onClick = { if (selectedRating > 0) onRate(selectedRating.toFloat()) }, enabled = selectedRating > 0) { Text("Отправить") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Пропустить") } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoaderOrdersContent(
    availableOrders: List<Order>, myOrders: List<Order>, isLoading: Boolean, isRefreshing: Boolean,
    userName: String, selectedTab: Int, activeOrder: Order?, onTabSelected: (Int) -> Unit,
    onMenuClick: () -> Unit, onTakeOrder: (Order) -> Unit, onCompleteOrder: (Order) -> Unit,
    onOrderClick: (Order) -> Unit, onRefresh: () -> Unit
) {
    val activeOrderCount = myOrders.count { it.status == OrderStatus.TAKEN }
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Column { Text("Грузчик", fontWeight = FontWeight.SemiBold); Text(text = userName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
                    navigationIcon = {
                        Box {
                            IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, contentDescription = "Меню") }
                            if (availableOrders.isNotEmpty()) Badge(modifier = Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp), containerColor = MaterialTheme.colorScheme.primary) { Text("${availableOrders.size}", fontSize = 9.sp, color = Color.White) }
                        }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { onTabSelected(0) }, text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text("Доступные"); if (availableOrders.isNotEmpty()) Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("${availableOrders.size}", fontSize = 10.sp, color = Color.White) } } })
                    Tab(selected = selectedTab == 1, onClick = { onTabSelected(1) }, text = { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Text("Мои заказы"); if (activeOrderCount > 0) Badge(containerColor = StatusOrange) { Text("$activeOrderCount", fontSize = 10.sp, color = Color.White) } } })
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                0 -> AvailableOrdersList(orders = availableOrders, isLoading = isLoading, isRefreshing = isRefreshing, onTakeOrder = onTakeOrder, onOrderClick = onOrderClick, onRefresh = onRefresh)
                1 -> MyOrdersList(orders = myOrders, isLoading = isLoading, isRefreshing = isRefreshing, activeOrder = activeOrder, onCompleteOrder = onCompleteOrder, onOrderClick = onOrderClick, onRefresh = onRefresh)
            }
        }
    }
}

@Composable
fun SkeletonCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(initialValue = 0.3f, targetValue = 0.9f, animationSpec = infiniteRepeatable(animation = tween(900, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "alpha")
    val shimmerColor = if (MaterialTheme.colorScheme.surface.value < 0xFF888888u) ShimmerDark else ShimmerLight
    Card(modifier = Modifier.fillMaxWidth(), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor.copy(alpha = alpha)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor.copy(alpha = alpha * 0.7f)))
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor.copy(alpha = alpha * 0.6f)))
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp)).background(shimmerColor.copy(alpha = alpha * 0.5f)))
        }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Column(modifier = Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(96.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(48.dp)) }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailableOrdersList(orders: List<Order>, isLoading: Boolean, isRefreshing: Boolean, onTakeOrder: (Order) -> Unit, onOrderClick: (Order) -> Unit, onRefresh: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && !isRefreshing -> { LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(4) { SkeletonCard() } } }
            orders.isEmpty() -> { EmptyState(Icons.Default.WorkOff, "Нет доступных заказов", "Новые заказы появятся здесь") }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    itemsIndexed(orders, key = { _, it -> it.id }) { index, order ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { delay(index.toLong() * 60L); visible = true }
                        AnimatedVisibility(visible = visible, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 3 }) {
                            AvailableOrderCard(order = order, onTake = { onTakeOrder(order) }, onClick = { onOrderClick(order) })
                        }
                    }
                }
            }
        }
        if (isRefreshing) { CircularProgressIndicator(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOrdersList(orders: List<Order>, isLoading: Boolean, isRefreshing: Boolean, activeOrder: Order?, onCompleteOrder: (Order) -> Unit, onOrderClick: (Order) -> Unit, onRefresh: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            isLoading && !isRefreshing -> { LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(3) { SkeletonCard() } } }
            orders.isEmpty() -> { EmptyState(Icons.Default.AssignmentTurnedIn, "У вас нет заказов", "Возьмите заказ на вкладке «Доступные»") }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activeOrder?.let { active -> item(key = "active_${active.id}") { ActiveOrderBanner(order = active, onComplete = { onCompleteOrder(active) }, onClick = { onOrderClick(active) }) } }
                    items(orders.filter { it.status != OrderStatus.TAKEN }, key = { it.id }) { order -> MyOrderCard(order = order, onComplete = { onCompleteOrder(order) }, onClick = { onOrderClick(order) }) }
                }
            }
        }
        if (isRefreshing) { CircularProgressIndicator(modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp)) }
    }
}

@Composable
fun ActiveOrderBanner(order: Order, onComplete: () -> Unit, onClick: () -> Unit = {}) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(1000); elapsedSeconds++ } }
    val h = elapsedSeconds / 3600; val m = (elapsedSeconds % 3600) / 60; val s = elapsedSeconds % 60
    val timerText = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Активный заказ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(timerText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(order.address, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text("${dateFormat.format(Date(order.dateTime))} · ${order.cargoDescription}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f), modifier = Modifier.padding(top = 2.dp))
            Text("${order.pricePerHour.toInt()} ₽/час", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onComplete() }, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Завершить заказ", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun AvailableOrderCard(order: Order, onTake: () -> Unit, onClick: () -> Unit = {}) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val accentColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(accentColor))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
                Text(order.address, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text(dateFormat.format(Date(order.dateTime)), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(order.cargoDescription, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                if (order.comment.isNotBlank()) Text("💬 ${order.comment}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${order.pricePerHour.toInt()} ₽/час", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                    if (order.estimatedHours > 1) Text(" · ~${order.estimatedHours} ч · ${(order.pricePerHour * order.estimatedHours).toInt()} ₽", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onTake() }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Взять заказ", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun MyOrderCard(order: Order, onComplete: () -> Unit, onClick: () -> Unit = {}) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val accentColor = when (order.status) { OrderStatus.AVAILABLE -> MaterialTheme.colorScheme.primary; OrderStatus.TAKEN, OrderStatus.IN_PROGRESS -> StatusOrange; OrderStatus.COMPLETED -> MaterialTheme.colorScheme.secondary; OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error }
    Card(modifier = Modifier.fillMaxWidth().clickable { onClick() }, elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), shape = RoundedCornerShape(12.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(accentColor))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(order.address, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    LoaderStatusChip(status = order.status)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(dateFormat.format(Date(order.dateTime)), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(order.cargoDescription, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${order.pricePerHour.toInt()} ₽/час", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                    if (order.estimatedHours > 1) Text(" · ~${(order.pricePerHour * order.estimatedHours).toInt()} ₽", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                order.workerRating?.let { rating ->
                    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { i -> Icon(if (i < rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = null, tint = if (i < rating.toInt()) GoldStar else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.size(14.dp)) }
                        Text(" Ваша оценка", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun LoaderStatusChip(status: OrderStatus) {
    val (text, color) = when (status) { OrderStatus.AVAILABLE -> "Доступен" to MaterialTheme.colorScheme.primary; OrderStatus.TAKEN -> "В работе" to StatusOrange; OrderStatus.IN_PROGRESS -> "В процессе" to StatusOrange; OrderStatus.COMPLETED -> "Завершён" to MaterialTheme.colorScheme.secondary; OrderStatus.CANCELLED -> "Отменён" to MaterialTheme.colorScheme.error }
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp)) { Text(text = text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color) }
}

@Composable
fun StatusChip(status: OrderStatus) = LoaderStatusChip(status)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailBottomSheet(
    order: Order,
    dispatcher: User?,
    worker: User?,
    onDismiss: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru"))
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {

            // Заголовок
            Text(order.address, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            LoaderStatusChip(status = order.status)
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Дата и время
            DetailRow(icon = Icons.Default.CalendarMonth, label = "Дата и время", value = dateFormat.format(Date(order.dateTime)))

            // Груз
            DetailRow(icon = Icons.Default.Inventory, label = "Описание груза", value = order.cargoDescription)

            // Цена
            DetailRow(icon = Icons.Default.Payments, label = "Оплата", value = buildString {
                append("${order.pricePerHour.toInt()} ₽/час")
                if (order.estimatedHours > 1) append(" · ~${order.estimatedHours} ч · ${(order.pricePerHour * order.estimatedHours).toInt()} ₽")
            })

            // Комментарий
            if (order.comment.isNotBlank()) {
                DetailRow(icon = Icons.Default.Comment, label = "Комментарий", value = order.comment)
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Диспетчер
            if (dispatcher != null) {
                Text("Диспетчер", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                ContactCard(name = dispatcher.name, phone = dispatcher.phone, context = context)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Грузчик (если взят)
            if (worker != null) {
                Text("Грузчик", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(8.dp))
                ContactCard(name = worker.name, phone = worker.phone, context = context)
            }
        }
    }
}

@Composable
fun DetailRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Top) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp).padding(top = 2.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ContactCard(name: String, phone: String, context: android.content.Context) {
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(name.take(1).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (phone.isNotBlank()) Text(phone, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (phone.isNotBlank()) {
                IconButton(onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL, android.net.Uri.parse("tel:$phone"))
                    context.startActivity(intent)
                }) {
                    Icon(Icons.Default.Phone, contentDescription = "Позвонить", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
