package com.craftinginterpreters.lox

internal sealed interface Stmt {
    data class Block(val statements: List<Stmt>) : Stmt
    data class Expression(val expression: Expr) : Stmt
    data class Print(val expression: Expr) : Stmt
    data class If(val condition: Expr, val then: Stmt, val otherwise: Stmt?) : Stmt
    data class While(val condition: Expr, val body: Stmt) : Stmt
    data object Break : Stmt
    data class Var(val name: Token.Identifier, val initializer: Expr?) : Stmt
    data class Function(val name: Token.Identifier, val params: List<Token.Identifier>, val body: List<Stmt>) : Stmt
    data class Return(val keyword: Token, val value: Expr?) : Stmt
}
