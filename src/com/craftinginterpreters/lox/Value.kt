package com.craftinginterpreters.lox

internal sealed interface Value {
    data object Nil : Value
    data class Boolean(val value: kotlin.Boolean) : Value
    data class Number(val value: Double) : Value
    data class String(val value: kotlin.String) : Value

    sealed interface Callable : Value
    data class NativeFunction(val name: kotlin.String, val block: () -> Value) : Callable
    open class Function(
        val name: kotlin.String?,
        val parameters: List<Token.Identifier>,
        val body: List<Stmt>,
        val closure: Environment
    ) : Callable

    class BoundInit(
        name: kotlin.String?,
        parameters: List<Token.Identifier>,
        body: List<Stmt>,
        closure: Environment,
        val instance: Instance
    ) : Function(name, parameters, body, closure)

    data class Class(val name: kotlin.String, val methods: Map<kotlin.String, Function>) : Callable

    data class Instance(val klass: Class) : Value {
        val fields = HashMap<kotlin.String, Value>()
    }
}
