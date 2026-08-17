package com.sakito.healthylife.data.backup

import com.sakito.healthylife.data.local.BodyMeasurementEntity
import com.sakito.healthylife.data.local.DietRecordWithEntries
import com.sakito.healthylife.data.local.WeightRecordEntity

object CsvManager {

    fun dietRecordsCsv(records: List<DietRecordWithEntries>): String {
        val sb = StringBuilder()
        sb.appendLine("recordId,date,createdAt,mealType,recordNote,foodName,unitName,unitGrams,quantity,actualWeight,calories,protein,animalProtein,plantProtein,fat,carb,fiber,entryNote")
        records.forEach { appendDietRecord(sb, it) }
        return sb.toString()
    }

    private fun appendDietRecord(sb: StringBuilder, r: DietRecordWithEntries) {
        r.entries.forEach { e ->
            sb.appendLine(
                listOf(
                    r.record.id.toString(),
                    r.record.date,
                    r.record.createdAt.toString(),
                    csvEscape(r.record.mealType),
                    csvEscape(r.record.note.orEmpty()),
                    csvEscape(e.foodName),
                    csvEscape(e.unitName),
                    formatNum(e.unitGrams),
                    formatNum(e.quantity),
                    formatNum(e.actualWeight),
                    formatNum(e.calories),
                    formatNum(e.protein),
                    e.animalProtein?.let { formatNum(it) } ?: "",
                    e.plantProtein?.let { formatNum(it) } ?: "",
                    formatNum(e.fat),
                    formatNum(e.carb),
                    e.fiber?.let { formatNum(it) } ?: "",
                    csvEscape(e.note.orEmpty())
                ).joinToString(",")
            )
        }
    }

    fun weightRecordsCsv(records: List<WeightRecordEntity>): String {
        val sb = StringBuilder()
        sb.appendLine("id,date,createdAt,weightKg,note")
        records.forEach {
            sb.appendLine(
                listOf(
                    it.id.toString(),
                    it.date,
                    it.createdAt.toString(),
                    formatNum(it.weightKg),
                    csvEscape(it.note.orEmpty())
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun bodyMeasurementsCsv(
        records: List<BodyMeasurementEntity>,
        typeName: (Long) -> String
    ): String {
        val sb = StringBuilder()
        sb.appendLine("id,date,createdAt,dimensionType,valueCm,note")
        records.forEach {
            sb.appendLine(
                listOf(
                    it.id.toString(),
                    it.date,
                    it.createdAt.toString(),
                    csvEscape(typeName(it.dimensionTypeId)),
                    formatNum(it.valueCm),
                    csvEscape(it.note.orEmpty())
                ).joinToString(",")
            )
        }
        return sb.toString()
    }

    fun csvEscape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun formatNum(value: Double): String {
        return if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }
}
