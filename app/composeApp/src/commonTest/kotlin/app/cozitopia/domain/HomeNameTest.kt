package app.cozitopia.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class HomeNameTest {
    @Test
    fun blankNamesFallBackToDefault() {
        assertEquals(DEFAULT_HOME_NAME, "   ".toHomeName())
        assertEquals(DEFAULT_HOME_NAME, "".toHomeName())
    }

    @Test
    fun namesAreTrimmed() {
        assertEquals("Beach House", "  Beach House ".toHomeName())
    }
}
