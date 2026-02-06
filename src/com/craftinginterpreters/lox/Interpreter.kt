package com.craftinginterpreters.lox

internal class RuntimeError(val token: Token, message: String) : RuntimeException(message)

internal class Interpreter(private val environment: Environment = Environment()) {

    internal fun interpret(statements: List<Stmt>) = try {
        statements.forEach(::execute)
    } catch (error: RuntimeError) {
        Lox.runtimeError(error)
    }

    // Returns whether a break statement was encountered during execution
    private fun execute(stmt: Stmt): Boolean {
        when (stmt) {
            is Stmt.Block -> return executeBlock(stmt.statements)
            is Stmt.Expression -> evaluate(stmt.expression)
            is Stmt.Print -> println(stringify(evaluate(stmt.expression)))
            is Stmt.If -> {
                if (truthy(evaluate(stmt.condition))) return execute(stmt.then)
                else if (stmt.otherwise != null) return execute(stmt.otherwise)
            }

            is Stmt.While -> while (truthy(evaluate(stmt.condition))) if (execute(stmt.body)) break
            Stmt.Break -> return true
            is Stmt.Var -> environment.define(stmt.name.lexeme, stmt.initializer?.let(::evaluate))
        }
        return false
    }

    private fun executeBlock(statements: List<Stmt>): Boolean =
        statements.any(Interpreter(Environment(environment))::execute)

    private fun evaluate(expr: Expr): Value = when (expr) {
        is Expr.Grouping -> evaluate(expr.expression)
        is Expr.Literal -> expr.value
        is Expr.Variable -> environment.get(expr.name)
        is Expr.Assign -> evaluate(expr.value).also { environment.assign(expr.name, it) }
        is Expr.Unary -> {
            val right = evaluate(expr.right)
            fun <R> numberOperand(block: (Double) -> R): R {
                if (right is Value.Number) return block(right.value)
                throw RuntimeError(expr.token, "Operand must be a number.")
            }

            when (expr.operator) {
                UnaryOperator.MINUS -> Value.Number(numberOperand(Double::unaryMinus))
                UnaryOperator.BANG -> Value.Boolean(!truthy(right))
            }
        }

        is Expr.Binary -> {
            val left = evaluate(expr.left)
            val right = evaluate(expr.right)
            fun <R> numberOperands(block: (Double, Double) -> R): R {
                if (left is Value.Number && right is Value.Number) return block(left.value, right.value)
                throw RuntimeError(expr.token, "Operands must be numbers.")
            }

            when (expr.operator) {
                BinaryOperator.MINUS -> Value.Number(numberOperands(Double::minus))
                BinaryOperator.SLASH -> Value.Number(numberOperands(Double::div))
                BinaryOperator.STAR -> Value.Number(numberOperands(Double::times))
                BinaryOperator.PLUS -> when (left) {
                    is Value.Number if right is Value.Number -> Value.Number(left.value + right.value)
                    is Value.String if right is Value.String -> Value.String(left.value + right.value)
                    else -> throw RuntimeError(expr.token, "Operands must be two numbers or two strings.")
                }

                BinaryOperator.GREATER -> Value.Boolean(numberOperands { a, b -> a > b })
                BinaryOperator.GREATER_EQUAL -> Value.Boolean(numberOperands { a, b -> a >= b })
                BinaryOperator.LESS -> Value.Boolean(numberOperands { a, b -> a < b })
                BinaryOperator.LESS_EQUAL -> Value.Boolean(numberOperands { a, b -> a <= b })
                BinaryOperator.BANG_EQUAL -> Value.Boolean(left != right)
                BinaryOperator.EQUAL_EQUAL -> Value.Boolean(left == right)
            }
        }

        is Expr.LogicalBinary -> {
            val left = evaluate(expr.left)
            val truthiness = truthy(left)
            when (expr.operator) {
                LogicalBinaryOperator.AND -> if (!truthiness) left else evaluate(expr.right)
                LogicalBinaryOperator.OR -> if (truthiness) left else evaluate(expr.right)
            }
        }
    }
}

private fun truthy(value: Value): Boolean = when (value) {
    Value.Nil -> false
    is Value.Boolean -> value.value
    else -> true
}

private fun stringify(value: Value): String = when (value) {
    Value.Nil -> "nil"
    is Value.Boolean -> value.value.toString()
    is Value.Number -> value.value.toString().removeSuffix(".0")
    is Value.String -> value.value
}
