package app.cozitopia.ui.profile

import app.cozitopia.data.ErrorBus
import app.cozitopia.data.ProfileRepository
import app.cozitopia.i18n.StringKey
import app.cozitopia.ui.ScreenViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ProfileViewModel(
    private val userId: String,
    private val repository: ProfileRepository,
    errors: ErrorBus,
) : ScreenViewModel(errors) {
    private val mutableDisplayName = MutableStateFlow("")
    val displayName: StateFlow<String> = mutableDisplayName

    init {
        launchCatching { mutableDisplayName.value = repository.displayName(userId) }
    }

    fun save(displayName: String) = launchCatching {
        repository.updateDisplayName(userId, displayName)
        mutableDisplayName.value = displayName.trim()
        errors.notify(StringKey.PROFILE_SAVED)
    }
}
