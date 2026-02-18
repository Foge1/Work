package com.loaderapp.ui.loader

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.User
import com.loaderapp.ui.components.AppBottomBar
import com.loaderapp.ui.components.BottomNavItem
import com.loaderapp.ui.history.HistoryScreen
import com.loaderapp.ui.profile.ProfileScreen
import com.loaderapp.ui.rating.RatingScreen
import com.loaderapp.ui.settings.SettingsScreen
import com.loaderapp.ui.theme.GoldStar
import com.loaderapp.ui.theme.ShimmerDark
import com.loaderapp.ui.theme.ShimmerLight
import com.loaderapp.ui.theme.StatusOrange
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

enum class LoaderDestination { ORDERS, SETTINGS, RATING, HISTORY, PROFILE }

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun LoaderScreen(
    viewModel: LoaderViewModel,
    userName: String,
    onSwitchRole: () -> Unit,
    onDarkThemeChanged: ((Boolean) -> Unit)? = null,
    onOrderClick: (Order, User?, User?) -> Unit = { _, _, _ -> }
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
    val workerCounts by viewModel.workerCounts.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var showSwitchDialog by remember { mutableStateOf(false) }
    var showRatingDialog by remember { mutableStateOf<Order?>(null) }
    var orderToTake by remember { mutableStateOf<Order?>(null) }
    var currentDestination by remember { mutableStateOf(LoaderDestination.ORDERS) }
    val activeOrder = myOrders.firstOrNull { it.status == OrderStatus.TAKEN }
    val scope = rememberCoroutineScope()

    BackHandler(enabled = currentDestination != LoaderDestination.ORDERS || selectedTab != 0) {
        when {
            currentDestination != LoaderDestination.ORDERS -> currentDestination = LoaderDestination.ORDERS
            selectedTab != 0 -> selectedTab = 0
        }
    }

    val navItems = listOf(
        BottomNavItem(Icons.Default.Assignment, "Заказы", availableOrders.size),
        BottomNavItem(Icons.Default.History, "История"),
        BottomNavItem(Icons.Default.Star, "Рейтинг"),
        BottomNavItem(Icons.Default.Person, "Профиль"),
        BottomNavItem(Icons.Default.Settings, "Настройки")
    )
    val destinations = listOf(
        LoaderDestination.ORDERS,
        LoaderDestination.HISTORY,
        LoaderDestination.RATING,
        LoaderDestination.PROFILE,
        LoaderDestination.SETTINGS
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            AppBottomBar(
                items = navItems,
                selectedIndex = destinations.indexOf(currentDestination).coerceAtLeast(0),
                onItemSelected = { index -> currentDestination = destinations[index] }
            )
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                fadeIn(tween(220)) + slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) { it / 10 } togetherWith
                        fadeOut(tween(160))
            },
            label = "loader_nav",
            modifier = Modifier.padding(paddingValues)
        ) { destination ->
            when (destination) {
                LoaderDestination.ORDERS -> LoaderOrdersContent(
                    availableOrders = availableOrders, myOrders = myOrders,
                    isLoading = isLoading, isRefreshing = isRefreshing, userName = userName,
                    selectedTab = selectedTab, activeOrder = activeOrder,
                    completedCount = completedCount,
                    onTabSelected = { selectedTab = it },
                    onTakeOrder = { orderToTake = it },
                    onCompleteOrder = { order -> viewModel.completeOrder(order); showRatingDialog = order },
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
                LoaderDestination.SETTINGS -> SettingsScreen(
                    onMenuClick = { /* нет drawer */ },
                    onBackClick = { currentDestination = LoaderDestination.ORDERS },
                    onDarkThemeChanged = onDarkThemeChanged,
                    onSwitchRole = { showSwitchDialog = true }
                )
                LoaderDestination.RATING -> RatingScreen(
                    userName = userName, userRating = averageRating?.toDouble() ?: 5.0,
                    onMenuClick = { /* нет drawer */ },
                    onBackClick = { currentDestination = LoaderDestination.ORDERS },
                    completedCount = completedCount, totalEarnings = totalEarnings ?: 0.0,
                    averageRating = averageRating ?: 0f, isDispatcher = false
                )
                LoaderDestination.HISTORY -> HistoryScreen(
                    orders = myOrders,
                    onMenuClick = { /* нет drawer */ },
                    onBackClick = { currentDestination = LoaderDestination.ORDERS }
                )
                LoaderDestination.PROFILE -> currentUser?.let { user ->
                    ProfileScreen(
                        user = user, completedCount = completedCount,
                        totalEarnings = totalEarnings ?: 0.0, averageRating = averageRating ?: 0f,
                        onMenuClick = { /* нет drawer */ },
                        onSaveProfile = { name, phone, birthDate ->
                            viewModel.saveProfile(name, phone, birthDate)
                        }
                    )
                }
            }
        }
    }

    orderToTake?.let { order ->
        TakeOrderBottomSheet(
            order = order,
            onDismiss = { orderToTake = null },
            onConfirm = { viewModel.takeOrder(order); orderToTake = null }
        )
    }
    showRatingDialog?.let { order ->
        RateOrderDialog(
            onDismiss = { showRatingDialog = null },
            onRate = { rating -> viewModel.rateOrder(order.id, rating); showRatingDialog = null }
        )
    }

    snackbarMessage?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearSnackbar()
        }
    }
    if (showSwitchDialog) {
        AlertDialog(
            onDismissRequest = { showSwitchDialog = false },
            title = { Text("Сменить роль?") },
            text = { Text("Вы хотите выйти из режима грузчика?") },
            confirmButton = {
                TextButton(onClick = { showSwitchDialog = false; onSwitchRole() }) { Text("Да") }
            },
            dismissButton = {
                TextButton(onClick = { showSwitchDialog = false }) { Text("Отмена") }
            }
        )
    }
    errorMessage?.let { LaunchedEffect(it) { viewModel.clearError() } }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun TakeOrderBottomSheet(order: Order, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Взять заказ?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    InfoRow("Адрес", order.address)
                    InfoRow("Дата", dateFormat.format(Date(order.dateTime)))
                    InfoRow("Груз", order.cargoDescription)
                    InfoRow(
                        "Оплата",
                        "${order.pricePerHour.toInt()} ₽/час" +
                                if (order.estimatedHours > 1) " · ~${(order.pricePerHour * order.estimatedHours).toInt()} ₽" else ""
                    )
                    if (order.comment.isNotBlank()) InfoRow("Комментарий", order.comment)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onConfirm() },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Подтвердить", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) { Text("Отмена") }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.35f)
        )
        Text(
            value, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.65f), maxLines = 2, overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RateOrderDialog(onDismiss: () -> Unit, onRate: (Float) -> Unit) {
    var selectedRating by remember { mutableStateOf(0) }
    val haptic = LocalHapticFeedback.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Оцените заказ") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Как прошёл заказ?", color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (i in 1..5) {
                        val isSelected = i <= selectedRating
                        IconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                selectedRating = i
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                if (isSelected) Icons.Default.Star else Icons.Default.StarBorder,
                                "$i звёзд",
                                tint = if (isSelected) GoldStar else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = selectedRating > 0) {
                    Text(
                        when (selectedRating) {
                            1 -> "Плохо"; 2 -> "Неплохо"; 3 -> "Хорошо"
                            4 -> "Очень хорошо"; 5 -> "Отлично! 🎉"; else -> ""
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (selectedRating > 0) onRate(selectedRating.toFloat()) },
                enabled = selectedRating > 0
            ) { Text("Отправить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Пропустить") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun LoaderOrdersContent(
    availableOrders: List<Order>, myOrders: List<Order>, isLoading: Boolean, isRefreshing: Boolean,
    userName: String, selectedTab: Int, activeOrder: Order?, onTabSelected: (Int) -> Unit,
    completedCount: Int = 0,
    onTakeOrder: (Order) -> Unit, onCompleteOrder: (Order) -> Unit,
    onOrderClick: (Order) -> Unit, onRefresh: () -> Unit,
    workerCounts: Map<Long, Int> = emptyMap()
) {
    val activeOrderCount = myOrders.count { it.status == OrderStatus.TAKEN }
    val pagerState = rememberPagerState(initialPage = selectedTab, pageCount = { 2 })
    val scope = rememberCoroutineScope()

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
                        Column {
                            Text("Грузчик", fontWeight = FontWeight.SemiBold)
                            Text(userName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        "Доступные" to (if (availableOrders.isNotEmpty()) "${availableOrders.size}" else null),
                        "Мои заказы" to (if (activeOrderCount > 0 || completedCount > 0) "$activeOrderCount/$completedCount" else null)
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
                                    label, fontSize = 14.sp,
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
        }
    ) { padding ->
        val pullRefreshState = rememberPullRefreshState(refreshing = isRefreshing, onRefresh = onRefresh)
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(modifier = Modifier.fillMaxSize().pullRefresh(pullRefreshState)) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (page) {
                        0 -> AvailableOrdersList(
                            orders = availableOrders, isLoading = isLoading,
                            isRefreshing = isRefreshing, onTakeOrder = onTakeOrder,
                            onOrderClick = onOrderClick, workerCounts = workerCounts
                        )
                        1 -> MyOrdersList(
                            orders = myOrders, isLoading = isLoading,
                            isRefreshing = isRefreshing, activeOrder = activeOrder,
                            onCompleteOrder = onCompleteOrder, onOrderClick = onOrderClick,
                            workerCounts = workerCounts
                        )
                        else -> Unit
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
}

@Composable
fun SkeletonCard() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        0.3f, 0.9f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha"
    )
    val shimmerColor = if (MaterialTheme.colorScheme.surface.value < 0xFF888888u) ShimmerDark else ShimmerLight
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor.copy(alpha)))
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor.copy(alpha * 0.7f)))
            Spacer(modifier = Modifier.height(6.dp))
            Box(modifier = Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(shimmerColor.copy(alpha * 0.6f)))
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth().height(36.dp).clip(RoundedCornerShape(8.dp)).background(shimmerColor.copy(alpha * 0.5f)))
        }
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "empty_float")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow"
    )
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .alpha(appear.value),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha * 0.15f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                modifier = Modifier.size(100.dp),
                shadowElevation = 0.dp
            ) {}
            Icon(
                icon, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                modifier = Modifier.size(48.dp).offset(y = (-floatOffset).dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            subtitle, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun WorkerProgressBadge(current: Int, required: Int, modifier: Modifier = Modifier) {
    val isFull = current >= required
    Surface(
        color = if (isFull) MaterialTheme.colorScheme.primary.copy(0.12f)
        else StatusOrange.copy(0.12f),
        shape = RoundedCornerShape(6.dp),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Group, null,
                tint = if (isFull) MaterialTheme.colorScheme.primary else StatusOrange,
                modifier = Modifier.size(13.dp)
            )
            Text(
                " $current / $required чел.", fontSize = 11.sp,
                color = if (isFull) MaterialTheme.colorScheme.primary else StatusOrange,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ActiveOrderBanner(order: Order, workerCount: Int = 0, onComplete: () -> Unit, onClick: () -> Unit = {}) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) { while (true) { delay(1000); elapsedSeconds++ } }
    val h = elapsedSeconds / 3600; val m = (elapsedSeconds % 3600) / 60; val s = elapsedSeconds % 60
    val timerText = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Активный заказ", fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 14.sp
                    )
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primary.copy(0.15f), shadowElevation = 0.dp) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Timer, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(timerText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(order.address, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "${dateFormat.format(Date(order.dateTime))} · ${order.cargoDescription}",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(0.7f),
                modifier = Modifier.padding(top = 2.dp)
            )
            if (order.requiredWorkers > 1) {
                WorkerProgressBadge(current = workerCount, required = order.requiredWorkers, modifier = Modifier.padding(top = 6.dp))
            }
            Text(
                "${order.pricePerHour.toInt()} ₽/час", fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onComplete() },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Done, null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Завершить заказ", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun AvailableOrderCard(order: Order, workerCount: Int = 0, onTake: () -> Unit, onClick: () -> Unit = {}) {
    val haptic = LocalHapticFeedback.current
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    val accentColor = MaterialTheme.colorScheme.primary
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        elevation = CardDefaults.cardElevation(0.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.10f), Color.Transparent)))
            )
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
                Text(order.address, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(dateFormat.format(Date(order.dateTime)), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("·", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                    Text(
                        com.loaderapp.ui.dispatcher.timeAgo(order.createdAt), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )
                }
                Text(order.cargoDescription, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                if (order.comment.isNotBlank()) {
                    Text("💬 ${order.comment}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                }
                if (order.requiredWorkers > 1 || order.minWorkerRating > 0f) {
                    Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        if (order.requiredWorkers > 1) WorkerProgressBadge(current = workerCount, required = order.requiredWorkers)
                        if (order.minWorkerRating > 0f) {
                            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp), shadowElevation = 0.dp) {
                                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = GoldStar, modifier = Modifier.size(12.dp))
                                    Text(" от ${order.minWorkerRating}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${order.pricePerHour.toInt()} ₽/час", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                    if (order.estimatedHours > 1) {
                        Text(" · ~${order.estimatedHours} ч · ${(order.pricePerHour * order.estimatedHours).toInt()} ₽", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Button(
                    onClick = { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onTake() },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Взять заказ", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun MyOrderCard(order: Order, workerCount: Int = 0, onComplete: () -> Unit, onClick: () -> Unit = {}) {
    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
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
                    .background(Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.10f), Color.Transparent)))
            )
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(order.address, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    LoaderStatusChip(order.status)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(dateFormat.format(Date(order.dateTime)), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("·", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f))
                    Text(
                        com.loaderapp.ui.dispatcher.timeAgo(order.createdAt), fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.7f)
                    )
                }
                Text(order.cargoDescription, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 2.dp))
                Row(modifier = Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${order.pricePerHour.toInt()} ₽/час", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = accentColor)
                    if (order.estimatedHours > 1) {
                        Text(" · ~${(order.pricePerHour * order.estimatedHours).toInt()} ₽", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (order.requiredWorkers > 1) {
                    WorkerProgressBadge(current = workerCount, required = order.requiredWorkers, modifier = Modifier.padding(top = 4.dp))
                }
                if (order.status == OrderStatus.COMPLETED) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                    order.completedAt?.let { completedAt ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(14.dp))
                            Text(
                                " Завершён ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(completedAt))}",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    if (order.estimatedHours > 0) {
                        val earned = (order.pricePerHour * order.estimatedHours).toInt()
                        Surface(color = MaterialTheme.colorScheme.secondary.copy(0.1f), shape = RoundedCornerShape(6.dp), shadowElevation = 0.dp) {
                            Text(
                                "💰 Заработано ~$earned ₽",
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
                order.workerRating?.let { rating ->
                    Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { i ->
                            Icon(
                                if (i < rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder, null,
                                tint = if (i < rating.toInt()) GoldStar else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(" Ваша оценка", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun LoaderStatusChip(status: OrderStatus) {
    val (text, color) = when (status) {
        OrderStatus.AVAILABLE -> "Доступен" to MaterialTheme.colorScheme.primary
        OrderStatus.TAKEN -> "Взят" to StatusOrange
        OrderStatus.IN_PROGRESS -> "В процессе" to StatusOrange
        OrderStatus.COMPLETED -> "Завершён" to MaterialTheme.colorScheme.secondary
        OrderStatus.CANCELLED -> "Отменён" to MaterialTheme.colorScheme.error
    }
    Surface(color = color.copy(0.12f), shape = RoundedCornerShape(6.dp), shadowElevation = 0.dp) {
        Text(
            text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color
        )
    }
}

@Composable
private fun AvailableOrdersList(
    orders: List<Order>, isLoading: Boolean, isRefreshing: Boolean,
    onTakeOrder: (Order) -> Unit, onOrderClick: (Order) -> Unit,
    workerCounts: Map<Long, Int>
) {
    when {
        isLoading && !isRefreshing -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { items(4) { SkeletonCard() } }
        orders.isEmpty() -> EmptyState(
            icon = Icons.Default.Inbox,
            title = "Нет доступных заказов",
            subtitle = "Новые заказы появятся здесь"
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(orders, key = { _, it -> it.id }) { index, order ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(index.toLong() * 50L); visible = true }
                AnimatedVisibility(visible, enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 4 }) {
                    AvailableOrderCard(
                        order = order, workerCount = workerCounts[order.id] ?: 0,
                        onTake = { onTakeOrder(order) }, onClick = { onOrderClick(order) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MyOrdersList(
    orders: List<Order>, isLoading: Boolean, isRefreshing: Boolean,
    activeOrder: Order?, onCompleteOrder: (Order) -> Unit,
    onOrderClick: (Order) -> Unit, workerCounts: Map<Long, Int>
) {
    when {
        isLoading && !isRefreshing -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) { items(4) { SkeletonCard() } }
        orders.isEmpty() -> EmptyState(
            icon = Icons.Default.Assignment,
            title = "Нет ваших заказов",
            subtitle = "Возьмите заказ на вкладке «Доступные»"
        )
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            activeOrder?.let { active ->
                item(key = "banner_${active.id}") {
                    ActiveOrderBanner(
                        order = active,
                        workerCount = workerCounts[active.id] ?: 0,
                        onComplete = { onCompleteOrder(active) },
                        onClick = { onOrderClick(active) }
                    )
                }
            }
            itemsIndexed(
                orders.filter { it.status != OrderStatus.TAKEN },
                key = { _, it -> it.id }
            ) { index, order ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { delay(index.toLong() * 50L); visible = true }
                AnimatedVisibility(visible, enter = fadeIn(tween(280)) + slideInVertically(tween(280)) { it / 4 }) {
                    MyOrderCard(
                        order = order, workerCount = workerCounts[order.id] ?: 0,
                        onComplete = { onCompleteOrder(order) }, onClick = { onOrderClick(order) }
                    )
                }
            }
        }
    }
}
