package com.tera.friendlist.utils

import android.content.Context
import android.util.Log
import com.tera.friendlist.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DateControl(private val context: Context) {

    private val sep = context.getString(R.string.separator)
    private val pattern = "dd" + sep + "MM" + sep + "yyyy"

    // Проверка ввода даты
    fun inputDate(dateCh: String): String {
        if (dateCh.isEmpty()) return ""

        var error = ""
        var day = 0
        var mon: Int
        var dayStr = ""
        var monStr: String

        val len = dateCh.length

        try {
            // День
            if (len > 1) {
                dayStr = dateCh.take(2)
                day = dayStr.toInt()
            }
            if (dayStr == "00") {
                error = context.getString(R.string.error_d0)
            }
            if (day > 31) {
                error = context.getString(R.string.error_31)
            }

            // Неделя
            if (len > 4) {
                monStr = dateCh.substring(3, 5)
                mon = monStr.toInt()
                if (monStr == "00") {
                    error = context.getString(R.string.error_m0)
                }
                if (mon > 12) {
                    error = context.getString(R.string.error_12)
                }
            }

            // Год
            if (len > 6) {
                val yearS = dateCh.substringAfterLast(sep)
                if ('0' == yearS[0]) error = context.getString(R.string.error_0)
                if ('3' == yearS[0]) error = context.getString(R.string.error_3)
            }

            if (len == 10) {
                val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                val currentDate = sdf.format(Date())
                val firstDate: Date = sdf.parse(dateCh) as Date
                val secondDate: Date = sdf.parse(currentDate) as Date

                if (firstDate.after(secondDate))
                    return context.getString(R.string.error_date_after)
            }

        } catch (e: Exception) {
            error = context.getString(R.string.error_other)
            Log.d("myLogs", "Error: $e")
        }
        return error
    }

    //-------------------
    // Проверка поля даты рождения
    fun checkDate(dateCh: String): String {
        if (dateCh.isEmpty()) return ""

        var len = dateCh.length

        // Месяц
        if (len < 3) return context.getString(R.string.error_mon_empty)
        var monStr: String = dateCh.substringAfter(sep)
        val keySep: Boolean = monStr.contains(sep)
        monStr = if (keySep)
            monStr.substringBefore(sep)
        else {
            len = monStr.length
            monStr.take(len)
        }
        val mon = monStr.toInt()

        if (mon == 0) return context.getString(R.string.error_m0)

        // Год
        if (len in 7..9) return context.getString(R.string.error_y4)

        return ""
    }

    // Проверка поля даты смерти
    fun checkYearDeath(birthDate: String, dateDeath: String): String {
        if (birthDate.isEmpty() || dateDeath.isEmpty()) return ""
        if (birthDate.length < 6 || dateDeath.length < 6) return ""
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        val firstDate: Date = sdf.parse(birthDate) as Date
        val secondDate: Date = sdf.parse(dateDeath) as Date

        if (firstDate.after(secondDate))
            return context.getString(R.string.error_date_death)

        return ""
    }

}













