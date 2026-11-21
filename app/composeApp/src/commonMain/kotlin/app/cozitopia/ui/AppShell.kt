package app.cozitopia.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.cozitopia.data.Membership
import app.cozitopia.i18n.AppLanguage
import app.cozitopia.i18n.LocalStrings
import app.cozitopia.i18n.StringKey
import app.cozitopia.i18n.key

private val WideBreakpoint = 840.dp
private val SidebarWidth = 208.dp
private val ContentMaxWidth = 1120.dp

class CozySnackbarVisuals(
    override val message: String,
    override val actionLabel: String?,
    override val duration: SnackbarDuration,
    val isError: Boolean,
) : SnackbarVisuals {
    override val withDismissAction: Boolean = false
}

@Composable
fun CozySnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data ->
        val isError = (data.visuals as? CozySnackbarVisuals)?.isError == true
        Snackbar(
            snackbarData = data,
            shape = MaterialTheme.shapes.large,
            containerColor = if (isError) MaterialTheme.colorScheme.error else Navy,
            contentColor = Color.White,
            actionColor = Color.White,
            modifier = Modifier.widthIn(max = 560.dp).padding(12.dp),
        )
    }
}

@Composable
fun AdaptiveLayout(content: @Composable () -> Unit) {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            CompositionLocalProvider(LocalWideLayout provides (maxWidth >= WideBreakpoint), content = content)
        }
    }
}

@Composable
fun ScreenColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val wide = LocalWideLayout.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = if (wide) 48.dp else 20.dp)
            .padding(top = if (wide) 8.dp else 16.dp, bottom = 32.dp)
            .widthIn(max = ContentMaxWidth),
        verticalArrangement = Arrangement.spacedBy(if (wide) 24.dp else 16.dp),
        content = content,
    )
}

class ShellHomes(
    val memberships: List<Membership>,
    val current: Membership?,
    val onSelect: (String) -> Unit,
)

@Composable
fun CozyShell(
    screen: Screen,
    onNavigate: (Screen) -> Unit,
    homes: ShellHomes,
    displayName: String,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    snackbarHost: SnackbarHostState,
    content: @Composable () -> Unit,
) {
    if (LocalWideLayout.current) {
        Box(Modifier.fillMaxSize()) {
            Row(Modifier.fillMaxSize()) {
                Sidebar(screen, onNavigate, homes, displayName, language, onLanguageChange)
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Breadcrumb(screen, homes.current)
                    Box(Modifier.weight(1f).fillMaxWidth()) { content() }
                }
            }
            CozySnackbarHost(snackbarHost, Modifier.align(Alignment.BottomCenter))
        }
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { PhoneTopBar(homes, language, onLanguageChange) },
            bottomBar = { PhoneNavigation(screen, onNavigate) },
            snackbarHost = { CozySnackbarHost(snackbarHost) },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) { content() }
        }
    }
}

@Composable
fun Wordmark(modifier: Modifier = Modifier) {
    Text(
        text = LocalStrings.current(StringKey.APP_NAME),
        style = MaterialTheme.typography.headlineMedium,
        color = Navy,
        modifier = modifier,
    )
}

@Composable
private fun Breadcrumb(screen: Screen, current: Membership?) {
    val strings = LocalStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().widthIn(max = ContentMaxWidth).padding(start = 48.dp, end = 48.dp, top = 32.dp, bottom = 24.dp),
    ) {
        Text(
            text = listOfNotNull(current?.home?.name, strings(screen.title)).joinToString("  /  ").uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (current != null) {
            Text(
                text = strings(StringKey.YOUR_ROLE, "role" to strings(current.role.key())),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Sidebar(
    screen: Screen,
    onNavigate: (Screen) -> Unit,
    homes: ShellHomes,
    displayName: String,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .width(SidebarWidth)
            .fillMaxHeight()
            .background(CreamCard)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 24.dp),
    ) {
        Wordmark(Modifier.padding(start = 12.dp, bottom = 24.dp))
        SidebarHomeSwitcher(homes)
        Spacer(Modifier.size(32.dp))
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Screen.entries.forEach { entry -> SidebarItem(entry, entry == screen) { onNavigate(entry) } }
        }
        Spacer(Modifier.weight(1f).heightIn(min = 32.dp))
        Text(
            text = strings(StringKey.LANGUAGE).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        )
        Row(Modifier.padding(start = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppLanguage.entries.forEach { option -> LanguagePill(option, option == language) { onLanguageChange(option) } }
        }
        Spacer(Modifier.size(28.dp))
        Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (displayName.isNotBlank()) {
                Text(displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    strings(StringKey.TAGLINE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SidebarItem(screen: Screen, selected: Boolean, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val background by animateColorAsState(
        when {
            selected -> MaterialTheme.colorScheme.primaryContainer
            hovered -> MaterialTheme.colorScheme.surfaceContainer
            else -> Color.Transparent
        },
        label = "sidebar-item",
    )
    val content = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(MaterialTheme.shapes.medium)
            .background(background)
            .hoverable(source)
            .selectable(selected = selected, interactionSource = source, indication = null, role = Role.Tab, onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Icon(screen.icon, contentDescription = null, tint = content, modifier = Modifier.size(20.dp))
        Text(
            LocalStrings.current(screen.title),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = content,
        )
    }
}

@Composable
private fun LanguagePill(language: AppLanguage, selected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(MinTouchTarget)
            .clip(MaterialTheme.shapes.medium)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Text(
            language.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SidebarHomeSwitcher(homes: ShellHomes) {
    val current = homes.current ?: return
    HomeMenu(homes) { open ->
        Surface(
            onClick = open,
            shape = MaterialTheme.shapes.large,
            color = CreamCard,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        ) {
            Column(Modifier.padding(horizontal = 12.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    LocalStrings.current(StringKey.CURRENT_HOME).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        current.home.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (homes.memberships.size > 1) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeMenu(homes: ShellHomes, anchor: @Composable (open: () -> Unit) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val others = homes.memberships.filter { it.home.id != homes.current?.home?.id }
    Box {
        anchor { if (others.isNotEmpty()) expanded = true }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            containerColor = CreamCard,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            others.forEach { membership ->
                DropdownMenuItem(
                    text = { Text(membership.home.name) },
                    onClick = {
                        expanded = false
                        homes.onSelect(membership.home.id)
                    },
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                )
            }
        }
    }
}

@Composable
private fun PhoneTopBar(homes: ShellHomes, language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Wordmark(Modifier.weight(1f))
            LanguageMenu(language, onLanguageChange)
        }
        val current = homes.current
        if (current != null) {
            Spacer(Modifier.size(8.dp))
            HomeMenu(homes) { open ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .fillMaxWidth()
                        .heightIn(min = MinTouchTarget)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(role = Role.DropdownList, onClick = open)
                        .padding(horizontal = 14.dp),
                ) {
                    Text(
                        current.home.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        strings(StringKey.CURRENT_HOME),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (homes.memberships.size > 1) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.padding(start = 4.dp).size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun LanguageMenu(language: AppLanguage, onLanguageChange: (AppLanguage) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = MinTouchTarget)
                .clip(MaterialTheme.shapes.medium)
                .clickable(role = Role.DropdownList, onClickLabel = LocalStrings.current(StringKey.LANGUAGE)) { expanded = true }
                .padding(horizontal = 12.dp),
        ) {
            Text(language.name, style = MaterialTheme.typography.labelLarge)
            Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            shape = MaterialTheme.shapes.large,
            containerColor = CreamCard,
        ) {
            AppLanguage.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        expanded = false
                        onLanguageChange(option)
                    },
                )
            }
        }
    }
}

@Composable
private fun PhoneNavigation(screen: Screen, onNavigate: (Screen) -> Unit) {
    NavigationBar(containerColor = CreamCard, tonalElevation = 0.dp) {
        Screen.entries.forEach { entry ->
            NavigationBarItem(
                selected = screen == entry,
                onClick = { onNavigate(entry) },
                icon = { Icon(entry.icon, contentDescription = null) },
                label = {
                    Text(
                        LocalStrings.current(entry.title),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}
