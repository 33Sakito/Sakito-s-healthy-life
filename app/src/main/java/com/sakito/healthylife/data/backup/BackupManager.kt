package com.sakito.healthylife.data.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.sakito.healthylife.HealthyLifeApp
import com.sakito.healthylife.data.local.BodyDimensionTypeEntity
import com.sakito.healthylife.data.local.BodyMeasurementEntity
import com.sakito.healthylife.data.local.CombinedComponentEntity
import com.sakito.healthylife.data.local.DietEntryEntity
import com.sakito.healthylife.data.local.DietRecordEntity
import com.sakito.healthylife.data.local.FoodEntity
import com.sakito.healthylife.data.local.FoodUnitEntity
import com.sakito.healthylife.data.local.MicronutrientEntity
import com.sakito.healthylife.data.local.SavedRangeEntity
import com.sakito.healthylife.data.local.WeightRecordEntity
import com.sakito.healthylife.data.settings.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupManager(private val app: HealthyLifeApp) {

    private val db = app.database

    suspend fun exportZip(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val zipBytes = buildZipBytes()
            app.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(zipBytes)
            } ?: error("无法打开导出文件")
        }
    }

    suspend fun importZip(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val input = app.contentResolver.openInputStream(uri) ?: error("无法打开备份文件")
            val backupJson = readBackupJson(input)
            restore(backupJson)
        }
    }

    private suspend fun buildZipBytes(): ByteArray {
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zip ->
            // backup.json
            val backupJson = buildBackupJson().toString()
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(backupJson.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // foods.csv
            val foodsCsv = app.foodRepository.exportFoodsCsv()
            zip.putNextEntry(ZipEntry("foods.csv"))
            zip.write(("\uFEFF" + foodsCsv).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // diet_records.csv
            val allRecords = db.dietRecordDao().getBetween(MIN_DATE, MAX_DATE)
            val dietCsv = CsvManager.dietRecordsCsv(allRecords)
            zip.putNextEntry(ZipEntry("diet_records.csv"))
            zip.write(("\uFEFF" + dietCsv).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // weight_records.csv
            val weights = db.weightRecordDao().getBetween(MIN_DATE, MAX_DATE)
            val weightCsv = CsvManager.weightRecordsCsv(weights)
            zip.putNextEntry(ZipEntry("weight_records.csv"))
            zip.write(("\uFEFF" + weightCsv).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // body_measurements.csv
            val measurements = db.bodyMeasurementDao().getBetween(MIN_DATE, MAX_DATE)
            val typeMap = db.bodyDimensionTypeDao().getAll().associate { it.id to it.name }
            val measurementCsv = CsvManager.bodyMeasurementsCsv(measurements) { typeMap[it] ?: "" }
            zip.putNextEntry(ZipEntry("body_measurements.csv"))
            zip.write(("\uFEFF" + measurementCsv).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return bos.toByteArray()
    }

    private suspend fun buildBackupJson(): JSONObject {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val settings = app.settingsRepository.settings.first()
        root.put("settings", buildSettingsJson(settings))

        val foodsArr = JSONArray()
        val foods = db.foodDao().getAll()
        for (food in foods) {
            val foodObj = foodToJson(food)
            foodsArr.put(foodObj)
        }
        root.put("foods", foodsArr)

        val dietArr = JSONArray()
        val records = db.dietRecordDao().getBetween(MIN_DATE, MAX_DATE)
        for (record in records) {
            val recordObj = JSONObject()
                .put("id", record.record.id)
                .put("date", record.record.date)
                .put("createdAt", record.record.createdAt)
                .put("mealType", record.record.mealType)
                .put("note", record.record.note ?: "")
            val entriesArr = JSONArray()
            record.entries.forEach { e ->
                entriesArr.put(
                    JSONObject()
                        .put("foodId", e.foodId ?: JSONObject.NULL)
                        .put("foodName", e.foodName)
                        .put("unitName", e.unitName)
                        .put("unitGrams", e.unitGrams)
                        .put("quantity", e.quantity)
                        .put("actualWeight", e.actualWeight)
                        .put("calories", e.calories)
                        .put("protein", e.protein)
                        .put("animalProtein", e.animalProtein ?: JSONObject.NULL)
                        .put("plantProtein", e.plantProtein ?: JSONObject.NULL)
                        .put("fat", e.fat)
                        .put("carb", e.carb)
                        .put("fiber", e.fiber ?: JSONObject.NULL)
                        .put("note", e.note ?: "")
                )
            }
            recordObj.put("entries", entriesArr)
            dietArr.put(recordObj)
        }
        root.put("dietRecords", dietArr)

        val weightArr = JSONArray()
        db.weightRecordDao().getBetween(MIN_DATE, MAX_DATE).forEach { w ->
            weightArr.put(
                JSONObject()
                    .put("id", w.id)
                    .put("date", w.date)
                    .put("createdAt", w.createdAt)
                    .put("weightKg", w.weightKg)
                    .put("note", w.note ?: "")
            )
        }
        root.put("weightRecords", weightArr)

        val dimArr = JSONArray()
        db.bodyDimensionTypeDao().getAll().forEach { d ->
            dimArr.put(
                JSONObject()
                    .put("id", d.id)
                    .put("name", d.name)
                    .put("isCustom", d.isCustom)
                    .put("sortOrder", d.sortOrder)
            )
        }
        root.put("bodyDimensionTypes", dimArr)

        val measurementArr = JSONArray()
        db.bodyMeasurementDao().getBetween(MIN_DATE, MAX_DATE).forEach { m ->
            measurementArr.put(
                JSONObject()
                    .put("id", m.id)
                    .put("date", m.date)
                    .put("createdAt", m.createdAt)
                    .put("dimensionTypeId", m.dimensionTypeId)
                    .put("valueCm", m.valueCm)
                    .put("note", m.note ?: "")
            )
        }
        root.put("bodyMeasurements", measurementArr)

        val rangesArr = JSONArray()
        db.savedRangeDao().getAll().forEach { r ->
            rangesArr.put(
                JSONObject()
                    .put("id", r.id)
                    .put("name", r.name)
                    .put("startDate", r.startDate)
                    .put("endDate", r.endDate)
                    .put("createdAt", r.createdAt)
            )
        }
        root.put("savedRanges", rangesArr)

        return root
    }

    private suspend fun foodToJson(food: FoodEntity): JSONObject {
        val units = db.foodUnitDao().getByFood(food.id)
        val micros = db.micronutrientDao().getByFood(food.id)
        val components = db.combinedComponentDao().getByParent(food.id)

        val unitsArr = JSONArray()
        units.forEach { u ->
            unitsArr.put(JSONObject().put("name", u.name).put("grams", u.grams).put("isBase", u.isBase))
        }
        val microsArr = JSONArray()
        micros.forEach { m ->
            microsArr.put(JSONObject().put("name", m.name).put("amountPer100g", m.amountPer100g).put("unit", m.unit))
        }
        val compsArr = JSONArray()
        components.forEach { c ->
            compsArr.put(
                JSONObject()
                    .put("ingredientFoodId", c.ingredientFoodId)
                    .put("unitId", c.unitId ?: JSONObject.NULL)
                    .put("unitName", c.unitName)
                    .put("unitGrams", c.unitGrams)
                    .put("quantity", c.quantity)
            )
        }

        return JSONObject()
            .put("id", food.id)
            .put("name", food.name)
            .put("caloriesPer100g", food.caloriesPer100g)
            .put("proteinPer100g", food.proteinPer100g)
            .put("animalProteinPer100g", food.animalProteinPer100g ?: JSONObject.NULL)
            .put("plantProteinPer100g", food.plantProteinPer100g ?: JSONObject.NULL)
            .put("fatPer100g", food.fatPer100g)
            .put("carbPer100g", food.carbPer100g)
            .put("fiberPer100g", food.fiberPer100g ?: JSONObject.NULL)
            .put("note", food.note ?: "")
            .put("isCustom", food.isCustom)
            .put("isCombined", food.isCombined)
            .put("usageCount", food.usageCount)
            .put("lastUsedAt", food.lastUsedAt ?: JSONObject.NULL)
            .put("createdAt", food.createdAt)
            .put("units", unitsArr)
            .put("micronutrients", microsArr)
            .put("components", compsArr)
    }

    private fun buildSettingsJson(s: AppSettings): JSONObject {
        return JSONObject()
            .put("decimalPlaces", s.decimalPlaces)
            .put("defaultMeal", s.defaultMeal)
            .put("trendDefaultValue", s.trendDefaultValue)
            .put("reminderEnabled", s.reminderEnabled)
            .put("reminderHour", s.reminderHour)
            .put("reminderMinute", s.reminderMinute)
    }

    private fun readBackupJson(input: InputStream): JSONObject {
        var backupJson: JSONObject? = null
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == "backup.json") {
                    val text = zip.readBytes().toString(Charsets.UTF_8)
                    backupJson = JSONObject(text)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return backupJson ?: error("备份文件中缺少 backup.json")
    }

    private suspend fun restore(root: JSONObject) {
        db.clearAllTables()
        db.withTransaction {
        val settingsObj = root.optJSONObject("settings")
        if (settingsObj != null) {
            val repo = app.settingsRepository
            repo.setDecimalPlaces(settingsObj.optInt("decimalPlaces", 1))
            repo.setDefaultMeal(settingsObj.optString("defaultMeal", "未分餐"))
            repo.setTrendDefaultValue(settingsObj.optString("trendDefaultValue", "latest"))
            repo.setReminderEnabled(settingsObj.optBoolean("reminderEnabled", true))
            repo.setReminderTime(settingsObj.optInt("reminderHour", 20), settingsObj.optInt("reminderMinute", 0))
            app.reminderScheduler.reschedule(
                AppSettings(
                    decimalPlaces = settingsObj.optInt("decimalPlaces", 1),
                    defaultMeal = settingsObj.optString("defaultMeal", "未分餐"),
                    trendDefaultValue = settingsObj.optString("trendDefaultValue", "latest"),
                    reminderEnabled = settingsObj.optBoolean("reminderEnabled", true),
                    reminderHour = settingsObj.optInt("reminderHour", 20),
                    reminderMinute = settingsObj.optInt("reminderMinute", 0)
                )
            )
        }

        // Foods
        val foodsArr = root.optJSONArray("foods") ?: JSONArray()
        val foodIdMap = mutableMapOf<Long, Long>() // old id -> new id
        for (i in 0 until foodsArr.length()) {
            val obj = foodsArr.getJSONObject(i)
            val newId = db.foodDao().insert(
                FoodEntity(
                    name = obj.optString("name"),
                    caloriesPer100g = obj.optDouble("caloriesPer100g", 0.0),
                    proteinPer100g = obj.optDouble("proteinPer100g", 0.0),
                    animalProteinPer100g = optNullableDouble(obj, "animalProteinPer100g"),
                    plantProteinPer100g = optNullableDouble(obj, "plantProteinPer100g"),
                    fatPer100g = obj.optDouble("fatPer100g", 0.0),
                    carbPer100g = obj.optDouble("carbPer100g", 0.0),
                    fiberPer100g = optNullableDouble(obj, "fiberPer100g"),
                    note = obj.optString("note").ifEmpty { null },
                    isCustom = obj.optBoolean("isCustom", true),
                    isCombined = obj.optBoolean("isCombined", false),
                    usageCount = obj.optInt("usageCount", 0),
                    lastUsedAt = optNullableLong(obj, "lastUsedAt"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
            foodIdMap[obj.getLong("id")] = newId

            val unitsArr = obj.optJSONArray("units") ?: JSONArray()
            for (j in 0 until unitsArr.length()) {
                val u = unitsArr.getJSONObject(j)
                db.foodUnitDao().insert(
                    FoodUnitEntity(
                        foodId = newId,
                        name = u.optString("name", "克"),
                        grams = u.optDouble("grams", 1.0),
                        isBase = u.optBoolean("isBase", false),
                        sortOrder = j
                    )
                )
            }
            if (unitsArr.length() == 0) {
                db.foodUnitDao().insert(FoodUnitEntity(foodId = newId, name = "克", grams = 1.0, isBase = true, sortOrder = 0))
            }

            val microsArr = obj.optJSONArray("micronutrients") ?: JSONArray()
            for (j in 0 until microsArr.length()) {
                val m = microsArr.getJSONObject(j)
                db.micronutrientDao().insert(
                    MicronutrientEntity(
                        foodId = newId,
                        name = m.optString("name"),
                        amountPer100g = m.optDouble("amountPer100g", 0.0),
                        unit = m.optString("unit")
                    )
                )
            }
        }

        // Combined components (second pass)
        for (i in 0 until foodsArr.length()) {
            val obj = foodsArr.getJSONObject(i)
            val oldId = obj.getLong("id")
            val newId = foodIdMap[oldId] ?: continue
            val compsArr = obj.optJSONArray("components") ?: JSONArray()
            for (j in 0 until compsArr.length()) {
                val c = compsArr.getJSONObject(j)
                val oldIngredientId = c.getLong("ingredientFoodId")
                val newIngredientId = foodIdMap[oldIngredientId] ?: continue
                db.combinedComponentDao().insert(
                    CombinedComponentEntity(
                        parentFoodId = newId,
                        ingredientFoodId = newIngredientId,
                        unitId = optNullableLong(c, "unitId"),
                        unitName = c.optString("unitName", "克"),
                        unitGrams = c.optDouble("unitGrams", 1.0),
                        quantity = c.optDouble("quantity", 1.0)
                    )
                )
            }
        }

        // Dimension types
        val dimArr = root.optJSONArray("bodyDimensionTypes") ?: JSONArray()
        val dimIdMap = mutableMapOf<Long, Long>()
        for (i in 0 until dimArr.length()) {
            val obj = dimArr.getJSONObject(i)
            val newId = db.bodyDimensionTypeDao().insert(
                BodyDimensionTypeEntity(
                    name = obj.optString("name"),
                    isCustom = obj.optBoolean("isCustom", true),
                    sortOrder = obj.optInt("sortOrder", i)
                )
            )
            dimIdMap[obj.getLong("id")] = newId
        }

        // Weight records
        val weightArr = root.optJSONArray("weightRecords") ?: JSONArray()
        for (i in 0 until weightArr.length()) {
            val obj = weightArr.getJSONObject(i)
            db.weightRecordDao().insert(
                WeightRecordEntity(
                    date = obj.optString("date"),
                    weightKg = obj.optDouble("weightKg", 0.0),
                    note = obj.optString("note").ifEmpty { null },
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Body measurements
        val measurementArr = root.optJSONArray("bodyMeasurements") ?: JSONArray()
        for (i in 0 until measurementArr.length()) {
            val obj = measurementArr.getJSONObject(i)
            val newDimId = dimIdMap[obj.optLong("dimensionTypeId")] ?: continue
            db.bodyMeasurementDao().insert(
                BodyMeasurementEntity(
                    date = obj.optString("date"),
                    dimensionTypeId = newDimId,
                    valueCm = obj.optDouble("valueCm", 0.0),
                    note = obj.optString("note").ifEmpty { null },
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }

        // Diet records
        val dietArr = root.optJSONArray("dietRecords") ?: JSONArray()
        for (i in 0 until dietArr.length()) {
            val obj = dietArr.getJSONObject(i)
            val recordId = db.dietRecordDao().insert(
                DietRecordEntity(
                    date = obj.optString("date"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    mealType = obj.optString("mealType", "未分餐"),
                    note = obj.optString("note").ifEmpty { null }
                )
            )
            val entriesArr = obj.optJSONArray("entries") ?: JSONArray()
            for (j in 0 until entriesArr.length()) {
                val e = entriesArr.getJSONObject(j)
                val oldFoodId = optNullableLong(e, "foodId")
                db.dietEntryDao().insert(
                    DietEntryEntity(
                        recordId = recordId,
                        foodId = oldFoodId?.let { foodIdMap[it] },
                        foodName = e.optString("foodName"),
                        unitName = e.optString("unitName", "克"),
                        unitGrams = e.optDouble("unitGrams", 1.0),
                        quantity = e.optDouble("quantity", 1.0),
                        actualWeight = e.optDouble("actualWeight", 0.0),
                        calories = e.optDouble("calories", 0.0),
                        protein = e.optDouble("protein", 0.0),
                        animalProtein = optNullableDouble(e, "animalProtein"),
                        plantProtein = optNullableDouble(e, "plantProtein"),
                        fat = e.optDouble("fat", 0.0),
                        carb = e.optDouble("carb", 0.0),
                        fiber = optNullableDouble(e, "fiber"),
                        note = e.optString("note").ifEmpty { null }
                    )
                )
            }
        }

        // Saved ranges
        val rangesArr = root.optJSONArray("savedRanges") ?: JSONArray()
        for (i in 0 until rangesArr.length()) {
            val obj = rangesArr.getJSONObject(i)
            db.savedRangeDao().insert(
                SavedRangeEntity(
                    name = obj.optString("name"),
                    startDate = obj.optString("startDate"),
                    endDate = obj.optString("endDate"),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            )
        }
        }
    }

    private fun optNullableDouble(obj: JSONObject, key: String): Double? {
        return if (obj.isNull(key)) null else obj.optDouble(key)
    }

    private fun optNullableLong(obj: JSONObject, key: String): Long? {
        return if (obj.isNull(key)) null else obj.optLong(key)
    }

    companion object {
        private const val MIN_DATE = "0000-01-01"
        private const val MAX_DATE = "9999-12-31"
    }
}
