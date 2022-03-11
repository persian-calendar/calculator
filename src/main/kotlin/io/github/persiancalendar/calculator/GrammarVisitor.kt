package io.github.persiancalendar.calculator

import io.github.persiancalendar.calculator.parser.GrammarBaseVisitor
import io.github.persiancalendar.calculator.parser.GrammarParser
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.pow

class GrammarVisitor : GrammarBaseVisitor<Double>() {
    private var registry = mutableMapOf("PI" to PI, "E" to E)

    private val _result = mutableListOf<String>()
    val result: List<String> = _result

    override fun visitAssign(ctx: GrammarParser.AssignContext): Double {
        registry[ctx.SYMBOL().text] = visit(ctx.expression())
        return .0
    }

    override fun visitPrintExpression(ctx: GrammarParser.PrintExpressionContext): Double {
        _result += visit(ctx.expression()).toString()
        return .0
    }

    override fun visitNumber(ctx: GrammarParser.NumberContext): Double {
        return ctx.NUMBER().text.toDouble()
    }

    override fun visitSymbol(ctx: GrammarParser.SymbolContext): Double {
        val symbol = ctx.SYMBOL().text
        return registry[symbol] ?: error("Symbol $symbol is unrecognized")
    }

    override fun visitExponentialExpression(
        ctx: GrammarParser.ExponentialExpressionContext
    ): Double {
        return ctx.atom().map(::visit).reduceRight { x, y -> x.pow(y) }
    }

    override fun visitMultiplicativeExpression(
        ctx: GrammarParser.MultiplicativeExpressionContext
    ): Double {
        return ctx.exponentialExpression().map(::visit).reduceIndexed { i, x, y ->
            when (ctx.getChild((i - 1) * 2 + 1).text) {
                "*" -> x * y
                "/" -> x / y
                "%" -> x % y
                else -> error("Unexpected operator")
            }
        }
    }

    override fun visitAdditiveExpression(ctx: GrammarParser.AdditiveExpressionContext): Double {
        return ctx.multiplicativeExpression().map(::visit).reduceIndexed { i, x, y ->
            when (ctx.getChild((i - 1) * 2 + 1).text) {
                "+" -> x + y
                "-" -> x - y
                else -> error("Unexpected operator")
            }
        }
    }

    override fun visitParenthesizedExpression(
        ctx: GrammarParser.ParenthesizedExpressionContext
    ): Double {
        return visit(ctx.expression())
    }
}
