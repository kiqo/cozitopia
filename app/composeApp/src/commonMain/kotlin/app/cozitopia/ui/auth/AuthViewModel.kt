package app.cozitopia.ui.auth

import androidx.lifecycle.viewModelScope
import app.cozitopia.data.AuthRepository
import app.cozitopia.data.AuthState
import app.cozitopia.data.ErrorBus
import app.cozitopia.ui.ScreenViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class AuthViewModel(
    private val repository: AuthRepository,
    errors: ErrorBus,
) : ScreenViewModel(errors) {
    val state: StateFlow<AuthState> =
        repository.state.stateIn(viewModelScope, SharingStarted.Eagerly, AuthState.Loading)

    val awaitingEmailConfirmation = MutableStateFlow(false)

    fun signUp(email: String, password: String, displayName: String) = launchCatching {
        awaitingEmailConfirmation.value = !repository.signUp(email, password, displayName)
    }

    fun signIn(email: String, password: String) = launchCatching {
        awaitingEmailConfirmation.value = false
        repository.signIn(email, password)
    }

    fun signOut() = launchCatching { repository.signOut() }
}
