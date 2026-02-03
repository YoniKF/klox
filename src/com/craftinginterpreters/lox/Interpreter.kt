package com.craftinginterpreters.lox

internal class RuntimeError(val token: Token, message: String) : RuntimeException(message)

internal class Interpreter(private val environment: Environment = Environment()) {

    internal fun interpret(statements: List<Stmt>) = try {
        statements.forEach(::execute)
    } catch (error: RuntimeError) {
        Lox.runtimeError(error)
    }

    private fun execute(stmt: Stmt) {
        when (stmt) {
            is Stmt.Expression -> evaluate(stmt.expression)
            is Stmt.Print -> println(stringify(evaluate(stmt.expression)))
            is Stmt.Var -> environment.define(stmt.name.lexeme, stmt.initializer?.let(::evaluate))
            is Stmt.Block -> executeBlock(stmt.statements)
        }
    }

    private fun executeBlock(statements: List<Stmt>) {
        val interpreter = Interpreter(Environment(environment))
        statements.forEach(interpreter::execute)
    }

    private fun evaluate(expr: Expr): Any? = when (expr) {
        is Expr.Literal -> expr.value
        is Expr.Grouping -> evaluate(expr.expression)
        is Expr.Unary -> {
            val right = evaluate(expr.right)
            fun <R> numberOperand(block: (Double) -> R): R {
                if (right is Double) return block(right)
                throw RuntimeError(expr.operator, "Operand must be a number.")
            }

            when (expr.operator.type) {
                TokenType.MINUS -> numberOperand(Double::unaryMinus)
                TokenType.BANG -> !truthy(right)
                else -> null // Unreachable
            }
        }

        is Expr.Binary -> {
            val left = evaluate(expr.left)
            val right = evaluate(expr.right)
            fun <R> numberOperands(block: (Double, Double) -> R): R {
                if (left is Double && right is Double) return block(left, right)
                throw RuntimeError(expr.operator, "Operands must be numbers.")
            }

            when (expr.operator.type) {
                TokenType.MINUS -> numberOperands(Double::minus)
                TokenType.SLASH -> numberOperands(Double::div)
                TokenType.STAR -> numberOperands(Double::times)
                TokenType.PLUS -> when (left) {
                    is Double if right is Double -> left + right
                    is String if right is String -> left + right
                    else -> throw RuntimeError(expr.operator, "Operands must be two numbers or two strings.")
                }

                TokenType.GREATER -> numberOperands { a, b -> a > b }
                TokenType.GREATER_EQUAL -> numberOperands { a, b -> a >= b }
                TokenType.LESS -> numberOperands { a, b -> a < b }
                TokenType.LESS_EQUAL -> numberOperands { a, b -> a <= b }
                TokenType.BANG_EQUAL -> left != right
                TokenType.EQUAL_EQUAL -> left == right
                else -> null // Unreachable
            }
        }

        is Expr.Variable -> environment.get(expr.name)
        is Expr.Assign -> evaluate(expr.value).also { environment.assign(expr.name, it) }
    }
}

private fun truthy(any: Any?) = when (any) {
    null -> false
    is Boolean -> any
    else -> true
}

private fun stringify(any: Any?) = when (any) {
    null -> "nil"
    is Double -> any.toString().removeSuffix(".0")
    else -> any.toString()
}
