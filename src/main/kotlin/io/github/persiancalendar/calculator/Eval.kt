package io.github.persiancalendar.calculator

import io.github.persiancalendar.calculator.parser.GrammarLexer
import io.github.persiancalendar.calculator.parser.GrammarParser
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.atn.ATNConfigSet
import org.antlr.v4.runtime.dfa.DFA
import java.util.*
import kotlin.math.*

private fun degOrRadFunction(action: (Double) -> Double): Value.Function {
    return Value.Function({
        val number = when (val value = it[0]) {
            is Value.Number -> when (value.unit) {
                "deg" -> Math.toRadians(value.value)
                null -> value.value
                else -> error("Only degree is acceptable is a degree")
            }
            else -> error("Unknown input for deg or rad function")
        }
        Value.Number(action(number))
    }, 1)
}

private fun unaryFunction(action: (Double) -> Double): Value.Function {
    return Value.Function({
        val number = when (val value = it[0]) {
            is Value.Number -> when (value.unit) {
                null -> value.value
                else -> error("Unknown unit")
            }
            else -> error("Unknown input for unary function")
        }
        Value.Number(action(number))
    }, 1)
}

private fun binaryFunction(action: (Double, Double) -> Double): Value.Function {
    return Value.Function({
        Value.Number(action((it[0] as Value.Number).value, (it[0] as Value.Number).value))
    }, 2)
}

private val constants = mapOf(
    "PI" to Value.Number(PI), "E" to Value.Number(E),
    "sin" to degOrRadFunction(::sin), "cos" to degOrRadFunction(::cos),
    "tan" to degOrRadFunction(::tan), "cot" to degOrRadFunction { 1 / tan(it) },
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

private val listener = object : ANTLRErrorListener {
    override fun syntaxError(
        recognizer: Recognizer<*, *>,
        offendingSymbol: Any?,
        line: Int,
        charPositionInLine: Int,
        msg: String,
        e: RecognitionException
    ) = error("$msg $offendingSymbol")

    override fun reportAmbiguity(
        recognizer: Parser?,
        dfa: DFA?,
        startIndex: Int,
        stopIndex: Int,
        exact: Boolean,
        ambigAlts: BitSet?,
        configs: ATNConfigSet?
    ) = print("an ambiguity spotted")

    override fun reportAttemptingFullContext(
        recognizer: Parser?,
        dfa: DFA?,
        startIndex: Int,
        stopIndex: Int,
        conflictingAlts: BitSet?,
        configs: ATNConfigSet?
    ) = print("an full context attempt happened")

    override fun reportContextSensitivity(
        recognizer: Parser?,
        dfa: DFA?,
        startIndex: Int,
        stopIndex: Int,
        prediction: Int,
        configs: ATNConfigSet?
    ) = error("an full context sensitivity happened")
}

fun eval(input: String): String {
    val lexer = GrammarLexer(CharStreams.fromString(input))
    lexer.addErrorListener(listener)
    val tokens = CommonTokenStream(lexer)
    val parser = GrammarParser(tokens)
    parser.addErrorListener(listener)
    val eval = GrammarVisitor(constants)
    val result = (eval.visit(parser.program()) as Value.Tuple).values
    return if (result.size == 1 && result[0] is Value.Number)
        (result[0] as Value.Number).detailedFormat()
    else result.joinToString("\n")
}
