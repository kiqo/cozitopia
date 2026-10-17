package app.cozitopia.data

import androidx.lifecycle.ViewModel
import io.github.jan.supabase.SupabaseClient

class AppContainer(client: SupabaseClient) : ViewModel() {
    val errors = ErrorBus()
    val auth = AuthRepository(client)
    val profiles = ProfileRepository(client)
    val homes = HomeRepository(client)
    val tasks = TaskRepository(client)
}
