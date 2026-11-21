package app.cozitopia

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cottage
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cozitopia.data.AppContainer
import app.cozitopia.data.AuthState
import app.cozitopia.data.SupabaseProvider
import app.cozitopia.i18n.AppLanguage
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.Strings
import app.cozitopia.i18n.systemLanguageTag
import app.cozitopia.ui.AdaptiveLayout
import app.cozitopia.ui.CozitopiaTheme
import app.cozitopia.ui.CozyShell
import app.cozitopia.ui.CozySnackbarHost
import app.cozitopia.ui.CozySnackbarVisuals
import app.cozitopia.ui.EmptyState
import app.cozitopia.ui.ScreenColumn
import app.cozitopia.ui.Screen
import app.cozitopia.ui.SecondaryButton
import app.cozitopia.ui.ShellHomes
import app.cozitopia.ui.auth.AuthScreen
import app.cozitopia.ui.auth.AuthViewModel
import app.cozitopia.ui.calendar.CalendarScreen
import app.cozitopia.ui.calendar.CalendarViewModel
import app.cozitopia.ui.checklist.ChecklistScreen
import app.cozitopia.ui.checklist.ChecklistViewModel
import app.cozitopia.ui.homes.HomesScreen
import app.cozitopia.ui.homes.HomesViewModel
import app.cozitopia.ui.members.MembersScreen
import app.cozitopia.ui.members.MembersViewModel
import app.cozitopia.ui.profile.ProfileScreen
import app.cozitopia.ui.profile.ProfileViewModel
import app.cozitopia.ui.text

@Composable
fun App() {
    var language by rememberSaveable { mutableStateOf(AppLanguage.fromSystem(systemLanguageTag())) }
    val strings = remember(language) { Strings(language) }
    CompositionLocalProvider(LocalStrings provides strings) {
        CozitopiaTheme {
            AdaptiveLayout {
                if (SupabaseProvider.isConfigured) {
                    ConfiguredApp(language) { language = it }
                } else {
                    NotConfiguredApp()
                }
            }
        }
    }
}

@Composable
private fun NotConfiguredApp() {
    val strings = LocalStrings.current
    ScreenColumn {
        EmptyState(
            icon = Icons.Outlined.SettingsSuggest,
            title = strings(StringKey.NOT_CONFIGURED_TITLE),
            body = strings(StringKey.NOT_CONFIGURED_BODY),
        )
    }
}

@Composable
private fun ConfiguredApp(language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    val container = viewModel { AppContainer(SupabaseProvider.create()) }
    val authViewModel = viewModel { AuthViewModel(container.auth, container.errors) }
    val authState by authViewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val strings by rememberUpdatedState(LocalStrings.current)

    LaunchedEffect(container) {
        container.errors.messages.collect { message ->
            snackbarHost.showSnackbar(
                CozySnackbarVisuals(
                    message = message.error.text(strings),
                    actionLabel = if (message.persistent) strings(StringKey.DISMISS) else null,
                    duration = if (message.persistent) SnackbarDuration.Indefinite else SnackbarDuration.Short,
                    isError = message.persistent,
                ),
            )
        }
    }

    when (val current = authState) {
        AuthState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        AuthState.SignedOut -> Box(Modifier.fillMaxSize()) {
            AuthScreen(authViewModel, language, onLanguageChange)
            CozySnackbarHost(snackbarHost, Modifier.align(Alignment.BottomCenter))
        }
        is AuthState.SignedIn -> key(current.userId) {
            SignedInApp(current, container, language, onLanguageChange, snackbarHost, authViewModel::signOut)
        }
    }
}

@Composable
private fun SignedInApp(
    user: AuthState.SignedIn,
    container: AppContainer,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    snackbarHost: SnackbarHostState,
    onSignOut: () -> Unit,
) {
    var screen by rememberSaveable { mutableStateOf(Screen.HOMES) }
    val homesViewModel = viewModel(key = "homes-${user.userId}") {
        HomesViewModel(user, container.homes, container.errors)
    }
    val profileViewModel = viewModel(key = "profile-${user.userId}") {
        ProfileViewModel(user.userId, container.profiles, container.errors)
    }
    val homesState by homesViewModel.state.collectAsState()
    val displayName by profileViewModel.displayName.collectAsState()
    val membership = homesState.current

    CozyShell(
        screen = screen,
        onNavigate = { screen = it },
        homes = ShellHomes(homesState.memberships, membership, homesViewModel::select),
        displayName = displayName,
        language = language,
        onLanguageChange = onLanguageChange,
        snackbarHost = snackbarHost,
    ) {
        when {
            screen == Screen.HOMES -> HomesScreen(homesViewModel)
            screen == Screen.PROFILE -> ProfileScreen(profileViewModel, user.email, onSignOut)
            membership == null -> ScreenColumn {
                EmptyState(
                    icon = Icons.Outlined.Cottage,
                    title = LocalStrings.current(StringKey.NO_HOMES_TITLE),
                    body = LocalStrings.current(StringKey.NO_HOME_SELECTED),
                ) {
                    SecondaryButton(LocalStrings.current(StringKey.NAV_HOMES), { screen = Screen.HOMES })
                }
            }
            else -> {
                val homeId = membership.home.id
                when (screen) {
                    Screen.MEMBERS -> {
                        val membersViewModel = viewModel(key = "members-$homeId") {
                            MembersViewModel(homeId, container.homes, container.errors, homesViewModel::refresh)
                        }
                        MembersScreen(membersViewModel, user.userId, membership)
                    }
                    Screen.CHECKLIST -> {
                        val checklistViewModel = viewModel(key = "checklist-$homeId") {
                            ChecklistViewModel(homeId, container.homes, container.tasks, container.errors)
                        }
                        ChecklistScreen(checklistViewModel, user.userId) { screen = Screen.CALENDAR }
                    }
                    else -> {
                        val calendarViewModel = viewModel(key = "calendar-$homeId-${membership.home.name}") {
                            CalendarViewModel(
                                homeId, membership.home.name, container.homes, container.tasks, container.errors,
                            )
                        }
                        CalendarScreen(calendarViewModel, user.userId)
                    }
                }
            }
        }
    }
}
