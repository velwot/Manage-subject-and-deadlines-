package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object DateUtils {

    private val displayDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    fun getCurrentDateFormatted(): String {
        return displayDateFormat.format(Date())
    }

    fun getCurrentIsoDate(): String {
        return isoDateFormat.format(Date())
    }

    fun getIsoDatePlusDays(days: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return isoDateFormat.format(cal.time)
    }

    fun formatDisplayDate(timestamp: Long): String {
        return displayDateFormat.format(Date(timestamp))
    }

    fun formatIsoToDisplay(isoDate: String): String {
        return try {
            val date = isoDateFormat.parse(isoDate)
            if (date != null) displayDateFormat.format(date) else isoDate
        } catch (e: Exception) {
            isoDate
        }
    }

    fun getDaysRemaining(targetIsoDate: String): Int {
        return try {
            val targetDate = isoDateFormat.parse(targetIsoDate) ?: return 0
            val targetCal = Calendar.getInstance().apply {
                time = targetDate
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }

            val nowCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val diffMillis = targetCal.timeInMillis - nowCal.timeInMillis
            (diffMillis / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
        } catch (e: Exception) {
            0
        }
    }
}
