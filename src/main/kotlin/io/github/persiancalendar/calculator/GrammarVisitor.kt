package io.github.persiancalendar.calculator

import io.github.persiancalendar.calculator.parser.GrammarBaseVisitor
import io.github.persiancalendar.calculator.parser.GrammarParser

class GrammarVisitor(private val defaultValues: Map<String, Value>) : GrammarBaseVisitor<Value>() {
    private var registry = defaultValues.toMutableMap()

    private val _result = mutableListOf<String>()
    val result: List<String> = _result

    override fun visitClear(ctx: GrammarParser.ClearContext?): Value {
        registry = defaultValues.toMutableMap()
        return Value.Null
    }

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
        return registry[symbol] ?: Value.Symbol(symbol)
    }

    override fun visitExponentialExpression(
        ctx: GrammarParser.ExponentialExpressionContext
    ): Value {
        return ctx.call().map(::visit).reduceRight { x, y ->
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
        val tuple = ctx.expression().map(::visit)
        return if (tuple.size == 1) tuple[0] else Value.Tuple(tuple)
    }

    override fun visitCall(ctx: GrammarParser.CallContext): Value {
        return ctx.atom().map(::visit).reduceRight { x, y -> // This should be a left reduce prolly
            when (x) {
                is Value.Function -> x(if (y is Value.Tuple) y.values else listOf(y))
                is Value.Number ->
                    if (y is Value.Symbol) x.withUnit(y) else error("Not supported number call")
                else -> error("Not supported call")
            }
        }
    }
}
