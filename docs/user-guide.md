# parseWorks User Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Basic Concepts](#basic-concepts)
4. [Step-by-Step Tutorials](#step-by-step-tutorials)
   1. [Tutorial 1: Creating Your First Parser](#tutorial-1-creating-your-first-parser)
   2. [Tutorial 2: Combining Parsers](#tutorial-2-combining-parsers)
   3. [Tutorial 3: Parsing Structured Data](#tutorial-3-parsing-structured-data)
   4. [Tutorial 4: Error Handling](#tutorial-4-error-handling)
   5. [Tutorial 5: Creating a Calculator Parser](#tutorial-5-creating-a-calculator-parser)
5. [Advanced Usage](#advanced-usage)
   1. [Recursive Parsers](#recursive-parsers)
   2. [Performance Optimization](#performance-optimization)
6. [API Reference](#api-reference)
7. [Troubleshooting](#troubleshooting)
8. [Best Practices](#best-practices)

## Introduction

parseWorks is a Java library for building LLR(*) parsers using parser combinators. Instead of external grammar files or code generation, you define your grammar directly in Java. It's designed to be lightweight, thread-safe, and provides helpful diagnostics when parsing fails.

### Key Features

- **Composable Combinators**: Build complex grammars by nesting simple parsers.
- **Detailed Diagnostics**: Error messages pinpoint exactly where and why a parse failed.
- **Thread-Safe**: Immutable parsers and input streams.
- **Zero Dependencies**: Requires only Java 17+ (JUnit for tests).
- **Failsafes**: Built-in detection for left-recursion and infinite loops on empty inputs.

## Installation

### Maven
Add this to your `pom.xml`:

```xml
<dependency>
   <groupId>io.github.parseworks</groupId>
   <artifactId>parseworks</artifactId>
   <version>2.2.0</version>
</dependency>
```

For the latest `SNAPSHOT`:
```xml
<repositories>
  <repository>
    <id>sonatype-snapshots</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
  </repository>
</repositories>

<dependency>
  <groupId>io.github.parseworks</groupId>
  <artifactId>parseworks</artifactId>
  <version>2.2.1-SNAPSHOT</version>
</dependency>
```

### Gradle
```groovy
implementation 'io.github.parseworks:parseworks:2.2.0'
```

## Basic Concepts

### Parser<I, A>
A `Parser` is a function transforming `Input<I>` into a `Result<I, A>`. Usually, `I` is `Character` for text parsing.

### Input<I>
Wraps the source data (e.g., String, char array) and tracks the current position.

### Result<I, A>
The outcome of a parse attempt:
- **Match**: Success. Contains the value `A` and the remaining `Input`.
- **Failure**: No match. Can be a simple mismatch or a `PartialMatch` (where part of a sequence matched before failing).

## Quick Start

### 1. Basic String Match
The simplest parser matches a literal string.

```java
import static io.github.parseworks.parsers.Combinators.*;

Parser<Character, String> hello = string("hello");
Result<Character, String> result = hello.parse(Input.of("hello world"));

if (result.matches()) {
    System.out.println("Got: " + result.value()); // "hello"
}
```

### 2. Composition with `then`
Use `then`, `skipThen`, and `thenSkip` to sequence parsers.

```java
// Match "hello", skip a space, then match "world"
Parser<Character, String> helloWorld = string("hello")
    .thenSkip(chr(' '))
    .then(string("world"))
    .map(h -> w -> h + " " + w);

System.out.println(helloWorld.parse(Input.of("hello world")).value());
```

### 3. Handling Structured Data
For anything more complex than strings, use `map` to build your domain objects.

```java
class KV {
    final String k, v;
    KV(String k, String v) { this.k = k; this.v = v; }
}

// key=value
Parser<Character, KV> kvParser = Lexical.regex("[a-z]+")
    .thenSkip(chr('='))
    .then(Lexical.regex("[^\\n]*"))
    .map(k -> v -> new KV(k, v));
```

#### Step 4: Parse multiple key-value pairs

```java
// Parser for multiple key-value pairs separated by newlines
Parser<Character, List<KV>> configParser = kvParser
    .oneOrMoreSeparatedBy(Lexical.chr('\n'));

// Parse a configuration file
String config = "server=localhost\nport=8080\nuser=admin";
Result<Character, List<KV>> result = configParser.parse(Input.of(config));

// Process the result
result.handle(
    match -> {
        System.out.println("Configuration loaded:");
        match.value().forEach(kv -> System.out.println("  " + kv.k + ": " + kv.v));
        return null;
    },
    noMatch -> {
        System.err.println("Failed to parse configuration: " + noMatch.error());
        return null;
    }
);
```

### Tutorial 4: Error Handling

By default, parseWorks tries to give useful error messages, but you can improve them by adding labels to your parsers.

#### Using `expecting(...)`

If a parser fails, the error message usually says what it was looking for (e.g., "Expected '='"). Often you want a more high-level description. `expecting(String label)` replaces the default "Expected ..." with your own text.

```java
// Better: "Expected identifier" instead of "Expected [A-Za-z]..."
Parser<Character, String> id = regex("[A-Za-z]+")
    .expecting("identifier");
```

#### Custom Failures

Use `fail(String message)` inside a `flatMap` for validation or `orElse` for specific error cases.

```java
Parser<Character, Integer> positive = number.flatMap(n -> 
    n > 0 ? Parser.pure(n) : fail("a positive number")
);
```

### Tutorial 5: Creating a Calculator Parser

In this tutorial, we'll create a parser for a simple calculator that can evaluate arithmetic expressions.

#### Step 1: Define the expression grammar

Our calculator will support:
- Numbers (integers)
- Addition and subtraction
- Multiplication and division
- Parentheses for grouping

The grammar can be defined as:

```
expr   ::= term ('+' term | '-' term)*
term   ::= factor ('*' factor | '/' factor)*
factor ::= number | '(' expr ')'
number ::= [0-9]+
```

#### Step 2: Create parsers for the basic elements

```java
// Parser for numbers
Parser<Character, Integer> number = Lexical.regex("[0-9]+")
    .map(Integer::parseInt);

// Create references for recursive parsers
Parser<Character, Integer> expr = Parser.ref();
Parser<Character, Integer> term = Parser.ref();
Parser<Character, Integer> factor = Parser.ref();
```

#### Step 3: Define the factor parser

```java
import static io.github.parseworks.parsers.Combinators.oneOf;
import static io.github.parseworks.parsers.Lexical.trim;

// Factor can be a number or an expression in parentheses
Parser<Character, Integer> parenFactor = Lexical.chr('(')
    .skipThen(trim(expr))
    .thenSkip(Lexical.chr(')'));

factor.set(
    trim(oneOf(number, parenFactor))
);
```

#### Step 4: Define the term parser (multiplication and division)

```java
import java.util.function.BinaryOperator;

// Parser for multiplication operator
Parser<Character, BinaryOperator<Integer>> mulOp = trim(Lexical.chr('*'))
    .as((a, b) -> a * b);

// Parser for division operator
Parser<Character, BinaryOperator<Integer>> divOp = trim(Lexical.chr('/'))
    .as((a, b) -> a / b);

// Term handles multiplication and division
term.set(
    factor.chainLeftZeroOrMore(oneOf(mulOp, divOp), 0)
);
```

#### Step 5: Define the expression parser (addition and subtraction)

```java
// Parser for addition operator
Parser<Character, BinaryOperator<Integer>> addOp = trim(Lexical.chr('+'))
    .as(Integer::sum);

// Parser for subtraction operator
Parser<Character, BinaryOperator<Integer>> subOp = trim(Lexical.chr('-'))
    .as((a, b) -> a - b);

// Expression handles addition and subtraction
expr.set(
    term.chainLeftZeroOrMore(oneOf(addOp, subOp), 0)
);
```

#### Step 6: Use the calculator

```java
// Parse and evaluate expressions
String[] expressions = {
    "2 + 3",
    "2 * 3 + 4",
    "2 + 3 * 4",
    "(2 + 3) * 4",
    "8 / 4 / 2"
};

for (String expression : expressions) {
    Result<Character, Integer> result = expr.parseAll(Input.of(expression));
    result.handle(
        match -> {
            System.out.println(expression + " = " + match.value());
            return null;
        },
        noMatch -> {
            System.err.println("Failed to parse " + expression + ": " + noMatch.error());
            return null;
        }
    );
}
```

## Advanced Usage

### Recursive Parsers

Recursive parsers are essential for parsing nested structures like expressions, JSON, XML, etc. parseWorks provides the `Parser.ref()` method to create recursive parsers.

#### Example: JSON Parser

Here's a simplified example of a JSON parser:

```java
import io.github.parseworks.parsers.Lexical;

// Create references for recursive parsers
Parser<Character, Object> jsonValue = Parser.ref();
Parser<Character, Map<String, Object>> jsonObject = Parser.ref();
Parser<Character, List<Object>> jsonArray = Parser.ref();

// Parser for JSON strings
Parser<Character, String> jsonString = Lexical.chr('"')
    .skipThen(
        Combinators.oneOf(
            Combinators.satisfy("<escaped-char>", (Character c) -> c == '\\').skipThen(Combinators.any(Character.class)),
            Combinators.satisfy("<string-char>", (Character c) -> c != '"' && c != '\\')
        ).zeroOrMore()
    )
    .thenSkip(Lexical.chr('"'))
    .map(Lists::join);

// Parser for JSON numbers
Parser<Character, Double> jsonNumber = Lexical.regex("-?[0-9]+(\\.[0-9]+)?")
    .map(Double::parseDouble);

// Parser for JSON booleans
Parser<Character, Boolean> jsonBoolean = Combinators.oneOf(
    Lexical.string("true").as(Boolean.TRUE),
    Lexical.string("false").as(Boolean.FALSE)
);

// Parser for JSON null
Parser<Character, Object> jsonNull = Lexical.string("null").as(null);

// Parser for JSON arrays
jsonArray.set(
    Lexical.chr('[')
        .skipThen(Lexical.trim(jsonValue).zeroOrMoreSeparatedBy(Lexical.trim(Lexical.chr(','))))
        .thenSkip(Lexical.chr(']'))
        .map(values -> (List<Object>) new ArrayList<>(values))
);

// Parser for JSON objects
Parser<Character, Map.Entry<String, Object>> jsonProperty = jsonString
    .thenSkip(Lexical.trim(Lexical.chr(':')))
    .then(jsonValue)
    .map(key -> value -> (Map.Entry<String, Object>) new AbstractMap.SimpleEntry<>(key, value));

jsonObject.set(
    Lexical.chr('{')
        .skipThen(Lexical.trim(jsonProperty).zeroOrMoreSeparatedBy(Lexical.trim(Lexical.chr(','))))
        .thenSkip(Lexical.chr('}'))
        .map(entries -> {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, Object> entry : entries) {
                map.put(entry.getKey(), entry.getValue());
            }
            return map;
        })
);

// Any JSON value
jsonValue.set(
    Combinators.oneOf(
        jsonString.map(s -> (Object) s),
        jsonNumber.map(n -> (Object) n),
        jsonBoolean.map(b -> (Object) b),
        jsonNull,
        jsonObject.map(o -> (Object) o),
        jsonArray.map(a -> (Object) a)
    )
);
```

### Performance Optimization

Here are some tips for optimizing parser performance:

1. **Reuse parsers**: Create parsers once and reuse them instead of creating new ones for each parse operation.

2. **Use `trim` wisely**: The `trim` combinator can be expensive if used excessively. Apply it only where necessary.

3. **Avoid excessive backtracking**: Try to make your parsers more deterministic to reduce backtracking.

4. **Use `oneOf` with care**: When using `oneOf`, order the parsers from most specific to least specific to reduce the number of attempts.

5. **Consider using memoization**: For complex parsers that are called repeatedly, consider implementing memoization to cache results.

## API Reference

### Core Classes

- **Parser<I, A>**: The main interface for parsers
- **Input<I>**: Represents a position in a stream of tokens
- **Result<I, A>**: Represents the outcome of parsing
- **Combinators**: Utility class with methods for creating and combining parsers

### Common Parser Combinators

- **string(String s)**: Creates a parser that recognizes the given string
- **regex(String pattern)**: Creates a parser that recognizes the given regex pattern
- **chr(char c)**: Creates a parser that recognizes the given character
- **oneOf(Parser... parsers)**: Creates a parser that tries each parser in sequence until one succeeds
- **oneOrMore()**: Creates a parser that applies the parser one or more times
- **zeroOrMore()**: Creates a parser that applies the parser zero or more times
- **optional()**: Creates a parser that optionally applies the parser
- **between(Parser open, Parser close)**: Creates a parser that applies the parser between the open and close parsers
- **oneOrMoreSeparatedBy(Parser separator)**: Creates a parser that applies the parser one or more times, separated by the separator parser
- **zeroOrMoreSeparatedBy(Parser separator)**: Creates a parser that applies the parser zero or more times, separated by the separator parser

### Result Handling

- **matches()**: Returns true if the result is a Match
- **error()**: Returns the error if the result is a NoMatch
- **value()**: Returns the parsed value if the result is a Match
- **handle(Function<Result<I, A>, R> onMatch, Function<Result<I, A>, R> onNoMatch)**: Handles both Match and NoMatch cases

## Troubleshooting

### Common Issues

#### Parser doesn't consume all input

If your parser doesn't consume all the input, you might want to use `parse` instead of `parseAll`:

```java
// This will fail if there's unconsumed input
Result<Character, A> result = parser.parseAll(Input.of("input"));
```

#### Left recursion issues

Left recursion can cause infinite loops. parseWorks has built-in protection against this, but it's still best to avoid left recursion when possible. Use right-recursion instead:

```java
// Avoid this (left recursion):
expr.set(expr.then(op).then(term).map(...));

// Use this instead (right recursion):
expr.set(term.then(op.then(expr).optional()).map(...));
```

#### Performance issues with complex parsers

If you're experiencing performance issues with complex parsers, try:

1. Simplifying your grammar
2. Breaking down complex parsers into smaller, reusable components
3. Using more specific parsers instead of general ones
4. Avoiding excessive backtracking

### Debugging Tips

1. **Use `orElse` for better error messages**:
   ```java
   Parser<Character, A> parser = actualParser.orElse(fail("Custom error message"));
   ```

2. **Print intermediate results**:
   ```java
   Parser<Character, A> debugParser = actualParser.map(result -> {
    System.out.println("Parsed: " + result);
    return result;
   });
   ```

3. **Check the error position**:
   ```java
   result.handle(
       match -> { /* ... */ },
       noMatch -> {
           System.err.println("Error at position " + noMatch.input().position());
           System.err.println("Input: " + noMatch.input());
           System.err.println("Message: " + noMatch.error());
           return null;
       }
   );
   ```
   
## Best Practices

1. **Keep it small**: Break complex grammars into small, named parsers. It's easier to test and debug.
2. **Name your parsers**: Instead of nesting everything, use descriptive variable names.
3. **Use `expecting()` at boundaries**: Don't label every single `chr()`, but do label major components like `identifier`, `statement`, or `expression`.
4. **Test edge cases**: Empty input, unexpected characters, and unfinished sequences are where parsers usually fail.
5. **Prefer `Lexical` for speed**: When parsing common patterns like numbers or identifiers, `Lexical.regex` or `Lexical.chr` are often faster and clearer than building them from individual character parsers.
6. **Watch out for backtracking**: The `oneOf` combinator tries parsers in order. Put the most specific or longest matches first to avoid unnecessary work or incorrect matches.