package com.craftinginterpreters.lox

internal fun print(expr: Expr): String = when (expr) {
    is Expr.Binary -> parenthesize(expr.operator.lexeme, expr.left, expr.right)
    is Expr.Grouping -> parenthesize("group", expr.expression)
    is Expr.Literal -> expr.value?.toString() ?: "nil"
    is Expr.Unary -> parenthesize(expr.operator.lexeme, expr.right)
}

private fun parenthesize(name: String, vararg exprs: Expr) = exprs.joinToString(
    prefix = "($name ", // This intermediate string that may be avoided by using buildString directly
    transform = ::print,
    separator = " ",
    postfix = ")",
)

@Suppress("unused")
private fun main() {
    val expression = Expr.Binary(
        Expr.Unary(
            Token(TokenType.MINUS, "-", null, 1),
            Expr.Literal(123)
        ),
        Token(TokenType.STAR, "*", null, 1),
        Expr.Grouping(
            Expr.Literal(45.67)
        )
    )

    println(print(expression))
}
