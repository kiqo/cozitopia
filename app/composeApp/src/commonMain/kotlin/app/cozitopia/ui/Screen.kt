package app.cozitopia.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import app.cozitopia.i18n.StringKey

enum class Screen(val title: StringKey, val icon: ImageVector) {
    HOMES(StringKey.NAV_HOMES, Icons.Filled.Home),
    MEMBERS(StringKey.NAV_MEMBERS, Icons.Filled.People),
    CHECKLIST(StringKey.NAV_CHECKLIST, Icons.Filled.Checklist),
    CALENDAR(StringKey.NAV_CALENDAR, Icons.Filled.CalendarMonth),
    PROFILE(StringKey.NAV_PROFILE, Icons.Filled.Person),
}
