package app.cozitopia.domain

import kotlin.math.roundToInt
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

object Schedule {
    private val FREQUENCY =
        Regex("""(\d+)\s*x\s*(?:pro|per|par|/)?\s*(\p{L}+)\s*(?:\((.+)\))?""", RegexOption.IGNORE_CASE)
    private val INTERVAL = Regex(
        """\b(?:alle|every|tous\s+les|toutes\s+les)\s+(\d+)\s+(\p{L}+)\s*(?:\((.+)\))?""",
        RegexOption.IGNORE_CASE,
    )
    private val SPECIFIC_DATE = Regex("""(\d{1,2})\.(\d{1,2})\.?""")
    private const val DAYS_PER_YEAR = 365
    private const val DAYS_PER_MONTH = 30
    private const val DAYS_PER_WEEK = 7
    private const val WEEKS_PER_MONTH = 4
    private const val WEEKS_PER_YEAR = 52
    private const val MONTHS_PER_YEAR = 12
    private const val MAX_MONTH_SEARCH = 37
    val DEFAULT_WEEKDAY = DayWindow.DEFAULT.weekday

    private val UNIT_WORDS = mapOf(
        RecurrenceUnit.DAY to setOf("tag", "tage", "tagen", "day", "days", "jour", "jours"),
        RecurrenceUnit.WEEK to setOf("woche", "wochen", "week", "weeks", "semaine", "semaines"),
        RecurrenceUnit.MONTH to setOf("monat", "monate", "monaten", "month", "months", "mois"),
        RecurrenceUnit.YEAR to setOf(
            "jahr", "jahre", "jahren", "year", "years", "an", "ans", "année", "années", "annee", "annees",
        ),
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

    fun defaultRecurrence(weekday: DayOfWeek): Recurrence = every(RecurrenceUnit.YEAR, 1, weekday, null)

    fun propose(frequencyText: String, weekday: DayOfWeek = DEFAULT_WEEKDAY): Recurrence? {
        val text = frequencyText.trim()
        return specificDate(text) ?: interval(text, weekday) ?: perUnit(text, weekday)
    }

    private fun specificDate(text: String): Recurrence? {
        val match = SPECIFIC_DATE.find(text) ?: return null
        val day = match.groupValues[1].toInt()
        val month = match.groupValues[2].toInt()
        if (day !in 1..31 || month !in 1..MONTHS_PER_YEAR) return null
        return Recurrence(RecurrenceUnit.YEAR, 1, month = month, dayOfMonth = day)
    }

    private fun interval(text: String, weekday: DayOfWeek): Recurrence? {
        val match = INTERVAL.find(text) ?: return null
        val unit = unitOf(match.groupValues[2]) ?: return null
        val count = match.groupValues[1].toInt().coerceAtLeast(1)
        return every(unit, count, weekday, monthHint(match.groupValues[3].lowercase()))
    }

    private fun perUnit(text: String, weekday: DayOfWeek): Recurrence? {
        val match = FREQUENCY.find(text) ?: return null
        val unit = unitOf(match.groupValues[2]) ?: return null
        val count = match.groupValues[1].toInt().coerceAtLeast(1)
        return perUnit(unit, count, weekday, monthHint(match.groupValues[3].lowercase()))
    }

    private fun unitOf(word: String): RecurrenceUnit? =
        UNIT_WORDS.entries.firstOrNull { word.lowercase() in it.value }?.key

    private fun every(unit: RecurrenceUnit, count: Int, weekday: DayOfWeek, startMonth: Int?): Recurrence =
        when (unit) {
            RecurrenceUnit.DAY -> Recurrence(unit, count)
            RecurrenceUnit.WEEK -> Recurrence(unit, count, weekday)
            RecurrenceUnit.MONTH -> Recurrence(
                unit, count, weekday, Ordinal.FIRST, month = if (count > 1) startMonth ?: 1 else null,
            )
            RecurrenceUnit.YEAR -> Recurrence(unit, count, weekday, Ordinal.FIRST, month = startMonth ?: 1)
        }

    private fun perUnit(unit: RecurrenceUnit, count: Int, weekday: DayOfWeek, startMonth: Int?): Recurrence =
        when (unit) {
            RecurrenceUnit.DAY -> every(unit, 1, weekday, null)
            RecurrenceUnit.WEEK -> if (count == 1) every(unit, 1, weekday, null) else daily(DAYS_PER_WEEK, count)
            RecurrenceUnit.MONTH -> when {
                count == 1 -> every(unit, 1, weekday, null)
                WEEKS_PER_MONTH % count == 0 -> every(RecurrenceUnit.WEEK, WEEKS_PER_MONTH / count, weekday, null)
                else -> daily(DAYS_PER_MONTH, count)
            }
            RecurrenceUnit.YEAR -> when {
                count == 1 -> every(unit, 1, weekday, startMonth)
                MONTHS_PER_YEAR % count == 0 ->
                    every(RecurrenceUnit.MONTH, MONTHS_PER_YEAR / count, weekday, startMonth)
                WEEKS_PER_YEAR % count == 0 -> every(RecurrenceUnit.WEEK, WEEKS_PER_YEAR / count, weekday, null)
                else -> daily(DAYS_PER_YEAR, count)
            }
        }

    private fun daily(days: Int, count: Int): Recurrence =
        Recurrence(RecurrenceUnit.DAY, (days.toDouble() / count).roundToInt().coerceAtLeast(1))

    fun nextDate(recurrence: Recurrence, from: LocalDate): LocalDate = when (recurrence.unit) {
        RecurrenceUnit.DAY -> from
        RecurrenceUnit.WEEK -> nextWeekday(from, recurrence.weekday ?: DEFAULT_WEEKDAY)
        RecurrenceUnit.MONTH -> recurrence.month
            ?.let { anchoredSeries(recurrence, it, from) }
            ?: nextInMonths(from, recurrence) { true }
        RecurrenceUnit.YEAR -> nextInMonths(from, recurrence) { month ->
            month == (recurrence.month ?: from.month.number)
        }
    }

    fun occurrences(recurrence: Recurrence, from: LocalDate, until: LocalDate): List<LocalDate> =
        generateSequence(nextDate(recurrence, from)) { advance(recurrence, it) }
            .takeWhile { it <= until }
            .toList()

    fun rrule(recurrence: Recurrence): String {
        val interval = recurrence.interval
        return when (recurrence.unit) {
            RecurrenceUnit.DAY -> "FREQ=DAILY;INTERVAL=$interval"
            RecurrenceUnit.WEEK ->
                "FREQ=WEEKLY;INTERVAL=$interval;BYDAY=${(recurrence.weekday ?: DEFAULT_WEEKDAY).ical()}"
            RecurrenceUnit.MONTH -> "FREQ=MONTHLY;INTERVAL=$interval;${monthlyRule(recurrence)}"
            RecurrenceUnit.YEAR ->
                "FREQ=YEARLY;INTERVAL=$interval;BYMONTH=${recurrence.month ?: 1};${monthlyRule(recurrence)}"
        }
    }

    fun occurrencesPerYear(recurrence: Recurrence): Double {
        val periodsPerYear = when (recurrence.unit) {
            RecurrenceUnit.DAY -> DAYS_PER_YEAR.toDouble()
            RecurrenceUnit.WEEK -> WEEKS_PER_YEAR.toDouble()
            RecurrenceUnit.MONTH -> MONTHS_PER_YEAR.toDouble()
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

    private fun anchoredSeries(recurrence: Recurrence, anchorMonth: Int, from: LocalDate): LocalDate =
        generateSequence(dateInMonth(from.year - 1, anchorMonth, recurrence)) { advance(recurrence, it) }
            .first { it >= from }

    private fun advance(recurrence: Recurrence, previous: LocalDate): LocalDate {
        val interval = recurrence.interval.coerceAtLeast(1)
        val firstOfMonth = LocalDate(previous.year, previous.month.number, 1)
        return when (recurrence.unit) {
            RecurrenceUnit.DAY -> previous.plus(interval, DateTimeUnit.DAY)
            RecurrenceUnit.WEEK -> previous.plus(interval, DateTimeUnit.WEEK)
            RecurrenceUnit.MONTH -> firstOfMonth.plus(interval, DateTimeUnit.MONTH).let {
                dateInMonth(it.year, it.month.number, recurrence)
            }
            RecurrenceUnit.YEAR -> firstOfMonth.plus(interval, DateTimeUnit.YEAR).let {
                dateInMonth(it.year, it.month.number, recurrence)
            }
        }
    }

    private fun nextInMonths(
        from: LocalDate,
        recurrence: Recurrence,
        isCandidateMonth: (Int) -> Boolean,
    ): LocalDate {
        var year = from.year
        var month = from.month.number
        repeat(MAX_MONTH_SEARCH) {
            if (isCandidateMonth(month)) {
                val candidate = dateInMonth(year, month, recurrence)
                if (candidate >= from) return candidate
            }
            month += 1
            if (month > MONTHS_PER_YEAR) {
                month = 1
                year += 1
            }
        }
        return from
    }

    private fun dateInMonth(year: Int, month: Int, recurrence: Recurrence): LocalDate {
        val first = LocalDate(year, month, 1)
        val lengthOfMonth = first.plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
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
}
