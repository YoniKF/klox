package com.craftinginterpreters.lox

internal class Environment(private val enclosing: Environment? = null) {
    private val values = HashMap<String, Value?>()

    internal val scoped = enclosing != null

    fun define(name: String, value: Value?): Environment {
        values[name] = value
        return this
    }

    fun get(name: Token.Identifier): Value {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme]?.let { return it }
            throw RuntimeError(name, "Uninitialized variable '${name.lexeme}'.")
        }
        enclosing?.run { return get(name) }
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun assign(name: Token.Identifier, value: Value) {
        if (values.containsKey(name.lexeme)) values[name.lexeme] = value
        else if (enclosing != null) enclosing.assign(name, value)
        else throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }
}
