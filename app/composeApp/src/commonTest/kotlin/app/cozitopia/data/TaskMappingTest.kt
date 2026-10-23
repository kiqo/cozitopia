package app.cozitopia.data

import app.cozitopia.domain.Assignee
import app.cozitopia.domain.ChoreTask
import app.cozitopia.domain.Ordinal
import app.cozitopia.domain.Recurrence
import app.cozitopia.domain.RecurrenceUnit
import app.cozitopia.domain.TaskWarning
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.builtins.ListSerializer

class TaskMappingTest {
    private val recurrences = listOf(
        Recurrence(RecurrenceUnit.DAY, 3),
        Recurrence(RecurrenceUnit.WEEK, 2, DayOfWeek.FRIDAY),
        Recurrence(RecurrenceUnit.MONTH, 1, DayOfWeek.SATURDAY, Ordinal.LAST),
        Recurrence(RecurrenceUnit.YEAR, 1, month = 6, dayOfMonth = 15),
    )
    private val assignees = listOf(
        Assignee.Everyone,
        Assignee.Unassigned,
        Assignee.of("user-a"),
        Assignee.of("user-a", "user-b"),
    )

    private fun task(index: Int, assignee: Assignee, recurrence: Recurrence) = ChoreTask(
        id = "id-$index",
        what = "Fenster putzen, \"gruendlich\"",
        where = "Wohnzimmer",
        frequencyText = "2x Monat",
        durationMinutes = 90,
        assignee = assignee,
        recurrence = recurrence,
        nextDate = LocalDate(2026, 10, 17),
        startTime = LocalTime(16, 0),
    )

    @Test
    fun roundTripsThroughDtoWithoutLoss() {
        val tasks = assignees.zip(recurrences).mapIndexed { index, (assignee, recurrence) ->
            task(index, assignee, recurrence)
        }

        val restored = tasks.map { it.toDto().toDomain() }

        assertEquals(tasks, restored)
    }

    @Test
    fun roundTripsThroughSaveTasksJson() {
        val tasks = assignees.zip(recurrences).mapIndexed { index, (assignee, recurrence) ->
            task(index, assignee, recurrence)
        }

        val json = taskJson.encodeToString(ListSerializer(TaskDto.serializer()), tasks.map(ChoreTask::toDto))
        val restored = taskJson.decodeFromString(ListSerializer(TaskDto.serializer()), json).map(TaskDto::toDomain)

        assertEquals(tasks, restored)
    }

    @Test
    fun roundTripsInvalidRowsThroughNulls() {
        val invalid = task(0, Assignee.Unassigned, recurrences.first()).copy(
            durationMinutes = 0,
            recurrence = null,
            nextDate = null,
            startTime = null,
            pinned = true,
            warnings = setOf(TaskWarning.INVALID_DURATION, TaskWarning.UNRECOGNIZED_FREQUENCY),
        )

        val dto = invalid.toDto()
        val restored = dto.toDomain()

        assertNull(dto.durationMinutes)
        assertNull(dto.recurrence)
        assertEquals(invalid, restored)
    }

    @Test
    fun usesSnakeCaseColumnNames() {
        val json = listOf(task(0, Assignee.Everyone, recurrences.first())).toJson().toString()

        val columns = listOf("frequency_text", "duration_minutes", "assignee_kind", "assignee_ids", "next_date")

        columns.forEach { assertEquals(true, json.contains("\"$it\""), it) }
    }
}
