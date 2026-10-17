package app.cozitopia.domain

import kotlin.math.roundToInt

data class CsvRow(
    val what: String,
    val where: String,
    val frequencyText: String,
    val durationMinutes: Int,
    val durationValid: Boolean,
    val assigneeNames: List<String>,
)

data class CsvImport(val rows: List<CsvRow>, val skippedLines: List<Int>)

data class AssigneeResolution(val assignee: Assignee, val unknownNames: List<String>)

object ChecklistCsv {
    const val TEMPLATE = "What,Where,Frequency,Duration,Assignee\n" +
        "Steckdosen reinigen,Alle Räume,1x Jahr,3h,\n" +
        "Terrasse reinigen,Garten,1x Jahr (Herbst),2h,Unassigned\n" +
        "Fenster putzen,Wohnzimmer,2x Monat,90min,Alice;Bob\n"

    val EVERYONE_WORDS = setOf("everyone", "alle", "tous", "tout le monde")
    val UNASSIGNED_WORDS = setOf("unassigned", "nicht zugewiesen", "non assigné")

    private val DURATION = Regex("""^(\d+(?:[.,]\d+)?)\s*(h|std|min|m)?$""", RegexOption.IGNORE_CASE)
    private val MINUTE_UNITS = setOf("min", "m")
    private const val MIN_FIELDS = 4
    private const val ASSIGNEE_INDEX = 4

    fun parse(text: String): CsvImport {
        val numbered = text.removePrefix("﻿").lines()
            .withIndex()
            .filter { it.value.isNotBlank() }
        if (numbered.isEmpty()) return CsvImport(emptyList(), emptyList())
        val delimiter = detectDelimiter(numbered.first().value)
        val records = numbered.map { (index, line) -> index + 1 to splitLine(line, delimiter) }
        val body = if (isRealRow(records.first().second)) records else records.drop(1)
        val rows = mutableListOf<CsvRow>()
        val skipped = mutableListOf<Int>()
        body.forEach { (lineNumber, fields) ->
            toRow(fields)?.let(rows::add) ?: skipped.add(lineNumber)
        }
        return CsvImport(rows, skipped)
    }

    fun resolveAssignee(names: List<String>, homies: List<Homie>): AssigneeResolution {
        val trimmed = names.map { it.trim() }.filter { it.isNotEmpty() }
        val normalized = trimmed.map { it.lowercase() }
        if (normalized.all { it in UNASSIGNED_WORDS }) {
            return AssigneeResolution(Assignee.Unassigned, emptyList())
        }
        val byName = homies.groupBy { it.name.trim().lowercase() }
        val unknown = trimmed.filter {
            val key = it.lowercase()
            key !in UNASSIGNED_WORDS && key !in EVERYONE_WORDS && key !in byName
        }.distinct()
        if (normalized.any { it in EVERYONE_WORDS }) {
            return AssigneeResolution(Assignee.Everyone, unknown)
        }
        val ids = normalized.flatMap { byName[it].orEmpty() }.map { it.userId }.toSet()
        val assignee = if (ids.isEmpty()) Assignee.Unassigned else Assignee(AssigneeKind.PEOPLE, ids)
        return AssigneeResolution(assignee, unknown)
    }

    fun parseDurationMinutes(text: String): Int = parseDuration(text) ?: 0

    fun parseDuration(text: String): Int? {
        if (text.isBlank()) return 0
        val match = DURATION.matchEntire(text.trim()) ?: return null
        val value = match.groupValues[1].replace(',', '.').toDouble()
        val isMinutes = match.groupValues[2].lowercase() in MINUTE_UNITS
        return (if (isMinutes) value else value * 60).roundToInt()
    }

    private fun toRow(fields: List<String>): CsvRow? {
        if (fields.size < MIN_FIELDS || fields[0].isBlank()) return null
        val duration = parseDuration(fields[3])
        val assigneeNames = fields.drop(ASSIGNEE_INDEX).flatMap { it.split(',', ';', '&') }
        return CsvRow(
            what = fields[0],
            where = fields[1],
            frequencyText = fields[2],
            durationMinutes = duration ?: 0,
            durationValid = duration != null,
            assigneeNames = assigneeNames.map { it.trim() }.filter { it.isNotEmpty() },
        )
    }

    private fun isRealRow(fields: List<String>): Boolean =
        toRow(fields)?.let { it.durationValid && Schedule.propose(it.frequencyText) != null } == true

    private fun detectDelimiter(line: String): Char =
        listOf(',', ';', '\t').maxBy { delimiter -> line.count { it == delimiter } }

    private fun splitLine(line: String, delimiter: Char): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        for (char in line) {
            when {
                quote != null && char == quote -> quote = null
                quote == null && (char == '"' || char == '\'') && current.isBlank() -> quote = char
                quote == null && char == delimiter -> {
                    fields += current.toString().trim()
                    current.clear()
                }
                else -> current.append(char)
            }
        }
        fields += current.toString().trim()
        return fields
    }
}
