package io.github.persiancalendar.calculator

import io.github.persiancalendar.calculator.parser.GrammarBaseVisitor
import io.github.persiancalendar.calculator.parser.GrammarParser
import kotlin.math.E
import kotlin.math.PI

class GrammarVisitor : GrammarBaseVisitor<Value>() {
    private var registry = mutableMapOf<String, Value>(
        "PI" to Value.Number(PI),
        "E" to Value.Number(E)
    )

    private val _result = mutableListOf<String>()
    val result: List<String> = _result

    override fun visitAssign(ctx: GrammarParser.AssignContext): Value {
        registry[ctx.SYMBOL().text] = visit(ctx.expression())
        return Value.Null
    }

    override fun visitPrintExpression(ctx: GrammarParser.PrintExpressionContext): Value {
        _result += visit(ctx.expression()).toString()
        return Value.Null
    }

    override fun visitNumber(ctx: GrammarParser.NumberContext): Value {
        return Value.Number(ctx.NUMBER().text.toDouble())
    }

    override fun visitSymbol(ctx: GrammarParser.SymbolContext): Value {
        val symbol = ctx.SYMBOL().text
        return registry[symbol] ?: error("Symbol $symbol is unrecognized")
    }

    override fun visitExponentialExpression(
        ctx: GrammarParser.ExponentialExpressionContext
    ): Value {
        return ctx.atom().map(::visit).reduceRight { x, y ->
            x as Value.Number; y as Value.Number
            x.pow(y)
        }
    }

    override fun visitMultiplicativeExpression(
        ctx: GrammarParser.MultiplicativeExpressionContext
    ): Value {
        return ctx.exponentialExpression().map(::visit).reduceIndexed { i, x, y ->
            when (ctx.getChild((i - 1) * 2 + 1).text) {
                "*" -> x * y
                "/" -> x / y
                "%" -> x % y
                else -> error("Unexpected operator")
            }
        }
    }

    override fun visitAdditiveExpression(ctx: GrammarParser.AdditiveExpressionContext): Value {
        return ctx.multiplicativeExpression().map(::visit).reduceIndexed { i, x, y ->
            x as Value.Number; y as Value.Number
            when (ctx.getChild((i - 1) * 2 + 1).text) {
                "+" -> x + y
                "-" -> x - y
                else -> error("Unexpected operator")
            }
        }
    }

    override fun visitParenthesizedExpression(
        ctx: GrammarParser.ParenthesizedExpressionContext
    ): Value {
        return visit(ctx.expression())
    }
}
