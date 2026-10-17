package app.cozitopia.ui

import app.cozitopia.domain.Assignee
import app.cozitopia.domain.ChoreTask
import app.cozitopia.domain.Homie
import app.cozitopia.domain.Recurrence
import app.cozitopia.domain.RecurrenceUnit
import app.cozitopia.domain.Role
import app.cozitopia.i18n.StringKey
import app.cozitopia.ui.checklist.ChecklistState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class ChecklistStateTest {
    private val task = ChoreTask(
        "t", "t", "", "", 60, Assignee.Unassigned, Recurrence(RecurrenceUnit.YEAR), LocalDate(2026, 10, 17),
    )
    private val homie = Homie("u1", "Alice", Role.ADMIN)

    @Test
    fun assignIsBlockedWithoutHomies() {
        val state = ChecklistState(tasks = listOf(task))

        assertEquals(StringKey.ERROR_NO_HOMIES, state.assignBlocker)
    }

    @Test
    fun assignIsBlockedWithoutTasks() {
        val state = ChecklistState(homies = listOf(homie))

        assertEquals(StringKey.ERROR_NO_TASKS, state.assignBlocker)
    }

    @Test
    fun assignIsAllowedWithHomiesAndTasks() {
        val state = ChecklistState(homies = listOf(homie), tasks = listOf(task))

        assertNull(state.assignBlocker)
    }
}
