package com.loaderapp.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.LoaderApplication
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onMenuClick: () -> Unit,
    onBackClick: () -> Unit,
    onDarkThemeChanged: ((Boolean) -> Unit)? = null,
    onSwitchRole: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val app = remember(context) { context.applicationContext as LoaderApplication }
    val scope = rememberCoroutineScope()

    val darkThemeEnabled by app.userPreferences.isDarkTheme.collectAsState(initial = false)
    val notificationsEnabled by app.userPreferences.isNotificationsEnabled.collectAsState(initial = true)
    val soundEnabled by app.userPreferences.isSoundEnabled.collectAsState(initial = true)
    val vibrationEnabled by app.userPreferences.isVibrationEnabled.collectAsState(initial = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
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
            Spacer(modifier = Modifier.height(8.dp))

            SettingsSectionHeader("Внешний вид")

            SettingsToggleItem(
                icon = Icons.Default.DarkMode,
                title = "Тёмная тема",
                subtitle = "Тёмное оформление интерфейса",
                checked = darkThemeEnabled,
                onCheckedChange = { enabled ->
                    scope.launch {
                        app.userPreferences.setDarkTheme(enabled)
                        onDarkThemeChanged?.invoke(enabled)
                    }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSectionHeader("Уведомления")

            SettingsToggleItem(
                icon = Icons.Default.Notifications,
                title = "Уведомления",
                subtitle = "Получать уведомления о новых заказах",
                checked = notificationsEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { app.userPreferences.setNotificationsEnabled(enabled) }
                }
            )

            SettingsToggleItem(
                icon = Icons.Default.VolumeUp,
                title = "Звук уведомлений",
                subtitle = "Звук при новых заказах",
                checked = soundEnabled && notificationsEnabled,
                enabled = notificationsEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { app.userPreferences.setSoundEnabled(enabled) }
                }
            )

            SettingsToggleItem(
                icon = Icons.Default.Vibration,
                title = "Вибрация",
                subtitle = "Вибрация при новых уведомлениях",
                checked = vibrationEnabled && notificationsEnabled,
                enabled = notificationsEnabled,
                onCheckedChange = { enabled ->
                    scope.launch { app.userPreferences.setVibrationEnabled(enabled) }
                }
            )

            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            SettingsSectionHeader("О приложении")

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Версия приложения", fontWeight = FontWeight.Medium)
                    Text(
                        text = "GruzchikiApp 9.4",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Блок "Аккаунт" — смена роли
            if (onSwitchRole != null) {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                SettingsSectionHeader("Аккаунт")

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSwitchRole() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Сменить роль", fontWeight = FontWeight.Medium)
                        Text(
                            text = "Переключиться между диспетчером и грузчиком",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        letterSpacing = 0.8.sp
    )
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}
