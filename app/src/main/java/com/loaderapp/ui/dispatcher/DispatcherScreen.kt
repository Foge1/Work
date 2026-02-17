package com.loaderapp.ui.dispatcher

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.User
import com.loaderapp.ui.history.HistoryScreen
import com.loaderapp.ui.loader.EmptyState
import com.loaderapp.ui.loader.SkeletonCard
import com.loaderapp.ui.rating.RatingScreen
import com.loaderapp.ui.settings.SettingsScreen
import com.loaderapp.ui.theme.StatusOrange
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class DispatcherDestination { ORDERS, SETTINGS, RATING, HISTORY, PROFILE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun DispatcherScreen(
    viewModel: DispatcherViewModel,
    userName: String,
    onSwitchRole: () -> Unit,
    onDarkThemeChanged: ((Boolean) -> Unit)? = null,
    onOrderClick: (Order, User?, User?) -> Unit = { _, _, _ -> }
) {
    val orders by viewModel.orders.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    val completedCount by viewModel.completedCount.collectAsState(initial = 0)
    val activeCount by viewModel.activeCount.collectAsState(initial = 0)
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val workerCounts by viewModel.workerCounts.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showSwitchDialog by remember { mutableStateOf(false) }
    var currentDestination by remember { mutableStateOf(DispatcherDestination.ORDERS) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Свободные", "В работе")
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val availableCount = orders.count { it.status == OrderStatus.AVAILABLE }
    val takenCount = orders.count { it.status == OrderStatus.TAKEN || it.status == OrderStatus.COMPLETED }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(240.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerShape = RectangleShape
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { scope.launch { drawerState.close() } }) {
                        Icon(Icons.Default.Menu, "Закрыть меню", tint = MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Column {
                        Text(userName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Диспетчер", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                HorizontalDivider()
                val primary = MaterialTheme.colorScheme.primary

                @Composable
                fun DrawerItem(label: String, selected: Boolean, badge: Int = 0, onClick: () -> Unit) {
                    val textColor = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Row(
                        modifier = Modifier.fillMaxWidth().height(48.dp).background(
                            if (selected) Brush.horizontalGradient(listOf(primary.copy(0.15f), Color.Transparent))
                            else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        ), verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(primary.copy(if (selected) 1f else 0f)))
                        Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent, onClick = onClick) {
                            Row(modifier = Modifier.fillMaxSize().padding(start = 20.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = textColor)
                                if (badge > 0) Badge(containerColor = primary) { Text("$badge", fontSize = 11.sp, color = Color.White) }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                DrawerItem("Заказы", currentDestination == DispatcherDestination.ORDERS, badge = availableCount) { currentDestination = DispatcherDestination.ORDERS; scope.launch { drawerState.close() } }
                DrawerItem("Профиль", currentDestination == DispatcherDestination.PROFILE) { currentDestination = DispatcherDestination.PROFILE; scope.launch { drawerState.close() } }
                DrawerItem("Рейтинг", currentDestination == DispatcherDestination.RATING) { currentDestination = DispatcherDestination.RATING; scope.launch { drawerState.close() } }
                DrawerItem("История", currentDestination == DispatcherDestination.HISTORY) { currentDestination = DispatcherDestination.HISTORY; scope.launch { drawerState.close() } }
                DrawerItem("Настройки", currentDestination == DispatcherDestination.SETTINGS) { currentDestination = DispatcherDestination.SETTINGS; scope.launch { drawerState.close() } }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DrawerItem("Сменить роль", false) { showSwitchDialog = true; scope.launch { drawerState.close() } }
            }
        }
    ) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                fadeIn(tween(220)) + slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 10 } togetherWith
                fadeOut(tween(160))
            },
            label = "dispatcher_nav"
        ) { destination ->
            when (destination) {
                DispatcherDestination.ORDERS -> OrdersContent(
                    orders = orders, isLoading = isLoading, isRefreshing = isRefreshing,
                    userName = userName, selectedTab = selectedTab, tabs = tabs,
                    availableCount = availableCount, takenCount = takenCount,
                    searchQuery = searchQuery, isSearchActive = isSearchActive,
                    onTabSelected = { selectedTab = it },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onCreateOrder = { showCreateDialog = true },
                    onCancelOrder = { viewModel.cancelOrder(it) },
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onSearchToggle = { viewModel.setSearchActive(it) },
                    onOrderClick = { order ->
                        scope.launch {
                            val dispatcher = viewModel.getUserById(order.dispatcherId)
                            val worker = order.workerId?.let { viewModel.getUserById(it) }
                            onOrderClick(order, dispatcher, worker)
                        }
                    },
                    onRefresh = { viewModel.refresh() },
                    workerCounts = workerCounts
                )
                DispatcherDestination.SETTINGS -> SettingsScreen(
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onBackClick = { currentDestination = DispatcherDestination.ORDERS },
                    onDarkThemeChanged = onDarkThemeChanged
                )
                DispatcherDestination.RATING -> RatingScreen(
                    userName = userName, userRating = 5.0,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onBackClick = { currentDestination = DispatcherDestination.ORDERS },
                    dispatcherCompletedCount = completedCount, dispatcherActiveCount = activeCount, isDispatcher = true
                )
                DispatcherDestination.HISTORY -> HistoryScreen(
                    orders = orders,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onBackClick = { currentDestination = DispatcherDestination.ORDERS }
                )
                DispatcherDestination.PROFILE -> {
                    val currentUser by viewModel.currentUser.collectAsState()
                    val completedCnt by viewModel.completedCount.collectAsState(initial = 0)
                    val activeCnt by viewModel.activeCount.collectAsState(initial = 0)
                    currentUser?.let { user ->
                        com.loaderapp.ui.profile.ProfileScreen(
                            user = user,
                            dispatcherCompletedCount = completedCnt,
                            dispatcherActiveCount = activeCnt,
                            onMenuClick = { scope.launch { drawerState.open() } },
                            onSaveProfile = { name, phone, birthDate -> viewModel.saveProfile(name, phone, birthDate) }
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateOrderDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { address, dateTime, cargo, price, hours, comment, requiredWorkers, minRating ->
                viewModel.createOrder(address, dateTime, cargo, price, hours, comment, requiredWorkers, minRating)
                showCreateDialog = false
            }
        )
    }
    if (showSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchDialog = false },
            title = { Text("Сменить роль?") },
            text = { Text("Вы хотите выйти из режима диспетчера?") },
            confirmButton = { TextButton(onClick = { showSwitchDialog = false; onSwitchRole() }) { Text("Да") } },
            dismissButton = { TextButton(onClick = { showSwitchDialog = false }) { Text("Отмена") } }
        )
    }
    errorMessage?.let { LaunchedEffect(it) { viewModel.clearError() } }
    snackbarMessage?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearSnackbar()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun OrdersContent(
    orders: List<Order>, isLoading: Boolean, isRefreshing: Boolean, userName: String,
    selectedTab: Int, tabs: List<String>, availableCount: Int, takenCount: Int,
    searchQuery: String, isSearchActive: Boolean, onTabSelected: (Int) -> Unit,
    onMenuClick: () -> Unit, onCreateOrder: () -> Unit, onCancelOrder: (Order) -> Unit,
    onSearchQueryChange: (String) -> Unit, onSearchToggle: (Boolean) -> Unit,
    onOrderClick: (Order) -> Unit, onRefresh: () -> Unit,
    workerCounts: Map<Long, Int> = emptyMap()
) {
    val availableOrders = orders.filter { it.status == OrderStatus.AVAILABLE }
    val takenOrders = orders.filter { it.status == OrderStatus.TAKEN || it.status == OrderStatus.COMPLETED }
    val focusRequester = remember { FocusRequester() }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = onRefresh
    )

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (isSearchActive) {
                            OutlinedTextField(
                                value = searchQuery, onValueChange = onSearchQueryChange,
                                placeholder = { Text("Поиск заказов...") }, singleLine = true,
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.Transparent, unfocusedBorderColor = Color.Transparent)
                            )
                            LaunchedEffect(Unit) { focusRequester.requestFocus() }
                        } else {
                            Column {
                                Text("Панель диспетчера", fontWeight = FontWeight.SemiBold)
                                Text(userName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    navigationIcon = {
                        if (isSearchActive) {
                            IconButton(onClick = { onSearchToggle(false) }) { Icon(Icons.Default.ArrowBack, "Назад") }
                        } else {
                            Box {
                                IconButton(onClick = onMenuClick) { Icon(Icons.Default.Menu, "Меню") }
                                if (availableCount > 0) Badge(
                                    modifier = Modifier.align(Alignment.TopEnd).offset(x = (-4).dp, y = 4.dp),
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) { Text("$availableCount", fontSize = 9.sp, color = Color.White) }
                            }
                        }
                    },
                    actions = {
                        if (!isSearchActive) IconButton(onClick = { onSearchToggle(true) }) { Icon(Icons.Default.Search, "Поиск") }
                        else if (searchQuery.isNotEmpty()) IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Clear, "Очистить") }
                    }
                )
                TabRow(selectedTabIndex = selectedTab) {
                    Tab(selected = selectedTab == 0, onClick = { onTabSelected(0) }, text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Свободные")
                            if (availableCount > 0) Badge(containerColor = MaterialTheme.colorScheme.primary) { Text("$availableCount", fontSize = 10.sp, color = Color.White) }
                        }
                    })
                    Tab(selected = selectedTab == 1, onClick = { onTabSelected(1) }, text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("В работе")
                            if (takenCount > 0) Badge(containerColor = StatusOrange) { Text("$takenCount", fontSize = 10.sp, color = Color.White) }
                        }
                    })
                }
            }
        },
        floatingActionButton = {
            val haptic = LocalHapticFeedback.current
            ExtendedFloatingActionButton(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onCreateOrder() },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Создать заказ") }
            )
        }
    ) { padding ->
        val currentOrders = if (selectedTab == 0) availableOrders else takenOrders

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
        ) {
            when {
                isLoading && !isRefreshing -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(4) { SkeletonCard() }
                }
                currentOrders.isEmpty() -> {
                    val icon = if (selectedTab == 0) Icons.Default.Inbox else Icons.Default.Assignment
                    val title = if (selectedTab == 0) { if (isSearchActive && searchQuery.isNotEmpty()) "Заказы не найдены" else "Нет свободных заказов" } else "Нет заказов в работе"
                    val subtitle = if (selectedTab == 0) { if (isSearchActive && searchQuery.isNotEmpty()) "Попробуйте другой запрос" else "Создайте первый заказ" } else "Свободные заказы появятся здесь"
                    EmptyState(icon = icon, title = title, subtitle = subtitle)
                }
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(currentOrders, key = { _, it -> it.id }) { index, order ->
                        var visible by remember { mutableStateOf(false) }
                        LaunchedEffect(Unit) { kotlinx.coroutines.delay(index.toLong() * 50L); visible = true }
                        AnimatedVisibility(visible, enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 4 }) {
                            OrderCard(order = order, onCancel = { onCancelOrder(it) }, onClick = { onOrderClick(order) }, workerCount = workerCounts[order.id] ?: 0)
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun OrderCard(order: Order, onCancel: (Order) -> Unit, onClick: () -> Unit = {}, workerCount: Int = 0) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val accentColor = when (order.status) {
        OrderStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
        OrderStatus.TAKEN, OrderStatus.IN_PROGRESS -> StatusOrange
        OrderStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(2.dp), shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(accentColor))
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(order.address, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    StatusChip(order.status)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(dateFormat.format(Date(order.dateTime)), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(order.cargoDescription, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                if (order.comment.isNotBlank()) Text("💬 ${order.comment}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                if (order.requiredWorkers > 1 || order.minWorkerRating > 0f) {
                    Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (order.requiredWorkers > 1) {
                            com.loaderapp.ui.loader.WorkerProgressBadge(current = workerCount, required = order.requiredWorkers)
                        }
                        if (order.minWorkerRating > 0f) {
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = com.loaderapp.ui.theme.GoldStar, modifier = Modifier.size(12.dp))
                                    Text(" от ${order.minWorkerRating}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${order.pricePerHour.toInt()} ₽/час", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                    if (order.estimatedHours > 1) Text(" · ~${order.estimatedHours} ч · ${(order.pricePerHour * order.estimatedHours).toInt()} ₽", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (order.status == OrderStatus.AVAILABLE) {
                    OutlinedButton(
                        onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onCancel(order) },
                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.error),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error)
                    ) { Text("Отменить", fontWeight = FontWeight.Medium) }
                }
            }
        }
    }
}

@Composable
fun StatusChip(status: OrderStatus) {
    val (text, color) = when (status) {
        OrderStatus.AVAILABLE -> "Доступен" to MaterialTheme.colorScheme.primary
        OrderStatus.TAKEN -> "Занят" to StatusOrange
        OrderStatus.IN_PROGRESS -> "В процессе" to StatusOrange
        OrderStatus.COMPLETED -> "Завершён" to MaterialTheme.colorScheme.secondary
        OrderStatus.CANCELLED -> "Отменён" to MaterialTheme.colorScheme.error
    }
    Surface(color = color.copy(0.12f), shape = RoundedCornerShape(6.dp)) {
        Text(text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}
