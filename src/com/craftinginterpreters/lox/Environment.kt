package com.craftinginterpreters.lox

internal class Environment {
    private val values = HashMap<String, Any?>()

    fun define(name: String, value: Any?) {
        values[name] = value
    }

    fun get(name: Token): Any? {
        // Value might be nil, so cannot just access and throw on null
        if (values.containsKey(name.lexeme)) return values[name.lexeme]
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun assign(name: Token, value: Any?) {
        if (values.containsKey(name.lexeme)) values[name.lexeme] = value
        else throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }
}
