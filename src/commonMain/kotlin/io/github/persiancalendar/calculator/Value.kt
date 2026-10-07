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
        // Zero if it isn't an operator
        private val precedence = when (function.name) {
            "+", "-" -> 1
            "*", "/", "%" -> 2
            "^", "**" -> 3
            else -> 0
        }

        override fun toString(): String = render(atRoot = false)

        fun render(atRoot: Boolean): String {
            val name = function.name
            if (precedence == 0) {
                return "$name(${arguments.joinToString(", ") { it.renderAsArgument() }})"
            }

            if (name == "*" && arguments.size == 2 && arguments[0] == Number(-1.0)) {
                val inner = arguments[1]
                val body =
                    if (inner is Expression) inner.render(atRoot = true) else inner.toString()
                val needsParens = inner is Expression && inner.precedence > 0
                return if (needsParens) "-($body)" else "-$body"
            }

            val body = when (arguments.size) {
                0 -> "0"
                1 -> if (name == "-") "(-${arguments[0].renderAsArgument()})"
                else arguments[0].renderAsArgument()

                else -> {
                    val rightAssoc = isRightAssociative(name)
                    arguments.mapIndexed { i, arg ->
                        when (arg) {
                            is Expression -> arg.renderAsOperand(
                                parentPrecedence = precedence,
                                parentRightAssoc = rightAssoc,
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
            parentPrecedence: Int,
            parentRightAssoc: Boolean,
            isFirst: Boolean,
            isLast: Boolean,
        ): String {
            if (precedence == 0) return render(atRoot = true)
            val needsParens =
                precedence < parentPrecedence || (precedence == parentPrecedence && ((isFirst && parentRightAssoc) || (isLast && !parentRightAssoc)))
            val body = render(atRoot = true)
            return if (needsParens) "($body)" else body
        }

        private fun isRightAssociative(name: String): Boolean = name == "^" || name == "**"
    }

    fun renderAsArgument(): String = if (this is Expression) render(atRoot = true) else toString()

    operator fun plus(other: Value): Value {
        if (this is Number && other is Number) {
            if (unit == other.unit) return Number(value + other.value, unit)
            val thisSecondFactor = timeUnits[unit]
            val otherSecondFactor = timeUnits[other.unit]
            if (thisSecondFactor == null || otherSecondFactor == null)
                error("This addition of units isn't supported")
            return Number(value * thisSecondFactor + other.value * otherSecondFactor, "s")
        }
        if (this.isZero()) return other
        if (other.isZero()) return this
        if (other is Number && other.value < 0.0 && other.unit == null) return Symbol("-")(
            this, Number(-other.value)
        )
        return Symbol("+")(this, other)
    }

    operator fun minus(other: Value): Value {
        if (other.isZero()) return this
        if (this == other) return Number(0.0)
        if (other is Number && other.value < 0.0 && other.unit == null) return this + Number(-other.value)
        if (this !is Number || other !is Number) return Symbol("-")(this, other)
        return this + Number(-1.0) * other
    }

    operator fun unaryMinus(): Value = Number(-1.0) * this

    private fun splitCoefficient(v: Value): Pair<Number?, List<Value>> = when (v) {
        is Number -> v to emptyList()
        is Expression -> {
            if (v.function.name == "*" && v.arguments.firstOrNull() is Number)
                (v.arguments.first() as Number) to v.arguments.drop(1)
            else null to listOf(v)
        }
        else -> null to listOf(v)
    }

    operator fun times(other: Value): Value {
        if (this is Number && other is Number) {
            if (unit != null && other.unit != null) error("Two numbers with unit are multiplied")
            return Number(value * other.value, unit ?: other.unit)
        }
        if (this.isZero() || other.isZero()) return Number(0.0)
        if (this.isOne()) return other
        if (other.isOne()) return this

        // Coefficient folding
        val (c1, r1) = splitCoefficient(this)
        val (c2, r2) = splitCoefficient(other)
        if (c1 != null && c2 != null) {
            val folded = c1 * c2            // Number * Number, respects units
            val rest = r1 + r2
            return rest.fold(folded, Value::times)
        }

        return Symbol("*")(this, other)
    }

    private fun Value.isZero() =
        this is Number && value == 0.0 && unit == null

    private fun Value.isOne() =
        this is Number && value == 1.0 && unit == null

    operator fun div(other: Value): Value {
        if (this is Number && other is Number) {
            val resultUnit = when {
                unit == other.unit -> null
                unit == null && other.unit != null -> "1/${other.unit}"
                unit != null && other.unit == null -> unit
                else -> "$unit/${other.unit}"
            }
            return Number(value / other.value, resultUnit)
        }
        if (this == other) return Number(1.0)
        if (this.isZero()) return Number(0.0)
        return Symbol("/")(this, other)
    }

    operator fun rem(other: Value): Value {
        if (this !is Number || other !is Number) return Symbol("%")(this, other)
        return Number(value % other.value)
    }

    fun pow(other: Value): Value {
        if (other is Number && other.value == 0.0) return Number(1.0)
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
