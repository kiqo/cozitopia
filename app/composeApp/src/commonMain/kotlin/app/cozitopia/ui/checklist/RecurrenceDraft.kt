package app.cozitopia.ui.checklist

import app.cozitopia.domain.Ordinal
import app.cozitopia.domain.Recurrence
import app.cozitopia.domain.RecurrenceUnit
import kotlinx.datetime.DayOfWeek

enum class RepeatMode { ORDINAL_WEEKDAY, WEEKDAY, DATE }

data class RecurrenceDraft(
    val mode: RepeatMode,
    val unit: RecurrenceUnit,
    val interval: Int,
    val ordinal: Ordinal,
    val weekday: DayOfWeek,
    val dayOfMonth: Int,
    val month: Int?,
) {
    fun toRecurrence(): Recurrence = when (mode) {
        RepeatMode.ORDINAL_WEEKDAY ->
            Recurrence(unit, interval, weekday, ordinal, month = month)
        RepeatMode.WEEKDAY -> Recurrence(unit, interval, weekday)
        RepeatMode.DATE -> Recurrence(unit, interval, month = month, dayOfMonth = dayOfMonth)
    }

    companion object {
        private const val MONTHS = 12
        private const val MAX_DAY = 31

        fun from(recurrence: Recurrence): RecurrenceDraft = RecurrenceDraft(
            mode = when {
                recurrence.ordinal != null && recurrence.weekday != null -> RepeatMode.ORDINAL_WEEKDAY
                recurrence.dayOfMonth != null || recurrence.weekday == null -> RepeatMode.DATE
                else -> RepeatMode.WEEKDAY
            },
            unit = recurrence.unit,
            interval = recurrence.interval,
            ordinal = recurrence.ordinal ?: Ordinal.FIRST,
            weekday = recurrence.weekday ?: DayOfWeek.SATURDAY,
            dayOfMonth = recurrence.dayOfMonth ?: 1,
            month = recurrence.month,
        )

        fun normalized(draft: RecurrenceDraft): RecurrenceDraft = draft.copy(
            interval = draft.interval.coerceAtLeast(1),
            dayOfMonth = draft.dayOfMonth.coerceIn(1, MAX_DAY),
            month = draft.month?.coerceIn(1, MONTHS),
        )
    }
}
