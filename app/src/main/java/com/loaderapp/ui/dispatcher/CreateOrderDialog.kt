package com.loaderapp.ui.dispatcher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateOrderDialog(
    onDismiss: () -> Unit,
    onCreate: (address: String, dateTime: Long, cargo: String, price: Double, hours: Int, comment: String, requiredWorkers: Int, minWorkerRating: Float) -> Unit
) {
    var address by remember { mutableStateOf("") }
    var cargo by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var estimatedHours by remember { mutableStateOf("1") }
    var comment by remember { mutableStateOf("") }
    var requiredWorkers by remember { mutableIntStateOf(1) }
    var minWorkerRating by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    var selectedDate by remember { mutableStateOf(calendar.timeInMillis) }
    var selectedHour by remember { mutableStateOf(calendar.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableStateOf(calendar.get(Calendar.MINUTE)) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("ru"))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый заказ") },
        containerColor = MaterialTheme.colorScheme.surface,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Адрес") },
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    )
                )

                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.DateRange, null, modifier = Modifier.padding(end = 8.dp))
                    Text(dateFormat.format(Date(selectedDate)))
                }

                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.AccessTime, null, modifier = Modifier.padding(end = 8.dp))
                    Text(String.format("%02d:%02d", selectedHour, selectedMinute))
                }

                OutlinedTextField(
                    value = cargo,
                    onValueChange = { cargo = it },
                    label = { Text("Описание груза") },
                    leadingIcon = { Icon(Icons.Default.Inventory, null) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text("₽/час") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                    OutlinedTextField(
                        value = estimatedHours,
                        onValueChange = { estimatedHours = it },
                        label = { Text("Часов") },
                        modifier = Modifier.weight(0.6f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }

                // Количество грузчиков
                HorizontalDivider()
                Text(
                    "Количество грузчиков",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { if (requiredWorkers > 1) requiredWorkers-- },
                        enabled = requiredWorkers > 1
                    ) {
                        Icon(Icons.Default.Remove, null)
                    }
                    Text(
                        text = requiredWorkers.toString(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { if (requiredWorkers < 20) requiredWorkers++ }
                    ) {
                        Icon(Icons.Default.Add, null)
                    }
                    Text(
                        text = if (requiredWorkers == 1) "чел." else "чел.",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Минимальный рейтинг
                HorizontalDivider()
                Text(
                    "Минимальный рейтинг грузчика",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = minWorkerRating,
                    onValueChange = { input ->
                        // Пропускаем только цифры, точку и запятую; не даём ввести > 5
                        val filtered = input.replace(',', '.').filter { it.isDigit() || it == '.' }
                        val num = filtered.toFloatOrNull()
                        if (filtered.isEmpty() || (num != null && num <= 5f) || filtered == ".")
                            minWorkerRating = filtered
                    },
                    label = { Text("От 0 до 5 (необязательно)") },
                    leadingIcon = { Icon(Icons.Default.Star, null, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (minWorkerRating.isNotEmpty()) {
                            IconButton(onClick = { minWorkerRating = "" }) {
                                Icon(Icons.Default.Clear, null, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = {
                        val v = minWorkerRating.toFloatOrNull()
                        if (minWorkerRating.isNotEmpty() && (v == null || v < 0 || v > 5)) {
                            Text("Введите число от 0 до 5", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                if (showError) {
                    Text(
                        "Заполните все обязательные поля корректно",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                if (address.isNotBlank() && cargo.isNotBlank() && price.isNotBlank()) {
                    try {
                        val cal = Calendar.getInstance()
                        cal.timeInMillis = selectedDate
                        cal.set(Calendar.HOUR_OF_DAY, selectedHour)
                        cal.set(Calendar.MINUTE, selectedMinute)
                        cal.set(Calendar.SECOND, 0)
                        val priceValue = price.toDoubleOrNull() ?: 0.0
                        val hoursValue = estimatedHours.toIntOrNull() ?: 1
                        val ratingValue = minWorkerRating.toFloatOrNull()?.coerceIn(0f, 5f) ?: 0f
                        if (priceValue > 0 && hoursValue > 0) {
                            onCreate(address, cal.timeInMillis, cargo, priceValue, hoursValue, comment, requiredWorkers, ratingValue)
                        } else showError = true
                    } catch (e: Exception) { showError = true }
                } else showError = true
            }) { Text("Создать") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDate = it }
                    showDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Отмена") } }
        ) { DatePicker(state = datePickerState) }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = selectedHour, initialMinute = selectedMinute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedHour = timePickerState.hour
                    selectedMinute = timePickerState.minute
                    showTimePicker = false
                }) { Text("ОК") }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text("Отмена") } },
            text = { TimePicker(state = timePickerState) }
        )
    }
}
