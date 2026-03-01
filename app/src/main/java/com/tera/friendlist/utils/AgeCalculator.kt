package com.tera.friendlist.utils

import android.content.Context
import android.util.Log
import com.tera.friendlist.R
import java.io.IOException
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

class AgeCalculator(private val context: Context) {

    private val sep = context.getString(R.string.separator)
    private val pattern = "dd" + sep + "MM" + sep + "yyyy"

    // Проверит разделитель
    fun checkSeparator(list: ArrayList<ModelPerson>): ArrayList<ModelPerson> {
        val listEmpty = ArrayList<ModelPerson>()
        if (list.isEmpty()) return listEmpty

        var separator = ""
        var date: String
        var dateDeath: String

        for (i in list.indices) {
            var pos = 0
            date = list[i].date
            dateDeath = list[i].died
            if (date.isNotEmpty()) {
                while (date[pos].isDigit()) {
                    pos++
                }
                separator = date[pos].toString()
                break
            } else if (dateDeath.isNotEmpty()) {
                while (dateDeath[pos].isDigit()) {
                    pos++
                }
                separator = dateDeath[pos].toString()
                break
            }
        }

        if (separator == sep || separator.isEmpty()) return list

        for (i in list.indices) {
            date = list[i].date
            dateDeath = list[i].died

            if (date.isNotEmpty()) date = date.replace(separator, sep)
            if (dateDeath.isNotEmpty()) dateDeath = dateDeath.replace(separator, sep)

            list[i].date = date
            list[i].died = dateDeath
        }
        return list
    }

    // Возраст
    fun getAge(birthDate: String, deathDate: String): String {
        if (birthDate.isEmpty()) return ""
//        Log.d("mylogs", "getAge, deathDate: $deathDate")

        // Возраст покойного
        if (deathDate.isNotEmpty()) {
            return ageDeceased(birthDate, deathDate)
        }

        val date = showZero(birthDate)
        if (date.length < 6) return ""

        val ageStr = context.getString(R.string.age)
        val formatter = DateTimeFormatter.ofPattern(pattern)
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        val currentDate = sdf.format(Date())
        val from = LocalDate.parse(date, formatter)
        val to = LocalDate.parse(currentDate, formatter)
        val period = Period.between(from, to)
        val age = period.years

        return "$ageStr ${ageDeclension(age)}"
    }

    // Возраст покойного
    private fun ageDeceased(dateBirth: String, dateDeath: String): String {
        var passed: String
        if (dateBirth.length < 6 && dateDeath.length > 6) {
            passed = goneSinceDeath(dateDeath)
            return passed
        }
        if (dateBirth.length < 6 || dateDeath.length < 6) return ""

        val formatter = DateTimeFormatter.ofPattern(pattern)
        val date1 = showZero(dateBirth)
        val date2 = showZero(dateDeath)

        var yearsWasStr = ""
        val wasStr = context.getString(R.string.was)
        // Было
        try {
            val from = LocalDate.parse(date1, formatter)
            val to = LocalDate.parse(date2, formatter)
            val period = Period.between(from, to)
            val yearsWas = period.years
            yearsWasStr = ageDeclension(yearsWas)
        } catch (e: IOException) {
            Log.d("mylogs", "ageDeceased, Error: $e")
        }

        // Прошло
        passed = goneSinceDeath(dateDeath)

        return "$wasStr $yearsWasStr. $passed"
    }

    // Прошло со дня смерти
    private fun goneSinceDeath(dateDeath: String): String {

        val passStr = context.getString(R.string.passed)
        val date = showZero(dateDeath)

        val day = date.take(2)
        val mon = date.substring(3, 5)
        val year = date.substring(date.length - 4)

        val input = "$year-$mon-$day"
        val futureDate = LocalDate.parse(input)
        // Разница в днях
        var days = (ChronoUnit.DAYS.between(LocalDate.now(), futureDate)).toInt()// Long
        var years = (ChronoUnit.YEARS.between(LocalDate.now(), futureDate)).toInt()
        days = -days
        years = -years
        var pass: String
        if (years > 0) {
            pass = ageDeclension(years) // Лет
        } else if (days > 50) {
            val mons = days / 30.5
            var monsStr = "%.1f".format(mons)
            monsStr = monsStr.replace(',', '.')
            val mon1 = monsStr.substringBefore('.').toInt()
            pass = "$monsStr ${monDeclension(mon1)}" // Месяцев
        } else
            pass = dayDeclension(days + 1) // Дней

        return "$passStr $pass"
    }

    // Поминки
    fun getWake(passed: String, dateA: String): Boolean {
        if (dateA.length < 6) return false

        val date = showZero(dateA)
        val keyWake = passed.contains("40")
        if (keyWake)
            return true

        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        val currentDate = sdf.format(Date())

        val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(pattern)

        val from = LocalDate.parse(date, dateFormatter)
        val to = LocalDate.parse(currentDate, dateFormatter)

        val period = Period.between(from, to)
        val years = period.years
        val months = period.months
        val days = period.days

        if (years == 0 && months == 0)
            if (days + 1 == 3 || days + 1 == 9)
                return true

        if (years == 1 && days == 0 && months == 0)
            return true

        return false
    }

    private val d1 = context.getString(R.string.d1)
    private val d2 = context.getString(R.string.d2)
    private val d5 = context.getString(R.string.d5)

    // Склонение дней
    private fun dayDeclension(day: Int): String =
        when {
            day % 100 in 11..14 -> "$day $d5"
            day % 10 == 1 -> "$day $d1"
            day % 10 in 2..4 -> "$day $d2"
            else -> "$day $d5"
        }

    private val m1 = context.getString(R.string.m1)
    private val m2 = context.getString(R.string.m2)
    private val m5 = context.getString(R.string.m5)

    // Склонение месяцев
    private fun monDeclension(mon: Int): String =
        when {
            mon % 100 in 11..14 -> m5
            mon % 10 == 1 -> m1
            mon % 10 in 2..4 -> m2
            else -> m5
        }

    private val y1 = context.getString(R.string.y1)
    private val y2 = context.getString(R.string.y2)
    private val y5 = context.getString(R.string.y5)

    // Склонение года
    private fun ageDeclension(age: Int): String =
        when {
            age % 100 in 11..14 -> "$age $y5"
            age % 10 == 1 -> "$age $y1"
            age % 10 in 2..4 -> "$age $y2"
            else -> "$age $y5"
        }

    // До дня рождения осталось
    fun dayLeft(birthDate: String): String {
        if (birthDate.isEmpty()) return ""
        val date = showZero(birthDate)
        val left = context.getString(R.string.left)
        val day = date.take(2)
        val mon = date.substring(3, 5)
        val sdf = SimpleDateFormat("yyyy", Locale.getDefault())
        val currYear = sdf.format(Date())

        val input = "$currYear-$mon-$day"
        val futureDate = LocalDate.parse(input)
        // Разница в днях
        val diffDays = (ChronoUnit.DAYS.between(LocalDate.now(), futureDate)).toInt()

        if (diffDays !in 0..31) return ""

        if (diffDays == 0) {
            return context.getString(R.string.today)
        }
        return "$left ${dayDeclension(diffDays)}"
//        return ""
    }


    fun hideZero(date: String): String {
        if (date.isEmpty()) return ""

        var day = date.substringBefore(sep)
        if ('0' == day[0]) {
            day = day[1].toString()
        }

        var year = date.substringAfter(sep)
        var mon = year.substringBefore(sep)
        if ('0' == mon[0]) {
            mon = mon[1].toString()
        }

        val keyDot = year.contains(sep)
        return if (keyDot) {
            year = year.substringAfterLast(sep)
            day + sep + mon + sep + year
        } else {
            day + sep + mon
        }
    }

    fun showZero(date: String): String {
        if (date.isEmpty()) return ""

        var day = date.substringBefore(sep)
        if (day.length == 1)
            day = "0$day"

        var year = date.substringAfter(sep)
        var mon = year.substringBefore(sep)
        if (mon.length == 1)
            mon = "0$mon"

        val keyDot = year.contains(sep)

        return if (keyDot) {
            year = year.substringAfterLast(sep)
            day + sep + mon + sep + year
        } else {
            day + sep + mon
        }
    }


}