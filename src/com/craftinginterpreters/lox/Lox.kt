package com.craftinginterpreters.lox

import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.system.exitProcess

internal object Lox {
    // https://www.freebsd.org/cgi/man.cgi?query=sysexits&apropos=0&sektion=0&manpath=FreeBSD+4.3-RELEASE&format=html
    private const val EX_USAGE = 64
    private const val EX_DATAERR = 65
    private const val EX_SOFTWARE = 70

    private val interpreter = Interpreter
    private var hadError = false
    private var hadRuntimeError = false

    @JvmStatic
    fun main(args: Array<String>) {
        when {
            args.size > 1 -> {
                println("Usage: klox [script]")
                exitProcess(EX_USAGE)
            }

            args.size == 1 -> {
                runFile(args[0])
            }

            else -> {
                runPrompt()
            }
        }
    }

    private fun runFile(path: String) {
        run(Path(path).readText())
        if (hadError) exitProcess(EX_DATAERR)
        if (hadRuntimeError) exitProcess(EX_SOFTWARE)
    }

    private fun runPrompt() {
        while (true) {
            print("> ")
            val line = readlnOrNull() ?: break
            run(line)
            hadError = false
        }
    }

    private fun run(source: String) {
        val tokens = scan(source)
        val expression = parse(tokens)
        if (hadError) return
        expression!!
        interpreter.interpret(expression)
    }

    internal fun error(line: Int, message: String) = report(line, "", message)

    internal fun error(token: Token, message: String) = report(
        token.line, " at ${if (token.type == TokenType.EOF) "end" else "'${token.lexeme}'"}", message
    )

    private fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error$where: $message")
        hadError = true
    }

    internal fun runtimeError(error: RuntimeError) {
        System.err.println("${error.message}\n[line ${error.token.line}]")
        hadRuntimeError = true
    }
}
