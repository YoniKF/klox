package com.craftinginterpreters.lox

internal enum class UnaryOperator { MINUS, BANG }

internal enum class BinaryOperator {
    MINUS, PLUS, SLASH, STAR,

    BANG_EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,
}

internal enum class LogicalBinaryOperator { AND, OR }

// Code in book uses the visitor pattern
internal sealed interface Expr {
    data class Grouping(val expression: Expr) : Expr
    data class Literal(val value: Value) : Expr
    data class Variable(val name: Token.Identifier) : Expr
    data class Assign(val name: Token.Identifier, val value: Expr) : Expr
    data class Unary(val operator: UnaryOperator, val token: Token.Simple, val right: Expr) : Expr
    data class Binary(val left: Expr, val operator: BinaryOperator, val token: Token.Simple, val right: Expr) : Expr
    data class LogicalBinary(
        val left: Expr, val operator: LogicalBinaryOperator, val token: Token.Simple, val right: Expr
    ) : Expr

    data class Call(val callee: Expr, val paren: Token.Simple, val arguments: List<Expr>) : Expr
    data class AnonymousFunction(val params: List<Token.Identifier>, val body: List<Stmt>) : Expr
    data class Get(val instance: Expr, val name: Token.Identifier) : Expr
    data class Set(val instance: Expr, val name: Token.Identifier, val value: Expr) : Expr
    data class This(val keyword: Token.This) : Expr
}
