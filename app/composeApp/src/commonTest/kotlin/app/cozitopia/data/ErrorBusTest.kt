package app.cozitopia.data

import app.cozitopia.i18n.StringKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class ErrorBusTest {
    @Test
    fun messagesReportedBeforeCollectingAreDeliveredInOrder() = runTest {
        val bus = ErrorBus()
        bus.report(AppError.Unknown("boom"))
        bus.notify(StringKey.PROFILE_SAVED)

        val received = bus.messages.take(2).toList()

        assertEquals(AppError.Unknown("boom"), received[0].error)
        assertTrue(received[0].persistent)
        assertEquals(AppError.Known(StringKey.PROFILE_SAVED), received[1].error)
        assertFalse(received[1].persistent)
    }
}
