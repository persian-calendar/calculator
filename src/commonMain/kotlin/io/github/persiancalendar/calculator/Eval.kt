package io.github.persiancalendar.calculator

import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.acosh
import kotlin.math.asin
import kotlin.math.asinh
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.atanh
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.expm1
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.ln1p
import kotlin.math.log
import kotlin.math.log10
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh
import kotlin.math.truncate
import kotlin.math.withSign

private fun degOrRadFunction(
    name: String,
    action: (Double) -> Double
): Pair<String, Value.Function> {
    return name to Value.Function({
        when (val value = it[0]) {
            is Value.Number -> Value.Number(
                action(
                    when (value.unit) {
                        "deg" -> value.value * PI / 180
                        null -> value.value
                        else -> error("Only degree is acceptable as a unit")
                    }
                )
            )
            else -> Value.Expression(Value.Symbol(name), it)
        }
    }, 1)
}

private fun unaryFunction(
    name: String,
    action: (Double) -> Double
): Pair<String, Value.Function> {
    return name to Value.Function({
        when (val value = it[0]) {
            is Value.Number -> Value.Number(
                action(
                    when (value.unit) {
                        null -> value.value
                        else -> error("This unary functions don't accept number with unit.")
                    }
                )
            )
            else -> Value.Expression(Value.Symbol(name), it)
        }
    }, 1)
}

internal enum class OperatorPrecedence(private vararg val values: String) {
    NotAnOperator, Additive("+", "-"), Multiplicative("*", "/", "%"), Exponential("^", "**");
    infix fun proceeds(other: OperatorPrecedence) = this.ordinal > other.ordinal
    val next get() = entries.getOrNull(ordinal + 1)
    val isRightAssociative get() = this == Exponential // Whether an infix operator groups right-to-left.
    companion object {
        fun match(name: String) = entries.firstOrNull { name in it.values } ?: NotAnOperator
    }
}

private fun binaryFunction(
    name: String,
    action: (Double, Double) -> Double,
): Pair<String, Value.Function> = name to Value.Function({ args ->
    val (x, y) = args
    if (x is Value.Number && y is Value.Number && x.unit == null && y.unit == null) {
        Value.Number(action(x.value, y.value))
    } else Value.Expression(Value.Symbol(name), args)
}, 2)

private val constants = mapOf(
    "PI" to Value.Number(PI),
    "E" to Value.Number(E),
    degOrRadFunction("sin", ::sin),
    degOrRadFunction("cos", ::cos),
    degOrRadFunction("tan", ::tan),
    degOrRadFunction("cot") { 1 / tan(it) },
    unaryFunction("asin", ::asin),
    unaryFunction("acos", ::acos),
    unaryFunction("atan", ::atan),
    binaryFunction("atan2", ::atan2),
    unaryFunction("sinh", ::sinh),
    unaryFunction("cosh", ::cosh),
    unaryFunction("tanh", ::tanh),
    unaryFunction("asinh", ::asinh),
    unaryFunction("acosh", ::acosh),
    unaryFunction("atanh", ::atanh),
    binaryFunction("hypot", ::hypot),
    unaryFunction("sqrt", ::sqrt),
    unaryFunction("exp", ::exp),
    binaryFunction("log", ::log),
    unaryFunction("ln", ::ln),
    unaryFunction("ceil", ::ceil),
    unaryFunction("floor", ::floor),
    unaryFunction("truncate", ::truncate),
    unaryFunction("round", ::round),
    unaryFunction("abs", ::abs),
    unaryFunction("sign", ::sign),
    binaryFunction("min", ::min),
    binaryFunction("max", ::max),
    binaryFunction("+") { x, y -> x + y },
    binaryFunction("-") { x, y -> x - y },
    binaryFunction("*") { x, y -> x * y },
    binaryFunction("/") { x, y -> x / y },
    binaryFunction("%") { x, y -> x % y },
    binaryFunction("^") { x, y -> x.pow(y) },
    binaryFunction("**") { x, y -> x.pow(y) },
    unaryFunction("cbrt", ::cbrt),
    unaryFunction("expm1", ::expm1),
    unaryFunction("ln1p", ::ln1p),
    unaryFunction("log10", ::log10),
    unaryFunction("log2", ::log2),
    binaryFunction("withSign") { x, y -> x.withSign(y) },
    "diff" to Value.Function({ diff(it[0], it[1] as Value.Symbol) }, 2),
    "integrate" to Value.Function({ integrate(it[0], it[1] as Value.Symbol) }, 2),
)

private sealed interface Token {
    object Minus : Token
    object Plus : Token
    object Pow : Token
    object Div : Token
    object Mod : Token
    object Mul : Token
    object LParen : Token
    object RParen : Token
    object Comma : Token
    object Equals : Token
    object Eof : Token
    object Separator : Token // "\n", "\r", ";"
    class Number(val value: Double) : Token
    class Symbol(val text: String) : Token

    val precedence: OperatorPrecedence?
        get() = when (this) {
            Minus, Plus -> OperatorPrecedence.Additive
            Div, Mul, Mod -> OperatorPrecedence.Multiplicative
            Pow -> OperatorPrecedence.Exponential
            else -> null
        }
}

private fun tokenize(input: String): List<Token> = buildList {
    var i = 0
    val n = input.length
    while (i < n) {
        val c = input[i]
        when {
            c == ' ' || c == '\t' -> i++

            c == '\r' -> {
                i++
                if (i < n && input[i] == '\n') i++
                add(Token.Separator)
            }

            c == '\n' || c == ';' -> {
                add(Token.Separator)
                i++
            }

            c == '#' -> {
                while (i < n && input[i] != '\n' && input[i] != '\r') i++
            }

            c == '/' && i + 1 < n && input[i + 1] == '/' -> {
                i += 2
                while (i < n && input[i] != '\n' && input[i] != '\r') i++
            }

            c == '*' && i + 1 < n && input[i + 1] == '*' -> {
                add(Token.Pow)
                i += 2
            }

            c == '^' -> {
                add(Token.Pow)
                i++
            }

            c == '+' -> {
                add(Token.Plus)
                i++
            }

            c == '-' -> {
                add(Token.Minus)
                i++
            }

            c == '/' -> {
                add(Token.Div)
                i++
            }

            c == '%' -> {
                add(Token.Mod)
                i++
            }

            c == '*' -> {
                add(Token.Mul)
                i++
            }

            c == '(' -> {
                add(Token.LParen)
                i++
            }

            c == ')' -> {
                add(Token.RParen)
                i++
            }

            c == ',' -> {
                add(Token.Comma)
                i++
            }

            c == '=' -> {
                add(Token.Equals)
                i++
            }

            c.isDigit() -> {
                val start = i
                if (c == '0') {
                    i++
                } else {
                    while (i < n && input[i].isDigit()) i++
                }
                if (i < n && input[i] == '.' && i + 1 < n && input[i + 1].isDigit()) {
                    i++
                    while (i < n && input[i].isDigit()) i++
                }
                if (i < n && (input[i] == 'e' || input[i] == 'E')) {
                    val save = i
                    i++
                    if (i < n && (input[i] == '+' || input[i] == '-')) i++
                    if (i < n && input[i].isDigit()) {
                        while (i < n && input[i].isDigit()) i++
                    } else {
                        i = save
                    }
                }
                add(Token.Number(input.substring(start, i).toDouble()))
            }

            c.isLetter() || c == '_' -> {
                val start = i
                while (i < n && (input[i].isLetterOrDigit() || input[i] == '_')) i++
                add(Token.Symbol(input.substring(start, i)))
            }

            else -> error("Unexpected character '$c' at $i")
        }
    }
    add(Token.Eof)
}

internal class Evaluator(input: String, private val symbolic: Boolean = false) {
    private val clearFunction: Value.Function = Value.Function({
        registry = defaultValues()
        Value.Null
    }, 0)

    private fun defaultValues() = (constants + ("clear" to clearFunction)).toMutableMap()
    private var registry = defaultValues()

    private val tokens: List<Token> = tokenize(input)
    private var position = 0

    operator fun invoke(): List<Value> {
        position = 0
        return buildList {
            skipSeparators()
            while (lookahead() != Token.Eof) {
                val value = parseStatement()
                if (value != null && value !is Value.Null) add(value)
                when (lookahead()) {
                    Token.Separator -> skipSeparators()
                    Token.Eof -> Unit
                    else -> error("Unexpected token '${lookahead()}'")
                }
            }
        }
    }

    private fun lookahead(offset: Int = 0): Token =
        tokens.getOrElse(position + offset) { Token.Eof }

    private fun expect(type: Token) {
        val token = lookahead()
        check(token == type) { "Expected $type but found $token" }
        position++
    }

    private fun expectNumber(): Double {
        val token = lookahead()
        if (token is Token.Number) {
            position++
            return token.value
        } else error("Expected number but found $token")
    }

    private fun expectSymbol(): String {
        val token = lookahead()
        if (token is Token.Symbol) {
            position++
            return token.text
        } else error("Expected symbol but found $token")
    }

    private fun skipSeparators() {
        while (lookahead() == Token.Separator) position++
    }

    private fun parseStatement(): Value? {
        if (lookahead() is Token.Symbol && lookahead(1) == Token.Equals) {
            val name = expectSymbol()
            expect(Token.Equals)
            registry[name] = parseExpression()
            return null
        }
        var value = parseExpression()
        if (value is Value.Function && value.inputCount == 0) value = value(emptyList())
        return value
    }

    private fun parseExpression(): Value = parseBinary(OperatorPrecedence.NotAnOperator)

    private fun parseBinary(minPrecedence: OperatorPrecedence): Value {
        var left = parseSignedAtom()
        while (true) {
            val token = lookahead()
            val precedence = token.precedence ?: break
            if (minPrecedence proceeds precedence) break
            position++
            val nextMinPrecedence = if (precedence.isRightAssociative) {
                precedence
            } else precedence.next ?: break
            val right = parseBinary(nextMinPrecedence)
            left = applyBinary(token, left, right)
        }
        return left
    }

    private fun parseSignedAtom(): Value = when (lookahead()) {
        Token.Minus -> {
            position++
            Value.Number(-1.0) * parseSignedAtom()
        }

        Token.Plus -> {
            position++
            parseSignedAtom()
        }

        else -> parseCall()
    }

    private fun parseCall(): Value {
        val atoms = buildList {
            add(parseAtom())
            while (when (lookahead()) {
                    is Token.Number -> true
                    is Token.Symbol -> true
                    Token.LParen -> true
                    else -> false
                }
            ) add(parseAtom())
        }
        return combineCall(atoms)
    }

    private fun parseAtom(): Value {
        return when (lookahead()) {
            is Token.Number -> Value.Number(expectNumber())
            is Token.Symbol -> {
                val name = expectSymbol()
                val entry = registry[name]
                if (entry is Value.Function && symbolic) {
                    Value.Symbol(name) // keep as symbol, evaluate nothing
                } else entry ?: Value.Symbol(name)
            }

            Token.LParen -> {
                expect(Token.LParen)
                if (lookahead() == Token.RParen) {
                    expect(Token.RParen)
                    return Value.Tuple(emptyList())
                }
                val expressions = mutableListOf(parseExpression())
                while (lookahead() == Token.Comma) {
                    expect(Token.Comma)
                    expressions += parseExpression()
                }
                expect(Token.RParen)
                if (expressions.size == 1) expressions[0] else Value.Tuple(expressions)
            }

            else -> error("Unexpected token '${lookahead()}'")
        }
    }

    private fun combineCall(atoms: List<Value>): Value {
        if (atoms.size % 2 == 0 && atoms.withIndex().all {
                if (it.index % 2 == 0) it.value is Value.Number else it.value is Value.Symbol
            })
            return atoms.chunked(2)
                .map { (x, y) -> (x as Value.Number) withUnit (y as Value.Symbol) }
                .reduce { acc, number -> (acc + number) as Value.Number }
        return atoms.reduceRight { x, y ->
            when (x) {
                is Value.Function -> x(if (y is Value.Tuple) y.values else listOf(y))
                is Value.Number ->
                    if (y is Value.Symbol) x withUnit y else error("Not supported number call")
                else -> error("Not supported call")
            }
        }
    }

    private fun applyBinary(token: Token, left: Value, right: Value): Value = when (token) {
        Token.Plus -> left + right
        Token.Minus -> left - right
        Token.Mul -> left * right
        Token.Div -> left / right
        Token.Mod -> left % right
        Token.Pow -> left.pow(right)
        else -> error("Unexpected operator $token")
    }
}

fun eval(input: String): String {
    val result = Evaluator(input)()
    return when (result.size) {
        1 if result[0] is Value.Number -> (result[0] as Value.Number).detailedFormat()
        1 -> result[0].renderAsArgument()
        else -> result.joinToString("\n")
    }
}
