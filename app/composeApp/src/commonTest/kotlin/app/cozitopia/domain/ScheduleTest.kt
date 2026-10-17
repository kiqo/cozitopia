package app.cozitopia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

class ScheduleTest {
    private val today = LocalDate(2026, 10, 17)
    private val saturday = DayOfWeek.SATURDAY

    @Test
    fun proposesRecurrences() {
        val cases = listOf(
            "1x Jahr" to Recurrence(RecurrenceUnit.YEAR, 1, saturday, Ordinal.FIRST, month = 1),
            "1x Jahr (Herbst)" to Recurrence(RecurrenceUnit.YEAR, 1, saturday, Ordinal.FIRST, month = 10),
            "1x year (March)" to Recurrence(RecurrenceUnit.YEAR, 1, saturday, Ordinal.FIRST, month = 3),
            "2x Jahr (Frühling)" to Recurrence(RecurrenceUnit.MONTH, 6, saturday, Ordinal.FIRST, month = 4),
            "5x Jahr" to Recurrence(RecurrenceUnit.DAY, 73),
            "4x Jahr" to Recurrence(RecurrenceUnit.MONTH, 3, saturday, Ordinal.FIRST, month = 1),
            "1x par an" to Recurrence(RecurrenceUnit.YEAR, 1, saturday, Ordinal.FIRST, month = 1),
            "1x Monat" to Recurrence(RecurrenceUnit.MONTH, 1, saturday, Ordinal.FIRST),
            "2x Monat" to Recurrence(RecurrenceUnit.WEEK, 2, saturday),
            "4x per month" to Recurrence(RecurrenceUnit.WEEK, 1, saturday),
            "3x par mois" to Recurrence(RecurrenceUnit.DAY, 10),
            "1x Woche" to Recurrence(RecurrenceUnit.WEEK, 1, saturday),
            "2x per week" to Recurrence(RecurrenceUnit.DAY, 4),
            "3x semaine" to Recurrence(RecurrenceUnit.DAY, 2),
            "1x Tag" to Recurrence(RecurrenceUnit.DAY, 1),
            "02.08." to Recurrence(RecurrenceUnit.YEAR, 1, month = 8, dayOfMonth = 2),
            "am 2.8" to Recurrence(RecurrenceUnit.YEAR, 1, month = 8, dayOfMonth = 2),
        )

        cases.forEach { (text, expected) ->
            val proposal = Schedule.propose(text)

            assertEquals(expected, proposal.recurrence, text)
            assertTrue(proposal.recognized, text)
        }
    }

    @Test
    fun flagsUnrecognizedFrequency() {
        listOf("sometimes", "", "3x Blume", "40.13.").forEach { text ->
            val proposal = Schedule.propose(text)

            assertFalse(proposal.recognized, text)
            assertEquals(RecurrenceUnit.YEAR, proposal.recurrence.unit, text)
        }
    }

    @Test
    fun computesNextDate() {
        val cases = listOf(
            "1x Jahr (Herbst)" to LocalDate(2027, 10, 2),
            "1x Jahr" to LocalDate(2027, 1, 2),
            "1x Monat" to LocalDate(2026, 11, 7),
            "1x Woche" to LocalDate(2026, 10, 17),
            "2x Monat" to LocalDate(2026, 10, 17),
            "02.08." to LocalDate(2027, 8, 2),
            "17.10." to LocalDate(2026, 10, 17),
            "2x Jahr (Frühling)" to LocalDate(2027, 4, 3),
        )

        cases.forEach { (text, expected) ->
            assertEquals(expected, Schedule.nextDate(Schedule.propose(text).recurrence, today), text)
        }
    }

    @Test
    fun computesOccurrences() {
        val firstSaturday = Recurrence(RecurrenceUnit.MONTH, 1, saturday, Ordinal.FIRST)
        val lastSunday = Recurrence(RecurrenceUnit.MONTH, 1, DayOfWeek.SUNDAY, Ordinal.LAST)
        val biweekly = Recurrence(RecurrenceUnit.WEEK, 2, saturday)
        val everyThirdDay = Recurrence(RecurrenceUnit.DAY, 3)
        val halfYearly = Recurrence(RecurrenceUnit.MONTH, 6, saturday, Ordinal.FIRST, month = 4)
        val yearly = Recurrence(RecurrenceUnit.YEAR, 1, month = 8, dayOfMonth = 2)

        assertEquals(
            listOf(LocalDate(2026, 11, 7), LocalDate(2026, 12, 5), LocalDate(2027, 1, 2)),
            Schedule.occurrences(firstSaturday, today, LocalDate(2027, 1, 31)),
        )
        assertEquals(
            listOf(LocalDate(2026, 10, 25), LocalDate(2026, 11, 29), LocalDate(2026, 12, 27)),
            Schedule.occurrences(lastSunday, today, LocalDate(2026, 12, 31)),
        )
        assertEquals(
            listOf(LocalDate(2026, 10, 17), LocalDate(2026, 10, 31), LocalDate(2026, 11, 14)),
            Schedule.occurrences(biweekly, today, LocalDate(2026, 11, 14)),
        )
        assertEquals(
            listOf(LocalDate(2026, 10, 17), LocalDate(2026, 10, 20), LocalDate(2026, 10, 23)),
            Schedule.occurrences(everyThirdDay, today, LocalDate(2026, 10, 24)),
        )
        assertEquals(
            listOf(LocalDate(2027, 4, 3), LocalDate(2027, 10, 2), LocalDate(2028, 4, 1)),
            Schedule.occurrences(halfYearly, today, LocalDate(2028, 6, 1)),
        )
        assertEquals(
            listOf(LocalDate(2027, 8, 2), LocalDate(2028, 8, 2)),
            Schedule.occurrences(yearly, today, LocalDate(2028, 12, 31)),
        )
        assertTrue(Schedule.occurrences(firstSaturday, today, LocalDate(2026, 10, 1)).isEmpty())
    }

    @Test
    fun buildsRecurrenceRules() {
        val cases = listOf(
            Recurrence(RecurrenceUnit.DAY, 3) to "FREQ=DAILY;INTERVAL=3",
            Recurrence(RecurrenceUnit.WEEK, 2, saturday) to "FREQ=WEEKLY;INTERVAL=2;BYDAY=SA",
            Recurrence(RecurrenceUnit.MONTH, 1, saturday, Ordinal.FIRST) to "FREQ=MONTHLY;INTERVAL=1;BYDAY=1SA",
            Recurrence(RecurrenceUnit.MONTH, 1, DayOfWeek.SUNDAY, Ordinal.LAST) to "FREQ=MONTHLY;INTERVAL=1;BYDAY=-1SU",
            Recurrence(RecurrenceUnit.YEAR, 1, month = 8, dayOfMonth = 2) to
                "FREQ=YEARLY;INTERVAL=1;BYMONTH=8;BYMONTHDAY=2",
        )

        cases.forEach { (recurrence, expected) -> assertEquals(expected, Schedule.rrule(recurrence)) }
    }
}
