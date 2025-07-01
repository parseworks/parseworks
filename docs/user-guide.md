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

parseWorks is a Java parser combinator framework for constructing LLR(*) parsers. It provides a lightweight, composable, and expressive way to build parsers directly in Java, using a style that aligns with Java naming conventions and idioms.

### Key Features

- **Composable Parser Combinators**: Build complex parsers by combining simpler ones
- **Informative Error Messages**: Get detailed information about parse failures
- **Thread-Safe**: All parsers and inputs are immutable
- **Lightweight**: Zero dependencies (except for JUnit in tests)
- **Left-Recursion Failsafe**: Prevents common pitfalls in recursive parsers
- **Looping Empty Input Detection**: Detects infinite loops on empty inputs

## Installation

### Requirements

parseWorks requires Java 17 or higher.

### Maven

Add the following dependency to your Maven `pom.xml`:

```xml
<dependency>
   <groupId>io.github.parseworks</groupId>
   <artifactId>parseworks</artifactId>
   <version>2.2.0</version>
</dependency>
```

### Gradle

Add the following dependency to your Gradle build file:

```groovy
implementation 'io.github.parseworks:parseworks:2.2.0'
```

## Basic Concepts

Before diving into the tutorials, let's understand the core concepts of parseWorks:

### Parser

A `Parser<I, A>` is a function that takes an input of type `I` and produces a result of type `A`. In most cases, `I` will be `Character`, meaning the parser processes text input character by character.

### Input

An `Input<I>` represents a position in a stream of tokens. It provides methods to get the current token, advance to the next token, and check if there are more tokens.

### Result

A `Result<I, A>` represents the outcome of parsing. It can be either:
- A success, containing the parsed value and the remaining input
- A failure, containing an error message and the position where the failure occurred

### Combinators

Combinators are functions that take one or more parsers as input and return a new parser. They allow you to build complex parsers by combining simpler ones.

## Step-by-Step Tutorials

### Tutorial 1: Creating Your First Parser

In this tutorial, we'll create a simple parser that recognizes a specific string.

#### Step 1: Import the necessary classes

```java
import io.github.parseworks.Parser;
import io.github.parseworks.Input;
import io.github.parseworks.Result;
import io.github.parseworks.TextParsers;

import static io.github.parseworks.Combinators.*;
import static io.github.parseworks.TextParsers.alphaNumeric;
```

#### Step 2: Create a parser for a specific string

```java
// Create a parser that recognizes the string "hello"
Parser<Character, String> helloParser = string("hello");
```

#### Step 3: Parse some input

```java
// Parse the input "hello world"
Result<Character, String> result = helloParser.parse(Input.of("hello world"));

// Check if parsing succeeded
if (result.isSuccess()) {
    System.out.println("Parsed: " + result.get());
    System.out.println("Remaining input: " + result.next().current());
} else {
    System.out.println("Parsing failed: " + result.error());
}
```

#### Step 4: Handle the result using the `handle` method

```java
// Alternative way to handle the result
String message = result.handle(
    success -> "Successfully parsed: " + success.get(),
    failure -> "Parsing failed: " + failure.error()
);
System.out.println(message);
```

### Tutorial 2: Combining Parsers

In this tutorial, we'll learn how to combine parsers to create more complex ones.

#### Step 1: Create basic parsers

```java
// Parser for the word "hello"
Parser<Character, String> helloParser = string("hello");

// Parser for the word "world"
Parser<Character, String> worldParser = string("world");

// Parser for whitespace
Parser<Character, String> whitespaceParser = chr(' ').many().map(chars -> {
    StringBuilder sb = new StringBuilder(chars.size());
    for (Character c : chars) {
        sb.append(c);
    }
    return sb.toString();
});
```

#### Step 2: Combine parsers using `then`

```java
// Parser for "hello world"
Parser<Character, String> helloWorldParser = helloParser
    .then(whitespaceParser)
    .then(worldParser)
    .map(hello -> space -> world -> hello + space + world);
```

#### Step 3: Use `skipThen` and `thenSkip` for cleaner combinations

```java
// Parser for "hello world" that ignores whitespace
Parser<Character, String> cleanerParser = helloParser
    .thenSkip(whitespaceParser)
    .then(worldParser)
    .map(hello -> world -> hello + " " + world);
```

#### Step 4: Parse input with the combined parser

```java
Result<Character, String> result = cleanerParser.parse(Input.of("hello    world"));
System.out.println(result.get()); // Outputs: "hello world"
```

### Tutorial 3: Parsing Structured Data

In this tutorial, we'll create a parser for a simple key-value format.

#### Step 1: Define the data structure

```java
class KeyValue {
    private final String key;
    private final String value;

    public KeyValue(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() { return key; }
    public String getValue() { return value; }

    @Override
    public String toString() {
        return key + "=" + value;
    }
}
```

#### Step 2: Create parsers for the components

```java
// Parser for keys (alphanumeric strings)
Parser<Character, String> keyParser = alphaNumeric.many().map(chars -> {
    StringBuilder sb = new StringBuilder(chars.size());
    for (Character c : chars) {
        sb.append(c);
    }
    return sb.toString();
});

// Parser for the equals sign
Parser<Character, Character> equalsParser = chr('=');

// example of a foundational level validation parser
// a Parser for values (any string until end of line)
Parser<Character, String> valueParser = satisfy("<not-newline>", c -> c != '\n').zeroOrMany().map(chars -> {
    StringBuilder sb = new StringBuilder(chars.size());
    for (Character c : chars) {
        sb.append(c);
    }
    return sb.toString();
});
```

#### Step 3: Combine the parsers

```java
// Parser for a key-value pair
Parser<Character, KeyValue> keyValueParser = keyParser
    .thenSkip(equalsParser)
    .then(valueParser)
    .map(key -> value -> new KeyValue(key, value));
```

#### Step 4: Parse multiple key-value pairs

```java
// Parser for multiple key-value pairs separated by newlines
Parser<Character, FList<KeyValue>> configParser = keyValueParser
    .manySeparatedBy(chr('\n'));

// Parse a configuration file
String config = "server=localhost\nport=8080\nuser=admin";
Result<Character, FList<KeyValue>> result = configParser.parse(Input.of(config));

// Process the result
result.handle(
    keyValues -> {
        System.out.println("Configuration loaded:");
        keyValues.forEach(kv -> System.out.println("  " + kv.getKey() + ": " + kv.getValue()));
        return null;
    },
    error -> {
        System.err.println("Failed to parse configuration: " + error.error());
        return null;
    }
);
```

### Tutorial 4: Error Handling

In this tutorial, we'll learn how to handle parsing errors gracefully.

#### Step 1: Create a parser that might fail

```java
// Parser for a JSON-like object
Parser<Character, Map<String, String>> objectParser = chr('{')
    .skipThen(
        keyValueParser.manySeparatedBy(string(","))
    )
    .thenSkip(chr('}'))
    .map(pairs -> {
        Map<String, String> map = new HashMap<>();
        for (KeyValue kv : pairs) {
            map.put(kv.getKey(), kv.getValue());
        }
        return map;
    });
```

#### Step 2: Try parsing valid input

```java
String validInput = "{name=John,age=30}";
Result<Character, Map<String, String>> validResult = objectParser.parse(Input.of(validInput));

validResult.handle(
    map -> {
        System.out.println("Successfully parsed object:");
        map.forEach((k, v) -> System.out.println("  " + k + ": " + v));
        return null;
    },
    error -> {
        System.err.println("Parsing failed: " + error.error());
        return null;
    }
);
```

#### Step 3: Try parsing invalid input

```java
String invalidInput = "{name=John,age=30"; // Missing closing brace
Result<Character, Map<String, String>> invalidResult = objectParser.parse(Input.of(invalidInput));

invalidResult.handle(
    map -> {
        System.out.println("Successfully parsed object:");
        map.forEach((k, v) -> System.out.println("  " + k + ": " + v));
        return null;
    },
    error -> {
        System.err.println("Parsing failed: " + error.fullErrorMessage());
        return null;
    }
);
```

#### Step 4: Provide better error messages

```java
// Improved parser with better error messages
Parser<Character, Map<String, String>> betterParser = chr('{')
    .skipThen(
        keyValueParser.manySeparatedBy(string(","))
            .orElse(Collections.emptyList()) // Default to empty list if parsing fails
    )
    .thenSkip(
        chr('}').orElse(fail("Missing closing brace '}'"))
    )
    .map(pairs -> {
        Map<String, String> map = new HashMap<>();
        for (KeyValue kv : pairs) {
            map.put(kv.getKey(), kv.getValue());
        }
        return map;
    });
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
// Parser for whitespace
Parser<Character, String> whitespace = TextParsers.whitespace.zeroOrMany().map(chars -> {
    StringBuilder sb = new StringBuilder(chars.size());
    for (Character c : chars) {
        sb.append(c);
    }
    return sb.toString();
});

// Parser for numbers
Parser<Character, Integer> number = NumericParsers.number;

// Create references for recursive parsers
Parser<Character, Integer> expr = Parser.ref();
Parser<Character, Integer> term = Parser.ref();
Parser<Character, Integer> factor = Parser.ref();
```

#### Step 3: Define the factor parser

```java


// Factor can be a number or an expression in parentheses
Parser<Character, Integer> numberFactor = number;
Parser<Character, Integer> parenFactor = chr('(')
        .skipThen(TextParsers.trim(expr))
        .thenSkip(chr(')'));

factor.

set(
        oneOf(numberFactor, parenFactor)
        .

trim(whitespace)
);
```

#### Step 4: Define the term parser (multiplication and division)

```java
// Parser for multiplication operator
Parser<Character, Integer> mulOp = chr('*').trim(whitespace)
    .as((a, b) -> a * b);

// Parser for division operator
Parser<Character, Integer> divOp = chr('/').trim(whitespace)
    .as((a, b) -> a / b);

// Term handles multiplication and division
term.set(
    factor.chainLeft(oneOf(mulOp, divOp), 0)
);
```

#### Step 5: Define the expression parser (addition and subtraction)

```java
// Parser for addition operator
Parser<Character, Integer> addOp = chr('+').trim(whitespace)
    .as((a, b) -> a + b);

// Parser for subtraction operator
Parser<Character, Integer> subOp = chr('-').trim(whitespace)
    .as((a, b) -> a - b);

// Expression handles addition and subtraction
expr.set(
    term.chainLeft(oneOf(addOp, subOp), 0)
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
        value -> {
            System.out.println(expression + " = " + value);
            return null;
        },
        error -> {
            System.err.println("Failed to parse " + expression + ": " + error.error());
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
// Create references for recursive parsers
Parser<Character, Object> jsonValue = Parser.ref();
Parser<Character, Map<String, Object>> jsonObject = Parser.ref();
Parser<Character, List<Object>> jsonArray = Parser.ref();

// Parser for JSON strings
Parser<Character, String> jsonString = chr('"')
    .skipThen(
        oneOf(
            satisfy("<escaped-char>", c -> c == '\\').then(any()),
            satisfy("<string-char>", c -> c != '"' && c != '\\')
        ).zeroOrMany()
    )
    .thenSkip(chr('"'))
    .map(chars -> {
        StringBuilder sb = new StringBuilder(chars.size());
        for (int i = 0; i < chars.size(); i++) {
            Character c = chars.get(i);
            if (c == '\\' && i + 1 < chars.size()) {
                Character next = chars.get(++i);
                switch (next) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default: sb.append(next);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    });

// Parser for JSON numbers
Parser<Character, Double> jsonNumber = NumericParsers.doubleValue;

// Parser for JSON booleans
Parser<Character, Boolean> jsonBoolean = oneOf(
    string("true").as(Boolean.TRUE),
    string("false").as(Boolean.FALSE)
);

// Parser for JSON null
Parser<Character, Object> jsonNull = string("null").as(null);

// Parser for JSON arrays
jsonArray.set(
    chr('[')
        .skipThen(jsonValue.manySeparatedBy(chr(',').trim(whitespace)).trim(whitespace))
        .thenSkip(chr(']'))
);
// leverage the static imported TextParsers.trim
// Parser for JSON objects
jsonObject.set(
    chr('{')
        .skipThen(
            jsonString.trim(whitespace)
                .thenSkip(trim(chr(':')))
                .then(jsonValue)
                .map(key -> value -> new AbstractMap.SimpleEntry<>(key, value))
                .manySeparatedBy(trim(chr(',')));
        )
        .thenSkip(trim(chr('}')))
        .map(entries -> {
            Map<String, Object> map = new HashMap<>();
            for (Map.Entry<String, Object> entry : entries) {
                map.put(entry.getKey(), entry.getValue());
            }
            return map;
        })
);

// Set the JSON value parser to handle all JSON value types
jsonValue.set(
    trim(oneOf(
        jsonString,
        jsonNumber,
        jsonBoolean,
        jsonNull,
        jsonArray,
        jsonObject
    ))
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
- **many()**: Creates a parser that applies the parser one or more times
- **zeroOrMany()**: Creates a parser that applies the parser one or more times
- **optional()**: Creates a parser that optionally applies the parser
- **between(Parser open, Parser close)**: Creates a parser that applies the parser between the open and close parsers
- **manySeparatedBy(Parser separator)**: Creates a parser that applies the parser one or more times, separated by the separator parser
- **zeroOrManySeparatedBy(Parser separator)**: Creates a parser that applies the parser zero or more times, separated by the separator parser

### Result Handling

- **isSuccess()**: Returns true if the result is a success
- **isError()**: Returns true if the result is a failure
- **get()**: Returns the parsed value if the result is a success
- **error()**: Returns the error if the result is a failure
- **handle(Function<A, R> onSuccess, Function<Failure<I>, R> onFailure)**: Handles both success and failure cases

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
       success -> { /* ... */ },
       failure -> {
           System.err.println("Error at position " + failure.getPosition());
           System.err.println("Input: " + failure.getInput());
           System.err.println("Message: " + failure.error());
           return null;
       }
   );
   ```
   
## Best Practices

1. **Compose parsers from smaller, reusable components**

   Break down complex parsers into smaller, reusable components. This makes your code more maintainable and easier to test.

2. **Use meaningful names for parsers**

   Give your parsers meaningful names that reflect what they parse. This makes your code more readable and easier to understand.

3. **Handle errors gracefully**

   Provide meaningful error messages when parsing fails. This helps users understand what went wrong and how to fix it.

4. **Test your parsers with a variety of inputs**

   Test your parsers with both valid and invalid inputs to ensure they behave correctly in all cases.

5. **Document your parsers**

   Document your parsers with comments that explain what they parse and how they work. This helps others understand your code.

6. **Use the right combinator for the job**

   Choose the appropriate combinator for each parsing task. For example, use `many()` for zero or more occurrences, `many1()` for one or more occurrences, and `optional()` for optional elements.

7. **Avoid excessive backtracking**

   Excessive backtracking can lead to performance issues. Try to make your parsers more deterministic to reduce backtracking.