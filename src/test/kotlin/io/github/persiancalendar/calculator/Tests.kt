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
            "2 + 2 + 12 * 2 = 28.0",
            "2 + 2 + 2 ^ 3 * 2 = 20.0",
            "2 + 2 + 2 ^ (3 * 2) = 68.0",
            "2 + (2 + 2) ^ (3 * 2) = 4098.0",
            "2 + (2 + 2) ^ 3 * 2 = 130.0",
            "2 ^ 2 ^ 3 = 256.0", // tests right associativity of exponential operator
            "2 + 2 + 2 * 2 / 2 = 6.0",
            "2 + 2 + 2 ** 10 = 1028.0",
            "sin(asin(0)) = 0.0",
            "sin(PI / 2) = 1.0",
            "asin 0 = 0.0",
            "ln 1 = 0.0",
            "'a = cos; a(0)' = 1.0",
            "'sin = cos; cos 0' = 1.0",
            "cos sin 0 = 1.0",
            //"(3, 2, 2) = (3, 2, 2)" shows call grammar should be fixed
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
