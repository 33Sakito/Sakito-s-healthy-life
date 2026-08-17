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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.ui.components.EmptyState
import com.sakito.healthylife.ui.components.formatNum
import com.sakito.healthylife.ui.viewmodel.BodyViewModel
import com.sakito.healthylife.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScreen() {
    val app = LocalContext.current.applicationContext as HealthyLifeApp
    val viewModel: BodyViewModel = viewModel(factory = BodyViewModel.factory(app))
    val weights by viewModel.weights.collectAsStateWithLifecycle()
    val dimensionTypes by viewModel.dimensionTypes.collectAsStateWithLifecycle()
    val measurements by viewModel.measurements.collectAsStateWithLifecycle()

    var tab by remember { mutableIntStateOf(0) }
    var showAddWeight by remember { mutableStateOf(false) }
    var showAddMeasurement by remember { mutableStateOf(false) }
    var showAddDimension by remember { mutableStateOf(false) }
    var selectedDimensionId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("身体记录") },
                actions = {
                    if (tab == 0) {
                        TextButton(onClick = { showAddWeight = true }) { Icon(Icons.Default.Add, contentDescription = null); Text("体重") }
                    } else {
                        if (dimensionTypes.isNotEmpty()) {
                            TextButton(onClick = { showAddMeasurement = true }) { Icon(Icons.Default.Add, contentDescription = null); Text("围度") }
                        }
                        TextButton(onClick = { showAddDimension = true }) { Text("管理类型") }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("体重") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("围度") })
            }

            if (tab == 0) {
                WeightList(weights = weights, onDelete = viewModel::deleteWeight)
            } else {
                if (dimensionTypes.isEmpty()) {
                    EmptyState("还没有维度类型")
                } else {
                    val currentTypeId = selectedDimensionId ?: dimensionTypes.first().id
                    LaunchedEffect(dimensionTypes, selectedDimensionId) {
                        if (selectedDimensionId == null || dimensionTypes.none { it.id == selectedDimensionId }) {
                            selectedDimensionId = dimensionTypes.firstOrNull()?.id
                        }
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        dimensionTypes.forEach { type ->
                            FilterChip(
                                selected = currentTypeId == type.id,
                                onClick = { selectedDimensionId = type.id },
                                label = { Text(type.name) }
                            )
                        }
                    }
                    MeasurementList(
                        records = measurements.filter { it.dimensionTypeId == currentTypeId },
                        typeName = dimensionTypes.firstOrNull { it.id == currentTypeId }?.name ?: "",
                        onDelete = viewModel::deleteMeasurement
                    )
                }
            }
        }
    }

    if (showAddWeight) {
        AddWeightDialog(
            onDismiss = { showAddWeight = false },
            onConfirm = { date, kg, note ->
                viewModel.addWeight(date, kg, note)
                showAddWeight = false
            }
        )
    }

    if (showAddMeasurement) {
        val typeId = selectedDimensionId ?: dimensionTypes.firstOrNull()?.id ?: 0L
        if (typeId != 0L) {
            AddMeasurementDialog(
                typeId = typeId,
                onDismiss = { showAddMeasurement = false },
                onConfirm = { date, value, note ->
                    viewModel.addMeasurement(date, typeId, value, note)
                    showAddMeasurement = false
                }
            )
        }
    }

    if (showAddDimension) {
        ManageDimensionTypesDialog(
            types = dimensionTypes,
            onDismiss = { showAddDimension = false },
            onAdd = { name ->
                viewModel.addDimensionType(name)
            },
            onRename = { id, name -> viewModel.renameDimensionType(id, name) },
            onDelete = { id -> viewModel.deleteDimensionType(id) }
        )
    }
}

@Composable
private fun WeightList(weights: List<com.sakito.healthylife.data.local.WeightRecordEntity>, onDelete: (Long) -> Unit) {
    if (weights.isEmpty()) {
        EmptyState("还没有体重记录")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(weights, key = { it.id }) { w ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${formatNum(w.weightKg, 1)} kg", style = MaterialTheme.typography.titleMedium)
                        Text("${DateUtils.formatChinese(w.date)} · ${java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(w.createdAt))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        w.note?.takeIf { it.isNotBlank() }?.let { Text("备注：$it", style = MaterialTheme.typography.bodySmall) }
                    }
                    IconButton(onClick = { onDelete(w.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun MeasurementList(
    records: List<com.sakito.healthylife.data.local.BodyMeasurementEntity>,
    typeName: String,
    onDelete: (Long) -> Unit
) {
    if (records.isEmpty()) {
        EmptyState("还没有${typeName}记录")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(records, key = { it.id }) { m ->
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${typeName} ${formatNum(m.valueCm, 1)} cm", style = MaterialTheme.typography.titleMedium)
                        Text(DateUtils.formatChinese(m.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        m.note?.takeIf { it.isNotBlank() }?.let { Text("备注：$it", style = MaterialTheme.typography.bodySmall) }
                    }
                    IconButton(onClick = { onDelete(m.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@Composable
private fun AddWeightDialog(onDismiss: () -> Unit, onConfirm: (String, Double, String?) -> Unit) {
    var date by remember { mutableStateOf(DateUtils.today()) }
    var kg by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录体重") },
        text = {
            Column {
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("日期 (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = kg, onValueChange = { kg = it }, label = { Text("体重 kg *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val value = kg.toDoubleOrNull()
                if (value != null) onConfirm(date, value, note.trim().ifEmpty { null })
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@Composable
private fun AddMeasurementDialog(typeId: Long, onDismiss: () -> Unit, onConfirm: (String, Double, String?) -> Unit) {
    var date by remember { mutableStateOf(DateUtils.today()) }
    var value by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("记录围度") },
        text = {
            Column {
                OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("日期 (yyyy-MM-dd)") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = value, onValueChange = { value = it }, label = { Text("数值 cm *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val v = value.toDoubleOrNull()
                if (v != null) onConfirm(date, v, note.trim().ifEmpty { null })
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManageDimensionTypesDialog(
    types: List<com.sakito.healthylife.data.local.BodyDimensionTypeEntity>,
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理维度类型") },
        text = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, label = { Text("新类型名称") }, modifier = Modifier.weight(1f))
                    TextButton(onClick = { if (newName.isNotBlank()) { onAdd(newName); newName = "" } }) { Text("添加") }
                }
                Spacer(Modifier.height(8.dp))
                types.forEach { type ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = type.name,
                            onValueChange = { onRename(type.id, it) },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { onDelete(type.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } }
    )
}
