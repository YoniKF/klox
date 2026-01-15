package com.craftinginterpreters.lox

import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.system.exitProcess

internal object Lox {
    private const val EX_USAGE = 64
    private const val EX_DATAERR = 65

    private var hasError = false

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
        if (hasError) exitProcess(EX_DATAERR)
    }

    private fun runPrompt() {
        while (true) {
            print("> ")
            val line = readlnOrNull() ?: break
            run(line)
            hasError = false
        }
    }

    private fun run(source: String) {
        val scanner = Scanner(source)
        val tokens = scanner.scanTokens()
        for (token in tokens) {
            println(token)
        }
    }

    internal fun error(line: Int, message: String) {
        report(line, "", message)
    }

    private fun report(line: Int, where: String, message: String) {
        System.err.println("[line $line] Error$where: $message")
        hasError = true
    }

}
