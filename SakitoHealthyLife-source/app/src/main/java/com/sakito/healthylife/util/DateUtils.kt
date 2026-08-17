package com.sakito.healthylife.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

object DateUtils {

    val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun today(): String = LocalDate.now().toString()

    fun parse(date: String): LocalDate? = try {
        LocalDate.parse(date, isoFormatter)
    } catch (e: Exception) {
        null
    }

    fun addDays(date: String, days: Long): String {
        val d = parse(date) ?: LocalDate.now()
        return d.plusDays(days).toString()
    }

    fun formatChinese(date: String): String {
        val d = parse(date) ?: return date
        return "${d.year}年${d.monthValue}月${d.dayOfMonth}日"
    }

    fun formatShort(date: String): String {
        val d = parse(date) ?: return date
        return "${d.monthValue}月${d.dayOfMonth}日"
    }

    fun daysBetween(start: String, end: String): Long {
        val s = parse(start) ?: return 0
        val e = parse(end) ?: return 0
        return ChronoUnit.DAYS.between(s, e)
    }

    fun presetRange(preset: String): Pair<String, String> {
        val today = LocalDate.now()
        return when (preset) {
            "最近一周" -> today.minusDays(6).toString() to today.toString()
            "最近一月" -> today.minusMonths(1).toString() to today.toString()
            "最近三个月" -> today.minusMonths(3).toString() to today.toString()
            "全部" -> "0000-01-01" to today.toString()
            else -> today.minusDays(29).toString() to today.toString()
        }
    }
}
