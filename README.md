# Introduction
<img src="./resources/parseWorks.png" alt="a nifty looking parse works logo" title="UwU It's a logo!" width="300" align="right">

**parseWorks** is a Java parser combinator framework for constructing [LLR(*) parsers](http://en.wikipedia.org/wiki/LL_parser). This library draws inspiration from Jon Hanson's [ParsecJ](https://github.com/jon-hanson/parsecj) and [FuncJ](https://github.com/typemeta/funcj) libraries.

### Key Features

- **Composable Parser Combinators**: Offers a DSL for constructing parsers.
- **Informative Error Messages**: Pinpoints parse failures effectively.
- **Thread-Safe**: Uses immutable parsers and inputs.
- **Lightweight**: Zero dependencies, except for JUnit in tests.
- **Left-Recursion Failsafe**: Prevents common pitfalls.
- **Looping Empty Input Detection**: Detects infinite loops on empty inputs.

---

# Table of Contents

1. [Introduction](#introduction)
2. [Getting Started](#getting-started)
    1. [Requirements](#requirements)
    2. [Installation](#installation)
3. [Parser Combinators](#parser-combinators)
    1. [Overview](#overview)
    2. [Types](#types)
4. [Existing Parsers](#existing-parsers)
    1. [`Parser` Class Parsers](#parser-class-parsers)
    2. [`Combinators` Class Parsers](#combinators-class-parsers)
    3. [`Text` Class Parsers](#text-class-parsers)
5. [Examples](#examples)
    1. [Simple Expression Parser](#simple-expression-parser)
    2. [Arithmetic Expressions](#arithmetic-expressions)
6. [Advanced Topics](#advanced-topics)
7. [Performance Considerations](#performance-considerations)

---

# Getting Started

## Requirements

parseWorks requires **Java 17** or higher.

## Installation

Add the following dependency to your Maven `pom.xml`:

```xml
<dependency>
   <groupId>io.github.parseworks</groupId>
   <artifactId>parseworks</artifactId>
   <version>0.1.1</version>
</dependency>
```

# Parser Combinators
## Overview
Traditionally, parsers are implemented using tools like Yacc/Bison or ANTLR, which rely on external grammar definitions and code generation. Parser combinators offer an alternative approach by allowing grammar rules to be directly expressed in the host programming language, combining the flexibility of recursive descent parsing with better abstraction and composability.

### Benefits of Parser Combinators

- **Expressiveness**: Grammar rules are written directly in Java.
- **Error Handling**: Automatically integrates error messages.
- **Reusable Building Blocks**: Combinators allow constructing complex parsers from simple ones.

## Types
<img src="./resources/hatter.png" alt="An image of the mad hatter lamenting his lack of using parse works" title="If only I wasn't a fictional character!" width="300"  align="right" >

### `Parser` Type

`Parser<I, A>` defines the core interface for parsers. Use the `Parser.parse` method to apply a parser to an `Input`, returning a `Result`.

#### Recursive Parsers with `Parser.ref()`

Recursive grammars require special handling. Use `Parser.ref()` to create uninitialized parser references, which can be set later to a parser that references itself.

```java
Parser<Character, String> expr = Parser.ref();
Parser<Character, String> temp = chr('X')
        .or(chr('a'))
        .then(expr).then(chr('b')).map(a -> e -> b -> a + e + b);

expr.set(temp);
```

### `Input` Type

`Input<I>` represents a position in a stream of tokens. Typically, the token type `I` is a character (`Chr`).

#### Creating `Input`

```java
char[] charData = { 'A', 'B', 'C', 'D' };

// Construct Input from a char array
Input<Character> chrArrInput = Input.of(charData);
// Construct Input from a String
Input<Character> strInput = Input.of("ABCD");
// Construct Input from a Reader
Input<Character> rdrInput = Input.of(new CharArrayReader(charData));
```

### `Result` Type

`Result<I, A>` encapsulates the outcome of parsing:

- **`Success`**: Contains the parsed value and the next `Input` position.
- **`Failure`**: Contains an error message and the failure position.

#### Example

```java
Result<Character, String> result = expr.parse(Input.of("ABCD"));
var response = result.handle(
        Result::get,
        failure -> "Error: " + failure.fullErrorMessage()
);
```

## Existing Parsers

Here is a sample list of the parsers available in the `Parser`, `Combinators`, and `Text` classes:

### `Parser` Class Parsers
- **`pure(A value)`**: Creates a parser that always succeeds with the given value.
- **`thenSkip(Parser<I, B> pb)`**: Chains this parser with another parser, applying them in sequence. The result of the first parser is returned, and the result of the second parser is ignored.
- **`skipThen(Parser<I, B> pb)`**: Chains this parser with another parser, applying them in sequence. The result of the first parser is ignored, and the result of the second parser is returned.
- **`between(I open, I close)`**: A parser for expressions with enclosing symbols. Validates the open symbol, then this parser, and then the close symbol. If all three succeed, the result of this parser is returned.
- **`fail()`**: Creates a parser that always fails with a generic error message.
- **`many(Parser<I, A> parser)`**: Applies the parser one or more times and collects the results.
- **`zeroOrMany()`**: Applies this parser zero or more times until it fails, and then returns a list of the results. If this parser fails on the first attempt, an empty list is returned.
- **`then(Parser<I, B> next)`**: Chains this parser with another parser, applying them in sequence. The result of the first parser is passed to the second parser.
- **`trim()`**: Trims leading and trailing whitespace from the input, before and after applying this parser.
- **`map(Function<A, R> func)`**: Transforms the result of this parser using the given function.
- **`as(R value)`**: Transforms the result of this parser to a constant value.
- **`not(Parser<I, A> parser)`**: Wraps the 'this' parser to only call it if the provided parser returns a fail.
- **`chain(Parser<I, BinaryOperator<A>> op, Associativity associativity)`**: Chains this parser with an operator parser, applying them in sequence based on the specified associativity. The result of the first parser is combined with the results of subsequent parsers using the operator.
- **`chainRightZeroOrMany(Parser<I, BinaryOperator<A>> op, A a)`**: A parser for an operand, followed by zero or more operands that are separated by operators. The operators are right-associative.
- **`chainRightMany(Parser<I, BinaryOperator<A>> op)`**: Parse right-associative operator expressions.
- **`chainLeftZeroOrMany(Parser<I, BinaryOperator<A>> op, A a)`**: A parser for an operand, followed by zero or more operands that are separated by operators. The operators are left-associative.
- **`chainLeftMany(Parser<I, BinaryOperator<A>> op)`**: A parser for an operand, followed by one or more operands that are separated by operators. The operators are left-associative.
- **`repeat(int target)`**: A parser that applies this parser the `target` number of times. If the parser fails before reaching the target of repetitions, the parser fails.
- **`repeatAtLeast(int target)`**: A parser that applies this parser the `target` number of times. If the parser fails before reaching the target of repetitions, the parser fails.
- **`repeat(int min, int max)`**: A parser that applies this parser between `min` and `max` times. If the parser fails before reaching the minimum number of repetitions, the parser fails.
- **`separatedByZeroOrMany(Parser<I, SEP> sep)`**: A parser that applies this parser zero or more times until it fails, alternating with calls to the separator parser. The results of this parser are collected in a list and returned by the parser.
- **`separatedByMany(Parser<I, SEP> sep)`**: A parser that applies this parser one or more times until it fails, alternating with calls to the separator parser. The results of this parser are collected in a non-empty list and returned by the parser.
- **`optional()`**: Wraps the result of this parser in an `Optional`. If the parser fails, it returns an empty `Optional`.

### `Combinators` Class Parsers
- **`oneOf(Parser<I, T>... parsers)`**: Tries multiple parsers in sequence until one succeeds.
- **`satisfy(Predicate<I> predicate, Function<I, String> errorMessage)`**: Parses a single item that satisfies the given predicate.
- **`satisfy(Predicate<I> predicate, String expectedType)`**: Parses a single item that satisfies the given predicate.

### `Text` Class Parsers
- **`digit()`**: Matches a single numeric character.
- **`letter()`**: Matches a single alphabetic character.
- **`whitespace()`**: Matches a single whitespace character.
- **`word()`**: Matches a sequence of alphabetic characters.
- **`integer()`**: Matches an integer.

---

# Examples

### Simple Expression Parser

Consider a grammar for parsing expressions like `x+y`:

```
sum ::= integer '+' integer
```

#### Implementation

```java
Parser<Character, Integer> sum = 
        number.thenSkip(chr('+')).then(number).map(Integer::sum);

int result = sum.parse(Input.of("1+2")).get();
assert result == 3;
```

#### Error Handling

An error can be caught and handled by the errorOptional method, which returns an `Optional` containing the error message.
```java
sum.parse(Input.of("1+z")).errorOptional().ifPresent(System.out::println);
```
An error can be caught and handled by the handle method, which takes a function for botha success and failure.

```java
var response2 = sum.parse(Input.of("1+z")).handle(
    success -> "Success: no way!",
    failure -> "Error: " + failure.fullErrorMessage()
);
System.out.println(response2);
// Error: Failure at position 2, saw 'z', expected <number>
```

### Arithmetic Expressions

Parsing recursive arithmetic expressions with a single variable `x`:

```
EXPR ::= VAR | NUM | BINEXPR
VAR ::= 'x'
NUM ::= <integer>
BINOP ::= '+' | '-' | '*' | '/'
BINEXPR ::= '(' EXPR BINOP EXPR ')'
```

#### Implementation
The parser can handle arithmetic expressions with variables, numbers, and binary expressions recursively.

```java
enum BinOp {
   ADD { BinaryOperator<Integer> op() { return Integer::sum; } },
   SUB { BinaryOperator<Integer> op() { return (a, b) -> a - b; } },
   MUL { BinaryOperator<Integer> op() { return (a, b) -> a * b; } },
   DIV { BinaryOperator<Integer> op() { return (a, b) -> a / b; } };
   abstract BinaryOperator<Integer> op();
}

Ref<Character, UnaryOperator<Integer>> expr = Parser.ref();

Parser<Character, UnaryOperator<Integer>> var = chr('x').map(x -> v -> v);
Parser<Character, UnaryOperator<Integer>> num = intr.map(i -> v -> i);
Parser<Character, BinOp> binOp = oneOf(
        chr('+').as(BinOp.ADD),
        chr('-').as(BinOp.SUB),
        chr('*').as(BinOp.MUL),
        chr('/').as(BinOp.DIV)
);

Parser<Character, UnaryOperator<Integer>> binExpr = chr('(')
        .skipThen(expr)
        .then(binOp)
        .then(expr.thenSkip(chr(')')))
        .map(left -> op -> right -> x ->  op.op().apply(left.apply(x), right.apply(x)));

expr.set(oneOf(var, num, binExpr));
```

#### Usage

```java
UnaryOperator<Integer> eval = expr.parse(Input.of("(x*((x/2)+x))")).getOrThrow();
int result = eval.apply(4);
assert result == 24;
```

---

# Advanced Topics

### Left-Recursion Handling

Left-recursive grammars are traditionally challenging for recursive-descent parsers. `parseWorks` includes a mechanism to detect and handle left-recursion safely, ensuring parsers remain performant. This is done by having, by default, parsers track the index that they are about to parse, and if parse fails to advance the index on the next execution of that specific parser. Will return a Failure with a message indicating that the parser is stuck in a loop.

### Looping Empty Input Detection
This issue occurs when you have two parsers nested parsers that can succeed with no consumption of input. If the top level parser is iterative, this can lead to infinite loops. To prevent this, `parseWorks` will detect when a parser in a loop fails to proceed and return a Failure with a message indicating that the parser is stuck in a loop.

---

# Performance Considerations

1. **Avoid Excessive Backtracking**: Use predictive parsing wherever possible.
2. **Minimize Intermediate Allocations**: Reuse combinators to reduce overhead.
3. **Benchmark Complex Grammars**: Test performance with realistic data inputs.

---

This guide introduces the essentials of `parseWorks` while providing practical examples and advanced tips. For further details, refer to the [official documentation](#).

