package com.loaderapp.ui.order

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.data.model.ChatMessage
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.User
import com.loaderapp.data.model.UserRole
import com.loaderapp.data.repository.AppRepository
import com.loaderapp.notification.NotificationHelper
import com.loaderapp.ui.theme.GoldStar
import com.loaderapp.ui.theme.StatusOrange
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    order: Order,
    dispatcher: User?,
    worker: User?,
    currentUser: User?,
    repository: AppRepository,
    onBack: () -> Unit,
    onTakeOrder: ((Order) -> Unit)? = null,
    onCompleteOrder: ((Order) -> Unit)? = null,
    onCancelOrder: ((Order) -> Unit)? = null,
    isDispatcher: Boolean = false,
    workerCount: Int = 0
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val clipboard = LocalClipboardManager.current
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru"))
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val notificationHelper = remember { NotificationHelper(context) }

    BackHandler { onBack() }

    // Chat state
    val messages by repository.getMessagesForOrder(order.id).collectAsState(initial = emptyList())
    var messageText by remember { mutableStateOf("") }
    val chatListState = rememberLazyListState()
    var chatExpanded by remember { mutableStateOf(false) }
    var lastKnownMessageCount by remember { mutableIntStateOf(0) }

    // Уведомление при новом входящем сообщении
    LaunchedEffect(messages.size) {
        if (messages.size > lastKnownMessageCount && lastKnownMessageCount > 0) {
            val newest = messages.last()
            if (newest.senderId != currentUser?.id) {
                notificationHelper.sendChatMessageNotification(order.address, newest.senderName, newest.text)
            }
        }
        lastKnownMessageCount = messages.size
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty() && chatExpanded) {
            chatListState.animateScrollToItem(messages.size - 1)
        }
    }

    // Staggered entrance animations
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(520, easing = FastOutSlowInEasing))
    }

    fun blockAlpha(start: Float, end: Float) =
        ((progress.value - start) / (end - start)).coerceIn(0f, 1f)
    fun blockOffset(start: Float, end: Float, from: Float = 24f) =
        from * (1f - ((progress.value - start) / (end - start)).coerceIn(0f, 1f))

    val headerAlphaVal = blockAlpha(0f, 0.45f)
    val headerOffsetVal = blockOffset(0f, 0.45f, 30f)
    val contentAlphaVal = blockAlpha(0.2f, 0.72f)
    val contentOffsetVal = blockOffset(0.2f, 0.72f, 24f)
    val actionsAlphaVal = blockAlpha(0.42f, 0.92f)
    val actionsOffsetVal = blockOffset(0.42f, 0.92f, 20f)

    val accentColor = when (order.status) {
        OrderStatus.AVAILABLE -> MaterialTheme.colorScheme.primary
        OrderStatus.TAKEN, OrderStatus.IN_PROGRESS -> StatusOrange
        OrderStatus.COMPLETED -> MaterialTheme.colorScheme.secondary
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
    }

    fun sendMessage() {
        val text = messageText.trim()
        if (text.isBlank() || currentUser == null) return
        messageText = ""
        scope.launch {
            repository.sendMessage(
                ChatMessage(
                    orderId = order.id,
                    senderId = currentUser.id,
                    senderName = currentUser.name,
                    senderRole = currentUser.role,
                    text = text
                )
            )
            if (messages.isNotEmpty()) {
                chatListState.animateScrollToItem(messages.size)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заказ #${order.id}", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            // Hero-блок с адресом и статусом
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(headerAlphaVal)
                    .offset(y = headerOffsetVal.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(accentColor.copy(alpha = 0.12f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                ) {
                    Column {
                        Surface(
                            color = accentColor.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .background(accentColor, CircleShape)
                                )
                                Text(
                                    text = statusLabel(order.status),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = accentColor
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = order.address,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 28.sp,
                                modifier = Modifier.weight(1f)
                            )
                            var addressCopied by remember { mutableStateOf(false) }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(order.address))
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    addressCopied = true
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    if (addressCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                                    contentDescription = "Скопировать адрес",
                                    tint = if (addressCopied) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${order.pricePerHour.toInt()} ₽/час",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor
                            )
                            if (order.estimatedHours > 1) {
                                Text(
                                    text = "  ·  ~${(order.pricePerHour * order.estimatedHours).toInt()} ₽",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }

                        if (order.requiredWorkers > 1) {
                            Spacer(modifier = Modifier.height(8.dp))
                            com.loaderapp.ui.loader.WorkerProgressBadge(
                                current = workerCount,
                                required = order.requiredWorkers
                            )
                        }
                    }
                }
            }

            // Основная информация
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .alpha(contentAlphaVal)
                    .offset(y = contentOffsetVal.dp)
            ) {
                Spacer(modifier = Modifier.height(4.dp))

                DetailSection(title = "Детали заказа") {
                    DetailRow(
                        icon = Icons.Default.CalendarMonth,
                        label = "Дата и время",
                        value = dateFormat.format(Date(order.dateTime)),
                        color = accentColor
                    )
                    DetailRow(
                        icon = Icons.Default.Inventory,
                        label = "Описание груза",
                        value = order.cargoDescription,
                        color = accentColor
                    )
                    if (order.requiredWorkers > 1) {
                        DetailRow(
                            icon = Icons.Default.Group,
                            label = "Грузчиков нужно",
                            value = "${order.requiredWorkers} чел.",
                            color = accentColor
                        )
                    }
                    if (order.minWorkerRating > 0f) {
                        DetailRow(
                            icon = Icons.Default.Star,
                            label = "Минимальный рейтинг",
                            value = "от ${order.minWorkerRating}",
                            color = accentColor
                        )
                    }
                    if (order.comment.isNotBlank()) {
                        DetailRow(
                            icon = Icons.Default.Comment,
                            label = "Комментарий",
                            value = order.comment,
                            color = accentColor
                        )
                    }
                }

                // Оценка
                order.workerRating?.let { rating ->
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailSection(title = "Оценка работы") {
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            repeat(5) { i ->
                                Icon(
                                    if (i < rating.toInt()) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = null,
                                    tint = if (i < rating.toInt()) GoldStar else MaterialTheme.colorScheme.onSurfaceVariant.copy(0.3f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${"%.1f".format(rating)} / 5.0",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = GoldStar
                            )
                        }
                    }
                }

                // Контакты
                if (dispatcher != null || worker != null) {
                    Spacer(modifier = Modifier.height(16.dp))
                    DetailSection(title = "Участники") {
                        dispatcher?.let { d ->
                            ContactRow(
                                name = d.name,
                                phone = d.phone,
                                role = "Диспетчер",
                                color = MaterialTheme.colorScheme.primary,
                                context = context
                            )
                        }
                        worker?.let { w ->
                            if (dispatcher != null) {
                                Spacer(modifier = Modifier.height(10.dp))
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            ContactRow(
                                name = w.name,
                                phone = w.phone,
                                role = "Грузчик",
                                color = StatusOrange,
                                context = context
                            )
                        }
                    }
                }

                // ── ЧАТ ──────────────────────────────────────────────────
                Spacer(modifier = Modifier.height(16.dp))

                // Заголовок секции чата с кнопкой раскрытия
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Шапка чата — всегда видна
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ЧАТ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                letterSpacing = 1.sp
                            )
                            if (messages.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "${messages.size}",
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = { chatExpanded = !chatExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (chatExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (chatExpanded) "Свернуть" else "Развернуть",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Содержимое чата (раскрывается)
                        AnimatedVisibility(
                            visible = chatExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f)
                                )

                                // Список сообщений
                                if (messages.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(
                                                Icons.Default.ChatBubbleOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                "Сообщений пока нет",
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                                            )
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        state = chatListState,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 300.dp),
                                        contentPadding = PaddingValues(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(messages, key = { it.id }) { msg ->
                                            val isOwn = msg.senderId == currentUser?.id
                                            ChatBubble(
                                                message = msg,
                                                isOwn = isOwn,
                                                accentColor = if (msg.senderRole == UserRole.DISPATCHER)
                                                    MaterialTheme.colorScheme.primary
                                                else StatusOrange
                                            )
                                        }
                                    }
                                }

                                // Поле ввода
                                if (currentUser != null) {
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(0.4f)
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = messageText,
                                            onValueChange = { messageText = it },
                                            modifier = Modifier.weight(1f),
                                            placeholder = {
                                                Text(
                                                    "Сообщение...",
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                                                )
                                            },
                                            maxLines = 3,
                                            shape = RoundedCornerShape(12.dp),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                            keyboardActions = KeyboardActions(onSend = { sendMessage() }),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                            )
                                        )
                                        IconButton(
                                            onClick = { sendMessage() },
                                            enabled = messageText.isNotBlank(),
                                            modifier = Modifier
                                                .size(44.dp)
                                                .background(
                                                    if (messageText.isNotBlank())
                                                        MaterialTheme.colorScheme.primary
                                                    else
                                                        MaterialTheme.colorScheme.surfaceVariant,
                                                    shape = CircleShape
                                                )
                                        ) {
                                            Icon(
                                                Icons.Default.Send,
                                                contentDescription = "Отправить",
                                                tint = if (messageText.isNotBlank())
                                                    MaterialTheme.colorScheme.onPrimary
                                                else
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(0.4f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                // ── КОНЕЦ ЧАТА ───────────────────────────────────────────

                // Кнопки действий
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp)
                        .alpha(actionsAlphaVal)
                        .offset(y = actionsOffsetVal.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!isDispatcher && order.status == OrderStatus.AVAILABLE && onTakeOrder != null) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTakeOrder(order)
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Взять заказ", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (!isDispatcher && order.status == OrderStatus.TAKEN && onCompleteOrder != null) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCompleteOrder(order)
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(Icons.Default.Done, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Завершить заказ", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (isDispatcher && order.status == OrderStatus.AVAILABLE && onCancelOrder != null) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onCancelOrder(order)
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp, MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Отменить заказ", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ChatBubble(
    message: ChatMessage,
    isOwn: Boolean,
    accentColor: Color
) {
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale("ru")) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwn) Alignment.End else Alignment.Start
    ) {
        // Имя отправителя (только для чужих)
        if (!isOwn) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = accentColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            message.senderName.take(1).uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
                Text(
                    text = message.senderName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = if (isOwn) 14.dp else 4.dp,
                topEnd = if (isOwn) 4.dp else 14.dp,
                bottomStart = 14.dp,
                bottomEnd = 14.dp
            ),
            color = if (isOwn)
                accentColor.copy(alpha = 0.85f)
            else
                MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.text,
                    fontSize = 14.sp,
                    color = if (isOwn) Color.White else MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = timeFormat.format(Date(message.sentAt)),
                    fontSize = 10.sp,
                    color = if (isOwn)
                        Color.White.copy(alpha = 0.7f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun DetailSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        text = title.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(bottom = 10.dp, start = 2.dp)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = color.copy(alpha = 0.12f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun ContactRow(
    name: String,
    phone: String,
    role: String,
    color: Color,
    context: Context
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(46.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(1).uppercase(),
                    color = color,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(text = name, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = color.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = role,
                        fontSize = 10.sp,
                        color = color,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            if (phone.isNotBlank()) {
                Text(
                    text = phone,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (phone.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Phone, "Позвонить", tint = color, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")))
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Message, "SMS",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private fun statusLabel(status: OrderStatus): String = when (status) {
    OrderStatus.AVAILABLE -> "Доступен"
    OrderStatus.TAKEN -> "В работе"
    OrderStatus.IN_PROGRESS -> "В процессе"
    OrderStatus.COMPLETED -> "Завершён"
    OrderStatus.CANCELLED -> "Отменён"
}
