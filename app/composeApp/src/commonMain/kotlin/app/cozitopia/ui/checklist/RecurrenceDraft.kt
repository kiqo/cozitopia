package app.cozitopia.ui.checklist

import app.cozitopia.domain.Ordinal
import app.cozitopia.domain.Recurrence
import app.cozitopia.domain.RecurrenceUnit
import app.cozitopia.domain.Schedule
import kotlinx.datetime.DayOfWeek

enum class RepeatMode { ORDINAL_WEEKDAY, WEEKDAY, DATE }

fun RecurrenceUnit.repeatModes(): List<RepeatMode> = when (this) {
    RecurrenceUnit.DAY -> emptyList()
    RecurrenceUnit.WEEK -> listOf(RepeatMode.WEEKDAY)
    RecurrenceUnit.MONTH, RecurrenceUnit.YEAR -> listOf(RepeatMode.ORDINAL_WEEKDAY, RepeatMode.DATE)
}

data class RecurrenceDraft(
    val mode: RepeatMode,
    val unit: RecurrenceUnit,
    val interval: Int,
    val ordinal: Ordinal,
    val weekday: DayOfWeek,
    val dayOfMonth: Int,
    val month: Int?,
) {
    val usesMonth: Boolean get() = unit == RecurrenceUnit.MONTH || unit == RecurrenceUnit.YEAR

    fun withValidMode(): RecurrenceDraft {
        val modes = unit.repeatModes()
        return if (mode in modes || modes.isEmpty()) this else copy(mode = modes.first())
    }

    fun toRecurrence(): Recurrence = when {
        unit == RecurrenceUnit.DAY -> Recurrence(unit, interval)
        unit == RecurrenceUnit.WEEK -> Recurrence(unit, interval, weekday)
        mode == RepeatMode.ORDINAL_WEEKDAY -> Recurrence(unit, interval, weekday, ordinal, month = month)
        else -> Recurrence(unit, interval, month = month, dayOfMonth = dayOfMonth)
    }

    companion object {
        private const val MONTHS = 12
        private const val MAX_DAY = 31

        fun from(recurrence: Recurrence): RecurrenceDraft = RecurrenceDraft(
            mode = if (recurrence.ordinal != null && recurrence.weekday != null) {
                RepeatMode.ORDINAL_WEEKDAY
            } else if (recurrence.unit == RecurrenceUnit.WEEK) {
                RepeatMode.WEEKDAY
            } else {
                RepeatMode.DATE
            },
            unit = recurrence.unit,
            interval = recurrence.interval,
            ordinal = recurrence.ordinal ?: Ordinal.FIRST,
            weekday = recurrence.weekday ?: Schedule.DEFAULT_WEEKDAY,
            dayOfMonth = recurrence.dayOfMonth ?: 1,
            month = recurrence.month,
        ).withValidMode()

        fun normalized(draft: RecurrenceDraft): RecurrenceDraft = draft.copy(
            interval = draft.interval.coerceAtLeast(1),
            dayOfMonth = draft.dayOfMonth.coerceIn(1, MAX_DAY),
            month = draft.month?.coerceIn(1, MONTHS),
        )
    }
}
