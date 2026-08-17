package com.sakito.healthylife.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.ui.components.SectionTitle
import com.sakito.healthylife.ui.viewmodel.FoodEditViewModel
import com.sakito.healthylife.ui.viewmodel.UnitDraft
import com.sakito.healthylife.ui.viewmodel.MicroDraft

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEditScreen(
    foodId: Long,
    onSaved: (Long) -> Unit,
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as HealthyLifeApp
    val viewModel: FoodEditViewModel = viewModel(
        key = "food_edit_$foodId",
        factory = FoodEditViewModel.factory(app, foodId)
    )
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (foodId == 0L) "新建食物" else "编辑食物") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Text("返回") }
                }
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
                Text(if (state.saving) "保存中..." else "保存")
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
                label = { Text("食物名称 *") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Text("营养数据（每100克）", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(state.calories, viewModel::updateCalories, "热量 kcal *", Modifier.weight(1f))
                NumberField(state.protein, viewModel::updateProtein, "总蛋白 g *", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(state.animalProtein, viewModel::updateAnimalProtein, "动物蛋白 g", Modifier.weight(1f))
                NumberField(state.plantProtein, viewModel::updatePlantProtein, "植物蛋白 g", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(state.fat, viewModel::updateFat, "脂肪 g *", Modifier.weight(1f))
                NumberField(state.carb, viewModel::updateCarb, "总碳水 g *", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            NumberField(state.fiber, viewModel::updateFiber, "膳食纤维 g", Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::updateNote,
                label = { Text("备注") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))
            SectionTitle("份量单位")
            state.units.forEachIndexed { index, unit ->
                UnitRow(
                    unit = unit,
                    canDelete = !unit.isBase,
                    onNameChange = { viewModel.updateUnit(index, unit.copy(name = it)) },
                    onGramsChange = { viewModel.updateUnit(index, unit.copy(grams = it)) },
                    onDelete = { viewModel.removeUnit(index) }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::addUnit) { Icon(Icons.Default.Add, contentDescription = "添加单位") }
                Text("添加自定义单位", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(16.dp))
            SectionTitle("微量营养素标记")
            state.micros.forEachIndexed { index, micro ->
                MicroRow(
                    micro = micro,
                    onNameChange = { viewModel.updateMicro(index, micro.copy(name = it)) },
                    onAmountChange = { viewModel.updateMicro(index, micro.copy(amount = it)) },
                    onUnitChange = { viewModel.updateMicro(index, micro.copy(unit = it)) },
                    onDelete = { viewModel.removeMicro(index) }
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = viewModel::addMicro) { Icon(Icons.Default.Add, contentDescription = "添加微量营养素") }
                Text("添加微量营养素", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}

@Composable
private fun UnitRow(
    unit: UnitDraft,
    canDelete: Boolean,
    onNameChange: (String) -> Unit,
    onGramsChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = unit.name,
            onValueChange = onNameChange,
            label = { Text(if (unit.isBase) "单位（克）" else "单位名称") },
            enabled = !unit.isBase,
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = unit.grams,
            onValueChange = onGramsChange,
            label = { Text("克数") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除单位")
            }
        }
    }
}

@Composable
private fun MicroRow(
    micro: MicroDraft,
    onNameChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = micro.name,
            onValueChange = onNameChange,
            label = { Text("名称") },
            modifier = Modifier.weight(1.2f)
        )
        OutlinedTextField(
            value = micro.amount,
            onValueChange = onAmountChange,
            label = { Text("含量") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f)
        )
        OutlinedTextField(
            value = micro.unit,
            onValueChange = onUnitChange,
            label = { Text("单位") },
            modifier = Modifier.weight(0.8f)
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "删除")
        }
    }
}
