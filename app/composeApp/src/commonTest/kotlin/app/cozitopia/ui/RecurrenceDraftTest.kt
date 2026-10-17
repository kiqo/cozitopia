package app.cozitopia.ui

import app.cozitopia.domain.Ordinal
import app.cozitopia.domain.RecurrenceUnit
import app.cozitopia.ui.checklist.RecurrenceDraft
import app.cozitopia.ui.checklist.RepeatMode
import app.cozitopia.ui.checklist.repeatModes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.datetime.DayOfWeek

class RecurrenceDraftTest {
    private fun draft(unit: RecurrenceUnit, mode: RepeatMode) = RecurrenceDraft(
        mode, unit, 2, Ordinal.LAST, DayOfWeek.MONDAY, 15, 3,
    )

    @Test
    fun everyCombinationProducesOnlyMeaningfulFields() {
        for (unit in RecurrenceUnit.entries) {
            for (mode in RepeatMode.entries) {
                val recurrence = draft(unit, mode).withValidMode().toRecurrence()

                when (unit) {
                    RecurrenceUnit.DAY -> {
                        assertNull(recurrence.weekday, "$unit $mode")
                        assertNull(recurrence.month, "$unit $mode")
                        assertNull(recurrence.dayOfMonth, "$unit $mode")
                    }
                    RecurrenceUnit.WEEK -> {
                        assertNotNull(recurrence.weekday, "$unit $mode")
                        assertNull(recurrence.ordinal, "$unit $mode")
                        assertNull(recurrence.month, "$unit $mode")
                    }
                    else -> assertNotNull(
                        if (recurrence.ordinal != null) recurrence.weekday else recurrence.dayOfMonth,
                        "$unit $mode",
                    )
                }
            }
        }
    }

    @Test
    fun changingUnitFallsBackToAnAllowedMode() {
        val weekly = draft(RecurrenceUnit.WEEK, RepeatMode.DATE).withValidMode()
        val monthly = draft(RecurrenceUnit.MONTH, RepeatMode.WEEKDAY).withValidMode()

        assertEquals(RepeatMode.WEEKDAY, weekly.mode)
        assertEquals(RepeatMode.ORDINAL_WEEKDAY, monthly.mode)
        assertEquals(emptyList(), RecurrenceUnit.DAY.repeatModes())
    }
}
