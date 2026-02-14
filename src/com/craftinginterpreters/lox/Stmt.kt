package com.craftinginterpreters.lox

internal sealed interface Stmt {
    sealed interface NonDeclaration : Stmt
    data class Block(val statements: List<Stmt>) : NonDeclaration
    data class Expression(val expression: Expr) : NonDeclaration
    data class Print(val expression: Expr) : NonDeclaration
    data class If(val condition: Expr, val then: NonDeclaration, val otherwise: NonDeclaration?) : NonDeclaration
    data class While(val condition: Expr, val body: NonDeclaration) : NonDeclaration
    data object Break : NonDeclaration
    data class Return(val keyword: Token, val value: Expr?) : NonDeclaration

    sealed interface Declaration : Stmt
    data class Var(val name: Token.Identifier, val initializer: Expr?) : Declaration
    data class Function(val name: Token.Identifier, val params: List<Token.Identifier>, val body: List<Stmt>) :
        Declaration
}
