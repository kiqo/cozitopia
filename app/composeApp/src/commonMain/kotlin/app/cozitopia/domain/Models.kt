package app.cozitopia.domain

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

enum class Role { ADMIN, HOMIE }

enum class RecurrenceUnit { DAY, WEEK, MONTH, YEAR }

enum class Ordinal { FIRST, SECOND, THIRD, FOURTH, LAST }

enum class AssigneeKind { EVERYONE, UNASSIGNED, PEOPLE }

enum class TaskWarning { UNKNOWN_ASSIGNEE, INVALID_DURATION, UNRECOGNIZED_FREQUENCY }

@Serializable
data class Homie(val userId: String, val name: String, val role: Role)

@Serializable
data class Home(val id: String, val name: String)

@Serializable
data class Invitation(
    val id: String,
    val homeId: String,
    val homeName: String,
    val email: String,
    val role: Role,
)

@Serializable
data class Assignee(val kind: AssigneeKind, val userIds: Set<String> = emptySet()) {
    fun includes(userId: String): Boolean = when (kind) {
        AssigneeKind.EVERYONE -> true
        AssigneeKind.UNASSIGNED -> false
        AssigneeKind.PEOPLE -> userId in userIds
    }

    companion object {
        val Everyone = Assignee(AssigneeKind.EVERYONE)
        val Unassigned = Assignee(AssigneeKind.UNASSIGNED)
        fun of(vararg userIds: String) = Assignee(AssigneeKind.PEOPLE, userIds.toSet())
    }
}

@Serializable
data class Recurrence(
    val unit: RecurrenceUnit,
    val interval: Int = 1,
    val weekday: DayOfWeek? = null,
    val ordinal: Ordinal? = null,
    val month: Int? = null,
    val dayOfMonth: Int? = null,
)

@Serializable
data class ChoreTask(
    val id: String,
    val what: String,
    val where: String,
    val frequencyText: String,
    val durationMinutes: Int,
    val assignee: Assignee,
    val recurrence: Recurrence,
    val nextDate: LocalDate,
    @Transient val warnings: Set<TaskWarning> = emptySet(),
)
