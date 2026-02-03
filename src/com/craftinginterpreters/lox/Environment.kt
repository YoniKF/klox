package com.craftinginterpreters.lox

internal class Environment(private val enclosing: Environment? = null) {
    private val values = HashMap<String, Value>()

    fun define(name: String, value: Value) {
        values[name] = value
    }

    fun get(name: Token.Simple): Value {
        values[name.lexeme]?.let { return it }
        enclosing?.run { return get(name) }
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun assign(name: Token.Simple, value: Value) {
        if (values.containsKey(name.lexeme)) values[name.lexeme] = value
        else if (enclosing != null) enclosing.assign(name, value)
        else throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }
}
