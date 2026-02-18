package com.loaderapp.ui.rating

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RatingScreen(
    userName: String,
    userRating: Double,
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit,
    // Stats for loader
    completedCount: Int = 0,
    totalEarnings: Double = 0.0,
    averageRating: Float = 0f,
    // Stats for dispatcher
    dispatcherCompletedCount: Int = 0,
    dispatcherActiveCount: Int = 0,
    isDispatcher: Boolean = false
) {
    val primary = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рейтинг") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Avatar circle
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(primary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = primary
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = userName,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (isDispatcher) "Диспетчер" else "Грузчик",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!isDispatcher) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Star rating display
                        val displayRating = if (averageRating > 0f) averageRating else userRating.toFloat()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(5) { index ->
                                val filled = index < displayRating.toInt()
                                val halfFilled = !filled && index < displayRating + 0.5f
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = if (filled || halfFilled) Color(0xFFFFC107)
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Text(
                            text = String.format("%.1f", displayRating),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Text(
                            text = "Средний рейтинг",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stats cards
            Text(
                text = "Статистика",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (!isDispatcher) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Выполнено",
                        value = completedCount.toString(),
                        unit = "заказов",
                        color = primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Заработано",
                        value = if (totalEarnings > 0) totalEarnings.toInt().toString() else "0",
                        unit = "₽ всего",
                        color = Color(0xFF4CAF50)
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Выполнено",
                        value = dispatcherCompletedCount.toString(),
                        unit = "заказов",
                        color = primary
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        title = "Активных",
                        value = dispatcherActiveCount.toString(),
                        unit = "сейчас",
                        color = Color(0xFFE67E22)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = primary.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Как формируется рейтинг?",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isDispatcher)
                            "Ваш показатель основан на количестве успешно выполненных заказов и качестве работы с грузчиками."
                        else
                            "Рейтинг формируется на основе оценок за выполненные заказы. Чем выше рейтинг, тем больше приоритетных заказов доступно.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    color: Color
) {
    // Animated value
    val animatedValue by animateFloatAsState(
        targetValue = value.toFloatOrNull() ?: 0f,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "stat_anim"
    )

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (value.toFloatOrNull() != null) animatedValue.toInt().toString() else value,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = unit,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
