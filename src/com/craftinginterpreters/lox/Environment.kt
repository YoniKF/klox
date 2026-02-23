package com.craftinginterpreters.lox

internal class Environment(private val enclosing: Environment? = null) {
    private val values = HashMap<String, Value?>()
    private var superclass: Value.Class? = null

    internal val scoped = enclosing != null

    fun define(name: String, value: Value?) {
        values[name] = value
    }

    fun get(name: Token.EnvironmentKey): Value {
        if (values.containsKey(name.lexeme)) {
            values[name.lexeme]?.let { return it }
            throw RuntimeError(name, "Uninitialized variable '${name.lexeme}'.")
        }
        enclosing?.run { return get(name) }
        throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun assign(name: Token.EnvironmentKey, value: Value) {
        if (values.containsKey(name.lexeme)) values[name.lexeme] = value
        else if (enclosing != null) enclosing.assign(name, value)
        else throw RuntimeError(name, "Undefined variable '${name.lexeme}'.")
    }

    fun defineSuper(value: Value.Class) {
        superclass = value
    }

    fun getSuper(token: Token.Simple): Value.Class =
        superclass ?: enclosing?.getSuper(token) ?: throw RuntimeError(token, "No superclass.")
}
