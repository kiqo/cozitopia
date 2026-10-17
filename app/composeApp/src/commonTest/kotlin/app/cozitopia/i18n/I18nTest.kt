package app.cozitopia.i18n

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class I18nTest {
    @Test
    fun everyKeyIsTranslatedInEveryLanguage() {
        AppLanguage.entries.forEach { language ->
            val table = translations.getValue(language)

            val missing = StringKey.entries.filter { table[it].isNullOrBlank() }

            assertTrue(missing.isEmpty(), "$language is missing $missing")
        }
    }

    @Test
    fun substitutesPlaceholders() {
        val strings = Strings(AppLanguage.DE)

        val text = strings(StringKey.INVITATION_TEXT, "home" to "MyHome", "role" to "Admin")

        assertEquals("MyHome als Admin", text)
    }

    @Test
    fun formatsDatesPerLanguage() {
        val date = LocalDate(2026, 3, 7)

        val formatted = AppLanguage.entries.map { Strings(it).date(date) }

        assertEquals(listOf("2026-03-07", "07.03.2026", "07.03.2026"), formatted)
    }

    @Test
    fun choosesStartupLanguageFromSystemTag() {
        val tags = listOf("de", "de-AT", "fr-CH", "en-US", "it", "")

        val languages = tags.map(AppLanguage::fromSystem)

        assertEquals(
            listOf(AppLanguage.DE, AppLanguage.DE, AppLanguage.FR, AppLanguage.EN, AppLanguage.EN, AppLanguage.EN),
            languages,
        )
    }
}
