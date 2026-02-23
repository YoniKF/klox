package com.craftinginterpreters.lox

internal sealed interface Value {
    data object Nil : Value
    data class Boolean(val value: kotlin.Boolean) : Value
    data class Number(val value: Double) : Value
    data class String(val value: kotlin.String) : Value

    sealed interface Callable : Value
    data class NativeFunction(val name: kotlin.String, val block: () -> Value) : Callable
    data class Function(
        val name: kotlin.String?, val parameters: List<Token.Identifier>, val body: List<Stmt>, val closure: Environment
    ) : Callable

    data class BoundInit(val function: Function, val instance: Instance) : Callable

    data class Class(val name: kotlin.String, val superclass: Class?, val methods: Map<kotlin.String, Function>) :
        Callable

    data class Instance(val klass: Class) : Value {
        val fields = HashMap<kotlin.String, Value>()
    }
}
