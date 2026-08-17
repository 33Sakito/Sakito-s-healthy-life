package com.sakito.healthylife.data.local

import com.sakito.healthylife.HealthyLifeApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

object SeedData {

    suspend fun seedIfEmpty(database: AppDatabase) = withContext(Dispatchers.IO) {
        if (database.foodDao().count() > 0) return@withContext
        val context = HealthyLifeApp.instance
        val csv = context.assets.open("foods_base.csv").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val lines = csv.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .drop(1) // header
            .toList()

        for (line in lines) {
            val cols = parseSeedLine(line)
            if (cols.size < 13) continue
            val name = cols[0].trim()
            if (name.isEmpty()) continue
            val foodId = database.foodDao().insert(
                FoodEntity(
                    name = name,
                    caloriesPer100g = cols[1].trim().toDoubleOrNull() ?: 0.0,
                    proteinPer100g = cols[2].trim().toDoubleOrNull() ?: 0.0,
                    animalProteinPer100g = cols[3].trim().toDoubleOrNull(),
                    plantProteinPer100g = cols[4].trim().toDoubleOrNull(),
                    fatPer100g = cols[5].trim().toDoubleOrNull() ?: 0.0,
                    carbPer100g = cols[6].trim().toDoubleOrNull() ?: 0.0,
                    fiberPer100g = cols[7].trim().toDoubleOrNull(),
                    note = cols[8].trim().ifEmpty { null },
                    isCustom = cols[9].trim().toBooleanStrictOrNull() ?: false,
                    isCombined = cols[10].trim().toBooleanStrictOrNull() ?: false
                )
            )
            val unitsJson = cols[11].trim().ifEmpty { "[]" }
            val units = JSONArray(unitsJson)
            for (i in 0 until units.length()) {
                val obj = units.getJSONObject(i)
                database.foodUnitDao().insert(
                    FoodUnitEntity(
                        foodId = foodId,
                        name = obj.optString("name", "克"),
                        grams = obj.optDouble("grams", 1.0),
                        isBase = obj.optBoolean("isBase", false),
                        sortOrder = i
                    )
                )
            }
            val microsJson = cols[12].trim().ifEmpty { "[]" }
            val micros = JSONArray(microsJson)
            for (i in 0 until micros.length()) {
                val obj = micros.getJSONObject(i)
                database.micronutrientDao().insert(
                    MicronutrientEntity(
                        foodId = foodId,
                        name = obj.optString("name", ""),
                        amountPer100g = obj.optDouble("amountPer100g", 0.0),
                        unit = obj.optString("unit", "")
                    )
                )
            }
        }

        // Default body dimension types
        val defaultDimensions = listOf("腰围", "胸围", "臀围", "大腿围", "小腿围", "手臂围")
        val bodyDao = database.bodyDimensionTypeDao()
        defaultDimensions.forEachIndexed { index, name ->
            if (bodyDao.findByName(name) == null) {
                bodyDao.insert(BodyDimensionTypeEntity(name = name, isCustom = false, sortOrder = index))
            }
        }
    }

    private fun parseSeedLine(line: String): List<String> {
        // If the CSV uses quoted JSON fields, use the standard quote-aware parser.
        // The built-in seed rows always have `,false,false,` before the units JSON,
        // so `,false,false,"` reliably indicates a properly quoted CSV file.
        if (line.contains(",false,false,\"")) {
            return parseCsvLine(line)
        }
        // Fallback for the simple unquoted seed file: split first 11 commas,
        // then split the trailing "<unitsJson>,<microsJson>" at the "],[" boundary.
        val parts = line.split(",", limit = 12)
        if (parts.size < 12) return parts
        val remainder = parts[11]
        val idx = remainder.indexOf("],[")
        return if (idx >= 0) {
            parts.take(11) + remainder.substring(0, idx + 1) + remainder.substring(idx + 2)
        } else {
            parts + "[]"
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
