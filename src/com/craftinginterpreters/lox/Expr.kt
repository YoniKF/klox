package com.craftinginterpreters.lox

// Code in book uses the visitor pattern
internal sealed interface Expr {
    data class Binary(val left: Expr, val operator: Token, val right: Expr) : Expr
    data class Grouping(val expression: Expr) : Expr
    data class Literal(val value: Any?) : Expr
    data class Unary(val operator: Token, val right: Expr) : Expr
    data class Variable(val name: Token) : Expr
    data class Assign(val name: Token, val value: Expr) : Expr
}
