package com.example.ui.util

import com.example.data.KanbanTask
import com.example.data.TaskStatus
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ScheduleBucket {
    OVERDUE,
    TODAY,
    UPCOMING,
    UNSCHEDULED,
    DONE
}

private val ptBrLocale = Locale("pt", "BR")

fun classifyScheduleBucket(task: KanbanTask, nowMillis: Long = System.currentTimeMillis()): ScheduleBucket {
    if (task.statusEnum() == TaskStatus.CONCLUIDO) return ScheduleBucket.DONE
    val dueMillis = task.scheduledAt?.toDate()?.time ?: return ScheduleBucket.UNSCHEDULED

    val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val dueCal = Calendar.getInstance().apply { timeInMillis = dueMillis }

    if (dueMillis < nowMillis && !isSameDay(nowCal, dueCal)) {
        return ScheduleBucket.OVERDUE
    }
    if (isSameDay(nowCal, dueCal)) {
        return if (dueMillis < nowMillis) ScheduleBucket.OVERDUE else ScheduleBucket.TODAY
    }
    return ScheduleBucket.UPCOMING
}

fun isTaskOverdue(task: KanbanTask, nowMillis: Long = System.currentTimeMillis()): Boolean {
    if (task.statusEnum() == TaskStatus.CONCLUIDO) return false
    val dueMillis = task.scheduledAt?.toDate()?.time ?: return false
    return dueMillis < nowMillis
}

fun formatScheduleBadge(timestamp: Timestamp?, nowMillis: Long = System.currentTimeMillis()): String? {
    val date = timestamp?.toDate() ?: return null
    val nowCal = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val targetCal = Calendar.getInstance().apply { time = date }
    val tomorrowCal = Calendar.getInstance().apply {
        timeInMillis = nowMillis
        add(Calendar.DAY_OF_YEAR, 1)
    }

    val timePart = SimpleDateFormat("HH:mm", ptBrLocale).format(date)
    return when {
        isSameDay(nowCal, targetCal) -> "Hoje • $timePart"
        isSameDay(tomorrowCal, targetCal) -> "Amanhã • $timePart"
        nowCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR) -> {
            val dayPart = SimpleDateFormat("dd MMM", ptBrLocale).format(date)
            "$dayPart • $timePart"
        }
        else -> {
            val fullPart = SimpleDateFormat("dd/MM/yy", ptBrLocale).format(date)
            "$fullPart • $timePart"
        }
    }
}

fun formatDateOnly(millis: Long): String {
    return SimpleDateFormat("EEE, dd MMM yyyy", ptBrLocale).format(Date(millis))
}

fun formatTimeOnly(millis: Long): String {
    return SimpleDateFormat("HH:mm", ptBrLocale).format(Date(millis))
}

fun combineUtcDateWithLocalTime(utcDateMillis: Long, hour: Int, minute: Int): Long {
    val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        timeInMillis = utcDateMillis
    }
    val localCal = Calendar.getInstance().apply {
        set(Calendar.YEAR, utcCal.get(Calendar.YEAR))
        set(Calendar.MONTH, utcCal.get(Calendar.MONTH))
        set(Calendar.DAY_OF_MONTH, utcCal.get(Calendar.DAY_OF_MONTH))
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    return localCal.timeInMillis
}

fun quickScheduleMillis(daysFromToday: Int, defaultHour: Int = 18, defaultMinute: Int = 0): Long {
    val cal = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, daysFromToday)
        set(Calendar.HOUR_OF_DAY, defaultHour)
        set(Calendar.MINUTE, defaultMinute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    if (daysFromToday == 0 && cal.timeInMillis < System.currentTimeMillis()) {
        cal.timeInMillis = System.currentTimeMillis() + 60 * 60 * 1000L
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
    }
    return cal.timeInMillis
}

private fun isSameDay(a: Calendar, b: Calendar): Boolean {
    return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
        a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}
