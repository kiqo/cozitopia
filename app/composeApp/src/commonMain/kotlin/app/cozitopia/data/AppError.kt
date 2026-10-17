package app.cozitopia.data

import app.cozitopia.i18n.StringKey
import io.github.jan.supabase.exceptions.HttpRequestException

sealed interface AppError {
    data class Known(val key: StringKey) : AppError

    data class Unknown(val message: String) : AppError

    companion object {
        private val DATABASE_CODES = mapOf(
            "NOT_ADMIN" to StringKey.ERROR_NOT_ADMIN,
            "ALREADY_MEMBER" to StringKey.ERROR_ALREADY_MEMBER,
            "DUPLICATE_INVITATION" to StringKey.ERROR_DUPLICATE_INVITATION,
            "INVITATION_NOT_FOUND" to StringKey.ERROR_INVITATION_NOT_FOUND,
            "NOT_MEMBER" to StringKey.ERROR_NOT_MEMBER,
            "LAST_ADMIN" to StringKey.ERROR_LAST_ADMIN,
            "INVALID_ROLE" to StringKey.ERROR_INVALID_ROLE,
            "INVALID_EMAIL" to StringKey.ERROR_INVALID_EMAIL,
        )

        fun from(throwable: Throwable): AppError {
            val message = throwable.message.orEmpty()
            val key = DATABASE_CODES.entries.firstOrNull { message.contains(it.key) }?.value
            return when {
                key != null -> Known(key)
                throwable is HttpRequestException -> Known(StringKey.ERROR_NETWORK)
                else -> Unknown(message.ifBlank { throwable::class.simpleName.orEmpty() })
            }
        }
    }
}
