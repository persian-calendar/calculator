package io.github.persiancalendar.calculator

import kotlin.math.pow
import kotlin.math.truncate

sealed interface Value {
    object Null : Value

    data class Symbol(val value: String) : Value {
        override fun toString(): String = value
    }

    data class Number(val value: Double) : Value {
        infix fun withUnit(unit: Symbol) = NumberWithUnit(value, unit.value)
        override fun toString(): String = formatNumber(value)
    }

    data class NumberWithUnit(val value: Double, val unit: String) : Value {
        override fun toString(): String = "${formatNumber(value)} $unit"
    }

    data class Function(
        private val body: (arguments: List<Value>) -> Value,
        val inputCount: Int? = null
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
        return Number(value + other.value)
    }

    operator fun minus(other: Value): Value {
        this as Number; other as Number
        return Number(value - other.value)
    }

    operator fun times(other: Value): Value {
        this as Number; other as Number
        return Number(value * other.value)
    }

    operator fun div(other: Value): Value {
        this as Number; other as Number
        return Number(value / other.value)
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
        private fun formatNumber(value: Double): String =
            if (value == truncate(value)) value.toInt().toString() else value.toString()
    }
}