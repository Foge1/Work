package com.loaderapp.ui.order

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.data.model.Order
import com.loaderapp.data.model.OrderStatus
import com.loaderapp.data.model.User
import com.loaderapp.ui.theme.GoldStar
import com.loaderapp.ui.theme.StatusOrange
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(
    order: Order,
    dispatcher: User?,
    worker: User?,
    onBack: () -> Unit,
    onTakeOrder: ((Order) -> Unit)? = null,
    onCompleteOrder: ((Order) -> Unit)? = null,
    onCancelOrder: ((Order) -> Unit)? = null,
    isDispatcher: Boolean = false
) {
    val context = LocalContext.current
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru"))
    val scrollState = rememberScrollState()

    // Staggered entrance animations
    // Единый прогресс анимации — без coroutineScope/launch внутри LaunchedEffect
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
                        // Статус-бейдж
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

                        Text(
                            text = order.address,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 28.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Цена
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${order.pricePerHour.toInt()} ₽/час",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor
                            )
                            if (order.estimatedHours > 1) {
                                Text(
                                    text = "  ·  ~${order.estimatedHours} ч  ·  ${(order.pricePerHour * order.estimatedHours).toInt()} ₽",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
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
                    DetailRow(
                        icon = Icons.Default.AccessTime,
                        label = "Ожидаемое время",
                        value = if (order.estimatedHours > 1) "~${order.estimatedHours} часов" else "~1 час",
                        color = accentColor
                    )
                    if (order.comment.isNotBlank()) {
                        DetailRow(
                            icon = Icons.Default.Comment,
                            label = "Комментарий",
                            value = order.comment,
                            color = accentColor
                        )
                    }
                }

                // Оценка (если есть)
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

                // Кнопки действий
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp)
                        .alpha(actionsAlphaVal)
                        .offset(y = actionsOffsetVal.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Грузчик: взять заказ
                    if (!isDispatcher && order.status == OrderStatus.AVAILABLE && onTakeOrder != null) {
                        Button(
                            onClick = { onTakeOrder(order) },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Взять заказ", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Грузчик: завершить заказ
                    if (!isDispatcher && order.status == OrderStatus.TAKEN && onCompleteOrder != null) {
                        Button(
                            onClick = { onCompleteOrder(order) },
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

                    // Диспетчер: отменить заказ
                    if (isDispatcher && order.status == OrderStatus.AVAILABLE && onCancelOrder != null) {
                        OutlinedButton(
                            onClick = { onCancelOrder(order) },
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
                Text(
                    text = name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
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

        // Кнопки звонка и SMS
        if (phone.isNotBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                        )
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Позвонить",
                        tint = color,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone"))
                        )
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Message,
                        contentDescription = "SMS",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
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
