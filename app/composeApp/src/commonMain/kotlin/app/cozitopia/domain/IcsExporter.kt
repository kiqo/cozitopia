package app.cozitopia.domain

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

object IcsExporter {
    fun filter(
        tasks: List<ChoreTask>,
        me: String,
        otherUserIds: Set<String> = emptySet(),
        includeAll: Boolean = false,
    ): List<ChoreTask> = tasks.filter { task ->
        includeAll ||
            task.assignee.kind == AssigneeKind.UNASSIGNED ||
            task.assignee.includes(me) ||
            otherUserIds.any(task.assignee::includes)
    }

    fun export(tasks: List<ChoreTask>, homeName: String): String = buildString {
        line("BEGIN:VCALENDAR")
        line("VERSION:2.0")
        line("PRODID:-//Cozitopia//Homies Calendar//EN")
        line("X-WR-CALNAME:${escape(homeName)}")
        tasks.forEach { task ->
            line("BEGIN:VEVENT")
            line("UID:${task.id}@cozitopia.app")
            line("DTSTAMP:${task.nextDate.basic()}T000000Z")
            line("DTSTART;VALUE=DATE:${task.nextDate.basic()}")
            line("DTEND;VALUE=DATE:${task.nextDate.plus(1, DateTimeUnit.DAY).basic()}")
            line("SUMMARY:${escape(task.what)}")
            line("LOCATION:${escape(task.where)}")
            line("DESCRIPTION:${escape("${task.durationMinutes} min")}")
            line("RRULE:${Schedule.rrule(task.recurrence)}")
            line("END:VEVENT")
        }
        line("END:VCALENDAR")
    }

    private fun StringBuilder.line(content: String) {
        append(content).append("\r\n")
    }

    private fun LocalDate.basic(): String = toString().replace("-", "")

    private fun escape(text: String): String =
        text.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n")
}
