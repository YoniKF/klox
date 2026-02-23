package com.craftinginterpreters.lox

internal enum class TokenType {
    // Commented out are types from the book which were given their own class.

    // Single-character tokens.
    LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
    COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

    // One or two character tokens.
    BANG, BANG_EQUAL,
    EQUAL, EQUAL_EQUAL,
    GREATER, GREATER_EQUAL,
    LESS, LESS_EQUAL,

    // Literals.
    // IDENTIFIER, STRING, NUMBER,

    // Keywords.
    AND, BREAK, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
    PRINT, RETURN, SUPER, /*THIS,*/ TRUE, VAR, WHILE,

    // EOF
}

internal sealed class Token(val line: Int) {
    internal class Eof(line: Int) : Token(line)
    internal sealed class WithLexeme(val lexeme: kotlin.String, line: Int) : Token(line)
    internal class Simple(val type: TokenType, lexeme: kotlin.String, line: Int) : WithLexeme(lexeme, line)
    internal class Number(lexeme: kotlin.String, val value: Double, line: Int) : WithLexeme(lexeme, line)
    internal class String(lexeme: kotlin.String, val value: kotlin.String, line: Int) : WithLexeme(lexeme, line)
    internal sealed class EnvironmentKey(lexeme: kotlin.String, line: Int) : WithLexeme(lexeme, line)
    internal class Identifier(lexeme: kotlin.String, line: Int) : EnvironmentKey(lexeme, line)
    internal class This(lexeme: kotlin.String, line: Int) : EnvironmentKey(lexeme, line)
}
