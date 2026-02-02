package com.craftinginterpreters.lox

internal class ParseError : RuntimeException()

internal fun parse(tokens: List<Token>): List<Stmt> {
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
                return statement()
            } catch (_: ParseError) {
                synchronize()
                return null
            }
        }

        private fun varDeclaration(): Stmt {
            val name = consume(TokenType.IDENTIFIER, "Expect variable name.")
            val initializer = if (match(TokenType.EQUAL)) expression() else null
            consume(TokenType.SEMICOLON, "Expect ';' after variable declaration.")
            return Stmt.Var(name, initializer)
        }

        private fun statement(): Stmt {
            if (match(TokenType.PRINT)) return printStatement()
            return expressionStatement()
        }

        private fun printStatement(): Stmt {
            val expr = expression()
            consume(TokenType.SEMICOLON, "Expect ';' after value.")
            return Stmt.Print(expr)
        }

        private fun expressionStatement(): Stmt {
            val expr = expression()
            consume(TokenType.SEMICOLON, "Expect ';' after expression.")
            return Stmt.Expression(expr)
        }

        private fun expression() = assignment()

        private fun assignment(): Expr {
            val expr = equality()
            if (match(TokenType.EQUAL)) {
                val equal = previous()
                val value = assignment()
                when (expr) {
                    is Expr.Variable -> return Expr.Assign(expr.name, value)
                    else -> error(equal, "Invalid assignment target.")
                }
            }
            return expr
        }

        private fun binary(next: () -> Expr, vararg operators: TokenType): () -> Expr = {
            var expr = next()
            while (match(*operators)) {
                val operator = previous()
                val right = next()
                expr = Expr.Binary(expr, operator, right)
            }
            expr
        }

        private val factor = binary(::unary, TokenType.SLASH, TokenType.STAR)
        private val term = binary(factor, TokenType.MINUS, TokenType.PLUS)
        private val comparison =
            binary(term, TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)
        private val equality = binary(comparison, TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)

        private fun unary(): Expr {
            if (match(TokenType.BANG, TokenType.MINUS)) {
                val operator = previous()
                val right = unary()
                return Expr.Unary(operator, right)
            }
            return primary()
        }

        private fun primary(): Expr {
            if (match(TokenType.FALSE)) return Expr.Literal(false)
            if (match(TokenType.TRUE)) return Expr.Literal(true)
            if (match(TokenType.NIL)) return Expr.Literal(null)

            if (match(TokenType.NUMBER, TokenType.STRING)) return Expr.Literal(previous().literal)
            if (match(TokenType.IDENTIFIER)) return Expr.Variable(previous())

            if (match(TokenType.LEFT_PAREN)) {
                val expr = expression()
                consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
                return Expr.Grouping(expr)
            }

            throw error(peek(), "Expect expression.")
        }

        private var current = 0
        private fun peek() = tokens[current]
        private fun previous() = tokens[current - 1]
        private fun check(type: TokenType) = peek().type == type
        private fun atEnd() = check(TokenType.EOF)

        private fun advance(): Token {
            if (!atEnd()) ++current
            return previous()
        }

        private fun match(vararg types: TokenType) = (peek().type in types).also { if (it) advance() }

        private fun consume(type: TokenType, message: String): Token {
            if (check(type)) return advance()
            throw error(peek(), message)
        }

        private fun error(token: Token, message: String): ParseError {
            Lox.error(token, message)
            return ParseError()
        }

        private fun synchronize() {
            advance()
            while (!atEnd()) {
                if (previous().type == TokenType.SEMICOLON) return
                when (peek().type) {
                    TokenType.CLASS, TokenType.FUN, TokenType.VAR, TokenType.FOR, TokenType.IF, TokenType.WHILE, TokenType.PRINT, TokenType.RETURN -> return

                    else -> advance()
                }
            }
        }
    }

    return parser.parse()
}
