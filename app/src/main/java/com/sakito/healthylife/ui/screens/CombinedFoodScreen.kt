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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import com.sakito.healthylife.ui.components.SectionTitle
import com.sakito.healthylife.ui.viewmodel.CombinedFoodViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CombinedFoodScreen(
    onSaved: (Long) -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as HealthyLifeApp
    val viewModel: CombinedFoodViewModel = viewModel(factory = CombinedFoodViewModel.factory(app))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val allFoods by viewModel.allFoods.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建组合食物") },
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
                Text(if (state.saving) "保存中..." else "保存组合食物")
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
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("组合食物名称 *") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            SectionTitle("食材列表")
            if (state.ingredients.isEmpty()) {
                Text("还没有添加食材", color = MaterialTheme.colorScheme.outline)
            }
            state.ingredients.forEachIndexed { index, ingredient ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column(Modifier.weight(1.4f)) {
                        Text(ingredient.food.food.name, style = MaterialTheme.typography.bodyLarge)
                        UnitDropdown(
                            units = ingredient.food.units.map { it.name to it.id },
                            selectedId = ingredient.unitId ?: -1,
                            onSelect = { id -> viewModel.updateIngredientUnit(index, id) }
                        )
                    }
                    OutlinedTextField(
                        value = ingredient.quantity,
                        onValueChange = { viewModel.updateIngredientQuantity(index, it) },
                        label = { Text("数量") },
                        modifier = Modifier.weight(0.7f)
                    )
                    IconButton(onClick = { viewModel.removeIngredient(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "移除")
                    }
                }
            }
            TextButton(onClick = { showPicker = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("添加食材")
            }

            Spacer(Modifier.height(12.dp))
            SectionTitle("保存单位")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.keepPer100g, onCheckedChange = viewModel::setKeepPer100g)
                Text("保留「每100克」")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = state.keepServing, onCheckedChange = viewModel::setKeepServing)
                Text("保留「一份」（整份总营养）")
            }

            SectionTitle("其他自定义单位")
            state.extraUnits.forEachIndexed { index, pair ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = pair.first,
                        onValueChange = { viewModel.updateExtraUnit(index, it, pair.second) },
                        label = { Text("单位名") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = pair.second,
                        onValueChange = { viewModel.updateExtraUnit(index, pair.first, it) },
                        label = { Text("克数") },
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { viewModel.removeExtraUnit(index) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            }
            TextButton(onClick = viewModel::addExtraUnit) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("添加自定义单位")
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("选择食材") },
            text = {
                LazyColumn(Modifier.height(360.dp)) {
                    items(allFoods, key = { it.id }) { food ->
                        TextButton(
                            onClick = {
                                viewModel.addIngredient(food.id, null, "1")
                                showPicker = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(food.name, modifier = Modifier.weight(1f))
                                Text("${food.caloriesPer100g.toInt()} kcal", color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPicker = false }) { Text("关闭") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitDropdown(
    units: List<Pair<String, Long>>,
    selectedId: Long,
    onSelect: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = units.firstOrNull { it.second == selectedId }?.first ?: "克"
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
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
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
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
