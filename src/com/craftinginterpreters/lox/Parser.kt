package com.craftinginterpreters.lox

internal class ParseError : RuntimeException()

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
                match(TokenType.VAR)?.let { return varDeclaration() }
                return statement()
            } catch (_: ParseError) {
                synchronize()
                return null
            }
        }

        private fun varDeclaration(): Stmt.Var {
            val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
            val initializer = match(TokenType.EQUAL)?.let { expression() }
            consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
            return Stmt.Var(name, initializer)
        }

        private fun statement(): Stmt {
            match(TokenType.PRINT)?.let { return printStatement() }
            match(TokenType.LEFT_BRACE)?.let { return Stmt.Block(block()) }
            return expressionStatement()
        }

        private fun printStatement(): Stmt.Print {
            val expr = expression()
            consume(TokenType.SEMICOLON, "Expect ';' after value.")
            return Stmt.Print(expr)
        }

        private fun expressionStatement(): Stmt {
            val expr = expression()
            if (match(TokenType.SEMICOLON) != null) return Stmt.Expression(expr)
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
            val expr = equality()
            val equal = match(TokenType.EQUAL) ?: return expr
            val value = assignment()
            return when (expr) {
                is Expr.Variable -> Expr.Assign(expr.name, value)
                else -> {
                    error(equal, "Invalid assignment target.")
                    expr // Return whatever we could parse
                }
            }
        }

        private fun binary(next: () -> Expr, map: Map<TokenType, BinaryOperator>): () -> Expr = {
            var expr = next()
            while (true) {
                val (token, operator) = match(map) ?: break
                val right = next()
                expr = Expr.Binary(expr, operator, token, right)
            }
            expr
        }

        private val factor = binary(
            ::unary, mapOf(
                TokenType.SLASH to BinaryOperator.SLASH,
                TokenType.STAR to BinaryOperator.STAR
            )
        )
        private val term = binary(
            factor, mapOf(
                TokenType.MINUS to BinaryOperator.MINUS,
                TokenType.PLUS to BinaryOperator.PLUS
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

        private val tokenToUnaryOperator = mapOf(
            TokenType.MINUS to UnaryOperator.MINUS,
            TokenType.BANG to UnaryOperator.BANG
        )

        private fun unary(): Expr {
            val (token, operator) = match(tokenToUnaryOperator) ?: return primary()
            val right = unary()
            return Expr.Unary(operator, token, right)
        }

        private fun primary(): Expr {
            match(TokenType.FALSE)?.let { return Expr.Literal(Value.Boolean(false)) }
            match(TokenType.TRUE)?.let { return Expr.Literal(Value.Boolean(true)) }
            match(TokenType.NIL)?.let { return Expr.Literal(Value.Nil) }

            matchNumberLiteral()?.let { return Expr.Literal(Value.Number(it)) }
            matchStringLiteral()?.let { return Expr.Literal(Value.String(it)) }
            match(TokenType.IDENTIFIER)?.let { return Expr.Variable(it) }

            match(TokenType.LEFT_PAREN)?.let {
                val expr = expression()
                consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
                return Expr.Grouping(expr)
            }

            throw error(peek(), "Expect expression.")
        }

        private var current = 0
        private fun peek(): Token = tokens[current]
        private fun advance() {
            ++current
        }

        @Suppress("SameParameterValue")
        private fun check(type: TokenType): Boolean = (peek() as? Token.Simple)?.type == type
        private fun atEnd(): Boolean = peek() is Token.Eof

        private fun match(type: TokenType): Token.Simple? = when (val token = peek()) {
            !is Token.Simple -> null
            else if token.type == type -> {
                advance()
                token
            }

            else -> null
        }

        private fun <R> match(map: Map<TokenType, R>): Pair<Token.Simple, R>? {
            val token = peek()
            if (token !is Token.Simple) return null
            val mapped = map[token.type] ?: return null
            advance()
            return Pair(token, mapped)
        }

        private fun matchNumberLiteral(): Double? = (peek() as? Token.Number)?.let { advance(); it.value }
        private fun matchStringLiteral(): String? = (peek() as? Token.String)?.let { advance(); it.value }

        private fun consume(type: TokenType, message: String): Token.Simple =
            match(type) ?: throw error(peek(), message)

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
