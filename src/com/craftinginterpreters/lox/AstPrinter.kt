package com.craftinginterpreters.lox

internal fun print(expr: Expr): String = when (expr) {
    is Expr.Binary -> parenthesize(expr.token.lexeme, expr.left, expr.right)
    is Expr.LogicalBinary -> parenthesize(expr.token.lexeme, expr.left, expr.right)
    is Expr.Grouping -> parenthesize("group", expr.expression)
    is Expr.Literal -> when (val value = expr.value) {
        is Value.Boolean -> value.value.toString()
        Value.Nil -> "nil"
        is Value.Number -> value.value.toString()
        is Value.String -> value.value
    }

    is Expr.Unary -> parenthesize(expr.token.lexeme, expr.right)
    is Expr.Variable -> expr.name.lexeme
    is Expr.Assign -> parenthesize("assign ${expr.name.lexeme}", expr.value)
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
            UnaryOperator.MINUS,
            Token.Simple(TokenType.MINUS, "-", 1),
            Expr.Literal(Value.Number(123.0))
        ),
        BinaryOperator.STAR, Token.Simple(TokenType.STAR, "*", 1),
        Expr.Grouping(
            Expr.Literal(Value.Number(45.67))
        )
    )

    println(print(expression))
}
