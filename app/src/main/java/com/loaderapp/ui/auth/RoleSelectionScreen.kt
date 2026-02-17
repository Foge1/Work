package com.loaderapp.ui.auth

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.data.model.User
import com.loaderapp.data.model.UserRole
import com.loaderapp.ui.theme.GoldStar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(onUserCreated: (User) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var showError by remember { mutableStateOf(false) }

    // Staggered появление элементов
    val iconAlpha = remember { Animatable(0f) }
    val iconScale = remember { Animatable(0.7f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(16f) }
    val fieldAlpha = remember { Animatable(0f) }
    val fieldOffset = remember { Animatable(20f) }
    val cardsAlpha = remember { Animatable(0f) }
    val cardsOffset = remember { Animatable(24f) }
    val btnAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        // Ждём завершения crossfade перехода из MainActivity (400ms),
        // только потом запускаем внутренние анимации параллельно
        delay(380)
        launch {
            iconAlpha.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
        launch {
            iconScale.animateTo(1f, spring(Spring.DampingRatioLowBouncy, Spring.StiffnessLow))
        }
        launch {
            delay(100)
            titleAlpha.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
            titleOffset.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
        }
        launch {
            delay(180)
            fieldAlpha.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
            fieldOffset.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
        }
        launch {
            delay(260)
            cardsAlpha.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
            cardsOffset.animateTo(0f, tween(350, easing = FastOutSlowInEasing))
        }
        launch {
            delay(340)
            btnAlpha.animateTo(1f, tween(350, easing = FastOutSlowInEasing))
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(52.dp))

            // Иконка с анимацией
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(80.dp)
                    .scale(iconScale.value)
                    .alpha(iconAlpha.value)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ГрузчикиApp",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .alpha(titleAlpha.value)
                    .offset(y = titleOffset.value.dp)
            )
            Text(
                text = "Сервис поиска грузчиков",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(bottom = 36.dp)
                    .alpha(titleAlpha.value)
                    .offset(y = titleOffset.value.dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; if (showError && it.isNotBlank()) showError = false },
                label = { Text("Ваше имя") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(fieldAlpha.value)
                    .offset(y = fieldOffset.value.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, keyboardType = KeyboardType.Text),
                isError = showError && name.isBlank(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Выберите роль",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .alpha(cardsAlpha.value)
                    .offset(y = cardsOffset.value.dp)
            )

            RoleCard(
                icon = Icons.Default.SupportAgent,
                title = "Диспетчер",
                description = "Создавайте заказы и управляйте грузчиками",
                selected = selectedRole == UserRole.DISPATCHER,
                onClick = { selectedRole = UserRole.DISPATCHER },
                modifier = Modifier
                    .alpha(cardsAlpha.value)
                    .offset(y = cardsOffset.value.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            RoleCard(
                icon = Icons.Default.LocalShipping,
                title = "Грузчик",
                description = "Принимайте заказы и зарабатывайте",
                selected = selectedRole == UserRole.LOADER,
                onClick = { selectedRole = UserRole.LOADER },
                modifier = Modifier
                    .alpha(cardsAlpha.value)
                    .offset(y = cardsOffset.value.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = showError) {
                Text(
                    text = if (name.isBlank()) "Введите ваше имя" else "Выберите роль для продолжения",
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && selectedRole != null) {
                        onUserCreated(User(name = name.trim(), phone = "", role = selectedRole!!))
                    } else {
                        showError = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .alpha(btnAlpha.value),
                shape = RoundedCornerShape(14.dp),
                enabled = name.isNotBlank() || selectedRole != null
            ) {
                Text("Продолжить", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector,
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderColor by animateColorAsState(
        targetValue = if (selected) primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        animationSpec = tween(220), label = "border"
    )
    val bgColor by animateColorAsState(
        targetValue = if (selected) primary.copy(alpha = 0.08f) else Color.Transparent,
        animationSpec = tween(220), label = "bg"
    )
    val iconTint by animateColorAsState(
        targetValue = if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(220), label = "icon"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.01f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy), label = "scale"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .then(
                Modifier.clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    onClick = onClick
                )
            ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(width = if (selected) 2.dp else 1.dp, color = borderColor),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (selected) primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (selected) primary else MaterialTheme.colorScheme.onSurface)
                Text(text = description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            }
            AnimatedVisibility(visible = selected, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(), exit = scaleOut() + fadeOut()) {
                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = primary, modifier = Modifier.size(22.dp).padding(start = 4.dp))
            }
        }
    }
}
