package app.cozitopia.ui.members

import app.cozitopia.data.ErrorBus
import app.cozitopia.data.HomeRepository
import app.cozitopia.data.Member
import app.cozitopia.domain.Role
import app.cozitopia.i18n.StringKey
import app.cozitopia.ui.ScreenViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MembersViewModel(
    private val homeId: String,
    private val repository: HomeRepository,
    errors: ErrorBus,
    private val onMembershipChanged: () -> Unit,
) : ScreenViewModel(errors) {
    private val mutableMembers = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = mutableMembers

    init {
        refresh()
    }

    fun refresh() = launchCatching { mutableMembers.value = repository.members(homeId) }

    fun changeRole(userId: String, role: Role) = launchCatching {
        repository.changeRole(homeId, userId, role)
        mutableMembers.value = repository.members(homeId)
    }

    fun remove(userId: String) = launchCatching {
        repository.removeMember(homeId, userId)
        mutableMembers.value = repository.members(homeId)
    }

    fun leave(userId: String) = launchCatching {
        repository.removeMember(homeId, userId)
        onMembershipChanged()
    }

    fun invite(email: String, role: Role) = launchCatching {
        repository.invite(homeId, email, role)
        errors.notify(StringKey.INVITATION_SENT)
    }
}
