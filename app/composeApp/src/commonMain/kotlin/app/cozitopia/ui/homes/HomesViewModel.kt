package app.cozitopia.ui.homes

import app.cozitopia.data.AuthState
import app.cozitopia.data.ErrorBus
import app.cozitopia.data.HomeRepository
import app.cozitopia.data.Membership
import app.cozitopia.domain.Invitation
import app.cozitopia.ui.ScreenViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class HomesState(
    val memberships: List<Membership> = emptyList(),
    val invitations: List<Invitation> = emptyList(),
    val selectedHomeId: String? = null,
) {
    val current: Membership?
        get() = memberships.firstOrNull { it.home.id == selectedHomeId } ?: memberships.firstOrNull()
}

class HomesViewModel(
    private val user: AuthState.SignedIn,
    private val repository: HomeRepository,
    errors: ErrorBus,
) : ScreenViewModel(errors) {
    private val mutableState = MutableStateFlow(HomesState())
    val state: StateFlow<HomesState> = mutableState

    init {
        refresh()
    }

    fun refresh() = launchCatching { load() }

    fun select(homeId: String) = mutableState.update { it.copy(selectedHomeId = homeId) }

    fun create(name: String) = launchCatching {
        val homeId = repository.createHome(name)
        load()
        select(homeId)
    }

    fun rename(homeId: String, name: String) = launchCatching {
        repository.rename(homeId, name)
        load()
    }

    fun accept(invitationId: String) = launchCatching {
        val homeId = repository.accept(invitationId)
        load()
        select(homeId)
    }

    fun decline(invitationId: String) = launchCatching {
        repository.decline(invitationId)
        load()
    }

    private suspend fun load() {
        val memberships = repository.myHomes(user.userId)
        val invitations = repository.pendingInvitations(user.email)
        mutableState.update { it.copy(memberships = memberships, invitations = invitations) }
    }
}
