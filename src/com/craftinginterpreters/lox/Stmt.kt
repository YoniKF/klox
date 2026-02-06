package com.craftinginterpreters.lox

internal sealed interface Stmt {
    data class Block(val statements: List<Stmt>) : Stmt
    data class Expression(val expression: Expr) : Stmt
    data class Print(val expression: Expr) : Stmt
    data class If(val condition: Expr, val then: Stmt, val otherwise: Stmt?) : Stmt
    data class While(val condition: Expr, val body: Stmt) : Stmt
    object Break : Stmt
    data class Var(val name: Token.Simple, val initializer: Expr?) : Stmt
}
