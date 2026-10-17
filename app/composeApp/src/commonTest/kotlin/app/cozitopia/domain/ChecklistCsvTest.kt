package app.cozitopia.domain

import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.translations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChecklistCsvTest {
    private val homies = listOf(
        Homie("u1", "Alice", Role.ADMIN),
        Homie("u2", "Bob", Role.HOMIE),
    )

    private val expectedRows = listOf(
        CsvRow("Steckdosen reinigen", "Alle Räume", "1x Jahr", 180, true, emptyList()),
        CsvRow("Terrasse reinigen", "Garten", "1x Jahr (Herbst)", 120, true, listOf("Unassigned")),
    )

    @Test
    fun parsesEveryDelimiterWithAndWithoutHeader() {
        val delimiters = listOf(",", ";", "\t")
        val headers = listOf("What,Where,Frequency,Duration,Assignee\n", "")

        for (delimiter in delimiters) {
            for (header in headers) {
                val first = listOf("Steckdosen reinigen", "Alle Räume", "1x Jahr", "3h", "")
                val second = listOf("Terrasse reinigen", "Garten", "1x Jahr (Herbst)", "2h", "Unassigned")
                val text = header.replace(",", delimiter) +
                    first.joinToString(delimiter) + "\n" +
                    second.joinToString(delimiter)

                val import = ChecklistCsv.parse(text)

                assertEquals(expectedRows, import.rows, "delimiter=[$delimiter] header=[$header]")
                assertTrue(import.skippedLines.isEmpty())
            }
        }
    }

    @Test
    fun parsesTemplateAndLocalizedHeaders() {
        val template = ChecklistCsv.parse(ChecklistCsv.TEMPLATE)
        val german = ChecklistCsv.parse("Was;Wo;Wie oft;Dauer;Zuständig\nFenster;Haus;1x Woche;1h;Alice")
        val french = ChecklistCsv.parse("Quoi,Où,Fréquence,Durée,Qui\nFenêtres,Maison,1x par mois,1h,Bob")

        assertEquals(3, template.rows.size)
        assertEquals(listOf("Alice", "Bob"), template.rows.last().assigneeNames)
        assertEquals(listOf("Alice"), german.rows.single().assigneeNames)
        assertEquals(listOf("Bob"), french.rows.single().assigneeNames)
    }

    @Test
    fun dropsFirstRowOnlyWhenItIsNotARealRow() {
        val header = ChecklistCsv.parse("Was?,Räume?,Wie oft?,Wie lange?\nA,B,1x Jahr,1h")
        val realFirst = ChecklistCsv.parse("A,B,1x Jahr,1h\nC,D,Alle 2 Monate,")
        val badDuration = ChecklistCsv.parse("A,B,1x Jahr,soon\nC,D,1x Jahr,1h")
        val badFrequency = ChecklistCsv.parse("A,B,Wie oft?,1h\nC,D,1x Jahr,1h")

        assertEquals(listOf("A"), header.rows.map { it.what })
        assertTrue(header.skippedLines.isEmpty())
        assertEquals(listOf("A", "C"), realFirst.rows.map { it.what })
        assertEquals(listOf("C"), badDuration.rows.map { it.what })
        assertEquals(listOf("C"), badFrequency.rows.map { it.what })
    }

    @Test
    fun parsesQuotedFieldsAndSeparatedAssignees() {
        val quoted = ChecklistCsv.parse("\"Bad, putzen\",'Haus, oben',2x Monat,90min,\"Alice;Bob\"").rows.single()
        val semicolons = ChecklistCsv.parse("Boden;Haus;1x Woche;1h;Alice;Bob").rows.single()
        val commasAndAmpersand = ChecklistCsv.parse("Boden,Haus,1x Woche,1h,\"Alice, Bob & Cara\"").rows.single()

        assertEquals("Bad, putzen", quoted.what)
        assertEquals("Haus, oben", quoted.where)
        assertEquals(listOf("Alice", "Bob"), quoted.assigneeNames)
        assertEquals(listOf("Alice", "Bob"), semicolons.assigneeNames)
        assertEquals(listOf("Alice", "Bob", "Cara"), commasAndAmpersand.assigneeNames)
    }

    @Test
    fun reportsSkippedLinesWithOriginalLineNumbers() {
        val text = listOf(
            "What,Where,Frequency,Duration,Assignee",
            "",
            "Ok,Here,1x Jahr,1h,",
            "short,line",
            ",Here,1x Jahr,1h,",
            "Fine,Here,1x Jahr,1h,",
        ).joinToString("\n")

        val import = ChecklistCsv.parse(text)

        assertEquals(listOf("Ok", "Fine"), import.rows.map { it.what })
        assertEquals(listOf(4, 5), import.skippedLines)
    }

    @Test
    fun parsesDurations() {
        val cases = mapOf("3h" to 180, "1.5h" to 90, "1,5 h" to 90, "90min" to 90, "2" to 120, "" to 0)

        cases.forEach { (text, minutes) ->
            assertEquals(minutes, ChecklistCsv.parseDurationMinutes(text), text)
        }
    }

    @Test
    fun flagsUnparseableDuration() {
        val row = ChecklistCsv.parse("X,Y,1x Jahr,1h,\nA,B,1x Jahr,4h (timeboxed),").rows.last()

        assertEquals(0, row.durationMinutes)
        assertFalse(row.durationValid)
    }

    @Test
    fun resolvesAssignees() {
        val cases = listOf(
            emptyList<String>() to Assignee.Unassigned,
            listOf("Unassigned") to Assignee.Unassigned,
            listOf("Nicht zugewiesen") to Assignee.Unassigned,
            listOf("Non assigné") to Assignee.Unassigned,
            listOf("Everyone") to Assignee.Everyone,
            listOf("Alle") to Assignee.Everyone,
            listOf("Tous") to Assignee.Everyone,
            listOf("tout le monde", "Alice") to Assignee.Everyone,
            listOf(" alice ") to Assignee.of("u1"),
            listOf("Alice", "BOB") to Assignee.of("u1", "u2"),
            listOf("Carol") to Assignee.Unassigned,
        )

        cases.forEach { (names, expected) ->
            assertEquals(expected, ChecklistCsv.resolveAssignee(names, homies).assignee, names.toString())
        }
    }

    @Test
    fun reportsUnknownNames() {
        val resolution = ChecklistCsv.resolveAssignee(listOf("Alice", "Carol", "Everyone"), homies)

        assertEquals(Assignee.Everyone, resolution.assignee)
        assertEquals(listOf("Carol"), resolution.unknownNames)
    }

    @Test
    fun helpTextDurationExamplesParseInCommaFiles() {
        val helpTexts = translations.values.map { it.getValue(StringKey.CHECKLIST_HELP) }

        for (help in helpTexts) {
            val durations = help.substringAfterLast(": ").trimEnd('.').split(", ")

            for (duration in durations) {
                val row = ChecklistCsv.parse("Task,Room,1x Jahr,$duration,Alice").rows.single()

                assertTrue(row.durationValid, duration)
                assertEquals(listOf("Alice"), row.assigneeNames, duration)
            }
        }
    }
}
