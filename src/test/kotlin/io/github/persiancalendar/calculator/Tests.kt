package io.github.persiancalendar.calculator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class Tests {
    @ParameterizedTest
    @CsvSource(
        delimiter = '=',
        value = [
            "2 + 2 = 4.0",
            "2 + 2 * 2 = 6.0",
            "2 + 2 * 2 + 2 = 8.0",
            "2 + 2 + 2 * 2 = 8.0",
            "2 + 2 + 2 * 2 = 8.0",
            "2 + 2 + 2 ^ 3 * 2 = 20.0",
            "2 + 2 + 2 ^ (3 * 2) = 68.0",
            "2 + (2 + 2) ^ (3 * 2) = 4098.0",
            "2 + (2 + 2) ^ 3 * 2 = 130.0",
            "2 ^ 2 ^ 3 = 256.0", // tests right associativity of exponential operator
            "2 + 2 + 2 * 2 / 2 = 6.0",
            "2 + 2 + 2 ** 10 = 1028.0",
        ]
    )
    fun `test single line eval`(input: String, expected: String) {
        assertEquals(expected, eval(input))
    }

    @Test
    fun `test multiline programs`() {
        assertEquals(
            "63.0\n1.0",
            eval(
                """
                    a = 3;
                    xa = 21
                    # comment
                    xa * a
                    // comment
                    PI / PI
                """.trimIndent()
            )
        )
    }
}
