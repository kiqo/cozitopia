package app.cozitopia.data

import app.cozitopia.i18n.StringKey
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class ErrorBus {
    private val mutableMessages = MutableSharedFlow<AppError>(extraBufferCapacity = BUFFER)
    val messages: SharedFlow<AppError> = mutableMessages.asSharedFlow()

    fun report(error: AppError) {
        mutableMessages.tryEmit(error)
    }

    fun notify(key: StringKey) = report(AppError.Known(key))

    private companion object {
        const val BUFFER = 16
    }
}
