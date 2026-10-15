@file:OptIn(ExperimentalWasmJsInterop::class)

package io.github.persiancalendar.calculator.web

import io.github.persiancalendar.calculator.eval
import kotlin.js.ExperimentalWasmJsInterop

external fun calculatorReady(evaluate: (expression: String) -> String)

fun main() {
    calculatorReady(::eval)
}
