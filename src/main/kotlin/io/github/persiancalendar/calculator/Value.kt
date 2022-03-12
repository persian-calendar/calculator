package io.github.persiancalendar.calculator

import kotlin.math.pow

sealed class Value {
    object Null : Value()
    class Number(val value: Double) : Value() {
        override fun toString(): String = value.toString()
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
}