package com.docuwallet.app.utils

import com.docuwallet.app.domain.model.AlertLevel
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object DateUtils {
    private const val DATE_FORMAT = "dd/MM/yyyy"

    fun getCurrentDate(): String {
        val formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return formatter.format(Date())
    }

    fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return formatter.format(date)
    }

    fun parseDate(dateString: String): Date? {
        return try {
            val formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
            formatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    fun calculateDaysRemaining(expirationDate: String): Int {
        if (expirationDate.isEmpty()) return Int.MAX_VALUE

        val expDate = parseDate(expirationDate) ?: return Int.MAX_VALUE
        val today = Date()

        val diff = expDate.time - today.time
        return TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS).toInt()
    }

    fun getAlertLevel(daysRemaining: Int): AlertLevel {
        return when {
            daysRemaining < 0 -> AlertLevel.EXPIRED
            daysRemaining <= Constants.DAYS_URGENT_THRESHOLD -> AlertLevel.URGENT
            daysRemaining <= Constants.DAYS_WARNING_THRESHOLD -> AlertLevel.WARNING
            else -> AlertLevel.SAFE
        }
    }

    fun formatDaysRemaining(days: Int): String {
        return when {
            days < 0 -> "Vencido hace ${-days} días"
            days == 0 -> "Vence hoy"
            days == 1 -> "Vence mañana"
            days <= 7 -> "Vence en $days días"
            days <= 30 -> "Vence en ${days / 7} semanas"
            else -> "Vence en ${days / 30} meses"
        }
    }
}