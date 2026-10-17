package app.cozitopia.data

import app.cozitopia.domain.Assignee
import app.cozitopia.domain.ChoreTask
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

val taskJson = Json {
    encodeDefaults = true
    ignoreUnknownKeys = true
}

fun ChoreTask.toDto(): TaskDto = TaskDto(
    id = id,
    what = what,
    location = where,
    frequencyText = frequencyText,
    durationMinutes = durationMinutes,
    assigneeKind = assignee.kind,
    assigneeIds = assignee.userIds.toList(),
    recurrence = recurrence,
    nextDate = nextDate,
)

fun TaskDto.toDomain(): ChoreTask = ChoreTask(
    id = id.orEmpty(),
    what = what,
    where = location,
    frequencyText = frequencyText,
    durationMinutes = durationMinutes,
    assignee = Assignee(assigneeKind, assigneeIds.toSet()),
    recurrence = recurrence,
    nextDate = nextDate,
)

fun List<ChoreTask>.toJson(): JsonElement =
    taskJson.encodeToJsonElement(ListSerializer(TaskDto.serializer()), map(ChoreTask::toDto))
