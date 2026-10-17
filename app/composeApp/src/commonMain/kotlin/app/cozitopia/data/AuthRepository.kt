package app.cozitopia.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

sealed interface AuthState {
    data object Loading : AuthState

    data object SignedOut : AuthState

    data class SignedIn(val userId: String, val email: String) : AuthState
}

class AuthRepository(private val client: SupabaseClient) {
    val state: Flow<AuthState> = client.auth.sessionStatus.map { status ->
        when (status) {
            is SessionStatus.Initializing -> AuthState.Loading
            is SessionStatus.Authenticated -> status.session.user
                ?.let { AuthState.SignedIn(it.id, it.email.orEmpty()) }
                ?: AuthState.SignedOut
            else -> AuthState.SignedOut
        }
    }

    suspend fun signUp(email: String, password: String, displayName: String): Boolean {
        val user = client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject { put("display_name", displayName.trim()) }
        }
        return user != null && client.auth.currentSessionOrNull() != null
    }

    suspend fun signIn(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun signOut() {
        client.auth.signOut()
    }
}
