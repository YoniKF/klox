package com.craftinginterpreters.lox

internal class ParseError : RuntimeException()

internal fun parse(tokens: List<Token>): Expr? {
    val parser = object {
        var current = 0

        fun expression() = equality()

        fun binary(next: () -> Expr, vararg operators: TokenType): () -> Expr = {
            var expr = next()
            while (match(*operators)) {
                val operator = previous()
                val right = next()
                expr = Expr.Binary(expr, operator, right)
            }
            expr
        }

        val factor = binary(::unary, TokenType.SLASH, TokenType.STAR)
        val term = binary(factor, TokenType.MINUS, TokenType.PLUS)
        val comparison = binary(term, TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL)
        val equality = binary(comparison, TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL)

        fun unary(): Expr {
            if (match(TokenType.BANG, TokenType.MINUS)) {
                val operator = previous()
                val right = unary()
                return Expr.Unary(operator, right)
            }
            return primary()
        }

        fun primary(): Expr {
            if (match(TokenType.FALSE)) return Expr.Literal(false)
            if (match(TokenType.TRUE)) return Expr.Literal(true)
            if (match(TokenType.NIL)) return Expr.Literal(null)

            if (match(TokenType.NUMBER, TokenType.STRING)) return Expr.Literal(previous().literal)

            if (match(TokenType.LEFT_PAREN)) {
                val expr = expression()
                consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.")
                return Expr.Grouping(expr)
            }

            throw error(peek(), "Expect expression.")
        }

        fun peek() = tokens[current]
        fun previous() = tokens[current - 1]
        fun check(type: TokenType) = peek().type == type
        fun atEnd() = check(TokenType.EOF)

        fun advance(): Token {
            if (!atEnd()) ++current
            return previous()
        }

        fun match(vararg types: TokenType) = (peek().type in types).also { if (it) advance() }

        fun consume(type: TokenType, message: String): Token {
            if (check(type)) return advance()
            throw error(peek(), message)
        }

        fun error(token: Token, message: String): ParseError {
            Lox.error(token, message)
            return ParseError()
        }

        @Suppress("unused")
        fun synchronize() {
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

    return try {
        parser.expression()
    } catch (_: ParseError) {
        null
    }
}
