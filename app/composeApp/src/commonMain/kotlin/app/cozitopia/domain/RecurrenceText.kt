package app.cozitopia.domain

import kotlinx.datetime.DayOfWeek

object RecurrenceText {
    private val HEAD = Regex("""^every\s+(\d+)\s*(day|week|month|year)(?:\(s\)|s)?$""", RegexOption.IGNORE_CASE)
    private val ORDINAL_WEEKDAY = Regex("""^(1st|2nd|3rd|4th|last)\s+(\p{L}+)$""", RegexOption.IGNORE_CASE)
    private val DAY = Regex("""^day\s+(\d{1,2})$""", RegexOption.IGNORE_CASE)
    private val MONTH = Regex("""^month\s+(\d{1,2})$""", RegexOption.IGNORE_CASE)
    private val ORDINAL_WORDS = mapOf(
        Ordinal.FIRST to "1st",
        Ordinal.SECOND to "2nd",
        Ordinal.THIRD to "3rd",
        Ordinal.FOURTH to "4th",
        Ordinal.LAST to "last",
    )
    private const val MAX_DAY = 31
    private const val MAX_MONTH = 12
    private const val ABBREVIATION_LENGTH = 3

    fun format(recurrence: Recurrence): String {
        val weekday = recurrence.weekday?.let { it.name.lowercase().replaceFirstChar(Char::uppercase) }
        val ordinal = recurrence.ordinal?.let(ORDINAL_WORDS::getValue)
        val detail = when {
            weekday != null && ordinal != null -> "$ordinal $weekday"
            weekday != null -> weekday
            recurrence.dayOfMonth != null -> "day ${recurrence.dayOfMonth}"
            else -> null
        }
        val unit = recurrence.unit.name.lowercase()
        return listOfNotNull(
            "Every ${recurrence.interval} $unit(s)",
            detail,
            recurrence.month?.let { "month $it" },
        ).joinToString(", ")
    }

    fun parse(text: String): Recurrence? {
        val parts = text.split(',').map { it.trim().replace(Regex("""\s+"""), " ") }
        val head = HEAD.matchEntire(parts.first()) ?: return null
        val interval = head.groupValues[1].toIntOrNull()?.takeIf { it >= 1 } ?: return null
        val unit = RecurrenceUnit.valueOf(head.groupValues[2].uppercase())
        var recurrence = Recurrence(unit, interval)
        val seen = mutableSetOf<String>()
        for (part in parts.drop(1)) {
            val (kind, updated) = applyPart(part, recurrence) ?: return null
            if (!seen.add(kind)) return null
            recurrence = updated
        }
        return recurrence.takeIf { isConsistent(it, seen) }
    }

    private fun applyPart(part: String, recurrence: Recurrence): Pair<String, Recurrence>? {
        ORDINAL_WEEKDAY.matchEntire(part)?.let { match ->
            val weekday = weekdayOf(match.groupValues[2]) ?: return null
            val ordinal = ORDINAL_WORDS.entries.first { it.value == match.groupValues[1].lowercase() }.key
            return "pattern" to recurrence.copy(weekday = weekday, ordinal = ordinal)
        }
        DAY.matchEntire(part)?.let { match ->
            val day = match.groupValues[1].toInt().takeIf { it in 1..MAX_DAY } ?: return null
            return "pattern" to recurrence.copy(dayOfMonth = day)
        }
        MONTH.matchEntire(part)?.let { match ->
            val month = match.groupValues[1].toInt().takeIf { it in 1..MAX_MONTH } ?: return null
            return "month" to recurrence.copy(month = month)
        }
        return weekdayOf(part)?.let { "weekday" to recurrence.copy(weekday = it) }
    }

    private fun isConsistent(recurrence: Recurrence, seen: Set<String>): Boolean = when (recurrence.unit) {
        RecurrenceUnit.DAY -> seen.isEmpty()
        RecurrenceUnit.WEEK -> seen.all { it == "weekday" }
        RecurrenceUnit.MONTH, RecurrenceUnit.YEAR -> "weekday" !in seen
    }

    private fun weekdayOf(word: String): DayOfWeek? = DayOfWeek.entries.firstOrNull {
        it.name.equals(word, ignoreCase = true) ||
            (word.length == ABBREVIATION_LENGTH && it.name.take(ABBREVIATION_LENGTH).equals(word, ignoreCase = true))
    }
}
