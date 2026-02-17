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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.data.model.User
import com.loaderapp.data.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoleSelectionScreen(onUserCreated: (User) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    var showError by remember { mutableStateOf(false) }

    // Единый Animatable прогресс 0->1, без delay-ов и параллельных job-ов
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(560, easing = FastOutSlowInEasing))
    }

    fun blockAlpha(start: Float, end: Float) =
        ((progress.value - start) / (end - start)).coerceIn(0f, 1f)
    fun blockOffset(start: Float, end: Float, from: Float = 22f) =
        from * (1f - ((progress.value - start) / (end - start)).coerceIn(0f, 1f))

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(52.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier
                    .size(80.dp)
                    .scale(0.72f + 0.28f * blockAlpha(0f, 0.42f))
                    .alpha(blockAlpha(0f, 0.38f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.LocalShipping, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                "ГрузчикиApp", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(blockAlpha(0.12f, 0.52f)).offset(y = blockOffset(0.12f, 0.52f).dp)
            )
            Text(
                "Сервис поиска грузчиков", fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 36.dp).alpha(blockAlpha(0.12f, 0.52f)).offset(y = blockOffset(0.12f, 0.52f).dp)
            )

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; if (showError && it.isNotBlank()) showError = false },
                label = { Text("Ваше имя") },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                modifier = Modifier.fillMaxWidth().alpha(blockAlpha(0.25f, 0.62f)).offset(y = blockOffset(0.25f, 0.62f).dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, keyboardType = KeyboardType.Text),
                isError = showError && name.isBlank(),
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                "Выберите роль", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                    .alpha(blockAlpha(0.38f, 0.72f)).offset(y = blockOffset(0.38f, 0.72f).dp)
            )

            RoleCard(
                icon = Icons.Default.SupportAgent, title = "Диспетчер",
                description = "Создавайте заказы и управляйте грузчиками",
                selected = selectedRole == UserRole.DISPATCHER,
                onClick = { selectedRole = UserRole.DISPATCHER },
                modifier = Modifier.alpha(blockAlpha(0.40f, 0.74f)).offset(y = blockOffset(0.40f, 0.74f).dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            RoleCard(
                icon = Icons.Default.LocalShipping, title = "Грузчик",
                description = "Принимайте заказы и зарабатывайте",
                selected = selectedRole == UserRole.LOADER,
                onClick = { selectedRole = UserRole.LOADER },
                modifier = Modifier.alpha(blockAlpha(0.46f, 0.78f)).offset(y = blockOffset(0.46f, 0.78f).dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(visible = showError) {
                Text(
                    text = if (name.isBlank()) "Введите ваше имя" else "Выберите роль для продолжения",
                    color = MaterialTheme.colorScheme.error, fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Button(
                onClick = {
                    if (name.isNotBlank() && selectedRole != null) {
                        onUserCreated(User(name = name.trim(), phone = "", role = selectedRole!!))
                    } else showError = true
                },
                modifier = Modifier.fillMaxWidth().height(54.dp)
                    .alpha(blockAlpha(0.55f, 0.88f)).offset(y = blockOffset(0.55f, 0.88f).dp),
                shape = RoundedCornerShape(14.dp),
                enabled = name.isNotBlank() || selectedRole != null
            ) {
                Text("Продолжить", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun RoleCard(
    icon: ImageVector, title: String, description: String,
    selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderColor by animateColorAsState(if (selected) primary else MaterialTheme.colorScheme.outline.copy(0.35f), tween(200), label = "border")
    val bgColor by animateColorAsState(if (selected) primary.copy(0.08f) else Color.Transparent, tween(200), label = "bg")
    val iconTint by animateColorAsState(if (selected) primary else MaterialTheme.colorScheme.onSurfaceVariant, tween(200), label = "icon")
    val scale by animateFloatAsState(if (selected) 1.01f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "scale")

    Card(
        modifier = modifier.fillMaxWidth().scale(scale).clickable(
            indication = null,
            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
            onClick = onClick
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = if (selected) primary.copy(0.12f) else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (selected) primary else MaterialTheme.colorScheme.onSurface)
                Text(description, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp))
            }
            AnimatedVisibility(selected, enter = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(), exit = scaleOut() + fadeOut()) {
                Icon(Icons.Default.CheckCircle, null, tint = primary, modifier = Modifier.size(22.dp).padding(start = 4.dp))
            }
        }
    }
}
