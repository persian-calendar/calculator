package io.github.persiancalendar.calculator

import io.github.persiancalendar.calculator.parser.GrammarLexer
import io.github.persiancalendar.calculator.parser.GrammarParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import kotlin.math.*

private fun unaryFunction(action: (Double) -> Double): Value.Function {
    return Value.Function({ Value.Number(action((it[0] as Value.Number).value)) }, 1)
}

private fun binaryFunction(action: (Double, Double) -> Double): Value.Function {
    return Value.Function({
        Value.Number(action((it[0] as Value.Number).value, (it[0] as Value.Number).value))
    }, 2)
}

private val constants = mapOf(
    "PI" to Value.Number(PI), "E" to Value.Number(E),
    "sin" to unaryFunction(::sin), "cos" to unaryFunction(::cos), "tan" to unaryFunction(::tan),
    "asin" to unaryFunction(::asin), "acos" to unaryFunction(::acos),
    "atan" to unaryFunction(::atan), "atan2" to binaryFunction(::atan2),
    "sinh" to unaryFunction(::sinh), "cosh" to unaryFunction(::cosh),
    "tanh" to unaryFunction(::tanh), "asinh" to unaryFunction(::asinh),
    "acosh" to unaryFunction(::acosh), "asinh" to unaryFunction(::atanh),
    "hypot" to binaryFunction(::hypot), "sqrt" to unaryFunction(::sqrt),
    "exp" to unaryFunction(::exp), "log" to binaryFunction(::log), "ln" to unaryFunction(::ln),
    "ceil" to unaryFunction(::ceil), "floor" to unaryFunction(::floor),
    "truncate" to unaryFunction(::truncate), "round" to unaryFunction(::round),
    "abs" to unaryFunction(::abs), "sign" to unaryFunction(::sign),
    "min" to binaryFunction(::min), "max" to binaryFunction(::max),
)

fun eval(input: String): String {
    val lexer = GrammarLexer(CharStreams.fromString(input))
    val tokens = CommonTokenStream(lexer)
    val parser = GrammarParser(tokens)
    val eval = GrammarVisitor(constants)
    return (eval.visit(parser.program()) as Value.Tuple).values.joinToString("\n")
}
