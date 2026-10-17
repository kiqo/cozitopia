package app.cozitopia.ui.profile

import app.cozitopia.data.ErrorBus
import app.cozitopia.data.ProfileRepository
import app.cozitopia.domain.DayWindow
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
    private val mutableWindow = MutableStateFlow(DayWindow.DEFAULT)
    val window: StateFlow<DayWindow> = mutableWindow

    init {
        launchCatching {
            val profile = repository.profile(userId)
            mutableDisplayName.value = profile.displayName
            mutableWindow.value = profile.window
        }
    }

    fun save(displayName: String) = launchCatching {
        repository.updateDisplayName(userId, displayName)
        mutableDisplayName.value = displayName.trim()
        errors.notify(StringKey.PROFILE_SAVED)
    }

    fun setWindow(window: DayWindow) = launchCatching {
        repository.updateWindow(userId, window)
        mutableWindow.value = window
        errors.notify(StringKey.PROFILE_SAVED)
    }
}
