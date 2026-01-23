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

Build on the concepts from the [user guide](user-guide.md) and explore advanced features and techniques in parseWorks. Use this guide when you already understand the fundamentals of parser combinators and want to apply the full power of the library in complex scenarios.

## Advanced Parser Combinators

### Chain Operations

Handling operator precedence and associativity is common in expression parsing. `chainLeftZeroOrMore` and `chainRightZeroOrMore` simplify this by applying a binary operator between parsed elements.

#### left-associativity with `chainLeftZeroOrMore`
Useful for addition, subtraction, etc., where `1+2+3` should be `(1+2)+3`.

```java
Parser<Character, Integer> addition = number.chainLeftZeroOrMore(
    chr('+').as(Integer::sum),
    0
);
```

#### right-associativity with `chainRightZeroOrMore`
Useful for exponentiation, where `2^3^2` should be `2^(3^2)`.

```java
Parser<Character, Integer> power = number.chainRightZeroOrMore(
    chr('^').as((a, b) -> (int)Math.pow(a, b)),
    1
);
```

### Recursive Parsers

For nested structures (JSON, expressions), use `Parser.ref()`. This creates a placeholder that you `set()` later, allowing the parser to refer to itself.

```java
Parser<Character, Expr> expr = Parser.ref();

// Parens: ( expr )
Parser<Character, Expr> parens = chr('(')
    .skipThen(expr)
    .thenSkip(chr(')'));

// A factor is a number OR a parenthesized expression
Parser<Character, Expr> factor = number.or(parens);

// Close the loop
expr.set(factor); 
```

### Repetition Nuances

While `zeroOrMore` and `oneOrMore` are common, you often need stricter bounds.

- `repeat(n)`: Exactly `n` times.
- `repeat(min, max)`: Between `min` and `max` times.
- `zeroOrMoreUntil(end)`: Consume items until the `end` parser matches.

### `takeWhile` and `until`

A common "gotcha": `takeWhile` requires a **parser** that returns `Boolean`, not a simple lambda predicate. This is because the condition itself might need to look ahead or consume input.

```java
// Correct: passing a parser
Parser<Character, Boolean> isAlpha = chr(Character::isLetter).as(true);
Parser<Character, List<Character>> word = any(Character.class).takeWhile(isAlpha);

// Cleaner alternative:
Parser<Character, List<Character>> word2 = chr(Character::isLetter).zeroOrMore();
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
        return fail("positive number");
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

parseWorks provides different ways to fail:

```java
// Syntax error (input doesn't match expected pattern)
Parser<Character, String> syntaxError = fail("Expected a valid syntax");

// Validation error (input parsed but failed validation)
Parser<Character, String> validationError = fail("Expected a valid value");

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

## Performance Tips

1. **Static Reuse**: Define common parsers (keywords, delimiters) as `static final` fields. Creating parsers on-the-fly inside a loop is a major performance killer.
2. **Deterministic Choices**: In `oneOf`, ensure your alternatives are as distinct as possible. If they overlap, put the longest/most specific one first.
3. **Be careful with `trim()`**: It's convenient but adds lookahead/backtracking. Apply it at the "token" level rather than around every single character parser.
4. **Regular Expressions**: `Lexical.regex` is backed by standard Java `Pattern`. It's fast for tokenization but avoid complex, nested groups if a simple `chr()` loop would suffice.

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
    factor.chainLeftZeroOrMore(oneOf(mulOp, divOp), 0)
);

// Expression handles addition and subtraction (lower precedence)
Parser<Character, BinaryOperator<Integer>> addOp = chr('+')
    .as((a, b) -> a + b);
Parser<Character, BinaryOperator<Integer>> subOp = chr('-')
    .as((a, b) -> a - b);

expr.set(
    term.chainLeftZeroOrMore(oneOf(addOp, subOp), 0)
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
            chr('\\').skipThen(any(Character.class)),
            chr(c -> c != '"' && c != '\\')
        ).zeroOrMore()
    )
    .thenSkip(chr('"'))
    .map(Lists::join);

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
        .skipThen(jsonValue.zeroOrMoreSeparatedBy(chr(',')))
        .thenSkip(chr(']'))
        .map(values -> (List<Object>) new ArrayList<>(values))
);

// Parser for JSON objects
jsonObject.set(
    chr('{')
        .skipThen(
            jsonString
                .thenSkip(chr(':'))
                .then(jsonValue)
                .map(key -> value -> (Map.Entry<String, Object>) new AbstractMap.SimpleEntry<>(key, value))
                .zeroOrMoreSeparatedBy(chr(','))
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
        jsonString.map(s -> (Object) s),
        jsonNumber.map(n -> (Object) n),
        jsonBoolean.map(b -> (Object) b),
        jsonNull,
        jsonArray.map(a -> (Object) a),
        jsonObject.map(o -> (Object) o)
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
    .map(left -> op -> right -> (Integer x) -> op.op().apply(left.apply(x), right.apply(x)));

// Set the expression parser to handle variables, numbers, and binary expressions
expr.set(oneOf(var, num, binExpr));

// Use the parser to evaluate expressions with variables
UnaryOperator<Integer> evaluator = expr.parse(Input.of("(x*(x+1))")).value();
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
    .skipThen(chr(c -> c != '"').zeroOrMore())
    .thenSkip(chr('"'))
    .map(Lists::join);

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
    .map(cond -> thenStmts -> (Optional<List<Statement>> elseStmts) -> 
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
    statement.zeroOrMore().map(stmts -> {
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
    assertEquals(0, parser.parse(Input.of("0")).value());
    assertEquals(123456789, parser.parse(Input.of("123456789")).value());
    
    // Test invalid inputs
    assertTrue(!parser.parse(Input.of("abc")).matches());
    assertTrue(!parser.parse(Input.of("")).matches());
    assertTrue(parser.parse(Input.of("42a")).matches());  // Parses "42", leaves "a"
    
    // Test with parseAll (consumes all input)
    assertTrue(parser.parseAll(Input.of("42")).matches());
    assertTrue(!parser.parseAll(Input.of("42a")).matches());  // Fails because "a" is left
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
        return termModule.getParser().chainLeftZeroOrMore(
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
        return factorModule.getParser().chainLeftZeroOrMore(
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
// parseWorks will detect and mitigate this specific example
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
assertTrue(identifier.parse(Input.of("abc123")).matches());

// Add more complex parsers
Parser<Character, Statement> assignStatement = identifier
    .thenSkip(string("="))
    .then(number)
    .thenSkip(chr(';'))
    .map(var -> val -> new AssignStatement(var, val));
assertTrue(assignStatement.parse(Input.of("abc123=42;")).matches());
```

By following these advanced techniques and patterns, you can leverage the full power of parseWorks to create sophisticated parsers for a wide range of applications.