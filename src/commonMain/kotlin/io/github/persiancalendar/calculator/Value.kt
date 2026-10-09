package io.github.persiancalendar.calculator

import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.truncate
import kotlin.reflect.KProperty

internal sealed interface Value {
    object Null : Value

    data class Symbol(val name: String) : Value {
        override fun toString(): String = name

        companion object {
            operator fun getValue(thisRef: Any?, property: KProperty<*>) = Symbol(property.name)
        }
    }

    data class Number(val value: Double, val unit: String? = null) : Value {
        private fun formatNumber(value: Double): String {
            return when (value) {
                Double.POSITIVE_INFINITY -> "Infinity"
                Double.NEGATIVE_INFINITY -> "-Infinity"
                Double.NaN -> "NaN"
                truncate(value) -> value.toInt().toString()
                else -> value.toString()
            }
        }

        infix fun withUnit(unit: Symbol): Number {
            if (this.unit != null) error("Trying to add unit to a number already with unit")
            return Number(value, unit.name)
        }

        override fun toString(): String = listOfNotNull(formatNumber(value), unit).joinToString(" ")

        fun detailedFormat(): String {
            if (unit != "s") return toString()
            return timeUnits.toList().fold("" to value) { (result, reminder), unit ->
                result + floor(reminder / unit.second).toInt() + unit.first + " " to
                        reminder % unit.second
            }.first.trim() + "\n" + timeUnits.map { "${formatNumber(value / it.value)} ${it.key}" }
                .joinToString("\n")
        }
    }

    data class Function(
        private val body: (arguments: List<Value>) -> Value, val inputCount: Int? = null
    ) : Value {
        operator fun invoke(arguments: List<Value>): Value {
            if (inputCount != null && arguments.size != inputCount)
                error("Invalid number of inputs")
            return body(arguments)
        }
    }

    data class Tuple(val values: List<Value>) : Value {
        override fun toString(): String =
            "(${values.joinToString(", ", transform = Value::toString)})"
    }

    data class Expression(val function: Symbol, val arguments: List<Value>) : Value {
        // Null if isn't an operator
        private val thisOperatorPrecedence = OperatorPrecedence.match(function.name)

        override fun toString(): String = render(atRoot = false)

        fun render(atRoot: Boolean): String {
            val name = function.name
            if (thisOperatorPrecedence == OperatorPrecedence.NotAnOperator) {
                return "$name(${arguments.joinToString(", ") { it.renderAsArgument() }})"
            }
            // "-1 * x" -> "-x"
            // "-1 * (a * b)" -> "-a * b"
            // "-1 * (a / b)" -> "-a / b"
            // "-1 * (a + b)" -> "-(a + b)"
            // "-1 * (a ^ b)" -> "-(a ^ b)"
            if (name == "*" && arguments.size >= 2 && arguments[0] == Number(-1.0)) {
                val rest = arguments.drop(1)
                val body = rest.joinToString(" * ") { arg ->
                    when (arg) {
                        is Expression if arg.thisOperatorPrecedence != OperatorPrecedence.NotAnOperator && arg.function.name != "*" && arg.function.name != "/" ->
                            "(${arg.renderAsArgument()})"

                        is Expression -> arg.renderAsArgument()
                        else -> arg.toString()
                    }
                }
                return "-$body"
            }
            val body = when (arguments.size) {
                0 -> "0"
                1 -> if (name == "-") "(-${arguments[0].renderAsArgument()})"
                else arguments[0].renderAsArgument()

                else -> {
                    val rightAssociative = OperatorPrecedence.match(name).isRightAssociative
                    arguments.mapIndexed { i, arg ->
                        when (arg) {
                            is Expression -> arg.renderAsOperand(
                                parentOperatorPrecedence = thisOperatorPrecedence,
                                parentRightAssociative = rightAssociative,
                                isFirst = i == 0,
                                isLast = i == arguments.lastIndex,
                            )

                            else -> arg.toString()
                        }
                    }.joinToString(" $name ")
                }
            }
            return if (atRoot) body else "($body)"
        }

        fun renderAsOperand(
            parentOperatorPrecedence: OperatorPrecedence,
            parentRightAssociative: Boolean,
            isFirst: Boolean,
            isLast: Boolean,
        ): String {
            if (thisOperatorPrecedence == OperatorPrecedence.NotAnOperator) return render(atRoot = true)
            val needsParens =
                parentOperatorPrecedence proceeds thisOperatorPrecedence || (thisOperatorPrecedence == parentOperatorPrecedence && ((isFirst && parentRightAssociative) || (isLast && !parentRightAssociative)))
            val body = render(atRoot = true)
            return if (needsParens) "($body)" else body
        }
    }

    fun renderAsArgument(): String = if (this is Expression) render(atRoot = true) else toString()

    operator fun plus(other: Value): Value {
        // "1 + 2" -> "3"
        if (this is Number && other is Number) {
            if (unit == other.unit) return Number(value + other.value, unit)
            val thisSecondFactor = timeUnits[unit]
            val otherSecondFactor = timeUnits[other.unit]
            if (thisSecondFactor == null || otherSecondFactor == null)
                error("This addition of units isn't supported")
            return Number(value * thisSecondFactor + other.value * otherSecondFactor, "s")
        }
        // "0 + x" -> "x"
        if (this.isZero()) return other
        // "x + 0" -> "x"
        if (other.isZero()) return this
        // "2*x + 3*x" -> "5*x"
        run {
            val (c1, t1) = coefficientTail()
            val (c2, t2) = other.coefficientTail()
            if (t1 == t2 && t1.isNotEmpty()) {
                val c = c1.value + c2.value
                if (c == 0.0) return Number(0.0)
                return t1.fold(Number(c), Value::times)
            }
        }
        // "a + (-b)" -> "a - b"
        if (other is Number && other.value < 0.0 && other.unit == null) {
            return Symbol("-")(this, Number(-other.value))
        }
        return Symbol("+")(this, other)
    }

    operator fun minus(other: Value): Value {
        // "x - 0" -> "x"
        if (other.isZero()) return this
        // "x - x" -> "0"
        if (this == other) return Number(0.0)
        // "5*x - 3*x" -> "2*x"
        run {
            val (c1, t1) = coefficientTail()
            val (c2, t2) = other.coefficientTail()
            if (t1 == t2 && t1.isNotEmpty()) {
                val c = c1.value - c2.value
                if (c == 0.0) return Number(0.0)
                return t1.fold(Number(c), Value::times)
            }
        }
        // "a - (-b)" -> "a + b"
        if (other is Number && other.value < 0.0 && other.unit == null) return this + Number(-other.value)
        // "-x" -> "-1 * x"
        if (this !is Number || other !is Number) return Symbol("-")(this, other)
        return this + Number(-1.0) * other
    }

    operator fun unaryMinus(): Value = Number(-1.0) * this

    /**
     * Splits the value into (coefficient, remaining factors) when it looks like
     * `Number * factor * factor * ...`. `Number` alone yields the number and
     * an empty tail; anything else yields `null` and the whole value.
     */
    private fun splitCoefficient(): Pair<Number?, List<Value>> = when (this) {
        is Number -> this to emptyList()
        is Expression -> if (this.function.name == "*" && this.arguments.firstOrNull() is Number) {
            (this.arguments.first() as Number) to this.arguments.drop(1)
        } else null to listOf(this)

        else -> null to listOf(this)
    }

    /** Same as [splitCoefficient], but a missing coefficient is treated as `1`. */
    private fun coefficientTail(): Pair<Number, List<Value>> {
        val (c, t) = this.splitCoefficient()
        return (c ?: Number(1.0)) to t
    }

    operator fun times(other: Value): Value {
        // "2 * 3" -> "6"
        if (this is Number && other is Number) {
            if (unit != null && other.unit != null) error("Two numbers with unit are multiplied")
            return Number(value * other.value, unit ?: other.unit)
        }
        // "0 * x" -> "0"
        if (this.isZero() || other.isZero()) return Number(0.0)
        // "1 * x" -> "x"
        if (this.isOne()) return other
        // "x * 1" -> "x"
        if (other.isOne()) return this
        // "x * x" -> "x ^ 2"
        if (this == other) return this.pow(Number(2.0))
        // "x^m * x^n" -> "x ^ (m + n)"
        if (this is Expression && this.function.name == "^" && other is Expression && other.function.name == "^" && this.arguments[0] == other.arguments[0]) {
            val e1 = this.arguments[1]
            val e2 = other.arguments[1]
            if (e1 is Number && e2 is Number) return this.arguments[0].pow(Number(e1.value + e2.value))
        }
        // "x * x^n" -> "x ^ (n + 1)"
        if (other is Expression && other.function.name == "^" && other.arguments[0] == this) {
            val e = other.arguments[1]
            if (e is Number) return this.pow(Number(e.value + 1))
        }
        // "x^n * x" -> "x ^ (n + 1)"
        if (this is Expression && this.function.name == "^" && this.arguments[0] == other) {
            val e = this.arguments[1]
            if (e is Number) return other.pow(Number(e.value + 1))
        }
        // "(2 * x) * (3 * y)" -> "6 * (x * y)"
        val (c1, r1) = this.splitCoefficient()
        val (c2, r2) = other.splitCoefficient()
        if (c1 != null && c2 != null) {
            val folded = c1 * c2
            val rest = r1 + r2
            return rest.fold(folded, Value::times)
        }
        // "x * (2 * y)" -> "2 * (x * y)"
        if (c1 == null && c2 != null && r2.size == 1) return c2 * (this * r2[0])
        // "(2 * x) * y" -> "2 * (x * y)"
        if (c2 == null && c1 != null && r1.size == 1) return c1 * (other * r1[0])
        // "c * (f / n)" -> "(c / n) * f" when n divides c
        if (this is Number && unit == null && other is Expression && other.function.name == "/" && other.arguments.size == 2 && other.arguments[1] is Number && (other.arguments[1] as Number).unit == null) {
            val n = (other.arguments[1] as Number).value
            if (n != 0.0 && value % n == 0.0) {
                return Number(value / n) * other.arguments[0]
            }
        }
        // "(-1 * a * b) * c" -> "-1 * a * b * c"
        if (c1 != null && r1.size > 1) return Expression(
            Symbol("*"), listOf(c1) + r1 + listOf(other)
        )
        // "f * (2 * g * h)" -> "2 * f * g * h"
        if (c2 != null && r2.size > 1) return Expression(
            Symbol("*"), listOf(c2, this) + r2
        )

        // "c * (f * g)" -> "c * f * g"
        // "(f * g) * c" -> "c * f * g"
        // "(f * g) * (h * i)" -> "f * g * h * i"
        val thisFlat = (this as? Expression)?.takeIf {
            it.function.name == "*" && it.arguments.firstOrNull() !is Number
        }?.arguments
        val otherFlat = (other as? Expression)?.takeIf {
            it.function.name == "*" && it.arguments.firstOrNull() !is Number
        }?.arguments
        if (thisFlat != null || otherFlat != null) {
            val combined = (thisFlat ?: listOf(this)) + (otherFlat ?: listOf(other))
            val (numbers, rest) = combined.partition { it is Number }
            return Expression(Symbol("*"), numbers + rest)
        }

        // "x * 2" -> "2 * x"
        if (other is Number && this !is Number) return Symbol("*")(other, this)
        return Symbol("*")(this, other)
    }

    private fun Value.isZero() =
        this is Number && value == 0.0 && unit == null

    private fun Value.isOne() =
        this is Number && value == 1.0 && unit == null

    operator fun div(other: Value): Value {
        // "1 / 2" -> "0.5"
        if (this is Number && other is Number) {
            val resultUnit = when {
                unit == other.unit -> null
                unit == null && other.unit != null -> "1/${other.unit}"
                unit != null && other.unit == null -> unit
                else -> "$unit/${other.unit}"
            }
            return Number(value / other.value, resultUnit)
        }
        // "x / x" -> "1"
        if (this == other) return Number(1.0)
        // "0 / x" -> "0"
        if (this.isZero()) return Number(0.0)
        // "x / 1" -> "x"
        if (other.isOne()) return this
        // "x^m / x^n" -> "x ^ (m - n)"
        if (this is Expression && this.function.name == "^" && other is Expression && other.function.name == "^" && this.arguments[0] == other.arguments[0]) {
            val e1 = this.arguments[1]
            val e2 = other.arguments[1]
            if (e1 is Number && e2 is Number) {
                val exp = e1.value - e2.value
                if (exp == 0.0) return Number(1.0)
                if (exp < 0.0) return Number(1.0) / this.arguments[0].pow(Number(-exp))
                return this.arguments[0].pow(Number(exp))
            }
        }
        // "x / x^n" -> "1 / x ^ (n - 1)"
        if (other is Expression && other.function.name == "^" && other.arguments[0] == this) {
            val e = other.arguments[1]
            if (e is Number) {
                val exp = 1 - e.value
                if (exp == 0.0) return Number(1.0)
                if (exp < 0.0) return Number(1.0) / this.pow(Number(-exp))
                return this.pow(Number(exp))
            }
        }
        // "x^n / x" -> "x ^ (n - 1)"
        if (this is Expression && this.function.name == "^" && this.arguments[0] == other) {
            val e = this.arguments[1]
            if (e is Number) {
                val exp = e.value - 1
                if (exp == 0.0) return Number(1.0)
                if (exp < 0.0) return Number(1.0) / other.pow(Number(-exp))
                return other.pow(Number(exp))
            }
        }
        // Coefficient extraction
        // "(c * f) / g" -> "c * (f / g)" when f/g changes shape,
        // otherwise fall through so we don't rebuild the input.
        val (c1, r1) = this.splitCoefficient()
        if (c1 != null && r1.size == 1 && other.splitCoefficient().first == null) {
            val tail = r1[0]
            val simplified = tail / other
            val trivialWrap =
                simplified is Expression && simplified.function.name == "/" && simplified.arguments == listOf<Value>(
                    tail, other
                )
            if (!trivialWrap) {
                // "(c * f) / g" -> "(c * f') / g'" when f/g = f'/g'
                if (simplified is Expression && simplified.function.name == "/") {
                    val (num, den) = simplified.arguments
                    return (c1 * num) / den
                }
                return c1 * simplified
            }
        }
        return Symbol("/")(this, other)
    }

    operator fun rem(other: Value): Value {
        if (this !is Number || other !is Number) return Symbol("%")(this, other)
        return Number(value % other.value)
    }

    fun pow(other: Value): Value {
        // "x ^ 0" -> "1"
        if (other is Number && other.value == 0.0) return Number(1.0)
        // "x ^ 1" -> "x"
        if (other is Number && other.value == 1.0) return this
        if (this !is Number || other !is Number) return Symbol("^")(this, other)
        return Number(value.pow(other.value))
    }

    operator fun invoke(vararg arguments: Value): Expression {
        this as Symbol
        return Expression(this, arguments.toList())
    }

    companion object {
        private val timeUnits = mapOf("d" to 86400, "h" to 3600, "m" to 60, "s" to 1)
    }
}
