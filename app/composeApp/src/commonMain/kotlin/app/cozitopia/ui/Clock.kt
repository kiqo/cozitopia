package app.cozitopia.ui

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

fun today(): LocalDate = Clock.System.todayIn(TimeZone.currentSystemDefault())
