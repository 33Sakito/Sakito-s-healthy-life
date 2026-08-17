package com.sakito.healthylife.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.data.local.DietRecordWithEntries
import com.sakito.healthylife.ui.components.EmptyState
import com.sakito.healthylife.ui.components.NutrientSummaryCard
import com.sakito.healthylife.ui.components.formatNum
import com.sakito.healthylife.ui.viewmodel.HomeViewModel
import com.sakito.healthylife.util.DateUtils
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddRecord: (String) -> Unit,
    onEditRecord: (Long, String) -> Unit,
    onManageFoods: () -> Unit
) {
    val app = LocalContext.current.applicationContext as HealthyLifeApp
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(app))
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val summary by viewModel.daySummary.collectAsStateWithLifecycle()
    val commonFoods by viewModel.commonFoods.collectAsStateWithLifecycle()
    val recentFoods by viewModel.recentFoods.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.shiftDate(-1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "前一天")
                        }
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(DateUtils.formatChinese(selectedDate), style = MaterialTheme.typography.titleMedium)
                        }
                        IconButton(onClick = { viewModel.shiftDate(1) }) {
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "后一天")
                        }
                    }
                },
                actions = {
                    TextButton(onClick = { onAddRecord(selectedDate) }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Text("记录")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                NutrientSummaryCard(summary = summary)
            }

            if (commonFoods.isNotEmpty() || recentFoods.isNotEmpty()) {
                item {
                    Text("快捷添加", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        commonFoods.take(5).forEach { food ->
                            AssistChip(onClick = { onAddRecord(selectedDate) }, label = { Text(food.name) })
                        }
                        recentFoods.take(3).forEach { food ->
                            if (commonFoods.none { it.id == food.id }) {
                                AssistChip(onClick = { onAddRecord(selectedDate) }, label = { Text(food.name) })
                            }
                        }
                    }
                }
            }

            item {
                Text("饮食记录", style = MaterialTheme.typography.titleMedium)
            }

            if (records.isEmpty()) {
                item { EmptyState("这一天还没有记录") }
            } else {
                items(records, key = { it.record.id }) { record ->
                    DietRecordCard(
                        record = record,
                        onClick = { onEditRecord(record.record.id, record.record.date) },
                        onCopy = { viewModel.copyRecord(record.record.id, DateUtils.today()) },
                        onDelete = { viewModel.deleteRecord(record.record.id) }
                    )
                }
            }

            item {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onManageFoods() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("去食物库管理食物")
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = DateUtils.parse(selectedDate)
                ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate().toString()
                        viewModel.selectDate(date)
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DietRecordCard(
    record: DietRecordWithEntries,
    onClick: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(record.record.mealType, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(
                    "${DateUtils.formatShort(record.record.date)} ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(record.record.createdAt))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Spacer(Modifier.height(4.dp))
            record.entries.forEach { entry ->
                Text(
                    "· ${entry.foodName} ${formatNum(entry.quantity, 1)} ${entry.unitName}（${formatNum(entry.actualWeight, 1)}g） ${formatNum(entry.calories, 1)} kcal",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            record.record.note?.takeIf { it.isNotBlank() }?.let {
                Text("备注：$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                IconButton(onClick = onCopy) { Icon(Icons.Default.ContentCopy, contentDescription = "复制到今天") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}
