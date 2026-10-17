package app.cozitopia.i18n

import kotlinx.browser.window

actual fun systemLanguageTag(): String = window.navigator.language
