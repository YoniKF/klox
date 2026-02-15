package com.craftinginterpreters.lox

internal fun scan(source: String): List<Token> {
    var start = 0
    var current = 0
    var line = 1
    fun atEnd() = current >= source.length
    fun advance() = source[current++]
    fun current() = source[current]
    fun text() = source.substring(start, current)
    fun peek() = source.getOrNull(current)
    fun peekNext() = source.getOrNull(current + 1)
    fun match(expected: Char): Boolean = when {
        atEnd() -> false
        current() != expected -> false
        else -> {
            advance()
            true
        }
    }

    fun advanceWhile(predicate: (Char) -> Boolean) {
        while (peek()?.let(predicate) ?: false) advance()
    }

    val tokens = ArrayList<Token>()

    fun addToken(type: TokenType) {
        tokens += Token.Simple(type, text(), line)
    }

    // A comment goes until the end of the line
    fun comment() = advanceWhile { it != '\n' }


    fun blockComment() {
        while (peek()?.let { it != '*' || peekNext() != '/' } ?: false) {
            if (advance() == '\n') ++line
        }

        if (atEnd()) {
            Lox.error(line, "Unterminated block comment.")
            return
        }

        // The closing "*/"
        advance()
        advance()
    }

    fun string() {
        while (peek()?.let { it != '"' } ?: false) {
            if (advance() == '\n') ++line
        }

        if (atEnd()) {
            Lox.error(line, "Unterminated string.")
            return
        }

        // The closing '"'
        advance()

        // Trim the surrounding quotes
        val value = source.substring(start + 1, current - 1)
        tokens += Token.String(text(), value, line)
    }

    fun number() {
        advanceWhile(::isDigit)
        // Look for a fractional part
        if (peek() == '.' && peekNext()?.let(::isDigit) ?: false) {
            // Consume the '.'
            advance()
            advanceWhile(::isDigit)
        }

        val value = text().toDouble()
        tokens += Token.Number(text(), value, line)
    }

    fun identifier() {
        advanceWhile(::isAlphaNumeric)
        val text = text()
        tokens += if (text == "this") Token.This(text, line)
        else keywords[text]?.let { Token.Simple(it, text(), line) } ?: Token.Identifier(text, line)
    }

    while (!atEnd()) {
        start = current
        when (val c = advance()) {
            '(' -> addToken(TokenType.LEFT_PAREN)
            ')' -> addToken(TokenType.RIGHT_PAREN)
            '{' -> addToken(TokenType.LEFT_BRACE)
            '}' -> addToken(TokenType.RIGHT_BRACE)
            ',' -> addToken(TokenType.COMMA)
            '.' -> addToken(TokenType.DOT)
            '-' -> addToken(TokenType.MINUS)
            '+' -> addToken(TokenType.PLUS)
            ';' -> addToken(TokenType.SEMICOLON)
            '*' -> addToken(TokenType.STAR)

            '!' -> addToken(if (match('=')) TokenType.BANG_EQUAL else TokenType.BANG)
            '=' -> addToken(if (match('=')) TokenType.EQUAL_EQUAL else TokenType.EQUAL)
            '<' -> addToken(if (match('=')) TokenType.LESS_EQUAL else TokenType.LESS)
            '>' -> addToken(if (match('=')) TokenType.GREATER_EQUAL else TokenType.GREATER)

            '/' -> {
                if (match('/')) {
                    comment()
                } else if (match('*')) {
                    blockComment()
                } else {
                    addToken(TokenType.SLASH)
                }
            }

            ' ', '\r', '\t' -> {}

            '\n' -> line++

            '"' -> string()

            else if isDigit(c) -> number()

            else if isAlpha(c) -> identifier()

            else -> Lox.error(line, "Unexpected character.")
        }
    }

    tokens += Token.Eof(line)
    return tokens
}

private fun isDigit(c: Char) = c in '0'..'9'
private fun isAlpha(c: Char) = c in 'a'..'z' || c in 'A'..'Z' || c == '_'
private fun isAlphaNumeric(c: Char) = isAlpha(c) || isDigit(c)

private val keywords = mapOf(
    "and" to TokenType.AND,
    "break" to TokenType.BREAK,
    "class" to TokenType.CLASS,
    "else" to TokenType.ELSE,
    "false" to TokenType.FALSE,
    "for" to TokenType.FOR,
    "fun" to TokenType.FUN,
    "if" to TokenType.IF,
    "nil" to TokenType.NIL,
    "or" to TokenType.OR,
    "print" to TokenType.PRINT,
    "return" to TokenType.RETURN,
    "super" to TokenType.SUPER,
    "true" to TokenType.TRUE,
    "var" to TokenType.VAR,
    "while" to TokenType.WHILE,
)
