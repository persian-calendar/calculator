package io.github.persiancalendar.calculator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class Tests {
    @Test
    fun `Initial test`() {
        assertEquals(4, Main().add(2, 2))
    }
}
