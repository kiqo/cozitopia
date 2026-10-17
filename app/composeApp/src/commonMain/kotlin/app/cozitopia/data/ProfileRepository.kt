package app.cozitopia.data

import app.cozitopia.domain.DayWindow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.datetime.isoDayNumber

class ProfileRepository(private val client: SupabaseClient) {
    suspend fun profile(userId: String): ProfileDto =
        client.from("profiles").select { filter { eq("id", userId) } }.decodeSingle<ProfileDto>()

    suspend fun updateDisplayName(userId: String, displayName: String) {
        client.from("profiles").update({ set("display_name", displayName.trim()) }) {
            filter { eq("id", userId) }
        }
    }

    suspend fun updateWindow(userId: String, window: DayWindow) {
        client.from("profiles").update(
            {
                set("preferred_weekday", window.weekday.isoDayNumber)
                set("preferred_start_time", window.start.toString())
                set("latest_end_time", window.end.toString())
            },
        ) {
            filter { eq("id", userId) }
        }
    }
}
