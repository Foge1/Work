package com.loaderapp.ui.dispatcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.loaderapp.ui.theme.GoldStar
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderScreen(
    onBack: () -> Unit,
    onCreate: (address: String, dateTime: Long, cargo: String, price: Double,
               hours: Int, comment: String, requiredWorkers: Int, minRating: Float) -> Unit
) {
    var address by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var estimatedHours by remember { mutableStateOf("1") }
    var comment by remember { mutableStateOf("") }
    var requiredWorkers by remember { mutableIntStateOf(1) }
    var minWorkerRating by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }
    var errorFields by remember { mutableStateOf(setOf<String>()) }

    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(calendar.timeInMillis) }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))
    val primary = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Новый заказ", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Назад")
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- Адрес ---
            AppField(
                icon = Icons.Default.LocationOn,
                label = "Адрес *",
                value = address,
                onValueChange = { address = it; errorFields = errorFields - "address" },
                placeholder = "Например: ул. Ленина, 15",
                isError = "address" in errorFields,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // --- Дата и время ---
            SectionLabel("Дата и время")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppPickerButton(
                    icon = Icons.Default.DateRange,
                    label = dateFormat.format(Date(selectedDate)),
                    modifier = Modifier.weight(1f),
                    onClick = { showDatePicker = true }
                )
                AppPickerButton(
                    icon = Icons.Default.AccessTime,
                    label = String.format("%02d:%02d", selectedHour, selectedMinute),
                    modifier = Modifier.weight(0.7f),
                    onClick = { showTimePicker = true }
                )
            }

            // --- Груз ---
            AppField(
                icon = Icons.Default.Inventory,
                label = "Описание груза *",
                value = cargo,
                onValueChange = { cargo = it; errorFields = errorFields - "cargo" },
                placeholder = "Что нужно перевезти",
                isError = "cargo" in errorFields,
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // --- Цена и часы ---
            SectionLabel("Стоимость")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppField(
                    icon = Icons.Default.AttachMoney,
                    label = "₽/час *",
                    value = price,
                    onValueChange = { price = it; errorFields = errorFields - "price" },
                    placeholder = "0",
                    isError = "price" in errorFields,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                AppField(
                    icon = Icons.Default.Timer,
                    label = "Часов",
                    value = estimatedHours,
                    onValueChange = { estimatedHours = it },
                    placeholder = "1",
                    modifier = Modifier.weight(0.65f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            // Итоговая сумма
            val priceVal = price.toDoubleOrNull() ?: 0.0
            val hoursVal = estimatedHours.toIntOrNull() ?: 1
            if (priceVal > 0 && hoursVal > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(primary.copy(alpha = 0.08f))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Итого за ~$hoursVal ч:", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${(priceVal * hoursVal).toInt()} ₽",
                        fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = primary
                    )
                }
            }

            // --- Количество грузчиков ---
            SectionLabel("Количество грузчиков")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = { if (requiredWorkers > 1) requiredWorkers-- },
                    enabled = requiredWorkers > 1,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Default.Remove, null,
                        tint = if (requiredWorkers > 1) primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = requiredWorkers.toString(),
                        fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = primary
                    )
                    Text("чел.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                    onClick = { if (requiredWorkers < 20) requiredWorkers++ },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Default.Add, null, tint = primary)
                }
            }

            // --- Минимальный рейтинг ---
            SectionLabel("Минимальный рейтинг грузчика")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.Star, null, tint = GoldStar, modifier = Modifier.size(22.dp))
                Column(modifier = Modifier.weight(1f)) {
                    val ratingDisplay = minWorkerRating.toFloatOrNull()
                    Text(
                        text = if (ratingDisplay != null) String.format("%.1f", ratingDisplay) else "Не указан",
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        color = if (ratingDisplay != null) primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("Нажмите на звёзды", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                // 5 звёзд для выбора рейтинга
                Row {
                    for (i in 1..5) {
                        val filled = (minWorkerRating.toFloatOrNull() ?: 0f) >= i
                        Icon(
                            Icons.Default.Star, null,
                            tint = if (filled) GoldStar else MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable {
                                    val current = minWorkerRating.toFloatOrNull() ?: 0f
                                    minWorkerRating = if (current.toInt() == i) "" else i.toString()
                                }
                        )
                    }
                }
            }

            // --- Комментарий ---
            AppField(
                icon = Icons.Default.Comment,
                label = "Комментарий",
                value = comment,
                onValueChange = { comment = it },
                placeholder = "Дополнительная информация",
                maxLines = 3,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            if (showError) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Error, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    Text("Заполните все обязательные поля", color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                }
            }

            // --- Кнопка создать ---
            Button(
                onClick = {
                    val errors = mutableSetOf<String>()
                    if (address.isBlank()) errors += "address"
                    if (cargo.isBlank()) errors += "cargo"
                    if (price.isBlank()) errors += "price"
                    errorFields = errors
                    if (errors.isNotEmpty()) { showError = true; return@Button }
                    val p = price.toDoubleOrNull() ?: 0.0
                    val h = estimatedHours.toIntOrNull() ?: 1
                    val r = minWorkerRating.toFloatOrNull()?.coerceIn(0f, 5f) ?: 0f
                    if (p <= 0 || h <= 0) { showError = true; return@Button }
                    val cal = Calendar.getInstance()
                    cal.timeInMillis = selectedDate
                    cal.set(Calendar.HOUR_OF_DAY, selectedHour)
                    cal.set(Calendar.MINUTE, selectedMinute)
                    cal.set(Calendar.SECOND, 0)
                    onCreate(address.trim(), cal.timeInMillis, cargo.trim(), p, h, comment.trim(), requiredWorkers, r)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Создать заказ", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val dpState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { dpState.selectedDateMillis?.let { selectedDate = it }; showDatePicker = false }) { Text("ОК") } },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = dpState) }
    }

    if (showTimePicker) {
        val tpState = rememberTimePickerState(initialHour = selectedHour, initialMinute = selectedMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = { TextButton(onClick = { selectedHour = tpState.hour; selectedMinute = tpState.minute; showTimePicker = false }) { Text("ОК") } },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Отмена") } },
            text = { TimePicker(state = tpState) }
        )
    }
}

// Стилизованное текстовое поле в дизайне приложения
@Composable
fun AppField(
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    isError: Boolean = false,
    maxLines: Int = 1,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val primary = MaterialTheme.colorScheme.primary
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        value.isNotEmpty() -> primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) MaterialTheme.colorScheme.error else primary,
            modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = if (maxLines > 1) 10.dp else 4.dp),
            verticalAlignment = if (maxLines > 1) Alignment.Top else Alignment.CenterVertically
        ) {
            Icon(
                icon, null,
                tint = if (value.isNotEmpty()) primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .size(20.dp)
                    .padding(top = if (maxLines > 1) 4.dp else 0.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicAppTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = placeholder,
                maxLines = maxLines,
                keyboardOptions = keyboardOptions
            )
        }
        if (isError) {
            Text(
                "Обязательное поле",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 3.dp, start = 4.dp)
            )
        }
    }
}

@Composable
private fun BasicAppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    maxLines: Int,
    keyboardOptions: KeyboardOptions
) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        maxLines = maxLines,
        singleLine = maxLines == 1,
        keyboardOptions = keyboardOptions,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp
        ),
        modifier = Modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                inner()
            }
        }
    )
}

// Кнопка-пикер для даты/времени
@Composable
fun AppPickerButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .border(1.5.dp, primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .background(primary.copy(alpha = 0.06f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, null, tint = primary, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = primary)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 2.dp)
    )
}
