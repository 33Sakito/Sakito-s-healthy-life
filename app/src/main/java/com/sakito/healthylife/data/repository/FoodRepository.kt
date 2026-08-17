package com.sakito.healthylife.data.repository

import androidx.room.withTransaction
import com.sakito.healthylife.data.local.AppDatabase
import com.sakito.healthylife.data.local.CombinedComponentEntity
import com.sakito.healthylife.data.local.FoodEntity
import com.sakito.healthylife.data.local.FoodUnitEntity
import com.sakito.healthylife.data.local.FoodWithDetails
import com.sakito.healthylife.data.local.MicronutrientEntity
import com.sakito.healthylife.data.model.CombinedComponentInput
import com.sakito.healthylife.data.model.FoodFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

data class FoodImportResult(
    val added: Int = 0,
    val updated: Int = 0,
    val skipped: Int = 0,
    val errors: List<String> = emptyList()
)

class FoodRepository(
    private val database: AppDatabase,
    private val foodDao: com.sakito.healthylife.data.local.FoodDao,
    private val unitDao: com.sakito.healthylife.data.local.FoodUnitDao,
    private val microDao: com.sakito.healthylife.data.local.MicronutrientDao,
    private val componentDao: com.sakito.healthylife.data.local.CombinedComponentDao
) {

    fun observeFoods(filter: FoodFilter, query: String): Flow<List<FoodEntity>> {
        val base = when (filter) {
            FoodFilter.ALL -> if (query.isBlank()) foodDao.observeAllByName() else foodDao.search(query.trim())
            FoodFilter.COMMON -> foodDao.observeCommon(50)
            FoodFilter.RECENT -> foodDao.observeRecent(50)
            FoodFilter.CUSTOM -> if (query.isBlank()) foodDao.observeCustom() else foodDao.search(query.trim())
        }
        return if (filter == FoodFilter.COMMON || filter == FoodFilter.RECENT) {
            base
        } else {
            base
        }
    }

    fun observeFood(id: Long): Flow<FoodWithDetails?> = foodDao.observeById(id)

    suspend fun getFood(id: Long): FoodWithDetails? = foodDao.getById(id)

    suspend fun bumpUsage(foodId: Long) {
        foodDao.bumpUsage(foodId)
    }

    suspend fun isNameTaken(name: String, excludeId: Long = 0): Boolean {
        val existing = foodDao.findByName(name.trim()) ?: return false
        return existing.id != excludeId
    }

    suspend fun saveFood(
        food: FoodEntity,
        units: List<FoodUnitEntity>,
        micros: List<MicronutrientEntity>
    ): Long = database.withTransaction {
        val foodId = if (food.id == 0L) {
            foodDao.insert(food)
        } else {
            foodDao.update(food)
            food.id
        }
        unitDao.deleteByFood(foodId)
        microDao.deleteByFood(foodId)
        unitDao.insertAll(units.mapIndexed { index, unit ->
            unit.copy(id = 0, foodId = foodId, sortOrder = index)
        })
        microDao.insertAll(micros.map { it.copy(id = 0, foodId = foodId) })
        foodId
    }

    suspend fun createCombinedFood(
        name: String,
        components: List<CombinedComponentInput>,
        keepPer100g: Boolean,
        keepServing: Boolean,
        extraUnits: List<Pair<String, Double>>,
        note: String? = null
    ): Long = database.withTransaction {
        require(components.isNotEmpty()) { "组合食物至少需要一个食材" }

        // Compute total nutrition and weight
        var totalGrams = 0.0
        var totalCalories = 0.0
        var totalProtein = 0.0
        var totalAnimalProtein = 0.0
        var totalPlantProtein = 0.0
        var totalFat = 0.0
        var totalCarb = 0.0
        var totalFiber = 0.0

        for (component in components) {
            val ingredient = foodDao.getById(component.ingredientFoodId)
                ?: error("食材不存在: id=${component.ingredientFoodId}")
            val grams = component.unitGrams * component.quantity
            val factor = grams / 100.0
            totalGrams += grams
            totalCalories += ingredient.food.caloriesPer100g * factor
            totalProtein += ingredient.food.proteinPer100g * factor
            totalAnimalProtein += (ingredient.food.animalProteinPer100g ?: 0.0) * factor
            totalPlantProtein += (ingredient.food.plantProteinPer100g ?: 0.0) * factor
            totalFat += ingredient.food.fatPer100g * factor
            totalCarb += ingredient.food.carbPer100g * factor
            totalFiber += (ingredient.food.fiberPer100g ?: 0.0) * factor
        }

        val per100 = if (totalGrams > 0) {
            FoodEntity(
                name = name.trim(),
                caloriesPer100g = totalCalories / totalGrams * 100,
                proteinPer100g = totalProtein / totalGrams * 100,
                animalProteinPer100g = if (totalAnimalProtein > 0) totalAnimalProtein / totalGrams * 100 else null,
                plantProteinPer100g = if (totalPlantProtein > 0) totalPlantProtein / totalGrams * 100 else null,
                fatPer100g = totalFat / totalGrams * 100,
                carbPer100g = totalCarb / totalGrams * 100,
                fiberPer100g = if (totalFiber > 0) totalFiber / totalGrams * 100 else null,
                note = note,
                isCustom = true,
                isCombined = true
            )
        } else {
            FoodEntity(
                name = name.trim(),
                caloriesPer100g = 0.0,
                proteinPer100g = 0.0,
                fatPer100g = 0.0,
                carbPer100g = 0.0,
                isCustom = true,
                isCombined = true,
                note = note
            )
        }

        val foodId = foodDao.insert(per100)

        val units = mutableListOf(
            FoodUnitEntity(foodId = foodId, name = "克", grams = 1.0, isBase = true, sortOrder = 0)
        )
        var sort = 1
        if (keepServing && totalGrams > 0) {
            units += FoodUnitEntity(foodId = foodId, name = "份", grams = totalGrams, isBase = false, sortOrder = sort++)
        }
        extraUnits.forEach { (unitName, grams) ->
            if (unitName.isNotBlank() && grams > 0) {
                units += FoodUnitEntity(foodId = foodId, name = unitName.trim(), grams = grams, isBase = false, sortOrder = sort++)
            }
        }
        unitDao.insertAll(units)

        componentDao.insertAll(components.map {
            CombinedComponentEntity(
                parentFoodId = foodId,
                ingredientFoodId = it.ingredientFoodId,
                unitId = it.unitId,
                unitName = it.unitName,
                unitGrams = it.unitGrams,
                quantity = it.quantity
            )
        })

        foodId
    }

    suspend fun deleteFood(id: Long) = database.withTransaction {
        val dependents = collectDependentIds(id)
        // Delete all dependent combined foods before the root.
        dependents.forEach { dependentId ->
            foodDao.getById(dependentId)?.let { foodDao.delete(it.food) }
        }
        foodDao.getById(id)?.let { foodDao.delete(it.food) }
    }

    private suspend fun collectDependentIds(rootId: Long): Set<Long> {
        val result = linkedSetOf<Long>()
        val queue = ArrayDeque<Long>()
        queue.add(rootId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            val parents = componentDao.getParentIdsUsingIngredient(current)
            for (parent in parents) {
                if (result.add(parent)) {
                    queue.add(parent)
                }
            }
        }
        result.remove(rootId)
        return result
    }

    suspend fun wouldCreateCycle(parentId: Long, ingredientId: Long): Boolean {
        if (parentId == 0L) return false
        if (parentId == ingredientId) return true
        val visited = mutableSetOf<Long>()
        val queue = ArrayDeque<Long>()
        queue.add(ingredientId)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (current == parentId) return true
            if (!visited.add(current)) continue
            val children = componentDao.getByParent(current)
            children.forEach { queue.add(it.ingredientFoodId) }
        }
        return false
    }

    suspend fun exportFoodsCsv(): String {
        val sb = StringBuilder()
        sb.appendLine("name,caloriesPer100g,proteinPer100g,animalProteinPer100g,plantProteinPer100g,fatPer100g,carbPer100g,fiberPer100g,note,isCustom,isCombined,units,micronutrients,components")
        val foods = foodDao.getAll()
        for (food in foods) {
            val units = unitDao.getByFood(food.id)
            val micros = microDao.getByFood(food.id)
            val components = componentDao.getByParent(food.id)
            val unitsJson = JSONArray()
            units.forEach { u ->
                unitsJson.put(JSONObject().put("name", u.name).put("grams", u.grams).put("isBase", u.isBase))
            }
            val microsJson = JSONArray()
            micros.forEach { m ->
                microsJson.put(JSONObject().put("name", m.name).put("amountPer100g", m.amountPer100g).put("unit", m.unit))
            }
            val compsJson = JSONArray()
            components.forEach { c ->
                val ingredientName = foodDao.getById(c.ingredientFoodId)?.food?.name ?: ""
                compsJson.put(
                    JSONObject()
                        .put("ingredientName", ingredientName)
                        .put("unitName", c.unitName)
                        .put("unitGrams", c.unitGrams)
                        .put("quantity", c.quantity)
                )
            }
            val row = listOf(
                csvEscape(food.name),
                formatNum(food.caloriesPer100g),
                formatNum(food.proteinPer100g),
                food.animalProteinPer100g?.let { formatNum(it) } ?: "",
                food.plantProteinPer100g?.let { formatNum(it) } ?: "",
                formatNum(food.fatPer100g),
                formatNum(food.carbPer100g),
                food.fiberPer100g?.let { formatNum(it) } ?: "",
                csvEscape(food.note.orEmpty()),
                food.isCustom.toString(),
                food.isCombined.toString(),
                csvEscape(unitsJson.toString()),
                csvEscape(microsJson.toString()),
                csvEscape(compsJson.toString())
            )
            sb.appendLine(row.joinToString(","))
        }
        return sb.toString()
    }

    suspend fun importFoodsCsv(csvText: String): FoodImportResult = database.withTransaction {
        var added = 0
        var updated = 0
        var skipped = 0
        val errors = mutableListOf<String>()
        val cleanText = csvText.removePrefix("\uFEFF")
        val lines = cleanText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .toList()
        if (lines.isEmpty()) return@withTransaction FoodImportResult()

        // First pass: food rows
        val rows = mutableListOf<List<String>>()
        for ((index, line) in lines.withIndex()) {
            if (index == 0 && line.startsWith("name,")) continue
            val cols = splitCsvLine(line)
            if (cols.size < 11) {
                errors.add("第${index + 1}行列数不足，已跳过")
                skipped++
                continue
            }
            rows.add(cols)
        }

        // Insert/update foods (without components)
        val idByName = mutableMapOf<String, Long>()
        for ((rowIndex, cols) in rows.withIndex()) {
            try {
                val name = cols[0].trim()
                if (name.isEmpty()) {
                    skipped++
                    continue
                }
                val existing = foodDao.findByName(name)
                val food = FoodEntity(
                    id = existing?.id ?: 0,
                    name = name,
                    caloriesPer100g = cols.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 0.0,
                    proteinPer100g = cols.getOrNull(2)?.trim()?.toDoubleOrNull() ?: 0.0,
                    animalProteinPer100g = cols.getOrNull(3)?.trim()?.toDoubleOrNull(),
                    plantProteinPer100g = cols.getOrNull(4)?.trim()?.toDoubleOrNull(),
                    fatPer100g = cols.getOrNull(5)?.trim()?.toDoubleOrNull() ?: 0.0,
                    carbPer100g = cols.getOrNull(6)?.trim()?.toDoubleOrNull() ?: 0.0,
                    fiberPer100g = cols.getOrNull(7)?.trim()?.toDoubleOrNull(),
                    note = cols.getOrNull(8)?.trim()?.ifEmpty { null },
                    isCustom = cols.getOrNull(9)?.trim()?.toBooleanStrictOrNull() ?: true,
                    isCombined = cols.getOrNull(10)?.trim()?.toBooleanStrictOrNull() ?: false,
                    createdAt = existing?.createdAt ?: System.currentTimeMillis()
                )
                val id = if (existing == null) {
                    foodDao.insert(food).also { added++ }
                } else {
                    foodDao.update(food)
                    existing.id.also { updated++ }
                }
                idByName[name] = id

                // Replace units/micros
                unitDao.deleteByFood(id)
                microDao.deleteByFood(id)
                val unitsJson = cols.getOrNull(11)?.trim().orEmpty().ifEmpty { "[]" }
                val unitsArr = try { JSONArray(unitsJson) } catch (e: Exception) { JSONArray() }
                for (i in 0 until unitsArr.length()) {
                    val obj = unitsArr.getJSONObject(i)
                    unitDao.insert(
                        FoodUnitEntity(
                            foodId = id,
                            name = obj.optString("name", "克"),
                            grams = obj.optDouble("grams", 1.0),
                            isBase = obj.optBoolean("isBase", false),
                            sortOrder = i
                        )
                    )
                }
                if (unitsArr.length() == 0) {
                    unitDao.insert(FoodUnitEntity(foodId = id, name = "克", grams = 1.0, isBase = true, sortOrder = 0))
                }
                componentDao.deleteByParent(id)
                val microsJson = cols.getOrNull(12)?.trim().orEmpty().ifEmpty { "[]" }
                val microsArr = try { JSONArray(microsJson) } catch (e: Exception) { JSONArray() }
                for (i in 0 until microsArr.length()) {
                    val obj = microsArr.getJSONObject(i)
                    microDao.insert(
                        MicronutrientEntity(
                            foodId = id,
                            name = obj.optString("name", ""),
                            amountPer100g = obj.optDouble("amountPer100g", 0.0),
                            unit = obj.optString("unit", "")
                        )
                    )
                }
            } catch (e: Exception) {
                errors.add("第${rowIndex + 2}行导入失败: ${e.message}")
                skipped++
            }
        }

        // Second pass: combined components
        for ((rowIndex, cols) in rows.withIndex()) {
            try {
                val name = cols[0].trim()
                val id = idByName[name] ?: continue
                val isCombined = cols.getOrNull(10)?.trim()?.toBooleanStrictOrNull() ?: false
                val compsJson = cols.getOrNull(13)?.trim().orEmpty().ifEmpty { "[]" }
                val compsArr = try { JSONArray(compsJson) } catch (e: Exception) { JSONArray() }
                if (!isCombined || compsArr.length() == 0) continue
                componentDao.deleteByParent(id)
                for (i in 0 until compsArr.length()) {
                    val obj = compsArr.getJSONObject(i)
                    val ingredientName = obj.optString("ingredientName", "")
                    val ingredientId = idByName[ingredientName] ?: foodDao.findByName(ingredientName)?.id
                        ?: continue
                    componentDao.insert(
                        CombinedComponentEntity(
                            parentFoodId = id,
                            ingredientFoodId = ingredientId,
                            unitId = null,
                            unitName = obj.optString("unitName", "克"),
                            unitGrams = obj.optDouble("unitGrams", 1.0),
                            quantity = obj.optDouble("quantity", 1.0)
                        )
                    )
                }
            } catch (e: Exception) {
                errors.add("组合食物第${rowIndex + 2}行组件导入失败: ${e.message}")
            }
        }

        FoodImportResult(added = added, updated = updated, skipped = skipped, errors = errors)
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    private fun splitCsvLine(line: String): List<String> {
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

    private fun formatNum(value: Double): String {
        return if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
    }
}
