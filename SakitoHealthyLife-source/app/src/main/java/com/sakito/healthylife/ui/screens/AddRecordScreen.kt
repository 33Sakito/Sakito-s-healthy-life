package com.sakito.healthylife.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.sakito.healthylife.data.local.FoodWithDetails
import com.sakito.healthylife.ui.components.EmptyState
import com.sakito.healthylife.ui.components.formatNum
import com.sakito.healthylife.ui.viewmodel.AddRecordViewModel
import com.sakito.healthylife.ui.viewmodel.FoodLibraryViewModel
import com.sakito.healthylife.util.DateUtils

private val mealOptions = listOf("未分餐", "早餐", "午餐", "晚餐", "加餐", "夜宵", "全天")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordScreen(
    recordId: Long,
    date: String?,
    onSaved: (Long) -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as HealthyLifeApp
    val viewModel: AddRecordViewModel = viewModel(
        key = "add_record_${recordId}_${date ?: "new"}",
        factory = AddRecordViewModel.factory(app, recordId, date)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFoodPicker by remember { mutableStateOf(false) }
    var pendingFoodId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recordId == 0L) "添加饮食记录" else "编辑饮食记录") },
                navigationIcon = { IconButton(onClick = onBack) { Text("返回") } }
            )
        },
        bottomBar = {
            Button(
                onClick = { viewModel.save(onSaved) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = !state.saving
            ) {
                Text(if (state.saving) "保存中..." else "保存记录")
            }
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
            }
            OutlinedTextField(
                value = state.date,
                onValueChange = viewModel::updateDate,
                label = { Text("日期 (yyyy-MM-dd)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            MealDropdown(selected = state.mealType, onSelect = viewModel::updateMealType)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::updateNote,
                label = { Text("备注（可选）") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("食物条目", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = { showFoodPicker = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("添加食物")
                }
            }

            if (state.entries.isEmpty()) {
                EmptyState("还没有食物条目")
            } else {
                state.entries.forEachIndexed { index, entry ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.foodName, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                "${entry.unitName} × ${formatNum(entry.quantity, 1)} = ${formatNum(entry.actualWeight, 1)}g · ${formatNum(entry.calories, 1)} kcal",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewModel.removeEntry(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除")
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showFoodPicker) {
        FoodSearchDialog(
            onDismiss = { showFoodPicker = false },
            onPick = { foodId ->
                showFoodPicker = false
                pendingFoodId = foodId
            }
        )
    }

    pendingFoodId?.let { foodId ->
        QuantityDialog(
            foodId = foodId,
            onDismiss = { pendingFoodId = null },
            onConfirm = { unitId, quantity, actualWeight, note ->
                viewModel.addFood(foodId, unitId, quantity, actualWeight, note)
                pendingFoodId = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MealDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("餐次") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            mealOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FoodSearchDialog(
    onDismiss: () -> Unit,
    onPick: (Long) -> Unit
) {
    val app = LocalContext.current.applicationContext as HealthyLifeApp
    val viewModel: FoodLibraryViewModel = viewModel(factory = FoodLibraryViewModel.factory(app))
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择食物") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("搜索食物") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true
                )
                Spacer(Modifier.height(8.dp))
                if (foods.isEmpty()) {
                    EmptyState("没有找到食物")
                } else {
                    LazyColumn(Modifier.height(320.dp)) {
                        items(foods, key = { it.id }) { food ->
                            TextButton(
                                onClick = { onPick(food.id) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text(food.name, modifier = Modifier.weight(1f))
                                    Text("${food.caloriesPer100g.toInt()} kcal", color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        }
    )
}

@Composable
private fun QuantityDialog(
    foodId: Long,
    onDismiss: () -> Unit,
    onConfirm: (Long?, String, String?, String) -> Unit
) {
    val app = LocalContext.current.applicationContext as HealthyLifeApp
    var food by remember { mutableStateOf<FoodWithDetails?>(null) }
    var selectedUnitId by remember { mutableStateOf<Long?>(null) }
    var quantity by remember { mutableStateOf("1") }
    var actualWeight by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }

    LaunchedEffect(foodId) {
        food = app.foodRepository.getFood(foodId)
        selectedUnitId = food?.units?.firstOrNull { it.isBase }?.id ?: food?.units?.firstOrNull()?.id
        food?.let {
            val unit = it.units.firstOrNull { u -> u.id == selectedUnitId } ?: it.units.first()
            actualWeight = unit.grams.toString()
        }
    }

    val currentFood = food
    if (currentFood == null) {
        AlertDialog(onDismissRequest = onDismiss, confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } }, text = { Text("加载中...") })
        return
    }

    val units = currentFood.units
    val selectedUnit = units.firstOrNull { it.id == selectedUnitId } ?: units.first()
    val qty = quantity.toDoubleOrNull() ?: 0.0
    val defaultWeight = selectedUnit.grams * qty

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(currentFood.food.name) },
        text = {
            Column {
                UnitDropdownSimple(
                    units = units.map { it.name to it.id },
                    selectedId = selectedUnit.id,
                    onSelect = {
                        selectedUnitId = it
                        val u = units.firstOrNull { u -> u.id == it } ?: units.first()
                        actualWeight = (u.grams * qty).toString()
                    }
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { value ->
                        quantity = value
                        val q = value.toDoubleOrNull() ?: 0.0
                        actualWeight = (selectedUnit.grams * q).toString()
                    },
                    label = { Text("数量") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = actualWeight,
                    onValueChange = { actualWeight = it },
                    label = { Text("实际重量（克）") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(selectedUnit.id, quantity, actualWeight.ifBlank { defaultWeight.toString() }, note)
            }) { Text("添加") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdownSimple(
    units: List<Pair<String, Long>>,
    selectedId: Long,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = units.firstOrNull { it.second == selectedId }?.first ?: "克"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("单位") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            units.forEach { (name, id) ->
                DropdownMenuItem(
                    text = { Text(name) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}
