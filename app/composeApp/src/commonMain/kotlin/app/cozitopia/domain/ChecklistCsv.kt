package app.cozitopia.domain

import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.number

data class CsvRow(
    val what: String,
    val where: String,
    val frequencyText: String,
    val durationMinutes: Int,
    val durationValid: Boolean,
    val assigneeNames: List<String>,
    val repetitionText: String = "",
    val nextStartText: String = "",
    val pinnedText: String = "",
)

data class CsvImport(val rows: List<CsvRow>, val skippedLines: List<Int>)

data class AssigneeResolution(val assignee: Assignee, val unknownNames: List<String>)

object ChecklistCsv {
    const val TEMPLATE = "What,Where,Frequency,Duration,Assignee\n" +
        "Steckdosen reinigen,Alle Räume,1x Jahr,3h,\n" +
        "Terrasse reinigen,Garten,1x Jahr (Herbst),2h,Unassigned\n" +
        "Fenster putzen,Wohnzimmer,2x Monat,90min,Alice;Bob\n"

    const val EXPORT_HEADER = "What,Where,Frequency,Duration,Assignee,Repetition,Next Start,Pinned"

    val EVERYONE_WORDS = setOf("everyone", "alle", "tous", "tout le monde")
    val UNASSIGNED_WORDS = setOf("unassigned", "nicht zugewiesen", "non assigné")

    private val DURATION = Regex("""^(\d+(?:[.,]\d+)?)\s*(h|std|min|m)?$""", RegexOption.IGNORE_CASE)
    private val START = Regex("""^(\d{4})-(\d{1,2})-(\d{1,2})[ T](\d{1,2}):(\d{2})$""")
    private val LINE_BREAK = Regex("""\s*\R\s*""")
    private val MINUTE_UNITS = setOf("min", "m")
    private val QUOTE_TRIGGERS = setOf(',', ';', '"', '\t')
    private const val MIN_FIELDS = 4
    private const val ASSIGNEE_INDEX = 4
    private const val REPETITION_INDEX = 5
    private const val START_INDEX = 6
    private const val PINNED_INDEX = 7
    private const val MINUTES_PER_HOUR = 60
    private const val YEAR_DIGITS = 4
    private const val PART_DIGITS = 2

    fun parse(text: String): CsvImport {
        val numbered = text.removePrefix("﻿").lines()
            .withIndex()
            .filter { it.value.isNotBlank() }
        if (numbered.isEmpty()) return CsvImport(emptyList(), emptyList())
        val delimiter = detectDelimiter(numbered.first().value)
        val records = numbered.map { (index, line) -> index + 1 to splitLine(line, delimiter) }
        val first = records.first().second
        val extended = first.getOrNull(REPETITION_INDEX).equals("Repetition", ignoreCase = true)
        val body = if (isRealRow(first)) records else records.drop(1)
        val rows = mutableListOf<CsvRow>()
        val skipped = mutableListOf<Int>()
        body.forEach { (lineNumber, fields) ->
            toRow(fields, extended)?.let(rows::add) ?: skipped.add(lineNumber)
        }
        return CsvImport(rows, skipped)
    }

    fun export(tasks: List<ChoreTask>, homies: List<Homie>): String =
        (listOf(EXPORT_HEADER) + tasks.map { exportLine(it, homies) }).joinToString("\n", postfix = "\n")

    fun formatStart(date: LocalDate, time: LocalTime): String =
        "${date.year.toString().padStart(YEAR_DIGITS, '0')}-${twoDigits(date.month.number)}-${twoDigits(date.day)} " +
            "${twoDigits(time.hour)}:${twoDigits(time.minute)}"

    fun parseStart(text: String): Pair<LocalDate, LocalTime>? {
        val match = START.matchEntire(text.trim()) ?: return null
        val (year, month, day, hour, minute) = match.destructured
        return try {
            LocalDate(year.toInt(), month.toInt(), day.toInt()) to LocalTime(hour.toInt(), minute.toInt())
        } catch (_: IllegalArgumentException) {
            null
        }
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

    private fun twoDigits(value: Int): String = value.toString().padStart(PART_DIGITS, '0')

    private fun exportLine(task: ChoreTask, homies: List<Homie>): String {
        val recurrence = task.recurrence
        val start = task.nextDate?.takeIf { recurrence != null }?.let { date ->
            task.startTime?.let { formatStart(date, it) }
        }
        return listOf(
            task.what,
            task.where,
            task.frequencyText,
            exportDuration(task),
            exportAssignee(task.assignee, homies),
            recurrence?.let(RecurrenceText::format).orEmpty(),
            start.orEmpty(),
            (task.pinned && start != null).toString(),
        ).joinToString(",") { quoteField(it) }
    }

    private fun exportDuration(task: ChoreTask): String = when {
        TaskWarning.INVALID_DURATION in task.warnings || task.durationMinutes == 0 -> ""
        task.durationMinutes % MINUTES_PER_HOUR == 0 -> "${task.durationMinutes / MINUTES_PER_HOUR}h"
        else -> "${task.durationMinutes}min"
    }

    fun exportAssignee(assignee: Assignee, homies: List<Homie>): String = when (assignee.kind) {
        AssigneeKind.EVERYONE -> "Everyone"
        AssigneeKind.UNASSIGNED -> "Unassigned"
        AssigneeKind.PEOPLE -> homies.filter { it.userId in assignee.userIds }
            .map { it.name }
            .ifEmpty { listOf("Unassigned") }
            .joinToString(";")
    }

    private fun quoteField(value: String): String {
        val flat = value.replace(LINE_BREAK, " ")
        val needsQuotes = flat.any { it in QUOTE_TRIGGERS } || flat.startsWith("'")
        return if (needsQuotes) "\"${flat.replace("\"", "\"\"")}\"" else flat
    }

    private fun toRow(fields: List<String>, extended: Boolean = false): CsvRow? {
        if (fields.size < MIN_FIELDS || fields[0].isBlank()) return null
        val duration = parseDuration(fields[3])
        val assigneeFields = fields.drop(ASSIGNEE_INDEX).let { if (extended) it.take(1) else it }
        val assigneeNames = assigneeFields.flatMap { it.split(',', ';', '&') }
        fun extra(index: Int): String = if (extended) fields.getOrElse(index) { "" } else ""
        return CsvRow(
            what = fields[0],
            where = fields[1],
            frequencyText = fields[2],
            durationMinutes = duration ?: 0,
            durationValid = duration != null,
            assigneeNames = assigneeNames.map { it.trim() }.filter { it.isNotEmpty() },
            repetitionText = extra(REPETITION_INDEX),
            nextStartText = extra(START_INDEX),
            pinnedText = extra(PINNED_INDEX),
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
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                quote != null && char == quote && line.getOrNull(index + 1) == quote -> {
                    current.append(char)
                    index += 1
                }
                quote != null && char == quote -> quote = null
                quote == null && (char == '"' || char == '\'') && current.isBlank() -> quote = char
                quote == null && char == delimiter -> {
                    fields += current.toString().trim()
                    current.clear()
                }
                else -> current.append(char)
            }
            index += 1
        }
        fields += current.toString().trim()
        return fields
    }
}
