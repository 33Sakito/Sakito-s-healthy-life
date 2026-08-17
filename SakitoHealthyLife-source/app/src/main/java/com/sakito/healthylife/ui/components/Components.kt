package com.sakito.healthylife.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sakito.healthylife.data.model.NutrientSummary
import com.sakito.healthylife.data.repository.TrendPoint
import com.sakito.healthylife.util.DateUtils
import java.text.DecimalFormat

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Inbox,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(8.dp))
        Text(text = text, color = MaterialTheme.colorScheme.outline, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun NutrientSummaryCard(
    summary: NutrientSummary,
    decimalPlaces: Int = 1,
    modifier: Modifier = Modifier,
    showAnimalPlant: Boolean = true
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                NutrientValue("热量", summary.calories, "kcal", decimalPlaces)
                NutrientValue("蛋白质", summary.protein, "g", decimalPlaces)
                NutrientValue("脂肪", summary.fat, "g", decimalPlaces)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                NutrientValue("碳水", summary.carb, "g", decimalPlaces)
                NutrientValue("膳食纤维", summary.fiber ?: 0.0, "g", decimalPlaces)
                if (showAnimalPlant) {
                    NutrientValue("动/植蛋白", summary.animalProtein ?: 0.0, "/${formatNum(summary.plantProtein ?: 0.0, decimalPlaces)}g", decimalPlaces, small = true)
                } else {
                    Spacer(Modifier)
                }
            }
        }
    }
}

@Composable
private fun NutrientValue(label: String, value: Double, unit: String, decimalPlaces: Int, small: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = formatNum(value, decimalPlaces),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = "$label ($unit)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

fun formatNum(value: Double, decimalPlaces: Int): String {
    if (decimalPlaces <= 0) return value.toLong().toString()
    val df = DecimalFormat("#." + "0".repeat(decimalPlaces))
    return df.format(value)
}

@Composable
fun TrendLineChart(
    points: List<TrendPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    showValues: Boolean = true,
    height: Int = 220
) {
    if (points.isEmpty()) {
        EmptyState("暂无数据")
        return
    }
    val textMeasurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val values = points.map { it.value }
    val min = values.minOrNull() ?: 0.0
    val max = values.maxOrNull() ?: 1.0
    val range = (max - min).takeIf { it > 0 } ?: 1.0
    val labelStyle = TextStyle(fontSize = 10.sp, color = colors.outline)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height.dp)
            .padding(horizontal = 8.dp)
    ) {
        val w = size.width
        val h = size.height
        val padTop = 24f
        val padBottom = 28f
        val chartH = h - padTop - padBottom
        val stepX = if (points.size > 1) w / (points.size - 1) else w

        // Horizontal grid lines
        for (i in 0..3) {
            val y = padTop + chartH * i / 3f
            drawLine(
                color = colors.outline.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = 1f
            )
        }

        val path = Path()
        points.forEachIndexed { index, point ->
            val x = if (points.size > 1) index * stepX else w / 2f
            val ratio = ((point.value - min) / range).toFloat().coerceIn(0f, 1f)
            val y = padTop + chartH * (1f - ratio)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        points.forEachIndexed { index, point ->
            val x = if (points.size > 1) index * stepX else w / 2f
            val ratio = ((point.value - min) / range).toFloat().coerceIn(0f, 1f)
            val y = padTop + chartH * (1f - ratio)
            drawCircle(color = lineColor, radius = 6f, center = Offset(x, y))
            if (showValues) {
                val text = formatNum(point.value, 1)
                val layout = textMeasurer.measure(AnnotatedString(text), style = labelStyle)
                drawText(
                    textLayoutResult = layout,
                    topLeft = Offset((x - layout.size.width / 2f).coerceIn(0f, w - layout.size.width), y - 22f)
                )
            }
        }

        // X labels: first, middle, last
        val labelIndices = listOf(0, points.lastIndex / 2, points.lastIndex).distinct()
        labelIndices.forEach { index ->
            val x = if (points.size > 1) index * stepX else w / 2f
            val dateText = DateUtils.formatShort(points[index].date)
            val layout = textMeasurer.measure(AnnotatedString(dateText), style = labelStyle)
            val drawX = (x - layout.size.width / 2f).coerceIn(0f, w - layout.size.width)
            drawText(layout, topLeft = Offset(drawX, h - 20f))
        }
    }
}

@Composable
fun StatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
