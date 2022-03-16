package io.github.persiancalendar.calculator

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.properties.ReadOnlyProperty

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
            "sin 90 deg = 1",
            "deg = :deg",
            "1/0 = Infinity",
            "-1/0 = -Infinity",
            "' -1/0' = -Infinity",
            "0/0 = NaN",
            "5 = 5",
            "12 / 2 = 6",
            "12 * 2 = 24",
            "5 - 1 = 4",
            "5 + 1 = 6",
            "5/5*5 = 5",
            "2 - 2 - 2 = -2",
            "2 * 2 + 2 = 6",
            "2 + 2 * 2 = 6",
            "(3) = 3",
            " ( 2+2 ) = 4",
            "2 * (2 + 2) = 8",
            "(2 * 2) / 2 = 2",
            "(2 + 2) * 2 = 8",
            "2 *    ( 2 - 2) = 0",
            "7 / 5 * ( 2 + 2 * 2 ) = 8.399999999999999",
            "7 / 5 * (((10 + 5) / 2.0 * 2 + (25-10/2*2.0)) / ((5 -7) - 4- 4/2 + 2) * 2) = -14",
            "'2 *2 + 2  ' = 6",
            "'2 *2 + 2  ' = 6",
            "'  2  + 2*      2' = 6",
            "'2 *-2 + 2  ' = -2",
            "'  2  + 2*      -2' = -2",
            "'-5+1' = -4",
            "-5++1 = -4",
            "2 *-2 + 2  * 2 + 2 -2 / -4 = 2.5",
            "'a = 2' = ''",
            "'// a' = ''", //"'# a' = ''", TODO: make this work
            "'' = ''", "';' = ''", "';;' = ''",
        ]
    )
    fun `test single line eval`(input: String, expected: String) {
        assertEquals(expected, eval(input))
    }

    @Test
    fun `test expressions`() {
        assertEquals(
            "(2 + 3 + 4)",
            Value.Expression(
                Value.Symbol("+"),
                listOf(2.0, 3.0, 4.0).map(Value::Number)
            ).toString()
        )
        val sin by Value.Symbol
        val x by Value.Symbol
        val two = Value.Number(2.0)
        assertEquals(
            "sin((:x ^ 2))",
            Value.Expression(
                Value.Symbol("sin"),
                listOf(Value.Expression(Value.Symbol("^"), listOf(x, two)))
            ).toString()
        )
        assertEquals(
            "sin((:x ^ ((4 + :x) + 2)))",
            sin(x.pow(two + two + x + two)).toString()
        )
    }

    @Test
    fun `test time calculator`() {
        assertEquals(
            """
                0d 0h 0m 2s
                2.3148148148148147E-5 d
                5.555555555555556E-4 h
                0.03333333333333333 m
                2 s
            """.trimIndent(),
            eval("2s")
        )
        assertEquals(
            """
                2d 9h 29m 19s
                2.3953587962962963 d
                57.48861111111111 h
                3449.3166666666666 m
                206959 s
            """.trimIndent(),
            eval("1d + 2h + 3m + 4s + 4h + 5s - 2030s + 28h")
        )
        assertEquals(
            """
                2d 9h 29m 19s
                2.3953587962962963 d
                57.48861111111111 h
                3449.3166666666666 m
                206959 s
            """.trimIndent(),
            eval("1d 2h 3m 4s + 4h 5s - 2030s + 28h")
        )
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "2 *-2 +aa  * 2 + 2 -2 / -4",
            "5+ 5 5 6 +  7",
            "7 / 5 * ((2 + 2) / (((5 -7) + 2) * 2)",
        ]
    )
    fun `test errors`(input: String) {
        assertThrows(Exception::class.java) { eval(input) }
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
