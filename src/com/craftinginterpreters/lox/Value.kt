package com.craftinginterpreters.lox

internal sealed interface Value {
    object Nil : Value
    data class Boolean(val value: kotlin.Boolean) : Value
    data class Number(val value: Double) : Value
    data class String(val value: kotlin.String) : Value
}
