package app.cozitopia

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import app.cozitopia.data.AppContainer
import app.cozitopia.data.AuthState
import app.cozitopia.data.SupabaseProvider
import app.cozitopia.i18n.AppLanguage
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.Strings
import app.cozitopia.i18n.systemLanguageTag
import app.cozitopia.ui.CozitopiaTheme
import app.cozitopia.ui.DropdownSelect
import app.cozitopia.ui.Screen
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
            if (SupabaseProvider.isConfigured) {
                ConfiguredApp(language) { language = it }
            } else {
                NotConfiguredApp(language) { language = it }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppFrame(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    snackbarHost: SnackbarHostState,
    onLogout: (() -> Unit)?,
    bottomBar: @Composable () -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val strings = LocalStrings.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings(StringKey.APP_NAME)) },
                actions = {
                    DropdownSelect(
                        selected = language.name,
                        options = AppLanguage.entries,
                        optionLabel = { it.name },
                        onSelect = onLanguageChange,
                    )
                    if (onLogout != null) {
                        IconButton(onClick = onLogout) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = strings(StringKey.LOG_OUT))
                        }
                    }
                },
            )
        },
        bottomBar = bottomBar,
        snackbarHost = { SnackbarHost(snackbarHost) },
        content = { padding -> content(Modifier.padding(padding)) },
    )
}

@Composable
private fun NotConfiguredApp(language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    val strings = LocalStrings.current
    AppFrame(language, onLanguageChange, remember { SnackbarHostState() }, null, {}) { modifier ->
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        ) {
            Text(text = strings(StringKey.NOT_CONFIGURED_TITLE), style = MaterialTheme.typography.titleLarge)
            Text(text = strings(StringKey.NOT_CONFIGURED_BODY))
        }
    }
}

@Composable
private fun ConfiguredApp(language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    val container = remember { AppContainer(SupabaseProvider.create()) }
    val authViewModel = viewModel { AuthViewModel(container.auth, container.errors) }
    val authState by authViewModel.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val strings by rememberUpdatedState(LocalStrings.current)
    var screen by rememberSaveable { mutableStateOf(Screen.HOMES) }
    val signedIn = authState as? AuthState.SignedIn

    LaunchedEffect(container) {
        container.errors.messages.collect { snackbarHost.showSnackbar(it.text(strings)) }
    }

    AppFrame(
        language = language,
        onLanguageChange = onLanguageChange,
        snackbarHost = snackbarHost,
        onLogout = signedIn?.let { { authViewModel.signOut() } },
        bottomBar = {
            if (signedIn != null) {
                NavigationBar {
                    Screen.entries.forEach { entry ->
                        NavigationBarItem(
                            selected = screen == entry,
                            onClick = { screen = entry },
                            icon = { Icon(entry.icon, contentDescription = null) },
                            label = { Text(LocalStrings.current(entry.title)) },
                        )
                    }
                }
            }
        },
    ) { modifier ->
        when (val current = authState) {
            AuthState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            AuthState.SignedOut -> AuthScreen(authViewModel, modifier)
            is AuthState.SignedIn -> key(current.userId) {
                SignedInContent(current, container, screen, { screen = it }, modifier)
            }
        }
    }
}

@Composable
private fun SignedInContent(
    user: AuthState.SignedIn,
    container: AppContainer,
    screen: Screen,
    onNavigate: (Screen) -> Unit,
    modifier: Modifier,
) {
    val homesViewModel = viewModel(key = "homes-${user.userId}") {
        HomesViewModel(user, container.homes, container.errors)
    }
    val homesState by homesViewModel.state.collectAsState()
    val membership = homesState.current

    when {
        screen == Screen.HOMES -> HomesScreen(homesViewModel, modifier)
        screen == Screen.PROFILE -> {
            val profileViewModel = viewModel(key = "profile-${user.userId}") {
                ProfileViewModel(user.userId, container.profiles, container.errors)
            }
            ProfileScreen(profileViewModel, modifier)
        }
        membership == null -> Box(modifier.fillMaxSize().padding(16.dp)) {
            Text(LocalStrings.current(StringKey.NO_HOME_SELECTED))
        }
        else -> {
            val homeId = membership.home.id
            when (screen) {
                Screen.MEMBERS -> {
                    val membersViewModel = viewModel(key = "members-$homeId") {
                        MembersViewModel(homeId, container.homes, container.errors, homesViewModel::refresh)
                    }
                    MembersScreen(membersViewModel, user.userId, membership.role, modifier)
                }
                Screen.CHECKLIST -> {
                    val checklistViewModel = viewModel(key = "checklist-$homeId") {
                        ChecklistViewModel(homeId, container.homes, container.tasks, container.errors)
                    }
                    ChecklistScreen(checklistViewModel, { onNavigate(Screen.CALENDAR) }, modifier)
                }
                else -> {
                    val calendarViewModel = viewModel(key = "calendar-$homeId-${membership.home.name}") {
                        CalendarViewModel(
                            homeId, membership.home.name, user.userId, container.homes, container.tasks, container.errors,
                        )
                    }
                    CalendarScreen(calendarViewModel, user.userId, modifier)
                }
            }
        }
    }
}
