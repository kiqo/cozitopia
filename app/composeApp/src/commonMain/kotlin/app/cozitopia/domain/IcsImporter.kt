package app.cozitopia.domain

import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

object IcsImporter {
    private val CONTINUATION = Regex("""\r?\n[ \t]""")
    private val ESCAPE = Regex("""\\(.)""")
    private val MINUTES = Regex("""^(\d+)\s*min$""", RegexOption.IGNORE_CASE)
    private val DATE_TIME = Regex("""^(\d{4})(\d{2})(\d{2})(?:T(\d{2})(\d{2})(\d{2})?Z?)?$""")
    private const val ASSIGNEE_SEPARATOR = ';'

    fun parse(text: String, homies: List<Homie>): List<ChoreTask> =
        events(text).map { toTask(it, homies) }

    private fun events(text: String): List<Map<String, String>> {
        val events = mutableListOf<MutableMap<String, String>>()
        val open = ArrayDeque<String>()
        text.removePrefix("﻿").replace(CONTINUATION, "").lineSequence().forEach { line ->
            val name = line.substringBefore(':').substringBefore(';').uppercase()
            val value = line.substringAfter(':', "")
            when {
                name == "BEGIN" -> {
                    open.addLast(value.uppercase())
                    if (open.last() == "VEVENT") events += mutableMapOf()
                }
                name == "END" -> open.removeLastOrNull()
                open.lastOrNull() == "VEVENT" -> events.last().getOrPut(name) { value }
            }
        }
        return events
    }

    private fun toTask(event: Map<String, String>, homies: List<Homie>): ChoreTask {
        val recurrence = event["RRULE"]?.let(Schedule::parseRrule)
        val start = event["DTSTART"]?.let(::parseStart)
        val duration = event["DESCRIPTION"]?.let { MINUTES.matchEntire(it.unescaped().trim()) }
            ?.groupValues?.get(1)?.toInt()
        val assignee = ChecklistCsv.resolveAssignee(
            event["X-COZITOPIA-ASSIGNEE"].unescaped().split(ASSIGNEE_SEPARATOR),
            homies,
        )
        val pinned = recurrence != null && start != null
        return ChoreTask(
            id = event["UID"]?.removeSuffix(IcsExporter.UID_SUFFIX)?.takeIf { it.isNotBlank() }
                ?: "import-${Uuid.random()}",
            what = event["SUMMARY"].unescaped(),
            where = event["LOCATION"].unescaped(),
            frequencyText = event["X-COZITOPIA-FREQUENCY"].unescaped()
                .ifBlank { recurrence?.let(RecurrenceText::format).orEmpty() },
            durationMinutes = duration ?: 0,
            assignee = assignee.assignee,
            recurrence = recurrence,
            nextDate = start?.first.takeIf { pinned },
            startTime = start?.second.takeIf { pinned },
            pinned = pinned,
            warnings = buildSet {
                if (duration == null) add(TaskWarning.INVALID_DURATION)
                if (recurrence == null) add(TaskWarning.UNRECOGNIZED_FREQUENCY)
                if (start == null) add(TaskWarning.INVALID_START)
                if (assignee.unknownNames.isNotEmpty()) add(TaskWarning.UNKNOWN_ASSIGNEE)
            },
        )
    }

    private fun parseStart(text: String): Pair<LocalDate, LocalTime>? {
        val match = DATE_TIME.matchEntire(text.trim()) ?: return null
        val (year, month, day, hour, minute) = match.destructured
        return try {
            LocalDate(year.toInt(), month.toInt(), day.toInt()) to
                if (hour.isEmpty()) DayWindow.DEFAULT.start else LocalTime(hour.toInt(), minute.toInt())
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun String?.unescaped(): String =
        orEmpty().replace(ESCAPE) { if (it.groupValues[1] in "nN") "\n" else it.groupValues[1] }
}
