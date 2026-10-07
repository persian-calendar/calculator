package io.github.persiancalendar.calculator

internal fun integrate(f: Value, symbol: Value.Symbol): Value {
    // "c" -> "c * x" when c doesn't contain the integration variable
    if (symbol !in f) return f * symbol

    return when (f) {
        // "x" -> "x ^ 2 / 2"
        is Value.Symbol -> symbol.pow(Value.Number(2.0)) / Value.Number(2.0)

        is Value.Expression -> when (val name = f.function.name) {
            // "f + g" -> "∫f + ∫g"
            "+", "-" -> Value.Expression(
                f.function,
                f.arguments.map { integrate(it, symbol) }
            )

            // "c * f" -> "c * ∫f" when c is constant in the variable
            "*" -> integrateProduct(f, symbol)

            // "c / u" -> "c * ln(u) / u'" for linear u, or "c * u^(-n)" if u is a power
            "/" -> integrateQuotient(f, symbol)

            // "u^n" -> "u^(n+1) / ((n+1) * u')" for constant n and constant u'
            "^" -> integratePower(f, symbol)

            // "f(u)" -> "F(u) / u'" for known antiderivative F and constant u'
            else -> {
                val antiderivative = antiderivatives[name]
                if (antiderivative == null || f.arguments.size != 1) {
                    unevaluated(f, symbol)
                } else {
                    val u = f.arguments[0]
                    val du = diff(u, symbol)
                    if (du is Value.Number && du.value != 0.0) antiderivative(u) / du
                    else unevaluated(f, symbol)
                }
            }
        }

        else -> unevaluated(f, symbol)
    }
}

private fun integrateProduct(f: Value.Expression, symbol: Value.Symbol): Value {
    val (constant, varying) = f.arguments.partition { symbol !in it }

    // "c" -> "c * x"
    if (varying.isEmpty()) return f * symbol

    // Nothing to pull out; only linear-in-x products work in our subset
    if (constant.isEmpty()) return unevaluated(f, symbol)

    val c = constant.reduce(Value::times)
    val rest = if (varying.size == 1) varying[0] else Value.Expression(Value.Symbol("*"), varying)

    val innerResult = integrate(rest, symbol)
    // If the inner integral gave up, the outer one should too
    if (innerResult == unevaluated(rest, symbol)) return unevaluated(f, symbol)
    return c * innerResult
}

private fun integrateQuotient(f: Value.Expression, symbol: Value.Symbol): Value {
    if (f.arguments.size != 2) return unevaluated(f, symbol)
    val (numerator, denominator) = f.arguments

    // "f / c" -> "∫f / c" when c is constant in the variable
    if (symbol !in denominator) return integrate(numerator, symbol) / denominator

    // "x / u" isn't in our subset
    if (symbol in numerator) return unevaluated(f, symbol)

    // "c / u" -> "c * ln(u) / u'" for linear u
    val du = diff(denominator, symbol)
    if (du is Value.Number && du.value != 0.0) {
        return numerator * Value.Symbol("ln")(denominator) / du
    }

    // "c / u^n" -> "c * ∫u^(-n)"
    if (denominator is Value.Expression && denominator.function.name == "^" &&
        denominator.arguments[1] is Value.Number
    ) {
        val base = denominator.arguments[0]
        val exponent = (denominator.arguments[1] as Value.Number).value
        return integrate(numerator * base.pow(Value.Number(-exponent)), symbol)
    }

    return unevaluated(f, symbol)
}

private fun integratePower(f: Value.Expression, symbol: Value.Symbol): Value {
    if (f.arguments.size != 2) return unevaluated(f, symbol)
    val base = f.arguments[0]
    val exponent = f.arguments[1]

    // Only constant exponents are supported
    if (exponent !is Value.Number) return unevaluated(f, symbol)

    val du = diff(base, symbol)
    if (du !is Value.Number || du.value == 0.0) return unevaluated(f, symbol)

    val n = exponent.value
    // "u ^ -1" -> "ln(u) / u'"
    if (n == -1.0) return Value.Symbol("ln")(base) / du

    // "u ^ n" -> "u ^ (n + 1) / ((n + 1) * u')"
    val nextN = n + 1
    val c = nextN * du.value
    // "u ^ -n" -> "1 / (c * u ^ (n - 1))" for n >= 2
    if (nextN < 0) return Value.Number(1.0 / c) / base.pow(Value.Number(-nextN))
    return base.pow(Value.Number(nextN)) / Value.Number(c)
}

/** Table of antiderivatives, one entry per unary function we can invert. */
private val antiderivatives = mapOf<String, (u: Value) -> Value>(
    "sin" to { u -> -Value.Symbol("cos")(u) },
    "cos" to { u -> Value.Symbol("sin")(u) },
    "exp" to { u -> Value.Symbol("exp")(u) },
    "sinh" to { u -> Value.Symbol("cosh")(u) },
    "cosh" to { u -> Value.Symbol("sinh")(u) },
    "tanh" to { u -> Value.Symbol("ln")(Value.Symbol("cosh")(u)) },
    "sqrt" to { u -> Value.Number(2.0 / 3.0) * u.pow(Value.Number(1.5)) },
    "ln" to { u -> u * Value.Symbol("ln")(u) - u },
)

private fun unevaluated(f: Value, symbol: Value.Symbol): Value =
    Value.Expression(Value.Symbol("integrate"), listOf(f, symbol))

private operator fun Value.contains(symbol: Value.Symbol): Boolean = when (this) {
    is Value.Symbol -> this == symbol
    is Value.Expression -> this.arguments.any { symbol in it }
    is Value.Tuple -> this.values.any { symbol in it }
    else -> false
}
