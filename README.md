# klox

An interpreter for the Lox programming language from the book [Crafting Interpreters](https://craftinginterpreters.com),
implemented in Kotlin (`klox`) instead of the reference Java (`jlox`).

There are several deviations from the design of `jlox`, striving for better type-safety and more functional style,
utilizing Kotlin features:

* The scanner and the parser are implemented as free functions.
* The flat `Token` type is replaced with more specific subtypes.
* Instead of the visitor pattern for the `Expr` and `Stmt` classes, sealed interfaces and the Kotlin `when`
  expression/statement are used in attempt to emulate a sum type with pattern matching.
* Lox values are represented by a sealed interface instead of being any Java object.

In addition, I opted for
implementing [Chapter 11: Resolving and Binding](https://craftinginterpreters.com/resolving-and-binding.html)
using persistent environments instead of semantic analysis.
This lead to some semantic differences in the interpreter:

* Allowing variables to appear in the initializer of variables with the same name (option 1
  in [§11.3.2 Resolving variable declarations](https://craftinginterpreters.com/resolving-and-binding.html#resolving-variable-declarations)).
* Allowing variables with the same name in the same scope, the latter shadows the former.

Consequently, all the validations performed by the resolver in the book (usage of `return` outside of functions etc.)
are performed instead by the parser.

Additional semantic differences are nicer stringification of function and class values, as well as features from the
following challenges:

* Chapter 4, Challenge 4: Block comments
* Chapter 8, Challenge 1: Allow expressions in prompt
* Chapter 8, Challenge 2: Runtime error for uninitialized variable access
* Chapter 9, Challenge 3: Break statements
* Chapter 10, Challenge 2: Anonymous functions
