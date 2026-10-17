package app.cozitopia.data

import app.cozitopia.i18n.StringKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

data class BusMessage(val error: AppError, val persistent: Boolean)

class ErrorBus {
    private val channel = Channel<BusMessage>(Channel.UNLIMITED)
    val messages: Flow<BusMessage> = channel.receiveAsFlow()

    fun report(error: AppError) {
        channel.trySend(BusMessage(error, persistent = true))
    }

    fun notify(key: StringKey) {
        channel.trySend(BusMessage(AppError.Known(key), persistent = false))
    }
}
