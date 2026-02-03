package com.craftinginterpreters.lox

internal sealed interface Stmt {
    data class Expression(val expression: Expr) : Stmt
    data class Print(val expression: Expr) : Stmt
    data class Var(val name: Token.Simple, val initializer: Expr?) : Stmt
    data class Block(val statements: List<Stmt>) : Stmt
}
