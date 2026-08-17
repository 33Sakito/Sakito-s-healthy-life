package com.sakito.healthylife.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.data.model.NutrientSummary
import com.sakito.healthylife.ui.components.EmptyState
import com.sakito.healthylife.ui.components.StatChip
import com.sakito.healthylife.ui.components.TrendLineChart
import com.sakito.healthylife.ui.components.formatNum
import com.sakito.healthylife.ui.viewmodel.NutritionMetric
import com.sakito.healthylife.ui.viewmodel.StatsTab
import com.sakito.healthylife.ui.viewmodel.StatsViewModel
import com.sakito.healthylife.util.DateUtils
import java.time.LocalDate
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen() {
    val app = LocalContext.current.applicationContext as HealthyLifeApp
    val viewModel: StatsViewModel = viewModel(factory = StatsViewModel.factory(app))
    val tab by viewModel.tab.collectAsStateWithLifecycle()
    val startDate by viewModel.startDate.collectAsStateWithLifecycle()
    val endDate by viewModel.endDate.collectAsStateWithLifecycle()
    val useAverage by viewModel.useAverage.collectAsStateWithLifecycle()
    val dimensionTypes by viewModel.dimensionTypes.collectAsStateWithLifecycle()
    val dimensionTypeId by viewModel.dimensionTypeId.collectAsStateWithLifecycle()
    val nutritionMetric by viewModel.nutritionMetric.collectAsStateWithLifecycle()
    val savedRanges by viewModel.savedRanges.collectAsStateWithLifecycle()
    val points by viewModel.trendPoints.collectAsStateWithLifecycle()

    var showSaveRange by remember { mutableStateOf(false) }
    var customStart by remember { mutableStateOf(startDate) }
    var customEnd by remember { mutableStateOf(endDate) }
    var customName by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("统计与趋势") }) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            TabRow(selectedTabIndex = tab.ordinal) {
                StatsTab.entries.forEach { t ->
                    Tab(selected = tab == t, onClick = { viewModel.selectTab(t) }, text = { Text(t.label) })
                }
            }

            Spacer(Modifier.height(12.dp))

            if (tab != StatsTab.CALENDAR) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("最近一周", "最近一月", "最近三个月", "全部").forEach { preset ->
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.setPreset(preset) },
                            label = { Text(preset) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = startDate, onValueChange = { customStart = it; viewModel.setRange(it, endDate) }, label = { Text("开始") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = endDate, onValueChange = { customEnd = it; viewModel.setRange(startDate, it) }, label = { Text("结束") }, modifier = Modifier.weight(1f))
                    IconButton(onClick = { showSaveRange = true }) { Icon(Icons.Default.Add, contentDescription = "保存范围") }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    savedRanges.forEach { range ->
                        AssistChip(
                            onClick = { viewModel.setRange(range.startDate, range.endDate) },
                            label = { Text(range.name) },
                            trailingIcon = {
                                IconButton(onClick = { viewModel.deleteRange(range.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "删除", modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }

                when (tab) {
                    StatsTab.WEIGHT -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(selected = !useAverage, onClick = { viewModel.setUseAverage(false) }, label = { Text("最新值") })
                            FilterChip(selected = useAverage, onClick = { viewModel.setUseAverage(true) }, label = { Text("平均值") })
                        }
                    }
                    StatsTab.MEASUREMENT -> {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            dimensionTypes.forEach { type ->
                                FilterChip(
                                    selected = dimensionTypeId == type.id,
                                    onClick = { viewModel.setDimensionType(type.id) },
                                    label = { Text(type.name) }
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(selected = !useAverage, onClick = { viewModel.setUseAverage(false) }, label = { Text("最新值") })
                            FilterChip(selected = useAverage, onClick = { viewModel.setUseAverage(true) }, label = { Text("平均值") })
                        }
                    }
                    StatsTab.NUTRITION -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NutritionMetric.entries.forEach { metric ->
                                FilterChip(
                                    selected = nutritionMetric == metric,
                                    onClick = { viewModel.setNutritionMetric(metric) },
                                    label = { Text(metric.label) }
                                )
                            }
                        }
                    }
                    StatsTab.CALENDAR -> {}
                }

                Spacer(Modifier.height(12.dp))
                TrendLineChart(points = points, showValues = points.size <= 31)
                if (points.isEmpty()) {
                    Text("当前范围暂无数据", color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(8.dp))
                }
            } else {
                CalendarTab(app = app)
            }

            Spacer(Modifier.height(16.dp))
            if (tab == StatsTab.NUTRITION) {
                NutritionStatCards(app = app, startDate = startDate, endDate = endDate)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showSaveRange) {
        AlertDialog(
            onDismissRequest = { showSaveRange = false },
            title = { Text("保存自定义范围") },
            text = {
                Column {
                    OutlinedTextField(value = customName, onValueChange = { customName = it }, label = { Text("范围名称") }, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    Text("$startDate ~ $endDate", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (customName.isNotBlank()) viewModel.saveRange(customName, startDate, endDate)
                    showSaveRange = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showSaveRange = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun NutritionStatCards(app: HealthyLifeApp, startDate: String, endDate: String) {
    val summary by produceNutrientSummary(app, startDate, endDate)
    val days = DateUtils.daysBetween(startDate, endDate).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("最近区间营养统计（日均）", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("日均热量", "${formatNum(summary.calories / days, 0)} kcal", Modifier.weight(1f))
            StatChip("日均蛋白", "${formatNum(summary.protein / days, 1)} g", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatChip("日均脂肪", "${formatNum(summary.fat / days, 1)} g", Modifier.weight(1f))
            StatChip("日均碳水", "${formatNum(summary.carb / days, 1)} g", Modifier.weight(1f))
        }
        if ((summary.animalProtein ?: 0.0) > 0 || (summary.plantProtein ?: 0.0) > 0) {
            val animal = summary.animalProtein ?: 0.0
            val plant = summary.plantProtein ?: 0.0
            val total = animal + plant
            Text(
                "动物蛋白 ${(animal / total * 100).toInt()}% / 植物蛋白 ${(plant / total * 100).toInt()}%",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun produceNutrientSummary(app: HealthyLifeApp, startDate: String, endDate: String): androidx.compose.runtime.State<NutrientSummary> {
    return androidx.compose.runtime.produceState(initialValue = NutrientSummary(), key1 = startDate, key2 = endDate) {
        value = app.dietRepository.getPeriodSummary(startDate, endDate)
    }
}

@Composable
private fun CalendarTab(app: HealthyLifeApp) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var dietDates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var weightDates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var measurementDates by remember { mutableStateOf<Set<String>>(emptySet()) }
    var dayStats by remember { mutableStateOf<com.sakito.healthylife.data.repository.DayStats?>(null) }

    LaunchedEffect(month) {
        dietDates = app.statsRepository.getDietDates()
        weightDates = app.statsRepository.getWeightDates()
        measurementDates = app.statsRepository.getMeasurementDates()
        val first = month.atDay(1).toString()
        val last = month.atEndOfMonth().toString()
        dayStats = app.statsRepository.getDayStats(first, last)
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("上月") }
            Text("${month.year}年${month.monthValue}月", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            TextButton(onClick = { month = month.plusMonths(1) }) { Text("下月") }
        }

        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                Text(
                    label,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(4.dp),
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        val firstDay = month.atDay(1)
        val leading = (firstDay.dayOfWeek.value - 1) % 7
        val daysInMonth = month.lengthOfMonth()
        val dayCells = List(leading) { 0 } + (1..daysInMonth).toList()
        dayCells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day == 0) {
                        Box(Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        val date = LocalDate.of(month.year, month.monthValue, day).toString()
                        val hasDiet = date in dietDates
                        val hasWeight = date in weightDates
                        val hasMeasure = date in measurementDates
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(
                                    when {
                                        hasDiet && hasWeight && hasMeasure -> MaterialTheme.colorScheme.primary
                                        hasDiet && hasWeight -> MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        hasDiet -> MaterialTheme.colorScheme.primaryContainer
                                        hasWeight -> MaterialTheme.colorScheme.tertiaryContainer
                                        hasMeasure -> MaterialTheme.colorScheme.secondaryContainer
                                        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    },
                                    MaterialTheme.shapes.small
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "$day",
                                color = if (hasDiet && hasWeight && hasMeasure) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                // Fill missing trailing cells to keep 7 columns
                repeat(7 - week.size) {
                    Box(Modifier.weight(1f).aspectRatio(1f))
                }
            }
        }

        dayStats?.let { stats ->
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatChip("饮食天数", "${stats.dietDays}", Modifier.weight(1f))
                StatChip("体重天数", "${stats.weightDays}", Modifier.weight(1f))
                StatChip("围度天数", "${stats.measurementDays}", Modifier.weight(1f))
            }
        }
    }
}
