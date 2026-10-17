package app.cozitopia.data

import app.cozitopia.domain.ChoreTask
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TaskRepository(private val client: SupabaseClient) {
    suspend fun tasks(homeId: String): List<ChoreTask> =
        client.from("tasks")
            .select { filter { eq("home_id", homeId) } }
            .decodeList<TaskDto>()
            .map(TaskDto::toDomain)

    suspend fun saveChecklist(homeId: String, tasks: List<ChoreTask>) {
        client.postgrest.rpc(
            "save_tasks",
            buildJsonObject {
                put("target_home", homeId)
                put("new_tasks", tasks.toJson())
            },
        )
    }
}
