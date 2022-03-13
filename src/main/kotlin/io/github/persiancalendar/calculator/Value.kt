package io.github.persiancalendar.calculator

import kotlin.math.pow
import kotlin.math.truncate

sealed interface Value {
    object Null : Value

    data class Symbol(val value: String) : Value {
        override fun toString(): String = ":$value"
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

        infix fun withUnit(unit: Symbol) = Number(value, unit.value)
        override fun toString(): String = listOfNotNull(formatNumber(value), unit).joinToString(" ")
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

    operator fun plus(other: Value): Value {
        this as Number; other as Number
        if (unit == other.unit) return Number(value + other.value, unit)
        val thisSecondFactor = timeUnits[unit]
        val otherSecondFactor = timeUnits[other.unit]
        if (thisSecondFactor == null || otherSecondFactor == null)
            error("This addition of units isn't supported")
        return Number(value * thisSecondFactor + other.value * otherSecondFactor, "s")

    }

    operator fun minus(other: Value): Value = this + Number(-1.0) * other

    operator fun times(other: Value): Value {
        this as Number; other as Number
        // TODO: Maybe just allowing multiply of two length units? What else should be accepted?
        if (unit != null && other.unit != null) error("Two numbers with unit are multiplied")
        return Number(value * other.value, unit ?: other.unit)
    }

    operator fun div(other: Value): Value {
        this as Number; other as Number
        val resultUnit = when {
            unit == other.unit -> null // 1m / 2m -> 1 (null)
            unit == null && other.unit != null -> "1/${other.unit}"
            unit != null && other.unit == null -> unit
            else -> "$unit/${other.unit}"
        }
        return Number(value / other.value, resultUnit)
    }

    operator fun rem(other: Value): Value {
        this as Number; other as Number
        return Number(value % other.value)
    }

    fun pow(other: Value): Value {
        this as Number; other as Number
        return Number(value.pow(other.value))
    }

    companion object {
        private val timeUnits = mapOf("d" to 86400, "h" to 3600, "m" to 60, "s" to 1)
    }
}