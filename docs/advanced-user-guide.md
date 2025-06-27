# parseWorks Advanced User Guide

## Table of Contents
1. [Introduction](#introduction)
2. [Advanced Parser Combinators](#advanced-parser-combinators)
   1. [Chain Operations](#chain-operations)
   2. [Recursive Parsers](#recursive-parsers)
   3. [Repetition and Sequences](#repetition-and-sequences)
   4. [Conditional Parsing](#conditional-parsing)
   5. [Negation and Validation](#negation-and-validation)
3. [Error Handling Strategies](#error-handling-strategies)
   1. [Custom Error Messages](#custom-error-messages)
   2. [Error Types](#error-types)
   3. [Recovery Strategies](#recovery-strategies)
4. [Performance Optimization](#performance-optimization)
   1. [Parser Reuse](#parser-reuse)
   2. [Avoiding Excessive Backtracking](#avoiding-excessive-backtracking)
   3. [Optimizing Regular Expressions](#optimizing-regular-expressions)
   4. [Memory Considerations](#memory-considerations)
5. [Working with Complex Grammars](#working-with-complex-grammars)
   1. [Operator Precedence](#operator-precedence)
   2. [Left vs. Right Recursion](#left-vs-right-recursion)
   3. [Handling Ambiguity](#handling-ambiguity)
6. [Advanced Examples](#advanced-examples)
   1. [JSON Parser](#json-parser)
   2. [Expression Evaluator with Variables](#expression-evaluator-with-variables)
   3. [Custom DSL Parser](#custom-dsl-parser)
7. [Integration Patterns](#integration-patterns)
   1. [Combining with Other Libraries](#combining-with-other-libraries)
   2. [Testing Strategies](#testing-strategies)
   3. [Modular Parser Design](#modular-parser-design)
8. [Troubleshooting](#troubleshooting)
   1. [Common Pitfalls](#common-pitfalls)
   2. [Debugging Techniques](#debugging-techniques)

## Introduction

This advanced user guide builds upon the concepts introduced in the [basic user guide](user-guide.md) and explores the more sophisticated features and techniques available in parseWorks. It's designed for users who are already familiar with the fundamentals of parser combinators and want to leverage the full power of the library.

## Advanced Parser Combinators

### Chain Operations

parseWorks provides several chain operations that are particularly useful for handling operator precedence and associativity in expression parsing:

#### chainLeft

The `chainLeft` method creates a parser that applies a binary operator between elements, with left associativity. This is useful for operators like addition and subtraction.

```java
// Parse expressions like "1+2+3" with left associativity (evaluates as ((1+2)+3))
Parser<Character, Integer> expr = number.chainLeft(
    chr('+').as((a, b) -> a + b),
    0  // Default value if no elements are found
);
```

#### chainRight

The `chainRight` method creates a parser that applies a binary operator between elements, with right associativity. This is useful for operators like exponentiation.

```java
// Parse expressions like "2^3^2" with right associativity (evaluates as (2^(3^2)))
Parser<Character, Integer> expr = number.chainRight(
    chr('^').as((a, b) -> (int)Math.pow(a, b)),
    1  // Default value if no elements are found
);
```

#### chain with Associativity

For more control, you can use the `chain` method with an explicit `Associativity` parameter:

```java
// Parse expressions with explicit associativity
Parser<Character, Integer> leftAssoc = number.chain(
    chr('+').as((a, b) -> a + b),
    Associativity.LEFT
);

Parser<Character, Integer> rightAssoc = number.chain(
    chr('^').as((a, b) -> (int)Math.pow(a, b)),
    Associativity.RIGHT
);
```

### Recursive Parsers

Recursive parsers are essential for parsing nested structures like expressions, JSON, XML, etc. parseWorks provides the `Parser.ref()` method to create recursive parsers.

#### Creating Recursive Parsers

```java
// Create a reference for a recursive parser
Parser<Character, Expression> expr = Parser.ref();

// Define the parser for terms (factors with multiplication/division)
Parser<Character, Expression> term = Parser.ref();

// Define the parser for factors (numbers or parenthesized expressions)
Parser<Character, Expression> factor = number.map(NumberExpression::new)
    .or(chr('(').skipThen(expr).thenSkip(chr(')')));

// Set the term parser to handle multiplication and division
term.set(
    factor.chainLeft(
        oneOf(
            chr('*').as((a, b) -> new BinaryExpression(a, Operator.MULTIPLY, b)),
            chr('/').as((a, b) -> new BinaryExpression(a, Operator.DIVIDE, b))
        ),
        null
    )
);

// Set the expression parser to handle addition and subtraction
expr.set(
    term.chainLeft(
        oneOf(
            chr('+').as((a, b) -> new BinaryExpression(a, Operator.ADD, b)),
            chr('-').as((a, b) -> new BinaryExpression(a, Operator.SUBTRACT, b))
        ),
        null
    )
);
```

#### Handling Left Recursion

Left recursion can cause infinite loops. parseWorks has built-in protection against this, but it's still best to avoid left recursion when possible. Use right-recursion instead:

```java
// Avoid this (left recursion):
expr.set(expr.then(op).then(term).map(...));

// Use this instead (right recursion):
expr.set(term.then(op.then(expr).optional()).map(...));
```

### Repetition and Sequences

parseWorks provides several methods for handling repetition and sequences:

#### repeat

The `repeat` method creates a parser that applies the parser exactly n times:

```java
// Parse exactly 3 digits
Parser<Character, FList<Character>> threeDigits = chr(Character::isDigit).repeat(3);
```

#### repeatAtLeast and repeatAtMost

For more flexible repetition:

```java
// Parse at least 1 digit
Parser<Character, FList<Character>> atLeastOneDigit = chr(Character::isDigit).repeatAtLeast(1);

// Parse at most 5 digits
Parser<Character, FList<Character>> atMostFiveDigits = chr(Character::isDigit).repeatAtMost(5);

// Parse between 2 and 4 digits
Parser<Character, FList<Character>> twoToFourDigits = chr(Character::isDigit).repeat(2, 4);
```

#### manySeparatedBy and zeroOrManySeparatedBy

For parsing lists of items separated by a delimiter:

```java
// Parse a comma-separated list of numbers (at least one)
Parser<Character, FList<Integer>> numberList = number.manySeparatedBy(chr(','));

// Parse a comma-separated list of numbers (possibly empty)
Parser<Character, FList<Integer>> optionalNumberList = number.zeroOrManySeparatedBy(chr(','));
```

### Conditional Parsing

#### takeWhile

The `takeWhile` method creates a parser that applies the parser as long as a condition is met:

```java
// Parse characters until a space is encountered
Parser<Character, FList<Character>> word = chr(c -> c != ' ').takeWhile(c -> c != ' ');
```

#### until

The `until` method creates a parser that applies the parser until a terminator is encountered:

```java
// Parse characters until a newline is encountered
Parser<Character, FList<Character>> line = any(Character.class).until(chr('\n'));
```

### Negation and Validation

#### not

The `not` method creates a parser that succeeds if the provided parser fails:

```java
// Parse any character that is not a digit
Parser<Character, Character> notDigit = chr(c -> !Character.isDigit(c));
// Alternative using not:
Parser<Character, Character> notDigit2 = not(chr(Character::isDigit));
```

#### isNot

The `isNot` method creates a parser that succeeds if the current input item is not equal to the provided value:

```java
// Parse any character except 'x'
Parser<Character, Character> notX = isNot('x');
```

#### Validation Errors

For custom validation:

```java
// Parse a positive number
Parser<Character, Integer> positiveNumber = number.flatMap(n -> {
    if (n > 0) {
        return Parser.pure(n);
    } else {
        return failValidation("positive number");
    }
});
```

## Error Handling Strategies

### Custom Error Messages

parseWorks provides several ways to create custom error messages:

#### Using fail

```java
// Create a parser that fails with a custom error message
Parser<Character, String> customError = fail("Expected a specific pattern");
```

#### Using orElse with fail

```java
// Try to parse a number, or fail with a custom error message
Parser<Character, Integer> numberOrError = number.orElse(fail("Expected a number"));
```

#### Error Types

parseWorks provides different error types for different kinds of failures:

```java
// Syntax error (input doesn't match expected pattern)
Parser<Character, String> syntaxError = failSyntax("Expected a valid syntax");

// Validation error (input parsed but failed validation)
Parser<Character, String> validationError = failValidation("Expected a valid value");

// Generic error
Parser<Character, String> genericError = fail("Something went wrong");
```

### Recovery Strategies

#### Using or

The `or` method provides a way to try an alternative parser if the first one fails:

```java
// Try to parse a number, or a string if that fails
Parser<Character, Object> numberOrString = number.map(n -> (Object)n)
    .or(string("null").map(s -> (Object)null));
```

#### Using orElse

The `orElse` method provides a way to return a default value if the parser fails:

```java
// Try to parse a number, or return 0 if that fails
Parser<Character, Integer> numberOrZero = number.orElse(0);
```

#### Using optional

The `optional` method creates a parser that optionally applies the parser:

```java
// Parse an optional sign followed by a number
Parser<Character, Integer> signedNumber = chr('-').optional().then(number)
    .map(sign -> num -> sign.isPresent() ? -num : num);
```

## Performance Optimization

### Parser Reuse

One of the most effective ways to optimize parser performance is to reuse parsers instead of creating new ones for each parse operation:

```java
// Define parsers once
private static final Parser<Character, String> KEYWORD_IF = string("if");
private static final Parser<Character, String> KEYWORD_ELSE = string("else");
private static final Parser<Character, String> KEYWORD_WHILE = string("while");

// Reuse them in multiple places
Parser<Character, String> keyword = oneOf(KEYWORD_IF, KEYWORD_ELSE, KEYWORD_WHILE);
```

### Avoiding Excessive Backtracking

Excessive backtracking can lead to performance issues. Try to make your parsers more deterministic:

```java
// Inefficient: Tries to parse a long string, then backtracks to try a shorter one
Parser<Character, String> inefficient = string("longerString").or(string("short"));

// More efficient: Tries the longer match first, which is more specific
Parser<Character, String> efficient = string("short").or(string("longerString"));
```

### Optimizing Regular Expressions

When using regular expressions, be mindful of their performance characteristics:

```java
// Inefficient: Uses a greedy quantifier that may require backtracking
Parser<Character, String> inefficient = regex(".*end");

// More efficient: Uses a non-greedy quantifier
Parser<Character, String> efficient = regex(".*?end");
```

### Memory Considerations

For parsing large inputs, consider the memory usage of your parsers:

```java
// Memory-intensive: Collects all characters into a list
Parser<Character, FList<Character>> memoryIntensive = any(Character.class).many();

// More memory-efficient: Processes characters one by one
Parser<Character, Integer> memoryEfficient = any(Character.class).many()
    .map(chars -> {
        int count = 0;
        for (Character c : chars) {
            // Process each character individually
            count++;
        }
        return count;
    });
```

## Working with Complex Grammars

### Operator Precedence

Operator precedence can be handled by creating separate parsers for each precedence level:

```java
// Expression grammar with operator precedence:
// expr   ::= term ('+' term | '-' term)*
// term   ::= factor ('*' factor | '/' factor)*
// factor ::= number | '(' expr ')'

// Create references for recursive parsers
Parser<Character, Integer> expr = Parser.ref();
Parser<Character, Integer> term = Parser.ref();
Parser<Character, Integer> factor = Parser.ref();

// Factor can be a number or an expression in parentheses
Parser<Character, Integer> numberFactor = number;
Parser<Character, Integer> parenFactor = chr('(')
    .skipThen(expr)
    .thenSkip(chr(')'));

factor.set(
    oneOf(numberFactor, parenFactor)
);

// Term handles multiplication and division (higher precedence)
Parser<Character, BinaryOperator<Integer>> mulOp = chr('*')
    .as((a, b) -> a * b);
Parser<Character, BinaryOperator<Integer>> divOp = chr('/')
    .as((a, b) -> a / b);

term.set(
    factor.chainLeft(oneOf(mulOp, divOp), 0)
);

// Expression handles addition and subtraction (lower precedence)
Parser<Character, BinaryOperator<Integer>> addOp = chr('+')
    .as((a, b) -> a + b);
Parser<Character, BinaryOperator<Integer>> subOp = chr('-')
    .as((a, b) -> a - b);

expr.set(
    term.chainLeft(oneOf(addOp, subOp), 0)
);
```

### Left vs. Right Recursion

As mentioned earlier, left recursion can cause infinite loops. Here's a more detailed example of how to convert left recursion to right recursion:

```java
// Left-recursive grammar (problematic):
// expr ::= expr '+' term | term

// Right-recursive equivalent:
// expr ::= term ('+' expr)?

// Implementation with right recursion:
Parser<Character, Integer> expr = Parser.ref();
Parser<Character, Integer> term = number;

expr.set(
    term.then(
        chr('+').skipThen(expr).optional()
    ).map(t -> rest -> rest.isPresent() ? t + rest.get() : t)
);
```

### Handling Ambiguity

Ambiguity in grammars can lead to unexpected parsing results. parseWorks uses ordered choice, which means the first matching parser wins:

```java
// This parser will always choose the first matching alternative
Parser<Character, String> ambiguous = string("if").or(string("ifelse"));

// For input "ifelse", it will parse "if" and leave "else" unparsed

// To resolve ambiguity, order parsers from most specific to least specific:
Parser<Character, String> unambiguous = string("ifelse").or(string("if"));

// Now for input "ifelse", it will correctly parse the entire string
```

## Advanced Examples

### JSON Parser

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
            chr('\\').then(any(Character.class)),
            chr(c -> c != '"' && c != '\\')
        ).many()
    )
    .thenSkip(chr('"'))
    .map(chars -> {
        StringBuilder sb = new StringBuilder();
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
Parser<Character, Double> jsonNumber = regex("-?(?:0|[1-9]\\d*)(?:\\.\\d+)?(?:[eE][+-]?\\d+)?")
    .map(Double::parseDouble);

// Parser for JSON booleans
Parser<Character, Boolean> jsonBoolean = string("true").as(true)
    .or(string("false").as(false));

// Parser for JSON null
Parser<Character, Object> jsonNull = string("null").as(null);

// Parser for JSON arrays
jsonArray.set(
    chr('[')
        .skipThen(jsonValue.manySeparatedBy(chr(',')))
        .thenSkip(chr(']'))
        .map(values -> new ArrayList<>(values))
);

// Parser for JSON objects
jsonObject.set(
    chr('{')
        .skipThen(
            jsonString
                .thenSkip(chr(':'))
                .then(jsonValue)
                .map(key -> value -> new AbstractMap.SimpleEntry<>(key, value))
                .manySeparatedBy(chr(','))
        )
        .thenSkip(chr('}'))
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
    oneOf(
        jsonString,
        jsonNumber,
        jsonBoolean,
        jsonNull,
        jsonArray,
        jsonObject
    )
);
```

### Expression Evaluator with Variables

Here's an example of an expression evaluator that can handle variables:

```java
enum BinOp {
    ADD { BinaryOperator<Integer> op() { return Integer::sum; } },
    SUB { BinaryOperator<Integer> op() { return (a, b) -> a - b; } },
    MUL { BinaryOperator<Integer> op() { return (a, b) -> a * b; } },
    DIV { BinaryOperator<Integer> op() { return (a, b) -> a / b; } };
    abstract BinaryOperator<Integer> op();
}

// Create a parser for expressions with variables
Parser<Character, UnaryOperator<Integer>> expr = Parser.ref();

// Parser for variables (x)
Parser<Character, UnaryOperator<Integer>> var = chr('x')
    .map(x -> v -> v);  // Identity function that returns the variable value

// Parser for numbers (constants)
Parser<Character, UnaryOperator<Integer>> num = regex("\\d+")
    .map(Integer::parseInt)
    .map(i -> v -> i);  // Constant function that ignores the variable value

// Parser for binary operators
Parser<Character, BinOp> binOp = oneOf(
    chr('+').as(BinOp.ADD),
    chr('-').as(BinOp.SUB),
    chr('*').as(BinOp.MUL),
    chr('/').as(BinOp.DIV)
);

// Parser for binary expressions
Parser<Character, UnaryOperator<Integer>> binExpr = chr('(')
    .skipThen(expr)
    .then(binOp)
    .then(expr.thenSkip(chr(')')))
    .map(left -> op -> right -> x -> op.op().apply(left.apply(x), right.apply(x)));

// Set the expression parser to handle variables, numbers, and binary expressions
expr.set(oneOf(var, num, binExpr));

// Use the parser to evaluate expressions with variables
UnaryOperator<Integer> evaluator = expr.parse(Input.of("(x*(x+1))")).get();
int result = evaluator.apply(5);  // Evaluates the expression with x = 5
System.out.println(result);  // Output: 30
```

### Custom DSL Parser

Here's an example of a parser for a simple domain-specific language (DSL):

```java
// Define the AST classes
interface Statement {}

class PrintStatement implements Statement {
    private final String message;
    
    public PrintStatement(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
}

class AssignStatement implements Statement {
    private final String variable;
    private final int value;
    
    public AssignStatement(String variable, int value) {
        this.variable = variable;
        this.value = value;
    }
    
    public String getVariable() {
        return variable;
    }
    
    public int getValue() {
        return value;
    }
}

class IfStatement implements Statement {
    private final String condition;
    private final List<Statement> thenBranch;
    private final List<Statement> elseBranch;
    
    public IfStatement(String condition, List<Statement> thenBranch, List<Statement> elseBranch) {
        this.condition = condition;
        this.thenBranch = thenBranch;
        this.elseBranch = elseBranch;
    }
    
    public String getCondition() {
        return condition;
    }
    
    public List<Statement> getThenBranch() {
        return thenBranch;
    }
    
    public List<Statement> getElseBranch() {
        return elseBranch;
    }
}

// Create the parsers
Parser<Character, Statement> statement = Parser.ref();
Parser<Character, List<Statement>> statements = Parser.ref();

// Parser for identifiers
Parser<Character, String> identifier = regex("[a-zA-Z][a-zA-Z0-9]*");

// Parser for numbers
Parser<Character, Integer> number = regex("\\d+").map(Integer::parseInt);

// Parser for strings
Parser<Character, String> stringLiteral = chr('"')
    .skipThen(chr(c -> c != '"').many())
    .thenSkip(chr('"'))
    .map(chars -> {
        StringBuilder sb = new StringBuilder();
        for (Character c : chars) {
            sb.append(c);
        }
        return sb.toString();
    });

// Parser for print statements
Parser<Character, Statement> printStatement = string("print")
    .skipThen(stringLiteral)
    .thenSkip(chr(';'))
    .map(PrintStatement::new);

// Parser for assignment statements
Parser<Character, Statement> assignStatement = identifier
    .thenSkip(string("="))
    .then(number)
    .thenSkip(chr(';'))
    .map(var -> val -> new AssignStatement(var, val));

// Parser for if statements
Parser<Character, Statement> ifStatement = string("if")
    .skipThen(chr('('))
    .skipThen(identifier)
    .thenSkip(chr(')'))
    .thenSkip(chr('{'))
    .then(statements)
    .thenSkip(chr('}'))
    .then(
        string("else")
            .skipThen(chr('{'))
            .skipThen(statements)
            .thenSkip(chr('}'))
            .optional()
    )
    .map(cond -> thenStmts -> elseStmts -> 
        new IfStatement(cond, thenStmts, elseStmts.orElse(Collections.emptyList()))
    );

// Set the statement parser
statement.set(
    oneOf(
        printStatement,
        assignStatement,
        ifStatement
    )
);

// Set the statements parser
statements.set(
    statement.many().map(stmts -> {
        List<Statement> result = new ArrayList<>();
        for (Statement stmt : stmts) {
            result.add(stmt);
        }
        return result;
    })
);

// Parse a simple program
String program = "x = 5; if (x) { print \"x is true\"; } else { print \"x is false\"; }";
List<Statement> ast = statements.parse(Input.of(program)).get();
```

## Integration Patterns

### Combining with Other Libraries

parseWorks can be combined with other libraries to create more powerful parsing solutions:

```java
// Combine with Jackson for JSON processing
Parser<Character, JsonNode> jsonParser = /* JSON parser implementation */;
ObjectMapper mapper = new ObjectMapper();

Parser<Character, MyObject> myObjectParser = jsonParser.map(jsonNode -> {
    try {
        return mapper.treeToValue(jsonNode, MyObject.class);
    } catch (JsonProcessingException e) {
        throw new RuntimeException(e);
    }
});
```

### Testing Strategies

Here are some strategies for testing parsers:

```java
// Test a parser with various inputs
@Test
public void testNumberParser() {
    Parser<Character, Integer> parser = number;
    
    // Test valid inputs
    assertEquals(42, parser.parse(Input.of("42")).get());
    assertEquals(0, parser.parse(Input.of("0")).get());
    assertEquals(123456789, parser.parse(Input.of("123456789")).get());
    
    // Test invalid inputs
    assertTrue(parser.parse(Input.of("abc")).isError());
    assertTrue(parser.parse(Input.of("")).isError());
    assertTrue(parser.parse(Input.of("42a")).isSuccess());  // Parses "42", leaves "a"
    
    // Test with parseAll (consumes all input)
    assertTrue(parser.parseAll(Input.of("42")).isSuccess());
    assertTrue(parser.parseAll(Input.of("42a")).isError());  // Fails because "a" is left
}
```

### Modular Parser Design

For complex parsers, it's often beneficial to use a modular design:

```java
// Define a parser module interface
interface ParserModule<T> {
    Parser<Character, T> getParser();
}

// Implement modules for different parts of the grammar
class ExpressionModule implements ParserModule<Expression> {
    private final TermModule termModule;
    
    public ExpressionModule(TermModule termModule) {
        this.termModule = termModule;
    }
    
    @Override
    public Parser<Character, Expression> getParser() {
        return termModule.getParser().chainLeft(
            oneOf(
                chr('+').as((a, b) -> new BinaryExpression(a, Operator.ADD, b)),
                chr('-').as((a, b) -> new BinaryExpression(a, Operator.SUBTRACT, b))
            ),
            null
        );
    }
}

class TermModule implements ParserModule<Expression> {
    private final FactorModule factorModule;
    
    public TermModule(FactorModule factorModule) {
        this.factorModule = factorModule;
    }
    
    @Override
    public Parser<Character, Expression> getParser() {
        return factorModule.getParser().chainLeft(
            oneOf(
                chr('*').as((a, b) -> new BinaryExpression(a, Operator.MULTIPLY, b)),
                chr('/').as((a, b) -> new BinaryExpression(a, Operator.DIVIDE, b))
            ),
            null
        );
    }
}

// Use dependency injection to wire up the modules
FactorModule factorModule = new FactorModule();
TermModule termModule = new TermModule(factorModule);
ExpressionModule expressionModule = new ExpressionModule(termModule);
Parser<Character, Expression> expressionParser = expressionModule.getParser();
```

## Troubleshooting

### Common Pitfalls

#### Infinite Recursion

One common pitfall is infinite recursion, which can happen when a parser refers to itself directly:

```java
// This will cause infinite recursion in most parser libraries
// parseworks will detect and mitigate this specific example
Parser<Character, String> badRecursion = Parser.ref();
badRecursion.set(badRecursion.or(string("x")));

// Fix: Ensure the recursive parser makes progress before recursing
Parser<Character, String> goodRecursion = Parser.ref();
goodRecursion.set(string("x").then(goodRecursion).optional().map(opt -> "x" + opt.orElse("")));
```

#### Greedy Matching

Another common pitfall is greedy matching, which can consume more input than intended:

```java
// This will greedily consume all characters until the end of input
Parser<Character, String> greedy = regex(".*");

// Fix: Use a non-greedy quantifier or be more specific
Parser<Character, String> nonGreedy = regex(".*?");
Parser<Character, String> specific = regex("[a-zA-Z]+");
```

#### Order of Alternatives

The order of alternatives in `oneOf` can affect the parsing result:

```java
// This will always parse "if" and never "ifelse"
Parser<Character, String> badOrder = oneOf(string("if"), string("ifelse"));

// Fix: Order from most specific to least specific
Parser<Character, String> goodOrder = oneOf(string("ifelse"), string("if"));
```

### Debugging Techniques

#### Tracing

You can add tracing to your parsers to see what's happening during parsing:

```java
// Add tracing to a parser
Parser<Character, Integer> tracedParser = number.map(n -> {
    System.out.println("Parsed number: " + n);
    return n;
});
```

#### Custom Error Messages

Custom error messages can help identify where parsing is failing:

```java
// Add custom error messages
Parser<Character, Integer> withErrorMessage = number.orElse(fail("Expected a number here"));
```

#### Incremental Development

Build your parser incrementally, testing each part as you go:

```java
// Start with simple parsers
Parser<Character, String> identifier = regex("[a-zA-Z][a-zA-Z0-9]*");
assertTrue(identifier.parse(Input.of("abc123")).isSuccess());

// Add more complex parsers
Parser<Character, Statement> assignStatement = identifier
    .thenSkip(string("="))
    .then(number)
    .thenSkip(chr(';'))
    .map(var -> val -> new AssignStatement(var, val));
assertTrue(assignStatement.parse(Input.of("abc123=42;")).isSuccess());
```

By following these advanced techniques and patterns, you can leverage the full power of parseWorks to create sophisticated parsers for a wide range of applications.