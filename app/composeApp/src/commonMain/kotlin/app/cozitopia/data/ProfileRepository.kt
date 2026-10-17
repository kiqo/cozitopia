package app.cozitopia.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class ProfileRepository(private val client: SupabaseClient) {
    suspend fun displayName(userId: String): String =
        client.from("profiles").select { filter { eq("id", userId) } }
            .decodeSingle<ProfileDto>().displayName

    suspend fun updateDisplayName(userId: String, displayName: String) {
        client.from("profiles").update({ set("display_name", displayName.trim()) }) {
            filter { eq("id", userId) }
        }
    }
}
