package app.cozitopia.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector
import app.cozitopia.i18n.StringKey

enum class Screen(val title: StringKey, val icon: ImageVector) {
    HOMES(StringKey.NAV_HOMES, Icons.Outlined.Home),
    MEMBERS(StringKey.NAV_MEMBERS, Icons.Outlined.Group),
    CHECKLIST(StringKey.NAV_CHECKLIST, Icons.Outlined.Checklist),
    CALENDAR(StringKey.NAV_CALENDAR, Icons.Outlined.CalendarToday),
    PROFILE(StringKey.NAV_PROFILE, Icons.Outlined.Person),
}
