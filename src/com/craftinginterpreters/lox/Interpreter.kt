package com.craftinginterpreters.lox

internal class RuntimeError(val token: Token, message: String) : RuntimeException(message)

private sealed interface Result {
    data object Continue : Result
    data object Break : Result
    data class Return(val value: Value?) : Result
}

internal class Interpreter {
    private val globals: Environment = Environment().apply {
        define("clock", Value.NativeFunction("clock") { Value.Number(System.currentTimeMillis() / 1000.0) })
    }

    internal fun interpret(statements: List<Stmt>) = try {
        statements.forEach {
            when (it) {
                is Stmt.NonDeclaration -> execute(globals, it)
                is Stmt.Declaration -> execute(globals, it)
            }
        }
    } catch (error: RuntimeError) {
        Lox.runtimeError(error)
    }
}

private fun execute(env: Environment, stmt: Stmt.NonDeclaration): Result = when (stmt) {
    is Stmt.Block -> executeBlock(Environment(env), stmt.statements)
    is Stmt.Expression -> evaluate(env, stmt.expression).let { Result.Continue }
    is Stmt.Print -> println(stringify(evaluate(env, stmt.expression))).let { Result.Continue }
    is Stmt.If -> {
        if (truthy(evaluate(env, stmt.condition))) execute(env, stmt.then)
        else if (stmt.otherwise != null) execute(env, stmt.otherwise)
        else Result.Continue
    }

    is Stmt.While -> generateSequence { execute(env, stmt.body) }.takeWhile {
        truthy(evaluate(env, stmt.condition))
    }.firstNotNullOfOrNull {
        when (it) {
            Result.Break -> Result.Continue
            Result.Continue -> null
            is Result.Return -> it
        }
    } ?: Result.Continue

    Stmt.Break -> Result.Break
    is Stmt.Return -> Result.Return(stmt.value?.let { evaluate(env, it) })
}

private fun execute(env: Environment, stmt: Stmt.Declaration): Environment = when (stmt) {
    is Stmt.Var -> wrapIfScoped(env).define(stmt.name.lexeme, stmt.initializer?.let { evaluate(env, it) })
    is Stmt.Function -> wrapIfScoped(env).define(
        stmt.name.lexeme, Value.Function(stmt.name.lexeme, stmt.params, stmt.body, env)
    )
}

private fun executeBlock(env: Environment, statements: List<Stmt>): Result {
    var env = env
    for (stmt in statements) {
        when (stmt) {
            is Stmt.NonDeclaration -> when (val result = execute(env, stmt)) {
                is Result.Break, is Result.Return -> return result
                is Result.Continue -> {}
            }

            is Stmt.Declaration -> env = execute(env, stmt)
        }
    }
    return Result.Continue
}

private fun evaluate(env: Environment, expr: Expr): Value = when (expr) {
    is Expr.Grouping -> evaluate(env, expr.expression)
    is Expr.Literal -> expr.value
    is Expr.Variable -> env.get(expr.name)
    is Expr.Assign -> evaluate(env, expr.value).also { env.assign(expr.name, it) }
    is Expr.Unary -> {
        val right = evaluate(env, expr.right)
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
        val left = evaluate(env, expr.left)
        val right = evaluate(env, expr.right)
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
        val left = evaluate(env, expr.left)
        val truthiness = truthy(left)
        when (expr.operator) {
            LogicalBinaryOperator.AND -> if (!truthiness) left else evaluate(env, expr.right)
            LogicalBinaryOperator.OR -> if (truthiness) left else evaluate(env, expr.right)
        }
    }

    is Expr.Call -> {
        val callee = evaluate(env, expr.callee)
        val arguments = expr.arguments.map { evaluate(env, it) }
        if (callee !is Value.Callable) throw RuntimeError(expr.paren, "Can only call functions and classes.")
        if (arguments.size != callee.arity) throw RuntimeError(
            expr.paren, "Expected ${callee.arity} arguments but got ${arguments.size}."
        )
        call(callee, arguments)
    }

    is Expr.AnonymousFunction -> Value.Function(null, expr.params, expr.body, env)
}

private fun call(callable: Value.Callable, arguments: List<Value>): Value = when (callable) {
    is Value.NativeFunction -> callable.block()
    is Value.Function -> {
        val env = Environment(callable.closure).apply {
            callable.parameters.zip(arguments) { parameter, argument -> define(parameter.lexeme, argument) }
        }
        val result = executeBlock(env, callable.body)
        (result as? Result.Return)?.value ?: Value.Nil
    }
}

private fun wrapIfScoped(env: Environment) = if (env.scoped) Environment(env) else env

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
    is Value.NativeFunction -> "<native fun ${value.name}>"
    is Value.Function -> value.name?.let { "<fun ${value.name}>" } ?: "<anonymous fun>"
}
