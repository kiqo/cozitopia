package app.cozitopia.i18n

import app.cozitopia.domain.Ordinal
import app.cozitopia.domain.RecurrenceUnit
import app.cozitopia.domain.Role
import kotlinx.datetime.DayOfWeek

fun Role.key(): StringKey = when (this) {
    Role.ADMIN -> StringKey.ROLE_ADMIN
    Role.HOMIE -> StringKey.ROLE_HOMIE
}

fun RecurrenceUnit.key(): StringKey = when (this) {
    RecurrenceUnit.DAY -> StringKey.UNIT_DAY
    RecurrenceUnit.WEEK -> StringKey.UNIT_WEEK
    RecurrenceUnit.MONTH -> StringKey.UNIT_MONTH
    RecurrenceUnit.YEAR -> StringKey.UNIT_YEAR
}

fun Ordinal.key(): StringKey = when (this) {
    Ordinal.FIRST -> StringKey.ORDINAL_FIRST
    Ordinal.SECOND -> StringKey.ORDINAL_SECOND
    Ordinal.THIRD -> StringKey.ORDINAL_THIRD
    Ordinal.FOURTH -> StringKey.ORDINAL_FOURTH
    Ordinal.LAST -> StringKey.ORDINAL_LAST
}

fun DayOfWeek.key(): StringKey = when (this) {
    DayOfWeek.MONDAY -> StringKey.WEEKDAY_MONDAY
    DayOfWeek.TUESDAY -> StringKey.WEEKDAY_TUESDAY
    DayOfWeek.WEDNESDAY -> StringKey.WEEKDAY_WEDNESDAY
    DayOfWeek.THURSDAY -> StringKey.WEEKDAY_THURSDAY
    DayOfWeek.FRIDAY -> StringKey.WEEKDAY_FRIDAY
    DayOfWeek.SATURDAY -> StringKey.WEEKDAY_SATURDAY
    DayOfWeek.SUNDAY -> StringKey.WEEKDAY_SUNDAY
}
