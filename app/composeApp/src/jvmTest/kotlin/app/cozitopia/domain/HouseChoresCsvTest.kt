package app.cozitopia.domain

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class HouseChoresCsvTest {
    private val today = LocalDate(2026, 10, 17)

    private fun choresFile(): File =
        generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .map { it.resolve("data/test/house_chores_de.csv") }
            .first { it.isFile }

    @Test
    fun importsTheRealWorldFileWithoutHeaderAndFlagsOnlyUnparseableCells() {
        val import = ChecklistCsv.parse(choresFile().readText())

        val tasks = TaskPlanner.plan(import.rows, emptyList(), today)

        assertEquals(39, import.rows.size)
        assertTrue(import.skippedLines.isEmpty())
        assertTrue(import.rows.none { it.what.startsWith("Was?") })
        assertEquals(
            listOf("Alle 2-3 Tage (Sommer + Winter)"),
            tasks.filter { it.recurrence == null }.map { it.frequencyText },
        )
        assertEquals(
            listOf("Innenschränke putzen", "Keller reinigen"),
            import.rows.filterNot { it.durationValid }.map { it.what },
        )
        assertTrue(tasks.filter { it.recurrence != null }.all { it.nextDate != null })
    }
}
