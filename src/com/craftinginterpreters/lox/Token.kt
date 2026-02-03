package com.craftinginterpreters.lox

internal sealed class Token(val line: Int) {
    internal class Eof(line: Int) : Token(line)
    internal sealed class WithLexeme(val lexeme: kotlin.String, line: Int) : Token(line)
    internal class Simple(val type: TokenType, lexeme: kotlin.String, line: Int) : WithLexeme(lexeme, line)
    internal class Number(lexeme: kotlin.String, val value: Double, line: Int) : WithLexeme(lexeme, line)
    internal class String(lexeme: kotlin.String, val value: kotlin.String, line: Int) : WithLexeme(lexeme, line)
}
