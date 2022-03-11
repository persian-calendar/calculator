package io.github.persiancalendar.calculator

import io.github.persiancalendar.calculator.parser.GrammarLexer
import io.github.persiancalendar.calculator.parser.GrammarParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream

fun eval(input: String): String {
    val lexer = GrammarLexer(CharStreams.fromString(input))
    val tokens = CommonTokenStream(lexer)
    val parser = GrammarParser(tokens)
    val eval = GrammarVisitor()
    eval.visit(parser.program())
    return eval.result.joinToString("\n")
}
