package com.loaderapp.ui.dispatcher

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.User
import com.loaderapp.ui.components.AppBottomBar
import com.loaderapp.ui.components.BottomNavItem
import com.loaderapp.ui.history.HistoryScreen
import com.loaderapp.ui.loader.EmptyState
import com.loaderapp.ui.loader.SkeletonCard
import com.loaderapp.ui.rating.RatingScreen
import com.loaderapp.ui.settings.SettingsScreen
import com.loaderapp.ui.theme.StatusOrange
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class DispatcherDestination { ORDERS, SETTINGS, RATING, HISTORY, PROFILE, CREATE }

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

    var showSwitchDialog by remember { mutableStateOf(false) }
    var currentDestination by remember { mutableStateOf(DispatcherDestination.ORDERS) }
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Свободные", "В работе")
    val scope = rememberCoroutineScope()

    val availableCount = orders.count { it.status == OrderStatus.AVAILABLE }
    val takenCount = orders.count { it.status == OrderStatus.TAKEN || it.status == OrderStatus.IN_PROGRESS }

    // BackHandler: закрыть поиск → вернуться на заказы → вернуться на первую вкладку
    BackHandler(enabled = isSearchActive || currentDestination != DispatcherDestination.ORDERS || selectedTab != 0) {
        when {
            isSearchActive -> viewModel.setSearchActive(false)
            currentDestination != DispatcherDestination.ORDERS -> currentDestination = DispatcherDestination.ORDERS
            selectedTab != 0 -> selectedTab = 0
        }
    }

    val navItems = listOf(
        BottomNavItem(Icons.Default.Assignment, "Заказы", availableCount),
        BottomNavItem(Icons.Default.History, "История"),
        BottomNavItem(Icons.Default.Star, "Рейтинг"),
        BottomNavItem(Icons.Default.Person, "Профиль"),
        BottomNavItem(Icons.Default.Settings, "Настройки")
    )
    val destinations = listOf(
        DispatcherDestination.ORDERS,
        DispatcherDestination.HISTORY,
        DispatcherDestination.RATING,
        DispatcherDestination.PROFILE,
        DispatcherDestination.SETTINGS
    )

    // CREATE не показывает BottomBar
    val showBottomBar = currentDestination != DispatcherDestination.CREATE

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                AppBottomBar(
                    items = navItems,
                    selectedIndex = destinations.indexOf(currentDestination).coerceAtLeast(0),
                    onItemSelected = { index -> currentDestination = destinations[index] }
                )
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                fadeIn(tween(220)) + slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 10 } togetherWith
                        fadeOut(tween(160))
            },
            label = "dispatcher_nav",
            modifier = Modifier.padding(paddingValues)
        ) { destination ->
            when (destination) {
                DispatcherDestination.ORDERS -> OrdersContent(
                    orders = orders, isLoading = isLoading, isRefreshing = isRefreshing,
                    userName = userName, selectedTab = selectedTab, tabs = tabs,
                    availableCount = availableCount, takenCount = takenCount,
                    completedCount = completedCount,
                    searchQuery = searchQuery, isSearchActive = isSearchActive,
                    onTabSelected = { selectedTab = it },
                    onCreateOrder = { currentDestination = DispatcherDestination.CREATE },
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
                DispatcherDestination.CREATE -> CreateOrderScreen(
                    onBack = { currentDestination = DispatcherDestination.ORDERS },
                    onCreate = { address, dateTime, cargo, price, hours, comment, requiredWorkers, minRating ->
                        viewModel.createOrder(address, dateTime, cargo, price, hours, comment, requiredWorkers, minRating)
                        currentDestination = DispatcherDestination.ORDERS
                    }
                )
                DispatcherDestination.SETTINGS -> SettingsScreen(
                    onMenuClick = { /* нет drawer */ },
                    onBackClick = { currentDestination = DispatcherDestination.ORDERS },
                    onDarkThemeChanged = onDarkThemeChanged,
                    onSwitchRole = { showSwitchDialog = true }
                )
                DispatcherDestination.RATING -> RatingScreen(
                    userName = userName, userRating = 5.0,
                    onMenuClick = { /* нет drawer */ },
                    onBackClick = { currentDestination = DispatcherDestination.ORDERS },
                    dispatcherCompletedCount = completedCount, dispatcherActiveCount = activeCount, isDispatcher = true
                )
                DispatcherDestination.HISTORY -> HistoryScreen(
                    orders = orders,
                    onMenuClick = { /* нет drawer */ },
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
                            onMenuClick = { /* нет drawer */ },
                            onSaveProfile = { name, phone, birthDate -> viewModel.saveProfile(name, phone, birthDate) }
                        )
                    }
                }
            }
        }
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
    completedCount: Int = 0,
    searchQuery: String, isSearchActive: Boolean, onTabSelected: (Int) -> Unit,
    onCreateOrder: () -> Unit, onCancelOrder: (Order) -> Unit,
    onSearchQueryChange: (String) -> Unit, onSearchToggle: (Boolean) -> Unit,
    onOrderClick: (Order) -> Unit, onRefresh: () -> Unit,
    workerCounts: Map<Long, Int> = emptyMap()
) {
    val availableOrders = orders.filter { it.status == OrderStatus.AVAILABLE }
    val takenOrders = orders.filter { it.status == OrderStatus.TAKEN || it.status == OrderStatus.IN_PROGRESS || it.status == OrderStatus.COMPLETED }
    val focusRequester = remember { FocusRequester() }
    val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)

    // Синхронизация pager ↔ selectedTab
    LaunchedEffect(selectedTab) {
        if (pagerState.currentPage != selectedTab) pagerState.animateScrollToPage(selectedTab)
    }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != selectedTab) onTabSelected(pagerState.currentPage)
    }

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
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                )
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
                            IconButton(onClick = { onSearchToggle(false) }) {
                                Icon(Icons.Default.ArrowBack, "Назад")
                            }
                        }
                    },
                    actions = {
                        if (!isSearchActive) {
                            IconButton(onClick = { onSearchToggle(true) }) {
                                Icon(Icons.Default.Search, "Поиск")
                            }
                        } else if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, "Очистить")
                            }
                        }
                    }
                )
                // Pill-style вкладки
                val pillPrimary = MaterialTheme.colorScheme.primary
                val pillSurface = MaterialTheme.colorScheme.surfaceVariant
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(pillSurface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "Свободные" to (if (availableCount > 0) "$availableCount" else null),
                        "В работе" to (if (takenCount > 0 || completedCount > 0) "$takenCount/$completedCount" else null)
                    ).forEachIndexed { index, (label, badge) ->
                        val selected = pagerState.currentPage == index
                        val badgeColor = if (index == 0) pillPrimary else StatusOrange
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                )
                                .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    label,
                                    fontSize = 14.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (selected) pillPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (badge != null) {
                                    Badge(containerColor = badgeColor) {
                                        Text(badge, fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            val listState = rememberLazyListState()
            val fabVisible by remember {
                derivedStateOf { listState.firstVisibleItemIndex == 0 || listState.firstVisibleItemScrollOffset == 0 }
            }
            val haptic = LocalHapticFeedback.current
            AnimatedVisibility(
                visible = fabVisible,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                ExtendedFloatingActionButton(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onCreateOrder() },
                    icon = { Icon(Icons.Default.Add, null) },
                    text = { Text("Создать заказ") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).pullRefresh(pullRefreshState)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val currentOrders = if (page == 0) availableOrders else takenOrders
                when {
                    isLoading && !isRefreshing -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(4) { SkeletonCard() }
                    }
                    currentOrders.isEmpty() -> {
                        val icon = if (page == 0) Icons.Default.Inbox else Icons.Default.Assignment
                        val title = if (page == 0) {
                            if (isSearchActive && searchQuery.isNotEmpty()) "Заказы не найдены" else "Нет свободных заказов"
                        } else "Нет заказов в работе"
                        val subtitle = if (page == 0) {
                            if (isSearchActive && searchQuery.isNotEmpty()) "Попробуйте другой запрос" else "Создайте первый заказ"
                        } else "Свободные заказы появятся здесь"
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
                            AnimatedVisibility(
                                visible,
                                enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 4 }
                            ) {
                                OrderCard(
                                    order = order,
                                    onCancel = { onCancelOrder(it) },
                                    onClick = { onOrderClick(order) },
                                    workerCount = workerCounts[order.id] ?: 0
                                )
                            }
                        }
                    }
                }
            }
            PullRefreshIndicator(
                refreshing = isRefreshing, state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun timeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / 60_000
    val hours = diff / 3_600_000
    val days = diff / 86_400_000
    return when {
        minutes < 1 -> "только что"
        minutes < 60 -> "$minutes мин. назад"
        hours < 24 -> "$hours ч. назад"
        days < 7 -> "$days д. назад"
        else -> SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

@Composable
fun OrderCard(order: Order, onCancel: (Order) -> Unit, onClick: () -> Unit = {}, workerCount: Int = 0) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    var showCancelConfirm by remember { mutableStateOf(false) }
    val accentColor = when (order.status) {
        OrderStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
        OrderStatus.TAKEN, OrderStatus.IN_PROGRESS -> StatusOrange
        OrderStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(0.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(accentColor.copy(alpha = 0.10f), Color.Transparent)
                        )
                    )
            )
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        order.address, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    StatusChip(order.status)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        dateFormat.format(Date(order.dateTime)), fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("·", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                    Text(
                        timeAgo(order.createdAt), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )
                }
                Text(
                    order.cargoDescription, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (order.comment.isNotBlank()) {
                    Text(
                        "💬 ${order.comment}", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                if (order.requiredWorkers > 1 || order.minWorkerRating > 0f) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (order.requiredWorkers > 1) {
                            com.loaderapp.ui.loader.WorkerProgressBadge(
                                current = workerCount, required = order.requiredWorkers
                            )
                        }
                        if (order.minWorkerRating > 0f) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.extraSmall
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Star, null,
                                        tint = com.loaderapp.ui.theme.GoldStar,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        " от ${order.minWorkerRating}", fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${order.pricePerHour.toInt()} ₽/час", fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold, color = accentColor
                    )
                    if (order.estimatedHours > 1) {
                        Text(
                            " · ~${order.estimatedHours} ч · ${(order.pricePerHour * order.estimatedHours).toInt()} ₽",
                            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (order.status == OrderStatus.COMPLETED) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    order.completedAt?.let { completedAt ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle, null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                " Завершён ${dateFormat.format(Date(completedAt))}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    order.workerRating?.let { rating ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            repeat(5) { i ->
                                Icon(
                                    if (i < rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                    null,
                                    tint = if (i < rating.toInt()) com.loaderapp.ui.theme.GoldStar
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Text(
                                " Оценка грузчика", fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
                if (order.status == OrderStatus.AVAILABLE) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            showCancelConfirm = true
                        },
                        modifier = Modifier.align(Alignment.End).padding(top = 8.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp, MaterialTheme.colorScheme.error
                        )
                    ) { Text("Отменить", fontWeight = FontWeight.Medium) }
                }
            }
        }
    }

    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            shape = MaterialTheme.shapes.extraLarge,
            icon = {
                Icon(
                    Icons.Default.Warning, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp)
                )
            },
            title = { Text("Отменить заказ?", fontWeight = FontWeight.SemiBold) },
            text = {
                Text(
                    "Заказ «${order.address}» будет отменён. Это действие нельзя отменить.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCancelConfirm = false; onCancel(order) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = MaterialTheme.shapes.small
                ) { Text("Отменить заказ") }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) { Text("Назад") }
            }
        )
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
    Surface(color = color.copy(0.12f), shape = RoundedCornerShape(6.dp), shadowElevation = 0.dp) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color
        )
    }
}
