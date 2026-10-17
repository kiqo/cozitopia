package app.cozitopia.i18n

enum class AppLanguage(val tag: String) {
    EN("en"),
    DE("de"),
    FR("fr");

    companion object {
        fun fromSystem(tag: String): AppLanguage =
            entries.firstOrNull { it != EN && tag.lowercase().startsWith(it.tag) } ?: EN
    }
}
