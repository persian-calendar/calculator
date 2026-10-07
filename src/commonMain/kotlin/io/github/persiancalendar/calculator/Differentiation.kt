package io.github.persiancalendar.calculator

fun diff(f: Value, symbol: Value.Symbol): Value {
    return when (f) {
        is Value.Number -> Value.Number(0.0)
        is Value.Symbol -> Value.Number(if (f.name == symbol.name) 1.0 else 0.0)
        is Value.Expression -> {
            when (val functionName = f.function.name) {
                "+", "-" -> f.arguments.map { diff(it, symbol) }
                    .fold(Value.Number(0.0), Value::plus)
                "*" -> {
                    f.arguments[0] * diff(f.arguments[1], symbol) +
                            f.arguments[1] * diff(f.arguments[0], symbol)
                }
                "/" -> {
                    (f.arguments[1] * diff(
                        f.arguments[0], symbol
                    ) - f.arguments[0] * diff(
                        f.arguments[1], symbol
                    )) / f.arguments[1].pow(Value.Number(2.0))
                }
                "^" -> {
                    if (f.arguments.size != 2)
                        error("Only exponential of two operands is supported")
                    val exponent = f.arguments[1]
                    if (exponent !is Value.Number)
                        error("Only constant exponents are supported")
                    exponent *
                            (f.arguments[0].pow(Value.Number(exponent.value - 1))) *
                            diff(f.arguments[0], symbol)
                }
                else -> {
                    if (functionName !in unaryDerivatives) error("Not supported function to differentiate $f")
                    if (f.arguments.size != 1) error("'$functionName' should have one argument")
                    chain(f.arguments[0], symbol, unaryDerivatives.getValue(functionName))
                }
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
    "tan" to { u, du ->
        (Value.Number(1.0) + Value.Symbol("tan")(u).pow(Value.Number(2.0))) * du
    },
)

private fun chain(
    inner: Value,
    symbol: Value.Symbol,
    outerDerivative: (u: Value, du: Value) -> Value,
): Value = outerDerivative(inner, diff(inner, symbol))
