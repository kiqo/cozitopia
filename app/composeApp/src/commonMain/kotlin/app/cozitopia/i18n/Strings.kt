package app.cozitopia.i18n

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

class Strings(val language: AppLanguage) {
    private val table = translations.getValue(language)

    operator fun invoke(key: StringKey, vararg placeholders: Pair<String, String>): String =
        placeholders.fold(table.getValue(key)) { text, (name, value) ->
            text.replace("{$name}", value)
        }

    fun date(date: LocalDate): String {
        val year = date.year.toString().padStart(YEAR_DIGITS, '0')
        val month = date.month.number.toString().padStart(PART_DIGITS, '0')
        val day = date.day.toString().padStart(PART_DIGITS, '0')
        return if (language == AppLanguage.EN) "$year-$month-$day" else "$day.$month.$year"
    }

    private companion object {
        const val YEAR_DIGITS = 4
        const val PART_DIGITS = 2
    }
}

val translations: Map<AppLanguage, Map<StringKey, String>> = mapOf(
    AppLanguage.EN to stringsEn,
    AppLanguage.DE to stringsDe,
    AppLanguage.FR to stringsFr,
)

val LocalStrings = staticCompositionLocalOf { Strings(AppLanguage.EN) }
