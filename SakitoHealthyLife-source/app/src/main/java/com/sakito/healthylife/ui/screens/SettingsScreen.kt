package com.sakito.healthylife.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.data.backup.BackupManager
import com.sakito.healthylife.ui.components.SectionTitle
import com.sakito.healthylife.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val app = LocalContext.current.applicationContext as HealthyLifeApp
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(app))
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var confirmImport by remember { mutableStateOf<Uri?>(null) }
    var showTimeDialog by remember { mutableStateOf(false) }

    val exportZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val result = BackupManager(app).exportZip(uri)
                message = if (result.isSuccess) "导出成功" else "导出失败：${result.exceptionOrNull()?.message}"
            }
        }
    }

    val importZipLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) confirmImport = uri
    }

    val exportFoodsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val csv = app.foodRepository.exportFoodsCsv()
                    app.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write("\uFEFF".toByteArray(Charsets.UTF_8))
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    }
                }.onSuccess { message = "食物库导出成功" }
                    .onFailure { message = "导出失败：${it.message}" }
            }
        }
    }

    val importFoodsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val text = app.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                    val result = app.foodRepository.importFoodsCsv(text)
                    message = "导入完成：新增${result.added}，更新${result.updated}，跳过${result.skipped}${if (result.errors.isNotEmpty()) "，错误${result.errors.size}" else ""}"
                }.onFailure { message = "导入失败：${it.message}" }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("设置") }) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            message?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Text(it, Modifier.padding(12.dp))
                }
                Spacer(Modifier.height(8.dp))
            }

            SectionTitle("数据管理")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exportZipLauncher.launch("sakito_healthy_life_backup.zip") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Text("导出全部")
                }
                OutlinedButton(onClick = { importZipLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Upload, contentDescription = null)
                    Text("导入备份")
                }
            }

            SectionTitle("食物库")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { exportFoodsLauncher.launch("foods.csv") }, modifier = Modifier.weight(1f)) {
                    Text("导出食物CSV")
                }
                OutlinedButton(onClick = { importFoodsLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "application/octet-stream")) }, modifier = Modifier.weight(1f)) {
                    Text("导入食物CSV")
                }
            }

            SectionTitle("提醒设置")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("每日记录提醒", style = MaterialTheme.typography.bodyLarge)
                    Text("默认每天 20:00，若当天无任何记录则提醒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(
                    checked = settings.reminderEnabled,
                    onCheckedChange = viewModel::setReminderEnabled
                )
            }
            if (settings.reminderEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("提醒时间：", style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { showTimeDialog = true }) {
                        Text("%02d:%02d".format(settings.reminderHour, settings.reminderMinute))
                    }
                }
            }

            SectionTitle("通用设置")
            DecimalPlacesDropdown(settings.decimalPlaces, viewModel::setDecimalPlaces)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = settings.defaultMeal,
                onValueChange = viewModel::setDefaultMeal,
                label = { Text("默认餐次") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            TrendDefaultDropdown(settings.trendDefaultValue, viewModel::setTrendDefaultValue)

            Spacer(Modifier.height(24.dp))
        }
    }

    confirmImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { confirmImport = null },
            title = { Text("导入备份") },
            text = { Text("导入将覆盖当前所有数据，确定继续吗？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val result = BackupManager(app).importZip(uri)
                        message = if (result.isSuccess) "导入成功，数据已恢复" else "导入失败：${result.exceptionOrNull()?.message}"
                        confirmImport = null
                    }
                }) { Text("覆盖导入") }
            },
            dismissButton = {
                TextButton(onClick = { confirmImport = null }) { Text("取消") }
            }
        )
    }

    if (showTimeDialog) {
        TimePickerDialog(
            currentHour = settings.reminderHour,
            currentMinute = settings.reminderMinute,
            onDismiss = { showTimeDialog = false },
            onConfirm = viewModel::setReminderTime
        )
    }
}

@Composable
private fun TimePickerDialog(
    currentHour: Int,
    currentMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    var hour by remember { mutableStateOf(currentHour.toString()) }
    var minute by remember { mutableStateOf(currentMinute.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置提醒时间") },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = hour, onValueChange = { hour = it }, label = { Text("时") }, modifier = Modifier.weight(1f))
                Text(":")
                OutlinedTextField(value = minute, onValueChange = { minute = it }, label = { Text("分") }, modifier = Modifier.weight(1f))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: currentHour
                val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: currentMinute
                onConfirm(h, m)
                onDismiss()
            }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecimalPlacesDropdown(selected: Int, onSelect: (Int) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = "保留 $selected 位小数",
            onValueChange = {},
            readOnly = true,
            label = { Text("数值小数位数") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(0, 1, 2, 3).forEach { value ->
                DropdownMenuItem(
                    text = { Text("保留 $value 位小数") },
                    onClick = { onSelect(value); expanded = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrendDefaultDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (selected == "average") "平均值" else "最新值"
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("趋势图默认取值") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("最新值") }, onClick = { onSelect("latest"); expanded = false })
            DropdownMenuItem(text = { Text("平均值") }, onClick = { onSelect("average"); expanded = false })
        }
    }
}
