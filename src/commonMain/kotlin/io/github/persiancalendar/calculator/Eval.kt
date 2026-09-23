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
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.log
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

private fun binaryFunction(action: (Double, Double) -> Double): Value.Function {
    return Value.Function({
        val (x, y) = it
        if ((x as Value.Number).unit != null || (y as Value.Number).unit != null)
            error("this binary function only accepts numbers without unit")
        Value.Number(action(x.value, y.value))
    }, 2)
}

private val constants = mapOf(
    "PI" to Value.Number(PI), "E" to Value.Number(E),
    degOrRadFunction("sin", ::sin), degOrRadFunction("cos", ::cos),
    degOrRadFunction("tan", ::tan), degOrRadFunction("cot") { 1 / tan(it) },
    unaryFunction("asin", ::asin), unaryFunction("acos", ::acos),
    unaryFunction("atan", ::atan), "atan2" to binaryFunction(::atan2),
    unaryFunction("sinh", ::sinh), unaryFunction("cosh", ::cosh),
    unaryFunction("tanh", ::tanh), unaryFunction("asinh", ::asinh),
    unaryFunction("acosh", ::acosh), unaryFunction("asinh", ::atanh),
    "hypot" to binaryFunction(::hypot), unaryFunction("sqrt", ::sqrt),
    unaryFunction("exp", ::exp), "log" to binaryFunction(::log), unaryFunction("ln", ::ln),
    unaryFunction("ceil", ::ceil), unaryFunction("floor", ::floor),
    unaryFunction("truncate", ::truncate), unaryFunction("round", ::round),
    unaryFunction("abs", ::abs), unaryFunction("sign", ::sign),
    "min" to binaryFunction(::min), "max" to binaryFunction(::max),
    "+" to binaryFunction { x, y -> x + y }, "-" to binaryFunction { x, y -> x - y },
    "*" to binaryFunction { x, y -> x * y }, "/" to binaryFunction { x, y -> x / y },
    "%" to binaryFunction { x, y -> x % y },
    "^" to binaryFunction { x, y -> x.pow(y) }, "**" to binaryFunction { x, y -> x.pow(y) },
    "diff" to Value.Function({ diff(it[0], it[1] as Value.Symbol) }),
)

private enum class TokenType { NUMBER, SYMBOL, OP, LPAREN, RPAREN, COMMA, EQUALS, NEWLINE, EOF }

private data class Token(val type: TokenType, val text: String, val index: Int)

private fun tokenize(input: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    val n = input.length
    while (i < n) {
        val c = input[i]
        when {
            c == ' ' || c == '\t' -> i++
            c == '\n' -> {
                tokens += Token(TokenType.NEWLINE, "\n", i)
                i++
            }
            c == '\r' -> {
                i++
                if (i < n && input[i] == '\n') i++
                tokens += Token(TokenType.NEWLINE, "\n", i)
            }
            c == ';' -> {
                tokens += Token(TokenType.NEWLINE, ";", i)
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
                tokens += Token(TokenType.OP, "**", i)
                i += 2
            }
            c == '+' || c == '-' || c == '*' || c == '/' || c == '%' || c == '^' -> {
                tokens += Token(TokenType.OP, c.toString(), i)
                i++
            }
            c == '(' -> {
                tokens += Token(TokenType.LPAREN, "(", i)
                i++
            }
            c == ')' -> {
                tokens += Token(TokenType.RPAREN, ")", i)
                i++
            }
            c == ',' -> {
                tokens += Token(TokenType.COMMA, ",", i)
                i++
            }
            c == '=' -> {
                tokens += Token(TokenType.EQUALS, "=", i)
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
                tokens += Token(TokenType.NUMBER, input.substring(start, i), start)
            }
            c.isLetter() || c == '_' -> {
                val start = i
                while (i < n && (input[i].isLetter() || input[i] == '_')) i++
                tokens += Token(TokenType.SYMBOL, input.substring(start, i), start)
            }
            else -> error("Unexpected character '$c' at $i")
        }
    }
    tokens += Token(TokenType.EOF, "", n)
    return tokens
}

private class Evaluator(input: String) {
    private val clearFunction: Value.Function = Value.Function({
        registry = defaultValues()
        Value.Null
    }, 0)

    private fun defaultValues() = (constants + ("clear" to clearFunction)).toMutableMap()
    private var registry = defaultValues()

    private val tokens: List<Token> = tokenize(input)
    private var position = 0
    private val eof = Token(TokenType.EOF, "", -1)

    operator fun invoke(): List<Value> {
        position = 0
        val results = mutableListOf<Value>()
        skipNewlines()
        while (peek().type != TokenType.EOF) {
            val value = parseStatement()
            if (value != null && value !is Value.Null) results.add(value)
            when (peek().type) {
                TokenType.NEWLINE -> skipNewlines()
                TokenType.EOF -> Unit
                else -> error("Unexpected token '${peek().text}'")
            }
        }
        return results
    }

    private fun peek(): Token = tokens.getOrElse(position) { eof }

    private fun lookahead(offset: Int): Token = tokens.getOrElse(position + offset) { eof }

    private fun consume(type: TokenType): Token {
        val token = peek()
        check(token.type == type) { "Expected $type but found ${token.text}" }
        position++
        return token
    }

    private fun skipNewlines() {
        while (peek().type == TokenType.NEWLINE) position++
    }

    private fun parseStatement(): Value? {
        if (peek().type == TokenType.SYMBOL && lookahead(1).type == TokenType.EQUALS) {
            val name = consume(TokenType.SYMBOL).text
            consume(TokenType.EQUALS)
            registry[name] = parseExpression()
            return null
        }
        var value = parseExpression()
        if (value is Value.Function && value.inputCount == 0) value = value(emptyList())
        return value
    }

    private fun parseExpression(): Value = parseAdditive()

    private fun parseAdditive(): Value {
        var left = parseMultiplicative()
        while (peek().type == TokenType.OP && peek().text in setOf("+", "-")) {
            val op = peek().text
            position++
            left = applyBinary(op, left, parseMultiplicative())
        }
        return left
    }

    private fun parseMultiplicative(): Value {
        var left = parseExponential()
        while (peek().type == TokenType.OP && peek().text in setOf("*", "/", "%")) {
            val op = peek().text
            position++
            left = applyBinary(op, left, parseExponential())
        }
        return left
    }

    private fun parseExponential(): Value {
        val operands = mutableListOf(parseSignedAtom())
        while (peek().type == TokenType.OP && peek().text in setOf("^", "**")) {
            position++
            operands += parseSignedAtom()
        }
        return operands.reduceRight { x, y -> x.pow(y) }
    }

    private fun parseSignedAtom(): Value {
        return when {
            peek().type == TokenType.OP && peek().text == "-" -> {
                position++
                Value.Number(-1.0) * parseSignedAtom()
            }
            peek().type == TokenType.OP && peek().text == "+" -> {
                position++
                parseSignedAtom()
            }
            else -> parseCall()
        }
    }

    private fun parseCall(): Value {
        val atoms = mutableListOf(parseAtom())
        while (peek().type == TokenType.NUMBER ||
            peek().type == TokenType.SYMBOL ||
            peek().type == TokenType.LPAREN
        ) atoms += parseAtom()
        return combineCall(atoms)
    }

    private fun parseAtom(): Value {
        return when (peek().type) {
            TokenType.NUMBER -> {
                val token = consume(TokenType.NUMBER)
                Value.Number(token.text.toDouble())
            }
            TokenType.SYMBOL -> {
                val name = consume(TokenType.SYMBOL).text
                registry[name] ?: Value.Symbol(name)
            }
            TokenType.LPAREN -> {
                consume(TokenType.LPAREN)
                if (peek().type == TokenType.RPAREN) {
                    consume(TokenType.RPAREN)
                    return Value.Tuple(emptyList())
                }
                val expressions = mutableListOf(parseExpression())
                while (peek().type == TokenType.COMMA) {
                    consume(TokenType.COMMA)
                    expressions += parseExpression()
                }
                consume(TokenType.RPAREN)
                if (expressions.size == 1) expressions[0] else Value.Tuple(expressions)
            }
            else -> error("Unexpected token '${peek().text}'")
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

    private fun applyBinary(op: String, left: Value, right: Value): Value = when (op) {
        "+" -> left + right
        "-" -> left - right
        "*" -> left * right
        "/" -> left / right
        "%" -> left % right
        "**", "^" -> left.pow(right)
        else -> error("Unexpected operator $op")
    }
}

fun eval(input: String): String {
    val result = Evaluator(input)()
    return if (result.size == 1 && result[0] is Value.Number)
        (result[0] as Value.Number).detailedFormat()
    else result.joinToString("\n")
}
