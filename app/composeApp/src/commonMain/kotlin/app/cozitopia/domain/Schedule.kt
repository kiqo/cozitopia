package app.cozitopia.domain

import kotlin.math.roundToInt
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

object Schedule {
    private val FREQUENCY = Regex("""(\d+)\s*x\s*(?:pro|per|par|/)?\s*(\p{L}+)\s*(?:\((.+)\))?""", RegexOption.IGNORE_CASE)
    private val DAYS_PER_MONTH = 30
    private val DAYS_PER_WEEK = 7

    private val UNIT_WORDS = mapOf(
        RecurrenceUnit.DAY to listOf("tag", "day", "jour"),
        RecurrenceUnit.WEEK to listOf("woche", "week", "semaine"),
        RecurrenceUnit.MONTH to listOf("monat", "month", "mois"),
        RecurrenceUnit.YEAR to listOf("jahr", "year", "an"),
    )

    private val SEASON_MONTHS = mapOf(
        "frühling" to 4, "spring" to 4, "printemps" to 4,
        "sommer" to 7, "summer" to 7, "été" to 7,
        "herbst" to 10, "autumn" to 10, "fall" to 10, "automne" to 10,
        "winter" to 1, "hiver" to 1,
    )

    private val MONTH_WORDS = mapOf(
        1 to listOf("januar", "january", "janvier"),
        2 to listOf("februar", "february", "février"),
        3 to listOf("märz", "march", "mars"),
        4 to listOf("april", "avril"),
        5 to listOf("mai", "may"),
        6 to listOf("juni", "june", "juin"),
        7 to listOf("juli", "july", "juillet"),
        8 to listOf("august", "août"),
        9 to listOf("september", "septembre"),
        10 to listOf("oktober", "october", "octobre"),
        11 to listOf("november", "novembre"),
        12 to listOf("dezember", "december", "décembre"),
    )

    fun propose(frequencyText: String): Recurrence {
        val match = FREQUENCY.find(frequencyText.trim())
            ?: return Recurrence(RecurrenceUnit.YEAR, weekday = DayOfWeek.SATURDAY, ordinal = Ordinal.FIRST, month = 1)
        val count = match.groupValues[1].toInt().coerceAtLeast(1)
        val word = match.groupValues[2].lowercase()
        val hint = match.groupValues[3].lowercase()
        val unit = UNIT_WORDS.entries.firstOrNull { (_, words) -> words.any { word.startsWith(it) } }?.key
            ?: RecurrenceUnit.YEAR
        val saturday = DayOfWeek.SATURDAY
        return when (unit) {
            RecurrenceUnit.DAY -> Recurrence(RecurrenceUnit.DAY, 1)
            RecurrenceUnit.WEEK ->
                Recurrence(RecurrenceUnit.DAY, (DAYS_PER_WEEK.toDouble() / count).roundToInt().coerceAtLeast(1))
            RecurrenceUnit.MONTH -> if (count == 1) {
                Recurrence(RecurrenceUnit.MONTH, 1, saturday, Ordinal.FIRST)
            } else {
                Recurrence(RecurrenceUnit.DAY, (DAYS_PER_MONTH.toDouble() / count).roundToInt().coerceAtLeast(1))
            }
            RecurrenceUnit.YEAR -> {
                val interval = if (12 % count == 0) 12 / count else 12
                Recurrence(
                    RecurrenceUnit.MONTH,
                    interval,
                    saturday,
                    Ordinal.FIRST,
                    month = monthHint(hint) ?: 1,
                ).let { if (interval == 12) it.copy(unit = RecurrenceUnit.YEAR) else it }
            }
        }
    }

    fun nextDate(recurrence: Recurrence, from: LocalDate): LocalDate = when (recurrence.unit) {
        RecurrenceUnit.DAY -> from
        RecurrenceUnit.WEEK -> nextWeekday(from, recurrence.weekday ?: DayOfWeek.SATURDAY)
        RecurrenceUnit.MONTH -> nextInMonths(from, recurrence, recurrence.month)
        RecurrenceUnit.YEAR -> nextInMonths(from, recurrence, recurrence.month ?: from.monthNumber)
    }

    fun rrule(recurrence: Recurrence): String {
        val interval = recurrence.interval
        return when (recurrence.unit) {
            RecurrenceUnit.DAY -> "FREQ=DAILY;INTERVAL=$interval"
            RecurrenceUnit.WEEK ->
                "FREQ=WEEKLY;INTERVAL=$interval;BYDAY=${(recurrence.weekday ?: DayOfWeek.SATURDAY).ical()}"
            RecurrenceUnit.MONTH -> "FREQ=MONTHLY;INTERVAL=$interval;${monthlyRule(recurrence)}"
            RecurrenceUnit.YEAR ->
                "FREQ=YEARLY;INTERVAL=$interval;BYMONTH=${recurrence.month ?: 1};${monthlyRule(recurrence)}"
        }
    }

    fun occurrencesPerYear(recurrence: Recurrence): Double {
        val periodsPerYear = when (recurrence.unit) {
            RecurrenceUnit.DAY -> 365.0
            RecurrenceUnit.WEEK -> 52.0
            RecurrenceUnit.MONTH -> 12.0
            RecurrenceUnit.YEAR -> 1.0
        }
        return periodsPerYear / recurrence.interval
    }

    private fun monthHint(hint: String): Int? =
        SEASON_MONTHS.entries.firstOrNull { hint.contains(it.key) }?.value
            ?: MONTH_WORDS.entries.firstOrNull { (_, words) -> words.any { hint.contains(it) } }?.key

    private fun monthlyRule(recurrence: Recurrence): String {
        val weekday = recurrence.weekday
        val ordinal = recurrence.ordinal
        return if (weekday != null && ordinal != null) {
            "BYDAY=${if (ordinal == Ordinal.LAST) "-1" else (ordinal.ordinal + 1).toString()}${weekday.ical()}"
        } else {
            "BYMONTHDAY=${recurrence.dayOfMonth ?: 1}"
        }
    }

    private fun nextInMonths(
        from: LocalDate,
        recurrence: Recurrence,
        month: Int?,
    ): LocalDate {
        var year = from.year
        var monthNumber = from.monthNumber
        repeat(MAX_MONTH_SEARCH) {
            if (month == null || monthNumber == month) {
                val candidate = dateInMonth(year, monthNumber, recurrence)
                if (candidate >= from) return candidate
            }
            monthNumber += 1
            if (monthNumber > 12) {
                monthNumber = 1
                year += 1
            }
        }
        return from
    }

    private fun dateInMonth(year: Int, month: Int, recurrence: Recurrence): LocalDate {
        val first = LocalDate(year, month, 1)
        val lengthOfMonth = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).dayOfMonth
        val weekday = recurrence.weekday
        val ordinal = recurrence.ordinal
        if (weekday == null || ordinal == null) {
            return LocalDate(year, month, (recurrence.dayOfMonth ?: 1).coerceIn(1, lengthOfMonth))
        }
        if (ordinal == Ordinal.LAST) {
            val last = LocalDate(year, month, lengthOfMonth)
            return last.minus((last.dayOfWeek.isoDayNumber - weekday.isoDayNumber + 7) % 7, DateTimeUnit.DAY)
        }
        val firstMatch = nextWeekday(first, weekday)
        return firstMatch.plus(ordinal.ordinal * 7, DateTimeUnit.DAY)
    }

    private fun nextWeekday(from: LocalDate, weekday: DayOfWeek): LocalDate =
        from.plus((weekday.isoDayNumber - from.dayOfWeek.isoDayNumber + 7) % 7, DateTimeUnit.DAY)

    private fun DayOfWeek.ical(): String = name.take(2)

    private const val MAX_MONTH_SEARCH = 37
}
