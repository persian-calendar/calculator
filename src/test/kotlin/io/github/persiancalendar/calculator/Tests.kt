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
            "1 / 2 = 0.5",
            "2 + 2 = 4",
            "2 + 2 * 2 = 6",
            "2 + 2 * 2 + 2 = 8",
            "2 + 2 + 2 * 2 = 8",
            "2 + 2 + 2 * 2 = 8",
            "2 + 2 + 12 * 2 = 28",
            "2 + 2 + 2 ^ 3 * 2 = 20",
            "2 + 2 + 2 ^ (3 * 2) = 68",
            "2 + (2 + 2) ^ (3 * 2) = 4098",
            "2 + (2 + 2) ^ 3 * 2 = 130",
            "2 ^ 2 ^ 3 = 256", // tests right associativity of exponential operator
            "2 + 2 + 2 * 2 / 2 = 6",
            "2 + 2 + 2 ** 10 = 1028",
            "sin(asin(0)) = 0",
            "sin(PI / 2) = 1",
            "asin 0 = 0",
            "ln 1 = 0",
            "'a = cos; a(0)' = 1",
            "'sin = cos; sin 0' = 1",
            "cos sin 0 = 1",
            "'sin = cos; clear; sin 0' = 0",
            "'clear = sin; clear 0' = 0",
            "(3, 2, 2) = (3, 2, 2)",
            "2s = 2 s",
            "sin 90 deg = 1",
            "deg = :deg",
        ]
    )
    fun `test single line eval`(input: String, expected: String) {
        assertEquals(expected, eval(input))
    }

    @Test
    fun `test multiline programs`() {
        assertEquals(
            "63\n1",
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
