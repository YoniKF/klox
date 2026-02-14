package com.craftinginterpreters.lox

internal sealed interface Value {
    data object Nil : Value
    data class Boolean(val value: kotlin.Boolean) : Value
    data class Number(val value: Double) : Value
    data class String(val value: kotlin.String) : Value

    sealed class Callable(val arity: Int) : Value
    data class NativeFunction(val name: kotlin.String, val block: () -> Value) : Callable(0)
    data class Function(
        val name: kotlin.String?,
        val parameters: List<Token.Identifier>,
        val body: List<Stmt>,
        val closure: Environment
    ) : Callable(parameters.size)
}
