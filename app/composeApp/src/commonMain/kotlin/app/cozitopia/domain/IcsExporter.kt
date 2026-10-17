package app.cozitopia.domain

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

sealed interface ExportScope {
    data object Own : ExportScope

    data object All : ExportScope

    data class OwnAndSelected(val userIds: Set<String>) : ExportScope

    data class ForHomie(val userId: String) : ExportScope
}

object IcsExporter {
    private const val MAX_OCTETS = 75
    private const val CRLF = "\r\n"

    fun filter(tasks: List<ChoreTask>, me: String, scope: ExportScope): List<ChoreTask> =
        when (scope) {
            ExportScope.All -> tasks
            ExportScope.Own -> tasks.filter { isOwn(it, me) }
            is ExportScope.ForHomie -> tasks.filter { isOwn(it, scope.userId) }
            is ExportScope.OwnAndSelected ->
                tasks.filter { task -> isOwn(task, me) || scope.userIds.any(task.assignee::includes) }
        }

    fun export(tasks: List<ChoreTask>, homeName: String, generatedAt: Instant): String = buildString {
        val stamp = generatedAt.toLocalDateTime(TimeZone.UTC)
        val dtstamp = "${stamp.date.basic()}T${stamp.hour.pad()}${stamp.minute.pad()}${stamp.second.pad()}Z"
        line("BEGIN:VCALENDAR")
        line("VERSION:2.0")
        line("PRODID:-//Cozitopia//Homies Calendar//EN")
        line("X-WR-CALNAME:${escape(homeName)}")
        tasks.forEach { task ->
            val start = task.nextDate ?: return@forEach
            val recurrence = task.recurrence ?: return@forEach
            line("BEGIN:VEVENT")
            line("UID:${task.id}@cozitopia.app")
            line("DTSTAMP:$dtstamp")
            val time = task.startTime?.takeIf { task.durationMinutes > 0 }
            if (time == null) {
                line("DTSTART;VALUE=DATE:${start.basic()}")
                line("DTEND;VALUE=DATE:${start.plus(1, DateTimeUnit.DAY).basic()}")
            } else {
                val begin = LocalDateTime(start, time)
                line("DTSTART:${begin.basic()}")
                line("DTEND:${begin.plusMinutes(task.durationMinutes).basic()}")
            }
            line("SUMMARY:${escape(task.what)}")
            line("LOCATION:${escape(task.where)}")
            line("DESCRIPTION:${escape("${task.durationMinutes} min")}")
            line("RRULE:${Schedule.rrule(recurrence)}")
            line("END:VEVENT")
        }
        line("END:VCALENDAR")
    }

    fun isOwn(task: ChoreTask, me: String): Boolean =
        task.assignee.kind == AssigneeKind.UNASSIGNED || task.assignee.includes(me)

    private fun StringBuilder.line(content: String) {
        var octets = 0
        var limit = MAX_OCTETS
        var index = 0
        while (index < content.length) {
            val length = if (content[index].isHighSurrogate() && index + 1 < content.length) 2 else 1
            val size = utf8Size(content[index], length)
            if (octets + size > limit) {
                append(CRLF).append(' ')
                octets = 0
                limit = MAX_OCTETS - 1
            }
            append(content, index, index + length)
            octets += size
            index += length
        }
        append(CRLF)
    }

    private fun utf8Size(char: Char, length: Int): Int = when {
        length == 2 -> 4
        char.code < 0x80 -> 1
        char.code < 0x800 -> 2
        else -> 3
    }

    private fun LocalDate.basic(): String = "${year.toString().padStart(4, '0')}${month.number.pad()}${day.pad()}"

    private fun LocalDateTime.basic(): String = "${date.basic()}T${hour.pad()}${minute.pad()}${second.pad()}"

    private fun LocalDateTime.plusMinutes(minutes: Int): LocalDateTime =
        toInstant(TimeZone.UTC).plus(minutes.minutes).toLocalDateTime(TimeZone.UTC)

    private fun Int.pad(): String = toString().padStart(2, '0')

    private fun escape(text: String): String =
        text.replace("\\", "\\\\")
            .replace(";", "\\;")
            .replace(",", "\\,")
            .replace("\r\n", "\\n")
            .replace("\n", "\\n")
            .replace("\r", "\\n")
}
