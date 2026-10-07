package io.github.persiancalendar.calculator

import kotlin.math.ln

internal fun diff(f: Value, symbol: Value.Symbol): Value {
    return when (f) {
        is Value.Number -> Value.Number(0.0)
        is Value.Symbol -> Value.Number(if (f.name == symbol.name) 1.0 else 0.0)
        is Value.Expression -> when (val functionName = f.function.name) {
            "+" -> f.arguments.map { diff(it, symbol) }.reduce(Value::plus)
            "-" -> f.arguments.map { diff(it, symbol) }.reduce(Value::minus)

            "*" -> f.arguments.indices.fold<Int, Value>(Value.Number(0.0)) { acc, i ->
                val term = f.arguments.mapIndexed { j, arg ->
                    if (j == i) diff(arg, symbol) else arg
                }.fold(Value.Number(1.0), Value::times)
                acc + term
            }

            "%" -> error("'%' is not differentiable")

            "/" -> {
                (f.arguments[1] * diff(
                    f.arguments[0], symbol
                ) - f.arguments[0] * diff(
                    f.arguments[1], symbol
                )) / f.arguments[1].pow(Value.Number(2.0))
            }

            "^" -> {
                val base = f.arguments[0]
                val exponent = f.arguments[1]
                if (exponent is Value.Number) {
                    exponent * base.pow(Value.Number(exponent.value - 1)) * diff(base, symbol)
                } else {
                    val du = diff(base, symbol)
                    val dv = diff(exponent, symbol)
                    base.pow(exponent) * (dv * Value.Symbol("ln")(base) + exponent * du / base)
                }
            }

            else -> when (functionName) {
                in unaryDerivatives -> {
                    if (f.arguments.size != 1) error("'$functionName' should have one argument")
                    chain(f.arguments[0], symbol, unaryDerivatives.getValue(functionName))
                }

                in binaryDerivatives -> {
                    if (f.arguments.size != 2) error("'$functionName' should have two arguments")
                    binaryDerivatives.getValue(functionName)(
                        f.arguments,
                        f.arguments.map { diff(it, symbol) },
                    )
                }

                else -> Value.Expression(Value.Symbol("diff"), listOf(f, symbol))
            }
        }

        else -> Value.Expression(Value.Symbol("diff"), listOf(f, symbol))
    }
}

private val unaryDerivatives = mapOf<String, (u: Value, du: Value) -> Value>(
    "sqrt" to { u, du -> Value.Number(.5) * du / Value.Symbol("sqrt")(u) },
    "ln" to { u, du -> du / u },
    "exp" to { u, du -> Value.Symbol("exp")(u) * du },
    "sin" to { u, du -> Value.Symbol("cos")(u) * du },
    "cos" to { u, du -> Value.Number(-1.0) * Value.Symbol("sin")(u) * du },
    "tan" to { u, du -> (Value.Number(1.0) + Value.Symbol("tan")(u).pow(Value.Number(2.0))) * du },
    "cot" to { u, du -> -(Value.Number(1.0) + Value.Symbol("cot")(u).pow(Value.Number(2.0))) * du },

    "asin" to { u, du -> du / Value.Symbol("sqrt")(Value.Number(1.0) - u.pow(Value.Number(2.0))) },
    "acos" to { u, du -> -du / Value.Symbol("sqrt")(Value.Number(1.0) - u.pow(Value.Number(2.0))) },
    "atan" to { u, du -> du / (Value.Number(1.0) + u.pow(Value.Number(2.0))) },

    "sinh" to { u, du -> Value.Symbol("cosh")(u) * du },
    "cosh" to { u, du -> Value.Symbol("sinh")(u) * du },
    "tanh" to { u, du -> (Value.Number(1.0) - Value.Symbol("tanh")(u).pow(Value.Number(2.0))) * du },

    "asinh" to { u, du -> du / Value.Symbol("sqrt")(u.pow(Value.Number(2.0)) + Value.Number(1.0)) },
    "acosh" to { u, du -> du / Value.Symbol("sqrt")(u.pow(Value.Number(2.0)) - Value.Number(1.0)) },
    "atanh" to { u, du -> du / (Value.Number(1.0) - u.pow(Value.Number(2.0))) },

    "abs" to { u, du -> Value.Symbol("sign")(u) * du },
    "sign" to { _, _ -> Value.Number(0.0) },
    "ceil" to { _, _ -> Value.Number(0.0) },
    "floor" to { _, _ -> Value.Number(0.0) },
    "truncate" to { _, _ -> Value.Number(0.0) },
    "round" to { _, _ -> Value.Number(0.0) },

    "cbrt" to { u, du -> du / (Value.Number(3.0) * Value.Symbol("cbrt")(u).pow(Value.Number(2.0))) },
    "expm1" to { u, du -> Value.Symbol("exp")(u) * du },
    "ln1p" to { u, du -> du / (Value.Number(1.0) + u) },
    "log2" to { u, du -> du / (u * Value.Number(ln(2.0))) },
    "log10" to { u, du -> du / (u * Value.Number(ln(10.0))) },
)

private val binaryDerivatives = mapOf<String, (args: List<Value>, dargs: List<Value>) -> Value>(
    // log(u, v) = ln(u)/ln(v)
    "log" to { args, dargs ->
        val (u, v) = args
        val (du, dv) = dargs
        (du / u * Value.Symbol("ln")(v) - Value.Symbol("ln")(u) * dv / v) / Value.Symbol("ln")(v).pow(
            Value.Number(2.0)
        )
    },
    // atan2(y, x): (x·dy − y·dx) / (x² + y²)
    "atan2" to { args, dargs ->
        val (y, x) = args
        val (dy, dx) = dargs
        (x * dy - y * dx) / (x.pow(Value.Number(2.0)) + y.pow(Value.Number(2.0)))
    },
    // hypot(x, y) = sqrt(x² + y²): (x·dx + y·dy) / hypot(x, y)
    "hypot" to { args, dargs ->
        val (x, y) = args
        val (dx, dy) = dargs
        (x * dx + y * dy) / Value.Symbol("hypot")(x, y)
    },
)

private fun chain(
    inner: Value,
    symbol: Value.Symbol,
    outerDerivative: (u: Value, du: Value) -> Value,
): Value = outerDerivative(inner, diff(inner, symbol))
