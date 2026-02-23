package com.craftinginterpreters.lox

internal fun parse(tokens: List<Token>, prompt: Boolean): List<Stmt> {
    val parser = object {
        fun parse(): List<Stmt> {
            val statements = ArrayList<Stmt>()
            while (!atEnd()) {
                declaration()?.also { statements += it }
            }
            return statements
        }

        private fun declaration(): Stmt? {
            try {
                if (match(TokenType.VAR)) return varDeclaration()
                if (match(TokenType.FUN)) return function()
                if (match(TokenType.CLASS)) return classDeclaration()
                return statement()
            } catch (_: ParseError) {
                synchronize()
                return null
            }
        }

        private fun varDeclaration(): Stmt.Var {
            val name = consumerIdentifier("Expect variable name.")
            val initializer = if (match(TokenType.EQUAL)) expression() else null
            consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
            return Stmt.Var(name, initializer)
        }

        private fun function(): Stmt = matchIdentifier()?.let {
            function("function", it)
        } ?: run {
            retreat() // Prepare to parse 'fun' as the beginning of an expression
            return expressionStatement()
        }

        private fun function(kind: String, name: Token.Identifier, init: Boolean = false): Stmt.Function = function(
            "Expect '(' after $kind name.", "Expect '{' before $kind body.", init
        ).let { (params, body) -> Stmt.Function(name, params, body) }

        private fun function(
            expectLeftParen: String, expectLeftBrace: String, init: Boolean = false
        ): Pair<List<Token.Identifier>, List<Stmt>> {
            consume(TokenType.LEFT_PAREN, expectLeftParen)
            val params = buildList {
                if (check(TokenType.RIGHT_PAREN)) return@buildList
                do {
                    if (size >= 255) error(peek(), "Can't have more than 255 parameters.")
                    add(consumerIdentifier("Expect parameter name."))
                } while (match(TokenType.COMMA))
            }
            consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.")
            consume(TokenType.LEFT_BRACE, expectLeftBrace)
            scopes.addLast(scopes.last().withFunction(init))
            val body = try {
                block()
            } finally {
                scopes.removeLast()
            }
            return Pair(params, body)
        }

        private fun classDeclaration(): Stmt.Class {
            val name = consumerIdentifier("Expect class name.")
            val superclass =
                matchToken(TokenType.LESS)?.let { Expr.Variable(consumerIdentifier("Expect superclass name")) }
            if (name.lexeme == superclass?.name?.lexeme) Lox.error(
                superclass.name,
                "A class can't inherit from itself."
            )
            consume(TokenType.LEFT_BRACE, "Expect '{' before class body.")
            scopes.addLast(Scope.klass(superclass != null))
            val methods = try {
                buildList {
                    while (!check(TokenType.RIGHT_BRACE) && !atEnd()) {
                        val methodName = consumerIdentifier("Expect method name.")
                        add(function("method", methodName, methodName.lexeme == "init"))
                    }
                    consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.")
                }
            } finally {
                scopes.removeLast()
            }
            return Stmt.Class(name, superclass, methods)
        }

        private fun statement(): Stmt.NonDeclaration {
            if (match(TokenType.PRINT)) return printStatement()
            if (match(TokenType.IF)) return ifStatement()
            if (match(TokenType.WHILE)) return whileStatement()
            if (match(TokenType.FOR)) return forStatement()
            matchToken(TokenType.BREAK)?.let { return breakStatement(it) }
            matchToken(TokenType.RETURN)?.let { return returnStatement(it) }
            if (match(TokenType.LEFT_BRACE)) return Stmt.Block(block())
            return expressionStatement()
        }

        private fun printStatement(): Stmt.Print {
            val expr = expression()
            consume(TokenType.SEMICOLON, "Expect ';' after value.")
            return Stmt.Print(expr)
        }

        fun ifStatement(): Stmt.If {
            consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.")
            val condition = expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.")
            val then = statement()
            val otherwise = if (match(TokenType.ELSE)) statement() else null
            return Stmt.If(condition, then, otherwise)
        }

        fun whileStatement(): Stmt.While {
            consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.")
            val condition = expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after while condition.")
            val body = loopBody()
            return Stmt.While(condition, body)
        }

        fun forStatement(): Stmt.NonDeclaration {
            consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.")

            val initializer = if (match(TokenType.SEMICOLON)) null
            else if (match(TokenType.VAR)) varDeclaration()
            else expressionStatement()

            val condition = if (check(TokenType.SEMICOLON)) null else expression()
            consume(TokenType.SEMICOLON, "Expect ';' after for condition.")

            val increment = if (check(TokenType.RIGHT_PAREN)) null else expression()
            consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.")

            val body = loopBody()

            val loop = Stmt.While(
                condition ?: Expr.Literal(Value.Boolean(true)),
                increment?.let { Stmt.Block(listOf(body, Stmt.Expression(increment))) } ?: body)
            return initializer?.let { Stmt.Block(listOf(initializer, loop)) } ?: loop
        }

        val scopes = mutableListOf(Scope.GLOBAL)

        private fun loopBody(): Stmt.NonDeclaration {
            scopes.addLast(scopes.last().withLoop())
            val body = try {
                statement()
            } finally {
                scopes.removeLast()
            }
            return body
        }

        private fun breakStatement(keyword: Token.Simple): Stmt.Break {
            if (!scopes.last().loop) error(keyword, "Can't break outside of a loop.")
            consume(TokenType.SEMICOLON, "Expect ';' after 'break'.")
            return Stmt.Break
        }

        private fun returnStatement(keyword: Token.Simple): Stmt.Return {
            val functionScope = scopes.last().function
            if (functionScope == null) error(keyword, "Can't return from top-level code.")
            val value = if (check(TokenType.SEMICOLON)) null else expression()
            consume(TokenType.SEMICOLON, "Expect ';' after return value.")
            if (value != null && functionScope == FunctionType.INIT) error(
                keyword,
                "Can't return a value from an initializer."
            )
            return Stmt.Return(keyword, value)
        }

        private fun expressionStatement(): Stmt.NonDeclaration {
            val expr = expression()
            if (match(TokenType.SEMICOLON)) return Stmt.Expression(expr)
            if (!prompt) throw error(peek(), "Expect ';' after expression.")
            if (!atEnd()) throw error(peek(), "Expect end of prompt after expression.")
            return Stmt.Print(expr)
        }

        private fun block(): List<Stmt> = buildList {
            while (!check(TokenType.RIGHT_BRACE) && !atEnd()) {
                declaration()?.also { add(it) }
            }
            consume(TokenType.RIGHT_BRACE, "Expect '}' after block.")
        }

        private fun expression() = assignment()

        private fun assignment(): Expr {
            val expr = or()
            val equal = matchToken(TokenType.EQUAL) ?: return expr
            val value = assignment()
            return when (expr) {
                is Expr.Variable -> Expr.Assign(expr.name, value)
                is Expr.Get -> Expr.Set(expr.instance, expr.name, value)
                else -> {
                    error(equal, "Invalid assignment target.")
                    expr // Return whatever we could parse
                }
            }
        }

        private fun <O> genericBinary(
            next: () -> Expr, map: Map<TokenType, O>, constructor: (Expr, O, Token.Simple, Expr) -> Expr
        ): () -> Expr = {
            var expr = next()
            while (true) {
                val (token, operator) = match(map) ?: break
                val right = next()
                expr = constructor(expr, operator, token, right)
            }
            expr
        }

        private fun binary(next: () -> Expr, map: Map<TokenType, BinaryOperator>) =
            genericBinary(next, map, Expr::Binary)

        private fun logicalBinary(next: () -> Expr, map: Map<TokenType, LogicalBinaryOperator>) =
            genericBinary(next, map, Expr::LogicalBinary)

        private val factor = binary(
            ::unary, mapOf(
                TokenType.SLASH to BinaryOperator.SLASH, TokenType.STAR to BinaryOperator.STAR
            )
        )
        private val term = binary(
            factor, mapOf(
                TokenType.MINUS to BinaryOperator.MINUS, TokenType.PLUS to BinaryOperator.PLUS
            )
        )
        private val comparison = binary(
            term, mapOf(
                TokenType.GREATER to BinaryOperator.GREATER,
                TokenType.GREATER_EQUAL to BinaryOperator.GREATER_EQUAL,
                TokenType.LESS to BinaryOperator.LESS,
                TokenType.LESS_EQUAL to BinaryOperator.LESS_EQUAL,
            )
        )
        private val equality = binary(
            comparison, mapOf(
                TokenType.BANG_EQUAL to BinaryOperator.BANG_EQUAL,
                TokenType.EQUAL_EQUAL to BinaryOperator.EQUAL_EQUAL,
            )
        )
        private val and = logicalBinary(equality, mapOf(TokenType.AND to LogicalBinaryOperator.AND))
        private val or = logicalBinary(and, mapOf(TokenType.OR to LogicalBinaryOperator.OR))

        private val tokenToUnaryOperator = mapOf(
            TokenType.MINUS to UnaryOperator.MINUS, TokenType.BANG to UnaryOperator.BANG
        )

        private fun unary(): Expr {
            val (token, operator) = match(tokenToUnaryOperator) ?: return call()
            val right = unary()
            return Expr.Unary(operator, token, right)
        }

        private fun call(): Expr {
            var expr = primary()
            while (true) {
                if (match(TokenType.LEFT_PAREN)) {
                    val arguments = buildList {
                        if (check(TokenType.RIGHT_PAREN)) return@buildList
                        do {
                            if (size >= 255) error(peek(), "Can't have more than 255 arguments.")
                            add(expression())
                        } while (match(TokenType.COMMA))
                    }
                    val paren = consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.")
                    expr = Expr.Call(expr, paren, arguments)
                } else if (match(TokenType.DOT)) {
                    val name = consumerIdentifier("Expect property name after '.'.")
                    expr = Expr.Get(expr, name)
                } else break
            }
            return expr
        }

        private fun primary(): Expr {
            if (match(TokenType.FALSE)) return Expr.Literal(Value.Boolean(false))
            if (match(TokenType.TRUE)) return Expr.Literal(Value.Boolean(true))
            if (match(TokenType.NIL)) return Expr.Literal(Value.Nil)

            matchNumberLiteral()?.let { return Expr.Literal(Value.Number(it)) }
            matchStringLiteral()?.let { return Expr.Literal(Value.String(it)) }
            matchIdentifier()?.let { return Expr.Variable(it) }
            matchThis()?.let { return thisExpression(it) }
            matchToken(TokenType.SUPER)?.let { return superExpression(it) }

            if (match(TokenType.LEFT_PAREN)) {
                val expr = expression()
                consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
                return Expr.Grouping(expr)
            }

            if (match(TokenType.FUN)) return anonymousFunction()

            throw error(peek(), "Expect expression.")
        }

        private fun thisExpression(keyword: Token.This): Expr.This {
            if (scopes.last().klass == null) error(keyword, "Can't use 'this' outside of a class.")
            return Expr.This(keyword)
        }

        private fun superExpression(keyword: Token.Simple): Expr.Super {
            when (scopes.last().klass) {
                null -> error(keyword, "Can't use 'super' outside of a class.")
                ClassType.SUBCLASS -> {}
                else -> Lox.error(keyword, "Can't use 'super' in a class with no superclass.")
            }
            consume(TokenType.DOT, "Expect '.' after 'super'.")
            val method = consumerIdentifier("Expect superclass method name.")
            return Expr.Super(keyword, method)
        }

        private fun anonymousFunction(): Expr.AnonymousFunction = function(
            "Expect '(' after 'fun'.", "Expect '{' before anonymous function body."
        ).let { (params, body) -> Expr.AnonymousFunction(params, body) }

        private var current = 0
        private fun peek(): Token = tokens[current]
        private fun advance() {
            ++current
        }

        private fun retreat() {
            --current
        }

        @Suppress("SameParameterValue")
        private fun check(type: TokenType): Boolean = (peek() as? Token.Simple)?.type == type
        private fun atEnd(): Boolean = peek() is Token.Eof

        private fun matchToken(type: TokenType): Token.Simple? = when (val token = peek()) {
            !is Token.Simple -> null
            else if token.type == type -> {
                advance()
                token
            }

            else -> null
        }

        private fun match(type: TokenType): Boolean = matchToken(type) != null

        private fun <R> match(map: Map<TokenType, R>): Pair<Token.Simple, R>? {
            val token = peek()
            if (token !is Token.Simple) return null
            val mapped = map[token.type] ?: return null
            advance()
            return Pair(token, mapped)
        }

        private fun matchNumberLiteral(): Double? = (peek() as? Token.Number)?.let { advance(); it.value }
        private fun matchStringLiteral(): String? = (peek() as? Token.String)?.let { advance(); it.value }
        private fun matchIdentifier(): Token.Identifier? = (peek() as? Token.Identifier)?.also { advance() }
        private fun matchThis(): Token.This? = (peek() as? Token.This)?.also { advance() }

        private fun consume(type: TokenType, message: String): Token.Simple =
            matchToken(type) ?: throw error(peek(), message)

        private fun consumerIdentifier(message: String): Token.Identifier =
            matchIdentifier() ?: throw error(peek(), message)

        private fun error(token: Token, message: String): ParseError {
            Lox.error(token, message)
            return ParseError()
        }

        private fun synchronize() {
            while (true) {
                when (val token = peek()) {
                    is Token.Eof -> return
                    is Token.Simple -> when (token.type) {
                        TokenType.SEMICOLON -> {
                            advance()
                            return
                        }

                        TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF, TokenType.WHILE, TokenType.PRINT, TokenType.RETURN -> return
                        else -> advance()
                    }

                    else -> advance()
                }
            }
        }
    }

    return parser.parse()
}

private enum class ClassType { OTHER, SUBCLASS }
private enum class FunctionType { OTHER, INIT }

private data class Scope(val klass: ClassType?, val function: FunctionType?, val loop: Boolean) {
    companion object {
        val GLOBAL = Scope(klass = null, function = null, loop = false)
        fun klass(sub: Boolean) =
            Scope(klass = if (sub) ClassType.SUBCLASS else ClassType.OTHER, function = null, loop = false)
    }

    fun withFunction(init: Boolean) =
        Scope(klass, function = if (init) FunctionType.INIT else FunctionType.OTHER, loop = false)

    fun withLoop() = Scope(klass, function, loop = true)
}

private class ParseError : RuntimeException()
