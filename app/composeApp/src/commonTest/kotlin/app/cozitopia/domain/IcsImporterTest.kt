package app.cozitopia.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class IcsImporterTest {
    private val generatedAt = Instant.parse("2026-10-17T09:05:07Z")
    private val homies = listOf(Homie("u-alice", "Alice", Role.ADMIN), Homie("u-bob", "Bob", Role.HOMIE))
    private val task = ChoreTask(
        id = "3f2b8c1e-0000-4000-8000-000000000001",
        what = "Fenster putzen, gruendlich",
        where = "Wohnzimmer; Kueche",
        frequencyText = "1x Jahr (Herbst)",
        durationMinutes = 90,
        assignee = Assignee.of("u-alice", "u-bob"),
        recurrence = Recurrence(RecurrenceUnit.YEAR, 1, DayOfWeek.SUNDAY, Ordinal.FIRST, month = 10),
        nextDate = LocalDate(2026, 10, 18),
        startTime = LocalTime(16, 30),
        pinned = true,
    )

    private fun exported(vararg tasks: ChoreTask) = IcsExporter.export(tasks.toList(), "MyHome", homies, generatedAt)

    @Test
    fun exportThenImportReproducesTheTask() {
        val ics = exported(task)

        val imported = IcsImporter.parse(ics, homies)

        assertEquals(listOf(task), imported)
    }

    @Test
    fun generatesAnIdWhenUidIsMissing() {
        val ics = exported(task).replace(Regex("UID:.*\r\n"), "")

        val imported = IcsImporter.parse(ics, homies)

        assertTrue(imported.single().id.startsWith("import-"))
    }

    @Test
    fun flagsEventsWithoutDurationOrRecurrence() {
        val ics = "BEGIN:VCALENDAR\r\nBEGIN:VEVENT\r\nUID:x\r\nSUMMARY:Foreign\r\nDTSTART;VALUE=DATE:20261017\r\nEND:VEVENT\r\nEND:VCALENDAR\r\n"

        val imported = IcsImporter.parse(ics, homies).single()

        assertEquals("x", imported.id)
        assertEquals(setOf(TaskWarning.INVALID_DURATION, TaskWarning.UNRECOGNIZED_FREQUENCY), imported.warnings)
        assertEquals(false, imported.pinned)
    }

    @Test
    fun repeatedImportSkipsKnownIdsAndCountsThem() {
        val imported = IcsImporter.parse(exported(task, task.copy(id = "other")), homies)
        val first = TaskPlanner.addNew(emptyList(), imported)

        val second = TaskPlanner.addNew(first.tasks, imported)

        assertEquals(TaskPlanner.ImportSummary(imported = 2, skipped = 0), first.summary)
        assertEquals(TaskPlanner.ImportSummary(imported = 0, skipped = 2), second.summary)
        assertEquals(first.tasks, second.tasks)
    }
}
