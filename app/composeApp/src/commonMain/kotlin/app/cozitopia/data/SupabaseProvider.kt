package app.cozitopia.data

import app.cozitopia.config.AppConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

object SupabaseProvider {
    val isConfigured: Boolean
        get() = AppConfig.SUPABASE_URL.isNotBlank() && AppConfig.SUPABASE_ANON_KEY.isNotBlank()

    fun create(): SupabaseClient =
        createSupabaseClient(AppConfig.SUPABASE_URL, AppConfig.SUPABASE_ANON_KEY) {
            install(Auth)
            install(Postgrest)
        }
}
