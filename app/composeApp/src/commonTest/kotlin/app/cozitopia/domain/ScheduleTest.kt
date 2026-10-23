package app.cozitopia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlinx.datetime.plus

class ScheduleTest {
    private val today = LocalDate(2026, 10, 17)
    private val sunday = DayOfWeek.SUNDAY

    @Test
    fun proposesRecurrences() {
        val cases = listOf(
            "1x Jahr" to Recurrence(RecurrenceUnit.YEAR, 1, sunday, Ordinal.FIRST, month = 1),
            "1x Jahr (Herbst)" to Recurrence(RecurrenceUnit.YEAR, 1, sunday, Ordinal.FIRST, month = 10),
            "1x year (March)" to Recurrence(RecurrenceUnit.YEAR, 1, sunday, Ordinal.FIRST, month = 3),
            "2x Jahr (Frühling)" to Recurrence(RecurrenceUnit.MONTH, 6, sunday, Ordinal.FIRST, month = 4),
            "5x Jahr" to Recurrence(RecurrenceUnit.DAY, 73),
            "4x Jahr" to Recurrence(RecurrenceUnit.MONTH, 3, sunday, Ordinal.FIRST, month = 1),
            "1x par an" to Recurrence(RecurrenceUnit.YEAR, 1, sunday, Ordinal.FIRST, month = 1),
            "1x Monat" to Recurrence(RecurrenceUnit.MONTH, 1, sunday, Ordinal.FIRST),
            "2x Monat" to Recurrence(RecurrenceUnit.WEEK, 2, sunday),
            "4x per month" to Recurrence(RecurrenceUnit.WEEK, 1, sunday),
            "3x par mois" to Recurrence(RecurrenceUnit.DAY, 10),
            "1x Woche" to Recurrence(RecurrenceUnit.WEEK, 1, sunday),
            "2x per week" to Recurrence(RecurrenceUnit.DAY, 4),
            "3x semaine" to Recurrence(RecurrenceUnit.DAY, 2),
            "1x Tag" to Recurrence(RecurrenceUnit.DAY, 1),
            "02.08." to Recurrence(RecurrenceUnit.YEAR, 1, month = 8, dayOfMonth = 2),
            "am 2.8" to Recurrence(RecurrenceUnit.YEAR, 1, month = 8, dayOfMonth = 2),
        )

        cases.forEach { (text, expected) ->
            assertEquals(expected, Schedule.propose(text), text)
        }
    }

    @Test
    fun flagsUnrecognizedFrequency() {
        listOf("sometimes", "", "3x Blume", "40.13.", "Alle 2-3 Tage (Sommer + Winter)").forEach { text ->
            assertNull(Schedule.propose(text), text)
        }
    }

    @Test
    fun proposesRepeatEveryNMonthsAndYears() {
        val cases = listOf(
            "Alle 2 Monate" to Recurrence(RecurrenceUnit.MONTH, 2, sunday, Ordinal.FIRST, month = 1),
            "alle 2 Monate" to Recurrence(RecurrenceUnit.MONTH, 2, sunday, Ordinal.FIRST, month = 1),
            "Alle 2 Monate (ab Februar)" to Recurrence(RecurrenceUnit.MONTH, 2, sunday, Ordinal.FIRST, month = 2),
            "every 3 months (from March)" to Recurrence(RecurrenceUnit.MONTH, 3, sunday, Ordinal.FIRST, month = 3),
            "tous les 2 mois (à partir de février)" to
                Recurrence(RecurrenceUnit.MONTH, 2, sunday, Ordinal.FIRST, month = 2),
            "Alle 6 Monate" to Recurrence(RecurrenceUnit.MONTH, 6, sunday, Ordinal.FIRST, month = 1),
            "Alle 3 Jahre" to Recurrence(RecurrenceUnit.YEAR, 3, sunday, Ordinal.FIRST, month = 1),
            "Alle 1 Jahr" to Recurrence(RecurrenceUnit.YEAR, 1, sunday, Ordinal.FIRST, month = 1),
            "Alle 2x Monat" to Recurrence(RecurrenceUnit.WEEK, 2, sunday),
            "2-3x Jahr" to Recurrence(RecurrenceUnit.MONTH, 4, sunday, Ordinal.FIRST, month = 1),
            "1-2x Woche" to Recurrence(RecurrenceUnit.DAY, 4),
        )

        cases.forEach { (text, expected) -> assertEquals(expected, Schedule.propose(text), text) }
    }

    @Test
    fun usesTheGivenWeekday() {
        val recurrence = Schedule.propose("1x Woche", DayOfWeek.MONDAY)

        assertEquals(Recurrence(RecurrenceUnit.WEEK, 1, DayOfWeek.MONDAY), recurrence)
        assertEquals(DayOfWeek.SUNDAY, Schedule.DEFAULT_WEEKDAY)
    }

    @Test
    fun computesNextDate() {
        val cases = listOf(
            "1x Jahr (Herbst)" to LocalDate(2027, 10, 3),
            "1x Jahr" to LocalDate(2027, 1, 3),
            "1x Monat" to LocalDate(2026, 11, 1),
            "1x Woche" to LocalDate(2026, 10, 18),
            "2x Monat" to LocalDate(2026, 10, 18),
            "02.08." to LocalDate(2027, 8, 2),
            "17.10." to LocalDate(2026, 10, 17),
            "2x Jahr (Frühling)" to LocalDate(2027, 4, 4),
        )

        cases.forEach { (text, expected) ->
            assertEquals(expected, Schedule.nextDate(checkNotNull(Schedule.propose(text)), today), text)
        }
    }

    @Test
    fun computesOccurrences() {
        val firstSunday = Recurrence(RecurrenceUnit.MONTH, 1, sunday, Ordinal.FIRST)
        val lastSunday = Recurrence(RecurrenceUnit.MONTH, 1, sunday, Ordinal.LAST)
        val biweekly = Recurrence(RecurrenceUnit.WEEK, 2, sunday)
        val everyThirdDay = Recurrence(RecurrenceUnit.DAY, 3)
        val halfYearly = Recurrence(RecurrenceUnit.MONTH, 6, sunday, Ordinal.FIRST, month = 4)
        val yearly = Recurrence(RecurrenceUnit.YEAR, 1, month = 8, dayOfMonth = 2)

        assertEquals(
            listOf(LocalDate(2026, 11, 1), LocalDate(2026, 12, 6), LocalDate(2027, 1, 3)),
            Schedule.occurrences(firstSunday, today, LocalDate(2027, 1, 31)),
        )
        assertEquals(
            listOf(LocalDate(2026, 10, 25), LocalDate(2026, 11, 29), LocalDate(2026, 12, 27)),
            Schedule.occurrences(lastSunday, today, LocalDate(2026, 12, 31)),
        )
        assertEquals(
            listOf(LocalDate(2026, 10, 18), LocalDate(2026, 11, 1), LocalDate(2026, 11, 15)),
            Schedule.occurrences(biweekly, today, LocalDate(2026, 11, 15)),
        )
        assertEquals(
            listOf(LocalDate(2026, 10, 17), LocalDate(2026, 10, 20), LocalDate(2026, 10, 23)),
            Schedule.occurrences(everyThirdDay, today, LocalDate(2026, 10, 24)),
        )
        assertEquals(
            listOf(LocalDate(2027, 4, 4), LocalDate(2027, 10, 3), LocalDate(2028, 4, 2)),
            Schedule.occurrences(halfYearly, today, LocalDate(2028, 6, 1)),
        )
        assertEquals(
            listOf(LocalDate(2027, 8, 2), LocalDate(2028, 8, 2)),
            Schedule.occurrences(yearly, today, LocalDate(2028, 12, 31)),
        )
        assertTrue(Schedule.occurrences(firstSunday, today, LocalDate(2026, 10, 1)).isEmpty())
    }

    @Test
    fun buildsRecurrenceRules() {
        val cases = listOf(
            Recurrence(RecurrenceUnit.DAY, 3) to "FREQ=DAILY;INTERVAL=3",
            Recurrence(RecurrenceUnit.WEEK, 2, sunday) to "FREQ=WEEKLY;INTERVAL=2;BYDAY=SU",
            Recurrence(RecurrenceUnit.MONTH, 1, sunday, Ordinal.FIRST) to "FREQ=MONTHLY;INTERVAL=1;BYDAY=1SU",
            Recurrence(RecurrenceUnit.MONTH, 1, sunday, Ordinal.LAST) to "FREQ=MONTHLY;INTERVAL=1;BYDAY=-1SU",
            Recurrence(RecurrenceUnit.YEAR, 1, month = 8, dayOfMonth = 2) to
                "FREQ=YEARLY;INTERVAL=1;BYMONTH=8;BYMONTHDAY=2",
        )

        cases.forEach { (recurrence, expected) -> assertEquals(expected, Schedule.rrule(recurrence)) }
    }

    @Test
    fun parsesRecurrenceRulesAsTheInverseOfBuilding() {
        val recurrences = listOf(
            Recurrence(RecurrenceUnit.DAY, 3),
            Recurrence(RecurrenceUnit.WEEK, 2, sunday),
            Recurrence(RecurrenceUnit.MONTH, 1, sunday, Ordinal.LAST),
            Recurrence(RecurrenceUnit.MONTH, 2, dayOfMonth = 5),
            Recurrence(RecurrenceUnit.YEAR, 1, DayOfWeek.FRIDAY, Ordinal.SECOND, month = 4),
            Recurrence(RecurrenceUnit.YEAR, 1, month = 8, dayOfMonth = 2),
        )

        val parsed = recurrences.map { Schedule.parseRrule(Schedule.rrule(it)) }

        assertEquals(recurrences, parsed)
    }

    @Test
    fun rejectsUnsupportedRecurrenceRules() {
        val rules = listOf("FREQ=HOURLY", "FREQ=DAILY;INTERVAL=0", "FREQ=YEARLY;BYMONTH=13;BYMONTHDAY=1", "FREQ=MONTHLY", "")

        rules.forEach { assertNull(Schedule.parseRrule(it), it) }
    }

    @Test
    fun nextDateFollowsTheSameSeriesAsAdvance() {
        val recurrence = Recurrence(RecurrenceUnit.MONTH, 5, sunday, Ordinal.FIRST, month = 1)

        val dates = Schedule.occurrences(recurrence, LocalDate(2027, 1, 1), LocalDate(2027, 12, 31))

        assertEquals(listOf(4, 9), dates.map { it.month.number })
        assertEquals(4, Schedule.nextDate(recurrence, LocalDate(2027, 3, 1)).month.number)
        assertEquals(dates.drop(1), Schedule.occurrences(recurrence, dates.first().plus(1, DateTimeUnit.DAY), LocalDate(2027, 12, 31)))
    }
}
