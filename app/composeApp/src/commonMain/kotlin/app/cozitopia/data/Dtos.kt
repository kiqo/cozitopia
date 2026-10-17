package app.cozitopia.data

import app.cozitopia.domain.AssigneeKind
import app.cozitopia.domain.DayWindow
import app.cozitopia.domain.Recurrence
import app.cozitopia.domain.Role
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.isoDayNumber
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String,
    val email: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("preferred_weekday") val preferredWeekday: Int = DayWindow.DEFAULT.weekday.isoDayNumber,
    @SerialName("preferred_start_time") val preferredStartTime: LocalTime = DayWindow.DEFAULT.start,
    @SerialName("latest_end_time") val latestEndTime: LocalTime = DayWindow.DEFAULT.end,
) {
    val window: DayWindow get() = DayWindow(DayOfWeek(preferredWeekday), preferredStartTime, latestEndTime)
}

@Serializable
data class ProfileRefDto(
    @SerialName("display_name") val displayName: String,
    val email: String,
    @SerialName("preferred_weekday") val preferredWeekday: Int = DayWindow.DEFAULT.weekday.isoDayNumber,
    @SerialName("preferred_start_time") val preferredStartTime: LocalTime = DayWindow.DEFAULT.start,
    @SerialName("latest_end_time") val latestEndTime: LocalTime = DayWindow.DEFAULT.end,
) {
    val window: DayWindow get() = DayWindow(DayOfWeek(preferredWeekday), preferredStartTime, latestEndTime)
}

@Serializable
data class HomeDto(val id: String, val name: String)

@Serializable
data class MembershipDto(
    val role: Role,
    val homes: HomeDto,
)

@Serializable
data class MemberDto(
    @SerialName("user_id") val userId: String,
    val role: Role,
    val profiles: ProfileRefDto,
)

@Serializable
data class HomeNameDto(val name: String)

@Serializable
data class InvitationDto(
    val id: String,
    @SerialName("home_id") val homeId: String,
    val email: String,
    val role: Role,
    val homes: HomeNameDto,
)

@Serializable
data class TaskDto(
    val id: String? = null,
    val what: String,
    val location: String,
    @SerialName("frequency_text") val frequencyText: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("assignee_kind") val assigneeKind: AssigneeKind,
    @SerialName("assignee_ids") val assigneeIds: List<String>,
    val recurrence: Recurrence,
    @SerialName("next_date") val nextDate: LocalDate,
    @SerialName("start_time") val startTime: LocalTime,
)
