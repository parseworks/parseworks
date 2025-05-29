package io.github.parseworks;

import io.github.parseworks.impl.IntObjectMap;
import io.github.parseworks.impl.parser.NoCheckParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import static io.github.parseworks.Combinators.is;

/**
 * <p>
 * The `Parser` class represents a parser that can parse input of type `I` and produce a result of type `A`.
 * </p>
 * This class provides various methods for creating parsers, applying them to input, and combining them with other parsers.
 * This is a thread-safe class that can be used to create parsers for different types of input.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 * @author jason bailey
 * @version $Id: $Id
 */
public class Parser<I, A> {

    /**
     * Transforms the result of this parser to a constant value, regardless of the actual parsed result.
     * <p>
     * The {@code as} method provides a way to acknowledge that a particular pattern was successfully
     * parsed, but replace its value with a predefined constant. This is useful when:
     * <ol>
     *   <li>You need to recognize a pattern but care only about its presence, not its content</li>
     *   <li>You want to map specific token patterns to semantic constants</li>
     *   <li>You need to convert parsing results to domain-specific enumerations or values</li>
     * </ol>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>First applies this parser to the input</li>
     *   <li>If this parser succeeds, its result is discarded and replaced with the specified value</li>
     *   <li>If this parser fails, the failure is propagated without providing the constant value</li>
     *   <li>The input position advances according to the original parser's consumption</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse "true" or "false" strings and convert them directly to boolean values
     * Parser<Character, Boolean> trueParser = string("true").as(Boolean.TRUE);
     * Parser<Character, Boolean> falseParser = string("false").as(Boolean.FALSE);
     * Parser<Character, Boolean> boolParser = trueParser.or(falseParser);
     *
     * // Succeeds with true for input "true"
     * // Succeeds with false for input "false"
     * // Fails for any other input
     * }</pre>
     *
     * @param value the constant value to return when this parser succeeds
     * @param <R>   the type of the constant value
     * @return a parser that returns the constant value when this parser succeeds
     * @see #map(Function) for transforming results with a function
     * @see #skipThen(Parser) for discarding this parser's result
     */
    public <R> Parser<I, R> as(R value) {
        return this.skipThen(pure(value));
    }

    /**
     * Creates a parser that always succeeds with the given constant value without consuming input.
     * <p>
     * The {@code pure} method creates a parser that ignores its input and always succeeds with
     * the specified value. The parsing process works as follows:
     * <ol>
     *   <li>When applied to any input, immediately succeeds without examining the input</li>
     *   <li>Returns a success result containing the specified value</li>
     *   <li>Preserves the original input position (does not consume any input)</li>
     * </ol>
     * <p>
     * This method is a fundamental building block for parser composition, implementing the
     * "pure" operation of the applicative functor pattern. It lifts plain values into the
     * parser context, enabling them to be combined with other parsers.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses an optimized {@code NoCheckParser} implementation for efficiency</li>
     *   <li>Always succeeds regardless of the input</li>
     *   <li>Does not modify or consume the input in any way</li>
     *   <li>Often used with {@link ApplyBuilder#apply} methods to combine parsers functionally</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser that always returns the value 42
     * Parser<Character, Integer> constant = Parser.pure(42);
     *
     * // Combine with other parsers in a data transformation pipeline
     * Parser<Character, User> userParser =
     *     Parser.pure(User::new)
     *           .apply(string("name:").skipThen(stringLiteral))
     *           .apply(string("age:").skipThen(intr));
     *
     * // Can be used to create empty containers for collection operations
     * Parser<Character, List<Integer>> emptyList = Parser.pure(new ArrayList<>());
     * }</pre>
     *
     * @param value the constant value that the parser will always return
     * @param <I>   the type of the input symbols (which will be ignored)
     * @param <A>   the type of the constant value
     * @return a parser that always succeeds with the given value
     * @see ApplyBuilder#apply(Function, Parser) for applying functions to parser results
     * @see ApplyBuilder#apply(Parser, Object) for applying parser-produced functions to values
     * @see ApplyBuilder for combining multiple parsers with transformation functions
     */
    public static <I, A> Parser<I, A> pure(A value) {
        return new NoCheckParser<>(in -> Result.success(in, value));
    }

    /**
     * A parser for expressions with enclosing bracket symbols.
     * Validates the open bracket, then this parser, and then the close bracket.
     * If all three succeed, the result of this parser is returned.
     * <p>
     * This method is useful for parsing expressions that are enclosed by the same bracket symbol,
     * such as parentheses, brackets, or quotes.
     *
     * @param bracket the bracket symbol
     * @return a parser for expressions with enclosing bracket symbols
     */
    public Parser<I, A> between(I bracket) {
        return between(bracket, bracket);
    }

    /**
     * A parser for expressions with enclosing symbols.
     * Validates the open symbol, then this parser, and then the close symbol.
     * If all three succeed, the result of this parser is returned.
     *
     * @param open  the open symbol
     * @param close the close symbol
     * @return a parser for expressions with enclosing symbols
     */
    public Parser<I, A> between(I open, I close) {
        return is(open).skipThen(this).thenSkip(is(close));
    }

    /**
     * Chains this parser with another parser, applying them in sequence and returning only the result of this parser.
     * <p>
     * The {@code thenSkip} method creates a parser that applies two parsers in sequence but keeps only the result
     * of the first parser. This is useful when certain syntax elements must be consumed but their values aren't needed
     * in the result. The parsing process works as follows:
     * <ol>
     *   <li>First applies this parser to the input</li>
     *   <li>If this parser succeeds, applies the second parser to the remaining input</li>
     *   <li>If both parsers succeed, returns the result of the first parser only</li>
     *   <li>If either parser fails, the entire composite parser fails</li>
     * </ol>
     * <p>
     * This method is particularly useful for parsing structures with required terminators, delimiters, or other
     * syntax elements that must be present but whose values aren't semantically important. Common examples include
     * semicolons at the end of statements, closing brackets, or trailing punctuation.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Both parsers must succeed for the composite parser to succeed</li>
     *   <li>Input is consumed by both parsers when successful</li>
     *   <li>Only the result value from the first parser is returned</li>
     *   <li>Implemented as a special case of {@link #then} with a mapping function that discards the second result</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse an integer followed by a semicolon, keeping only the integer
     * Parser<Character, Integer> number = intr;
     * Parser<Character, Character> semicolon = chr(';');
     * Parser<Character, Integer> statement = number.thenSkip(semicolon);
     *
     * // Succeeds with 42 for input "42;"
     * // Fails for input "42" (missing semicolon)
     * // Fails for input ";42" (wrong order)
     * }</pre>
     *
     * @param pb  the next parser to apply in sequence (whose result will be discarded)
     * @param <B> the type of the result of the next parser
     * @return a parser that applies this parser and then the next parser, returning only the result of this parser
     * @see #skipThen(Parser) for the opposite operation (discard first result, keep second)
     * @see #then(Parser) for keeping both parser results
     */
    public <B> Parser<I, A> thenSkip(Parser<I, B> pb) {
        return this.then(pb).map((a, b) -> a);
    }

    /**
     * Chains this parser with another parser, applying them in sequence and returning only the result of the second parser.
     * <p>
     * The {@code skipThen} method creates a parser that applies two parsers in sequence but keeps only the result
     * of the second parser. This is useful when certain syntax elements must be consumed but their values aren't needed
     * in the result. The parsing process works as follows:
     * <ol>
     *   <li>First applies this parser to the input</li>
     *   <li>If this parser succeeds, applies the second parser to the remaining input</li>
     *   <li>If both parsers succeed, returns the result of the second parser only</li>
     *   <li>If either parser fails, the entire composite parser fails</li>
     * </ol>
     * <p>
     * This method is particularly useful for parsing structures with required prefixes, introducer tokens, or other
     * syntax elements that must be present but whose values aren't semantically important. Common examples include
     * keywords preceding values, opening delimiters, or syntactic markers that indicate the start of a construct.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Both parsers must succeed for the composite parser to succeed</li>
     *   <li>Input is consumed by both parsers when successful</li>
     *   <li>Only the result value from the second parser is returned</li>
     *   <li>The first parser is used purely for its side effect of consuming input</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a "key:" prefix followed by an integer, keeping only the integer
     * Parser<Character, String> keyPrefix = string("key:");
     * Parser<Character, Integer> number = intr;
     * Parser<Character, Integer> keyValue = keyPrefix.skipThen(number);
     *
     * // Succeeds with 42 for input "key:42"
     * // Fails for input "42" (missing required prefix)
     * // Fails for input "key:" (missing required value)
     * }</pre>
     *
     * @param pb  the next parser to apply in sequence (whose result will be kept)
     * @param <B> the type of the result of the next parser
     * @return a parser that applies this parser and then the next parser, returning only the result of the next parser
     * @see #thenSkip(Parser) for the opposite operation (keep first result, discard second)
     * @see #then(Parser) for keeping both parser results
     */
    public <B> Parser<I, B> skipThen(Parser<I, B> pb) {
        return new Parser<>(in -> {
            Result<I, A> left = this.apply(in);
            if (left.isError()) {
                return left.cast();
            }
            return pb.apply(left.next());
        });
    }

    /**
     * Chains this parser with another parser, applying them in sequence and allowing for
     * further parser composition.
     * <p>
     * The {@code then} method is a fundamental parser combinator that enables sequential
     * parsing operations. When parsers are chained using this method:
     * <ol>
     *   <li>First, this parser is applied to the input</li>
     *   <li>If this parser succeeds, the next parser is applied to the remaining input</li>
     *   <li>The result is an {@code ApplyBuilder} that allows further parser composition
     *       and eventually mapping the results to a final value</li>
     * </ol>
     * <p>
     * Unlike {@code thenSkip} or {@code skipThen}, this method preserves both parse results
     * for later processing through the {@code map} methods of the returned builder.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a pair of digit and letter, returning them as a Pair
     * Parser<Character, Character> digit = chr(Character::isDigit);
     * Parser<Character, Character> letter = chr(Character::isLetter);
     *
     * Parser<Character, Pair<Character, Character>> digitLetterPair =
     *     digit.then(letter).map((d, l) -> new Pair<>(d, l));
     *
     * // Succeeds with Pair('1', 'a') for input "1a"
     * // Fails for input "a1" or "12"
     * }</pre>
     *
     * @param next the next parser to apply in sequence
     * @param <B>  the type of the result of the next parser
     * @return an ApplyBuilder that allows further parser composition
     * @throws IllegalArgumentException if the next parameter is null
     * @see #thenSkip(Parser) for keeping only this parser's result
     * @see #skipThen(Parser) for keeping only the next parser's result
     */
    public <B> ApplyBuilder<I, A, B> then(Parser<I, B> next) {
        return ApplyBuilder.of(this, next);
    }

    /**
     * A parser for expressions with enclosing bracket symbols.
     * Validates the open bracket, then this parser, and then the close bracket.
     * If all three succeed, the result of this parser is returned.
     *
     * @param bracket the bracket symbol
     * @return a parser for expressions with enclosing bracket symbols
     */
    public <B> Parser<I, A> between(Parser<I, B> bracket) {
        return between(bracket, bracket);
    }

    /**
     * A parser for expressions with enclosing symbols.
     * Validates the open symbol, then this parser, and then the close symbol.
     * If all three succeed, the result of this parser is returned.
     *
     * @param open  the open symbol
     * @param close the close symbol
     * @return a parser for expressions with enclosing symbols
     */
    public <B, C> Parser<I, A> between(Parser<I, B> open, Parser<I, C> close) {
        return open.skipThen(this).thenSkip(close);
    }

    /**
     * Creates a parser for left-associative operator expressions that succeeds even when no operands are found.
     * <p>
     * The {@code chainLeftZeroOrMany} method extends {@link #chainLeftMany(Parser)} to handle the case
     * where no operands are present in the input by providing a default value. It processes the input as follows:
     * <ol>
     *   <li>First attempts to parse a left-associative operator expression using {@code chainLeftMany}</li>
     *   <li>If successful, returns the parsed expression value</li>
     *   <li>If parsing fails (no valid expression found), returns the provided default value</li>
     * </ol>
     * <p>
     * This method is particularly useful for handling optional expressions or providing default values
     * in grammar rules where an expression might not be present. The left associativity means that
     * operators are evaluated from left to right. For example, "a-b-c" is interpreted as "(a-b)-c".
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Combines {@link #chainLeftMany(Parser)} with {@link #or(Parser)} and {@link #pure(Object)}</li>
     *   <li>No input is consumed if the expression cannot be parsed</li>
     *   <li>Always succeeds, either with the parsed result or the default value</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse arithmetic expressions with left-associative subtraction
     * Parser<Character, Integer> number = intr;
     * Parser<Character, BinaryOperator<Integer>> subtract =
     *     chr('-').as((a, b) -> a - b);
     *
     * // Parse subtraction expression or return 0 if none found
     * Parser<Character, Integer> expression =
     *     number.chainLeftZeroOrMany(subtract, 0);
     *
     * // Parses "5-3-2" as (5-3)-2 = 0
     * // Returns 0 for empty input
     * }</pre>
     *
     * @param op the parser that recognizes and returns binary operators
     * @param a  the default value to return if no expression can be parsed
     * @return a parser that handles left-associative expressions or returns the default value
     * @throws IllegalArgumentException if the operator parser is null
     * @see #chainLeftMany(Parser) for the version that requires at least one operand
     * @see #chainRightZeroOrMany(Parser, Object) for the right-associative equivalent
     * @see Associativity for associativity options
     */
    public Parser<I, A> chainLeftZeroOrMany(Parser<I, BinaryOperator<A>> op, A a) {
        return this.chainLeftMany(op).or(pure(a));
    }

    /**
     * Creates a parser for left-associative operator expressions requiring at least one operand.
     * <p>
     * The {@code chainLeftMany} method provides specialized support for parsing expressions
     * with left associativity, which means operators are evaluated from left to right. For example,
     * in "a-b-c", the operations are grouped as "(a-b)-c" rather than "a-(b-c)".
     * <p>
     * This method is particularly useful for operators that naturally associate left-to-right,
     * such as:
     * <ul>
     *   <li>Arithmetic operators like addition and subtraction (e.g., 5-3-2 = (5-3)-2 = 0)</li>
     *   <li>Function application (e.g., f(g(x)) is evaluated as (f applied to (g applied to x)))</li>
     *   <li>Method chaining (e.g., a.b().c() is evaluated as (a.b()).c())</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ol>
     *   <li>First applies this parser to get the initial operand</li>
     *   <li>Then repeatedly tries to parse an operator followed by another operand</li>
     *   <li>Combines the results from left to right using the binary operators</li>
     *   <li>Fails if no valid expression is found</li>
     * </ol>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse arithmetic expressions with left-associative subtraction
     * Parser<Character, Integer> number = intr;
     * Parser<Character, BinaryOperator<Integer>> subtract =
     *     chr('-').as((a, b) -> a - b);
     *
     * Parser<Character, Integer> expression = number.chainLeftMany(subtract);
     *
     * // Parses "5-3-2" as (5-3)-2 = 0
     * // Parses "7" as simply 7
     * // Fails for empty input
     * }</pre>
     *
     * @param op the parser that recognizes and returns binary operators
     * @return a parser that handles left-associative expressions with at least one operand
     * @throws IllegalArgumentException if the operator parser is null
     * @see #chain(Parser, Associativity) for the more general method with explicit associativity
     * @see #chainLeftZeroOrMany(Parser, Object) for a version that provides a default value
     * @see #chainRightMany(Parser) for the right-associative equivalent
     */
    public Parser<I, A> chainLeftMany(Parser<I, BinaryOperator<A>> op) {
        return chain(op, Associativity.LEFT);
    }

    /**
     * Creates a parser for right-associative operator expressions that succeeds even when no operands are found.
     * <p>
     * The {@code chainRightZeroOrMany} method extends {@link #chainRightMany(Parser)} to handle the case
     * where no operands are present in the input by providing a default value. It processes the input as follows:
     * <ol>
     *   <li>First attempts to parse a right-associative operator expression using {@code chainRightMany}</li>
     *   <li>If successful, returns the parsed expression value</li>
     *   <li>If parsing fails (no valid expression found), returns the provided default value</li>
     * </ol>
     * <p>
     * This method is particularly useful for handling optional expressions or providing default values
     * in grammar rules where an expression might not be present. The right associativity means that
     * operators are evaluated from right to left. For example, "a+b+c" is interpreted as "a+(b+c)".
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Combines {@link #chainRightMany(Parser)} with {@link #or(Parser)} and {@link #pure(Object)}</li>
     *   <li>No input is consumed if the expression cannot be parsed</li>
     *   <li>Always succeeds, either with the parsed result or the default value</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse arithmetic expressions with right-associative exponentiation
     * Parser<Character, Integer> number = intr;
     * Parser<Character, BinaryOperator<Integer>> power =
     *     chr('^').as((base, exp) -> (int)Math.pow(base, exp));
     *
     * // Parse exponentiation expression or return 1 if none found
     * Parser<Character, Integer> expression =
     *     number.chainRightZeroOrMany(power, 1);
     *
     * // Parses "2^3^2" as 2^(3^2) = 2^9 = 512
     * // Returns 1 for empty input
     * }</pre>
     *
     * @param op the parser that recognizes and returns binary operators
     * @param a  the default value to return if no expression can be parsed
     * @return a parser that handles right-associative expressions or returns the default value
     * @throws IllegalArgumentException if the operator parser is null
     * @see #chainRightMany(Parser) for the version that requires at least one operand
     * @see #chainLeftZeroOrMany(Parser, Object) for the left-associative equivalent
     * @see Associativity for associativity options
     */
    public Parser<I, A> chainRightZeroOrMany(Parser<I, BinaryOperator<A>> op, A a) {
        return this.chainRightMany(op).or(pure(a));
    }

    /**
     * Creates a parser that tries this parser first, and if it fails, tries an alternative parser.
     * <p>
     * The {@code or} method implements choice between parsers, similar to the logical OR operation
     * or the | (pipe) operator in regular expressions. It provides a way to try multiple parsing
     * strategies in sequence until one succeeds. The parsing process works as follows:
     * <ol>
     *   <li>First applies this parser to the input</li>
     *   <li>If this parser succeeds, its result is returned</li>
     *   <li>If this parser fails, the alternative parser is applied to the <i>original</i> input</li>
     *   <li>The result of the first successful parser is returned</li>
     *   <li>If both parsers fail, the composite parser fails</li>
     * </ol>
     * <p>
     * Important implementation details:
     * <ul>
     *   <li>The alternative parser is only tried if the first parser fails</li>
     *   <li>If the first parser fails, no input is consumed before trying the alternative</li>
     *   <li>This implements ordered choice - the first matching parser takes precedence</li>
     *   <li>The result types of both parsers must be the same</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse either an integer or a string "null" as an Optional<Integer>
     * Parser<Character, Integer> intParser = intr;
     * Parser<Character, Integer> nullParser = string("null").as(null);
     *
     * Parser<Character, Integer> optionalInt = intParser.or(nullParser);
     *
     * // Succeeds with 42 for input "42"
     * // Succeeds with null for input "null"
     * // Fails for input "abc" or ""
     * }</pre>
     *
     * @param other the alternative parser to try if this parser fails
     * @return a parser that tries this parser first, and if it fails, tries the alternative parser
     * @throws IllegalArgumentException if the other parameter is null
     * @see #orElse(Object) for providing a default value instead of an alternative parser
     * @see Combinators#oneOf for choosing between multiple parsers
     */
    public Parser<I, A> or(Parser<I, A> other) {
        return new Parser<>(in -> {
            Result<I, A> result = this.apply(in);
            return result.isSuccess() ? result : other.apply(in);
        });
    }

    /**
     * Creates a parser for right-associative operator expressions requiring at least one operand.
     * <p>
     * The {@code chainRightMany} method provides specialized support for parsing expressions
     * with right associativity, which means operators are evaluated from right to left. For example,
     * in "a^b^c", the operations are grouped as "a^(b^c)" rather than "(a^b)^c".
     * <p>
     * This method is particularly useful for operators that naturally associate right-to-left,
     * such as:
     * <ul>
     *   <li>Exponentiation (e.g., 2^3^2 = 2^(3^2) = 2^9 = 512)</li>
     *   <li>Assignment operators (e.g., a=b=c is equivalent to a=(b=c))</li>
     *   <li>Conditional operators (e.g., a?b:c?d:e is equivalent to a?b:(c?d:e))</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ol>
     *   <li>First applies this parser to get the initial operand</li>
     *   <li>Then repeatedly tries to parse an operator followed by another operand</li>
     *   <li>Combines the results from right to left using the binary operators</li>
     *   <li>Fails if no valid expression is found</li>
     * </ol>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse arithmetic expressions with right-associative exponentiation
     * Parser<Character, Integer> number = intr;
     * Parser<Character, BinaryOperator<Integer>> power =
     *     chr('^').as((base, exp) -> (int)Math.pow(base, exp));
     *
     * Parser<Character, Integer> expression = number.chainRightMany(power);
     *
     * // Parses "2^3^2" as 2^(3^2) = 2^9 = 512
     * // Parses "5" as simply 5
     * // Fails for empty input
     * }</pre>
     *
     * @param op the parser that recognizes and returns binary operators
     * @return a parser that handles right-associative expressions with at least one operand
     * @throws IllegalArgumentException if the operator parser is null
     * @see #chain(Parser, Associativity) for the more general method with explicit associativity
     * @see #chainRightZeroOrMany(Parser, Object) for a version that provides a default value
     * @see #chainLeftMany(Parser) for the left-associative equivalent
     */
    public Parser<I, A> chainRightMany(Parser<I, BinaryOperator<A>> op) {
        return chain(op, Associativity.RIGHT);
    }

    /**
     * Creates a parser that handles operator expressions with specified associativity.
     * <p>
     * The {@code chain} method is a powerful parser combinator for parsing sequences of
     * operands separated by operators, commonly used for expression parsing. It processes
     * the input as follows:
     * <ol>
     *   <li>First applies this parser to obtain an initial operand</li>
     *   <li>Then repeatedly tries to parse an operator followed by another operand</li>
     *   <li>Combines the results using the parsed operators according to the specified associativity</li>
     * </ol>
     * <p>
     * The method supports two associativity modes:
     * <ul>
     *   <li><b>LEFT</b>: Operators are evaluated from left to right. For example, "a+b+c" is
     *       interpreted as "(a+b)+c"</li>
     *   <li><b>RIGHT</b>: Operators are evaluated from right to left. For example, "a+b+c" is
     *       interpreted as "a+(b+c)"</li>
     * </ul>
     * <p>
     * This method serves as the foundation for more specific chainXxx methods like
     * {@link #chainLeftMany(Parser)} and {@link #chainRightMany(Parser)}.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse addition expressions with left associativity
     * Parser<Character, Integer> number = intr;
     * Parser<Character, BinaryOperator<Integer>> add =
     *     chr('+').as((a, b) -> a + b);
     *
     * Parser<Character, Integer> expression =
     *     number.chain(add, Associativity.LEFT);
     *
     * // Parses "1+2+3" as (1+2)+3 = 6
     * }</pre>
     *
     * @param op            the parser that recognizes and returns binary operators
     * @param associativity the associativity rule to apply (LEFT or RIGHT)
     * @return a parser that handles operator expressions with the specified associativity
     * @throws IllegalArgumentException if any parameter is null
     * @see #chainLeftMany(Parser) for a specialized left-associative version
     * @see #chainRightMany(Parser) for a specialized right-associative version
     * @see Associativity for the associativity options
     */
    public Parser<I, A> chain(Parser<I, BinaryOperator<A>> op, Associativity associativity) {
        if (associativity == Associativity.LEFT) {
            final Parser<I, UnaryOperator<A>> plo =
                    op.then(this)
                            .map((f, y) -> x -> f.apply(x, y));
            return this.then(plo.zeroOrMany())
                    .map((a, lf) -> lf.foldLeft(a, (acc, f) -> f.apply(acc)));
        } else {
            return this.then(op.then(this).map(Pair::new).zeroOrMany())
                    .map((a, pairs) -> pairs.stream().reduce(a, (acc, tuple) -> tuple.left().apply(tuple.right(), acc), (a1, a2) -> a1));
        }
    }

    /**
     * Creates a parser that applies this parser zero or more times until it fails,
     * collecting all successful results into a list.
     * <p>
     * This method implements the Kleene star (*) operation from formal language theory,
     * matching the pattern represented by this parser repeated any number of times (including zero).
     * The parsing process works as follows:
     * <ol>
     *   <li>Attempts to apply this parser at the current input position</li>
     *   <li>If successful, adds the result to the collection and advances the input position</li>
     *   <li>Repeats until this parser fails or end of input is reached</li>
     *   <li>Returns all collected results as an {@code FList}</li>
     * </ol>
     * <p>
     * If this parser fails on the first attempt or the input is empty, an empty list is returned
     * and the parser still succeeds.
     * <p>
     * Important implementation details:
     * <ul>
     *   <li>The parser checks for infinite loops by ensuring input position advances</li>
     *   <li>This is a greedy operation that consumes as much input as possible</li>
     *   <li>The resulting parser always succeeds, even with empty input</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse zero or more digits
     * Parser<Character, Character> digit = chr(Character::isDigit);
     * Parser<Character, FList<Character>> digits = digit.zeroOrMany();
     *
     * // Succeeds with [1,2,3] for input "123"
     * // Succeeds with [] for input "abc" (empty list, no input consumed)
     * // Succeeds with [1,2,3] for input "123abc" (consuming only "123")
     * }</pre>
     *
     * @return a parser that applies this parser zero or more times until it fails,
     * returning a list of all successful parse results
     * @see #many() for a version that requires at least one match
     * @see #repeat(int, int) for a version with explicit min and max counts
     */
    public Parser<I, FList<A>> zeroOrMany() {
        return repeatInternal(0, Integer.MAX_VALUE, null);
    }

    /**
     * Creates a parser that verifies the current input element is not equal to a specific value.
     * <p>
     * The {@code isNot} method provides a convenient way to exclude specific tokens from being
     * accepted. It operates as follows:
     * <ol>
     *   <li>First checks if the current input element matches the specified value</li>
     *   <li>If the value matches, the parser fails</li>
     *   <li>If the value does not match, this parser is applied and its result is returned</li>
     * </ol>
     * <p>
     * This method is implemented as a combination of {@link #not(Parser)} and {@link Combinators#is(Object)},
     * providing a more readable way to express "match anything except this specific value".
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse any character that is not a semicolon
     * Parser<Character, Character> notSemicolon = chr().isNot(';');
     *
     * // Succeeds with 'a' for input "a"
     * // Succeeds with '5' for input "5"
     * // Fails for input ";"
     * }</pre>
     *
     * @param value the value that should not be matched
     * @return a parser that succeeds only when the input element is not equal to the specified value
     * @see #not(Parser) for the more general negative lookahead mechanism
     * @see Combinators#is(Object) for the complementary parser that matches a specific value
     */
    public Parser<I, A> isNot(I value) {
        return this.not(is(value));
    }

    /**
     * Creates a negative lookahead parser that succeeds only if this parser succeeds and the provided parser fails.
     * <p>
     * This method implements negative lookahead, which is a powerful parsing technique where we verify
     * that a certain pattern does NOT appear at the current position before proceeding. The negative
     * lookahead operates as follows:
     * <ol>
     *   <li>First tries the provided parser at the current position</li>
     *   <li>If that parser succeeds, this composite parser fails</li>
     *   <li>If that parser fails, this parser is applied and its result is returned</li>
     * </ol>
     * <p>
     * Important: The lookahead parser never consumes any input. It only checks if a pattern matches
     * without advancing the parser position.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a letter that is not followed by a digit
     * Parser<Character, Character> letter = chr(Character::isLetter);
     * Parser<Character, Character> digit = chr(Character::isDigit);
     * Parser<Character, Character> letterNotFollowedByDigit = letter.not(digit);
     *
     * // Succeeds for "a" or "aX", fails for "a1"
     * }</pre>
     *
     * @param parser the parser to use as negative lookahead
     * @param <B>    the result type of the lookahead parser (not used in the result)
     * @return a parser that succeeds only if this parser succeeds and the lookahead parser fails
     * @throws IllegalArgumentException if the parser parameter is null
     */
    public <B> Parser<I, A> not(Parser<I, B> parser) {
        return new Parser<>(in -> {
            Result<I, B> result = parser.apply(in);
            if (result.isSuccess()) {
                return Result.failure(in, "Parser to fail", String.valueOf(result.get()));
            }
            return this.apply(in);
        });
    }

    /**
     * Creates a parser that applies this parser one or more times until it fails,
     * collecting all successful results into a list.
     * <p>
     * The {@code many} method implements the Kleene plus (+) operation from formal language theory,
     * matching the pattern represented by this parser repeated at least once. The parsing process
     * works as follows:
     * <ol>
     *   <li>First applies this parser at the current input position</li>
     *   <li>If successful, adds the result to the collection and advances the input position</li>
     *   <li>Repeats until this parser fails or end of input is reached</li>
     *   <li>Returns all collected results as an {@code FList}</li>
     * </ol>
     * <p>
     * This method is similar to {@link #zeroOrMany()}, but requires at least one successful match
     * to succeed. If this parser fails on the first attempt, the entire parser fails.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>The parser checks for infinite loops by ensuring input position advances</li>
     *   <li>This is a greedy operation that consumes as much input as possible</li>
     *   <li>Fails if no matches are found</li>
     *   <li>For guaranteed success regardless of input, use {@link #zeroOrMany()} instead</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse one or more digits
     * Parser<Character, Character> digit = chr(Character::isDigit);
     * Parser<Character, FList<Character>> digits = digit.many();
     *
     * // Succeeds with [1,2,3] for input "123"
     * // Succeeds with [1,2,3] for input "123abc" (consuming only "123")
     * // Fails for input "abc" (no digits found)
     * }</pre>
     *
     * @return a parser that applies this parser one or more times until it fails,
     * returning a list of all successful parse results
     * @see #zeroOrMany() for a version that succeeds even with zero matches
     * @see #repeat(int) for a version with an exact count
     * @see #repeat(int, int) for a version with explicit min and max counts
     */
    public Parser<I, FList<A>> many() {
        return repeatInternal(1, Integer.MAX_VALUE, null);
    }

    /**
     * Creates a parser that applies this parser one or more times until a terminator parser succeeds,
     * collecting all results into a list.
     * <p>
     * The {@code manyUntil} method combines the behavior of {@link #many()} with a termination condition.
     * It repeatedly applies this parser until either:
     * <ol>
     *   <li>The terminator parser succeeds, indicating the end of the sequence</li>
     *   <li>This parser fails after at least one successful parse</li>
     *   <li>The end of input is reached</li>
     * </ol>
     * <p>
     * This method is particularly useful for parsing delimited sequences where the delimiter isn't
     * part of the elements being collected, such as:
     * <ul>
     *   <li>Content between opening and closing markers (e.g., string content until a quote)</li>
     *   <li>Token sequences that end with a specific terminator (e.g., statements until semicolon)</li>
     *   <li>Sections of input that continue until a boundary marker is reached</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>At each position, first checks if the terminator succeeds</li>
     *   <li>If the terminator succeeds, stops collection and returns results so far</li>
     *   <li>Requires at least one successful match to succeed (min=1)</li>
     *   <li>The terminator parser is consumed when found (its input position advances)</li>
     *   <li>Internally implemented using {@link #repeatInternal(int, int, Parser)}</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse all characters until a semicolon
     * Parser<Character, Character> anyChar = any();
     * Parser<Character, Character> semicolon = chr(';');
     * Parser<Character, FList<Character>> content = anyChar.manyUntil(semicolon);
     *
     * // Succeeds with ['a','b','c'] for input "abc;" (consuming all input including semicolon)
     * // Fails for input ";" (no characters found before semicolon)
     * // Fails if no semicolon is found and no characters could be parsed
     * }</pre>
     *
     * @param until the parser that signals when to stop collecting elements
     * @return a parser that applies this parser one or more times until the terminator succeeds
     * @throws IllegalArgumentException if the until parameter is null
     * @see #zeroOrManyUntil(Parser) for a version that succeeds even with zero matches
     * @see #many() for a version that collects until this parser fails
     * @see #repeatInternal(int, int, Parser) for the underlying implementation
     */
    public Parser<I, FList<A>> manyUntil(Parser<I, ?> until) {
        return repeatInternal(1, Integer.MAX_VALUE, until);
    }

    /**
     * Creates a parser that always succeeds, optionally containing this parser's result.
     * <p>
     * The {@code optional} method creates a parser that attempts to apply this parser, but always
     * succeeds regardless of the result. The parsing process works as follows:
     * <ol>
     *   <li>First attempts to apply this parser to the input</li>
     *   <li>If this parser succeeds, its result is wrapped in a non-empty {@link Optional}</li>
     *   <li>If this parser fails, an empty {@link Optional} is returned without consuming any input</li>
     * </ol>
     * <p>
     * This method is particularly useful for parsing optional elements in a grammar, such as optional
     * parameters, modifiers, or any syntax structures that may or may not be present. It provides
     * a convenient way to handle the presence or absence of elements without disrupting the overall
     * parsing flow.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Combines {@link #map(Function)} with {@link #orElse(Object)} to achieve the optional behavior</li>
     *   <li>Always succeeds, never causing parsing failure</li>
     *   <li>No input is consumed when the parser fails</li>
     *   <li>The result type is transformed from {@code A} to {@code Optional<A>}</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse an optional minus sign followed by a number
     * Parser<Character, Character> minus = chr('-');
     * Parser<Character, Optional<Character>> optionalMinus = minus.optional();
     * Parser<Character, Integer> number = intr;
     *
     * // Combine to parse signed numbers
     * Parser<Character, Integer> signedNumber = optionalMinus.then(number)
     *     .map(sign -> num -> sign.isPresent() ? -num : num);
     *
     * // Succeeds with 42 for input "42"
     * // Succeeds with -42 for input "-42"
     * // Fails for input "abc" (no number found)
     * }</pre>
     *
     * @return a parser that always succeeds, returning either an Optional containing this
     * parser's result or an empty Optional
     * @see #orElse(Object) for providing a default value instead of an Optional
     * @see #zeroOrMany() for collecting zero or more occurrences of a pattern
     */
    public Parser<I, Optional<A>> optional() {
        return this.map(Optional::of).orElse(Optional.empty());
    }

    /**
     * Creates a parser that provides a default value if this parser fails.
     * <p>
     * The {@code orElse} method provides a way to handle parser failures by substituting a default value
     * rather than propagating the failure. This is different from the {@code or} method which tries an
     * alternative parsing strategy.
     * <p>
     * When applied to input:
     * <ol>
     *   <li>First attempts to apply this parser to the input</li>
     *   <li>If this parser succeeds, its result is returned</li>
     *   <li>If this parser fails, a success result containing the default value is returned</li>
     * </ol>
     * <p>
     * Important: When returning the default value, the input position remains unchanged from the original position.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse an integer, or use 0 as default if parsing fails
     * Parser<Character, Integer> parser = intr.orElse(0);
     *
     * // Succeeds with 42 for input "42"
     * // Succeeds with 0 for input "abc"
     * }</pre>
     *
     * @param other the default value to return if this parser fails
     * @return a parser that returns either the successful parse result or the default value
     */
    public Parser<I, A> orElse(A other) {
        return new NoCheckParser<>(in -> {
            Result<I, A> result = this.apply(in);
            if (result.isError()) {
                return Result.success(in, other);
            }
            return result;
        });
    }

    /**
     * Parses the input without requiring that the entire input is consumed.
     *
     * @param in the input to parse
     * @return the result of parsing the input
     */
    public Result<I, A> parse(Input<I> in) {
        return parse(in, false);
    }

    /**
     * Parses the input with an option to require complete input consumption.
     * <p>
     * The {@code parse} method is the core parsing method that applies this parser to the given input.
     * It serves as the foundation for all other parsing methods in this class. The parsing process
     * works as follows:
     * <ol>
     *   <li>Applies this parser to the provided input</li>
     *   <li>If the parser succeeds and {@code consumeAll} is {@code true}, verifies that all input has been consumed</li>
     *   <li>Returns a {@link Result} object containing either the successful parse result or an error</li>
     * </ol>
     * <p>
     * The {@code consumeAll} parameter provides control over whether parsing should succeed only when
     * the entire input is processed. This is useful for distinguishing between:
     * <ul>
     *   <li>Complete parsing: When you expect the parser to consume all available input</li>
     *   <li>Partial parsing: When you want to parse just a portion of the input, leaving the rest for later processing</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>First applies this parser using the {@link #apply(Input)} method</li>
     *   <li>If {@code consumeAll} is {@code true} and the parser succeeds but doesn't consume all input,
     *       returns a failure result with an "eof" error message</li>
     *   <li>Thread-safe, maintaining parser immutability</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a complete JSON document
     * Parser<Character, JsonValue> jsonParser = // ... json parser definition
     * Input<Character> input = Input.of("{\"name\":\"John\",\"age\":30}");
     * Result<Character, JsonValue> result = jsonParser.parse(input, true);
     * // result.isSuccess() == true only if the entire JSON document was valid and fully consumed
     *
     * // Parse just a number from the beginning of input
     * Parser<Character, Integer> numberParser = intr;
     * Input<Character> partialInput = Input.of("42 and more text");
     * Result<Character, Integer> partialResult = numberParser.parse(partialInput, false);
     * // partialResult.isSuccess() == true, partialResult.get() == 42
     * }</pre>
     *
     * @param in         the input to parse
     * @param consumeAll whether to require that the entire input is consumed
     * @return the result of parsing the input
     * @see #parse(Input) for parsing without requiring complete input consumption
     * @see #parseAll(Input) for parsing with mandatory complete input consumption
     * @see Result for the structure of successful and failure results
     */
    public Result<I, A> parse(Input<I> in, boolean consumeAll) {
        Result<I, A> result = this.apply(in);
        if (consumeAll && result.isSuccess()) {
            result = result.next().isEof() ? result : Result.failure(result.next(), "eof");
        }
        return result;
    }

    /**
     * Applies this parser to the given input, performing the core parsing operation.
     * <p>
     * The {@code apply} method is the fundamental parsing operation that processes input according
     * to this parser's rules and produces a result. It's the primary mechanism by which all parsers
     * evaluate input, with built-in protection against infinite recursive loops. The parsing process
     * works as follows:
     * <ol>
     *   <li>Checks for potential infinite recursion by tracking parser/position combinations</li>
     *   <li>Records the current parser and input position in thread-local tracking arrays</li>
     *   <li>Delegates to the parser's apply handler function to perform the actual parsing</li>
     *   <li>Cleans up the tracking state after parsing completes</li>
     *   <li>Returns a result containing either the successfully parsed value or failure information</li>
     * </ol>
     * <p>
     * This method serves as the core implementation called by all higher-level parsing methods like
     * {@link #parse(Input)} and {@link #parseAll(Input)}. It's rarely called directly by client code
     * but is the foundation of the entire parsing process.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses thread-local storage to efficiently track parser positions across the call stack</li>
     *   <li>Implements a loop detection algorithm that prevents infinite recursion in recursive grammars</li>
     *   <li>Automatically fails with an {@code INFINITE_LOOP_ERROR} message if recursion is detected</li>
     *   <li>Ensures proper cleanup of tracking data in both success and failure cases</li>
     *   <li>Thread-safe implementation that allows concurrent parsing with different parsers</li>
     * </ul>
     * <p>
     * Example usage (typically internal):
     * <pre>{@code
     * Parser<Character, Integer> numberParser = intr;
     * Input<Character> input = Input.of("123");
     *
     * // Direct application of the parser to input
     * Result<Character, Integer> result = numberParser.apply(input);
     *
     * if (result.isSuccess()) {
     *     // Successfully parsed 123
     *     Integer value = result.getValue();             // 123
     *     Input<Character> remaining = result.getRemaining();  // Input after consuming "123"
     * } else {
     *     // Parse failure
     *     String errorMsg = result.getErrorMessage();    // Description of the parsing error
     *     Input<Character> errorPos = result.getInput(); // Position where the error occurred
     * }
     * }</pre>
     *
     * @param in the input to parse
     * @return a result containing either the successfully parsed value or failure information
     * @see Result for the structure that contains parsing results
     * @see #parse(Input) for a higher-level method that wraps this core functionality
     * @see #parseAll(Input) for parsing that requires consuming the entire input
     */
    public Result<I, A> apply(Input<I> in) {
        // Fast path - avoid ThreadLocal lookup if position is different
        int lastPosition = in.position();

        // Use a more efficient data structure than Map
        IntObjectMap<Object> config = this.contextLocal.get();

        // Use containsKey+put instead of get+put to reduce lookup
        if (config.get(lastPosition) == this) {
            return Result.failure(in, null, INFINITE_LOOP_ERROR);
        }

        config.put(lastPosition, this);
        try {
            return applyHandler.apply(in);
        } finally {
            // Remove the parser from the context after parsing
            config.remove(lastPosition);
        }
    }

    /**
     * Parses the input string without requiring that the entire input is consumed.
     *
     * @param input the input string to parse
     * @return the result of parsing the input string
     */
    @SuppressWarnings("unchecked")
    public Result<I, A> parse(CharSequence input) {
        return parse((Input<I>) Input.of(input), false);
    }

    /**
     * Parses the input and ensures that the entire input is consumed.
     * <p>
     * The {@code parseAll} method applies this parser to the given input and verifies
     * that no unconsumed input remains. It is useful for scenarios where the input
     * must be fully parsed without leaving any trailing characters.
     * <p>
     * The parsing process works as follows:
     * <ol>
     *   <li>Applies this parser to the provided input</li>
     *   <li>If the parser succeeds, checks whether the input has been fully consumed</li>
     *   <li>If any unconsumed input remains, the method returns a failure result</li>
     *   <li>If the parser fails or unconsumed input exists, an error is returned</li>
     * </ol>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a complete integer from the input
     * Parser<Character, Integer> intParser = intr;
     * Input<Character> input = Input.of("123");
     * Result<Character, Integer> result = intParser.parseAll(input);
     *
     * if (result.isSuccess()) {
     *     Integer value = result.get(); // Successfully parsed value
     * } else {
     *     String error = result.getErrorMessage(); // Error message for failure
     * }
     * }</pre>
     *
     * @param in the input to parse
     * @return the result of parsing the input, ensuring the entire input is consumed
     * @see #parse(Input) for parsing without requiring complete input consumption
     * @see #parse(Input, boolean) for parsing with an explicit consumeAll flag
     */
    public Result<I, A> parseAll(Input<I> in) {
        return parse(in, true);
    }

    /**
     * Parses the input string and ensures that the entire input is consumed.
     *
     * @param input the input string to parse
     * @return the result of parsing the input string
     */
    @SuppressWarnings("unchecked")
    public Result<I, A> parseAll(CharSequence input) {
        return parse((Input<I>) Input.of(input), true);
    }

    /**
     * Creates a parser that applies this parser exactly the specified number of times,
     * collecting all results into a list.
     * <p>
     * The {@code repeat} method creates a parser that matches this parser's pattern an exact
     * number of times in sequence. The parsing process works as follows:
     * <ol>
     *   <li>Attempts to apply this parser exactly {@code target} times in sequence</li>
     *   <li>If successful for all iterations, returns all results collected in an {@code FList}</li>
     *   <li>If this parser fails before reaching the target count, the entire parser fails</li>
     * </ol>
     * <p>
     * This method is useful when parsing structures with a known, fixed number of elements,
     * such as fixed-length records, tuples, or specific syntax patterns with exact repetition
     * requirements.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>All parse results are collected in order of occurrence</li>
     *   <li>The parser checks for infinite loops by ensuring input position advances</li>
     *   <li>The input position is advanced after each successful application</li>
     *   <li>Unlike {@link #many()}, this parser requires exactly the specified number of matches</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse exactly 3 digits
     * Parser<Character, Character> digit = chr(Character::isDigit);
     * Parser<Character, FList<Character>> threeDigits = digit.repeat(3);
     *
     * // Succeeds with [1,2,3] for input "123"
     * // Succeeds with [1,2,3] for input "123abc" (consuming only "123")
     * // Fails for input "12" (not enough digits)
     * // Fails for input "ab12" (first element not a digit)
     * }</pre>
     *
     * @param target the exact number of times to apply this parser
     * @return a parser that applies this parser exactly the specified number of times
     * @throws IllegalArgumentException if the target count is negative
     * @see #repeat(int, int) for a version with minimum and maximum repetition counts
     * @see #repeatAtLeast(int) for a version with only a minimum count
     * @see #many() for matching one or more occurrences without an upper limit
     * @see #zeroOrMany() for matching zero or more occurrences
     */
    public Parser<I, FList<A>> repeat(int target) {
        return repeatInternal(target, target, null);
    }

    /**
     * Creates a parser that applies this parser between a minimum and maximum number of times,
     * collecting all results into a list.
     * <p>
     * The {@code repeat} method with min and max arguments creates a parser that matches this parser's pattern
     * a variable number of times within the specified range. The parsing process works as follows:
     * <ol>
     *   <li>Attempts to apply this parser repeatedly</li>
     *   <li>Requires at least {@code min} successful applications to succeed</li>
     *   <li>Will not attempt more than {@code max} applications, even if more matches are possible</li>
     *   <li>Returns all results collected in an {@code FList}</li>
     *   <li>If this parser fails before reaching the minimum count, the entire parser fails</li>
     * </ol>
     * <p>
     * This method generalizes several other repetition parsers, allowing explicit control over both
     * the lower and upper bounds of repetition. It is particularly useful for parsing structures with
     * flexible but constrained repetition patterns, such as optional sections with limits, or required
     * elements with additional optional elements.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>All parse results are collected in order of occurrence</li>
     *   <li>The parser checks for infinite loops by ensuring input position advances</li>
     *   <li>The input position is advanced after each successful application</li>
     *   <li>Collection stops either when the maximum count is reached or when this parser fails</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse between 2 and 4 digits
     * Parser<Character, Character> digit = chr(Character::isDigit);
     * Parser<Character, FList<Character>> digits = digit.repeat(2, 4);
     *
     * // Succeeds with [1,2,3,4] for input "1234"
     * // Succeeds with [1,2,3,4] for input "12345" (consuming only "1234")
     * // Succeeds with [1,2,3] for input "123"
     * // Succeeds with [1,2] for input "12"
     * // Fails for input "1" (not enough digits)
     * // Fails for input "abc" (no digits found)
     * }</pre>
     *
     * @param min the minimum number of times to apply this parser
     * @param max the maximum number of times to apply this parser
     * @return a parser that applies this parser between min and max times
     * @throws IllegalArgumentException if min or max is negative, or if min is greater than max
     * @see #repeat(int) for a version with an exact count
     * @see #repeatAtLeast(int) for a version with only a minimum count
     * @see #repeatAtMost(int) for a version with only a maximum count
     * @see #many() for matching one or more occurrences without an upper limit
     * @see #zeroOrMany() for matching zero or more occurrences without an upper limit
     */
    public Parser<I, FList<A>> repeat(int min, int max) {
        return repeatInternal(min, max, null);
    }

    /**
     * Creates a parser that applies this parser at least the specified number of times,
     * collecting all results into a list.
     * <p>
     * The {@code repeatAtLeast} method creates a parser that matches this parser's pattern
     * a minimum number of times, but with no upper limit. The parsing process works as follows:
     * <ol>
     *   <li>Attempts to apply this parser at least {@code target} times in sequence</li>
     *   <li>If successful for the minimum number of iterations, continues applying this parser
     *       until it fails or the end of input is reached</li>
     *   <li>Returns all results collected in an {@code FList}</li>
     *   <li>If this parser fails before reaching the minimum count, the entire parser fails</li>
     * </ol>
     * <p>
     * This method combines the behavior of {@link #repeat(int)} and {@link #many()}, requiring
     * a minimum number of matches but allowing for additional matches if available. It is useful
     * for parsing structures with a minimum required length but variable total length.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>All parse results are collected in order of occurrence</li>
     *   <li>The parser checks for infinite loops by ensuring input position advances</li>
     *   <li>The input position is advanced after each successful application</li>
     *   <li>After the minimum count is reached, collection continues until parser failure</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse at least 2 digits
     * Parser<Character, Character> digit = chr(Character::isDigit);
     * Parser<Character, FList<Character>> twoOrMoreDigits = digit.repeatAtLeast(2);
     *
     * // Succeeds with [1,2,3] for input "123"
     * // Succeeds with [1,2,3] for input "123abc" (consuming only "123")
     * // Succeeds with [1,2] for input "12"
     * // Fails for input "1" (not enough digits)
     * // Fails for input "abc" (no digits found)
     * }</pre>
     *
     * @param target the minimum number of times to apply this parser
     * @return a parser that applies this parser at least the specified number of times
     * @throws IllegalArgumentException if the target count is negative
     * @see #repeat(int) for a version with an exact count
     * @see #repeat(int, int) for a version with explicit minimum and maximum counts
     * @see #many() for matching one or more occurrences (equivalent to repeatAtLeast(1))
     * @see #zeroOrMany() for matching zero or more occurrences (equivalent to repeatAtLeast(0))
     */
    public Parser<I, FList<A>> repeatAtLeast(int target) {
        return repeatInternal(target, Integer.MAX_VALUE, null);
    }

    /**
     * Creates a parser that applies this parser zero up to a maximum number of times,
     * collecting all successful results into a list.
     * <p>
     * The {@code repeatAtMost} method creates a parser that matches this parser's pattern
     * up to a maximum number of times, but never more. The parsing process works as follows:
     * <ol>
     *   <li>Attempts to apply this parser repeatedly, up to {@code max} times</li>
     *   <li>Stops applying this parser once it reaches the maximum count or the parser fails</li>
     *   <li>Returns all successful results collected in an {@code FList}</li>
     *   <li>Always succeeds, even if no successful matches are found (returning an empty list)</li>
     * </ol>
     * <p>
     * This method is useful for parsing optional repeated structures with an upper bound,
     * such as optional parameters, modifiers, or any syntax elements that may appear
     * a limited number of times but aren't required.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>All parse results are collected in order of occurrence</li>
     *   <li>The parser checks for infinite loops by ensuring input position advances</li>
     *   <li>Unlike {@link #repeat(int)}, this parser doesn't require any matches to succeed</li>
     *   <li>Unlike {@link #zeroOrMany()}, this parser has an upper limit on matches</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse up to 3 digits
     * Parser<Character, Character> digit = chr(Character::isDigit);
     * Parser<Character, FList<Character>> upToThreeDigits = digit.repeatAtMost(3);
     *
     * // Succeeds with [1,2,3] for input "123"
     * // Succeeds with [1,2,3] for input "1234" (consuming only "123")
     * // Succeeds with [1,2] for input "12"
     * // Succeeds with [] for input "abc" (empty list, no input consumed)
     * }</pre>
     *
     * @param max the maximum number of times to apply this parser
     * @return a parser that applies this parser at most the specified number of times
     * @throws IllegalArgumentException if the max count is negative
     * @see #repeat(int) for a version with an exact count
     * @see #repeatAtLeast(int) for a version with only a minimum count
     * @see #repeat(int, int) for a version with explicit minimum and maximum counts
     * @see #zeroOrMany() for matching zero or more occurrences without an upper limit
     */
    public Parser<I, FList<A>> repeatAtMost(int max) {
        return repeatInternal(0, max, null);
    }

    /**
     * Creates a parser that parses a potentially empty sequence of elements separated by a delimiter.
     * <p>
     * The {@code separatedByZeroOrMany} method creates a parser that matches elements using this parser,
     * with each element separated by the specified separator parser. The parsing process works as follows:
     * <ol>
     *   <li>Attempts to parse a first element using this parser</li>
     *   <li>If the first element is found, then repeatedly tries to parse a separator followed by another element</li>
     *   <li>Collects all parsed elements (ignoring separators) into an {@code FList}</li>
     *   <li>Returns an empty list if no elements are found (unlike {@link #separatedByMany(Parser)} which requires at least one element)</li>
     * </ol>
     * <p>
     * This method is particularly useful for parsing common data formats like comma-separated lists,
     * space-delimited tokens, or any syntax that involves items separated by delimiters, where empty
     * collections are valid. Examples include empty parameter lists, empty arrays, or optional sequences.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Builds on {@link #separatedByMany(Parser)} but succeeds even when no elements are found</li>
     *   <li>Only the elements are collected; separator values are discarded</li>
     *   <li>Always succeeds, returning an empty list if no elements match</li>
     *   <li>The input position remains unchanged if no elements are found</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a comma-separated list of numbers, allowing empty lists
     * Parser<Character, Integer> number = intr;
     * Parser<Character, Character> comma = chr(',');
     * Parser<Character, FList<Integer>> optionalList = number.separatedByZeroOrMany(comma);
     *
     * // Succeeds with [1,2,3] for input "1,2,3"
     * // Succeeds with [] for input "" (empty list)
     * // Succeeds with [42] for input "42"
     * // Succeeds with [] for input ";" (empty list, no input consumed)
     * }</pre>
     *
     * @param sep   the parser that recognizes the separator between elements
     * @param <SEP> the type of the separator parse result (which is discarded)
     * @return a parser that parses zero or more elements separated by the given separator
     * @throws IllegalArgumentException if the separator parser is null
     * @see #separatedByMany(Parser) for a version that requires at least one element
     * @see #zeroOrMany() for collecting repeated elements without separators
     * @see #repeat(int, int) for collecting a specific range of elements
     */
    public <SEP> Parser<I, FList<A>> separatedByZeroOrMany(Parser<I, SEP> sep) {
        return this.separatedByMany(sep).map(l -> l).or(pure(new FList<>()));
    }

    /**
     * Transforms the result of this parser using the given function without affecting the parsing logic.
     * <p>
     * The {@code map} method is a fundamental parser combinator that allows transformation of successful
     * parse results without changing how the input is consumed. It operates as follows:
     * <ol>
     *   <li>First applies this parser to the input</li>
     *   <li>If this parser succeeds, applies the provided function to the result value</li>
     *   <li>Returns a new parser that produces the transformed result</li>
     *   <li>If this parser fails, the failure is propagated without applying the function</li>
     * </ol>
     * <p>
     * This method is essential for converting parsed values to different types or structures while
     * maintaining the same parsing behavior. The input position handling remains unchanged from the
     * original parser.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a digit character and convert it to its integer value
     * Parser<Character, Character> digitChar = chr(Character::isDigit);
     * Parser<Character, Integer> digitValue = digitChar.map(c -> Character.getNumericValue(c));
     *
     * // Succeeds with 5 for input "5"
     * // Fails for input "a"
     * }</pre>
     *
     * @param func the function to apply to the parsed result
     * @param <R>  the type of the transformed result
     * @return a new parser that applies the given function to successful parse results
     * @throws IllegalArgumentException if the function parameter is null
     * @see #as(Object) for mapping to a constant value
     * @see ApplyBuilder for mapping multiple parser results
     */
    public <R> Parser<I, R> map(Function<A, R> func) {
        return new Parser<>(in -> apply(in).map(func));
    }

    /**
     * Creates a parser that parses a non-empty sequence of elements separated by a delimiter.
     * <p>
     * The {@code separatedByMany} method creates a parser that matches elements using this parser,
     * with each element separated by the specified separator parser. The parsing process works as follows:
     * <ol>
     *   <li>First parses an initial element using this parser (required)</li>
     *   <li>Then repeatedly tries to parse a separator followed by another element</li>
     *   <li>Collects all parsed elements (ignoring separators) into an {@code FList}</li>
     *   <li>Succeeds only if at least one element is found</li>
     * </ol>
     * <p>
     * This method is particularly useful for parsing common data formats like comma-separated lists,
     * space-delimited tokens, or any syntax that requires at least one element with optional additional
     * elements separated by delimiters. Examples include non-empty parameter lists, argument sequences,
     * or any collection that must contain at least one item.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>The first element is parsed with this parser</li>
     *   <li>Subsequent elements are parsed as pairs of separator followed by element</li>
     *   <li>Only the elements are collected; separator values are discarded</li>
     *   <li>Fails if no elements can be parsed</li>
     *   <li>The input position is advanced after each successful match</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a comma-separated list of numbers, requiring at least one number
     * Parser<Character, Integer> number = intr;
     * Parser<Character, Character> comma = chr(',');
     * Parser<Character, FList<Integer>> numberList = number.separatedByMany(comma);
     *
     * // Succeeds with [1,2,3] for input "1,2,3"
     * // Succeeds with [42] for input "42"
     * // Fails for input "" (empty input)
     * // Fails for input "," (no elements found)
     * }</pre>
     *
     * @param sep   the parser that recognizes the separator between elements
     * @param <SEP> the type of the separator parse result (which is discarded)
     * @return a parser that parses one or more elements separated by the given separator
     * @throws IllegalArgumentException if the separator parser is null
     * @see #separatedByZeroOrMany(Parser) for a version that allows empty sequences
     * @see #many() for collecting repeated elements without separators
     * @see #repeat(int, int) for collecting a specific range of elements
     */
    public <SEP> Parser<I, FList<A>> separatedByMany(Parser<I, SEP> sep) {
        return this.then(sep.skipThen(this).zeroOrMany()).map(a -> l -> l.prepend(a));
    }

    /**
     * Initializes a parser reference with another parser's behavior.
     * <p>
     * The {@code set} method initializes a parser reference created by {@link #ref()} with the
     * parsing behavior of another parser. This is a key component in creating recursive parsers
     * that can reference themselves or contain mutual references. The method works as follows:
     * <ol>
     *   <li>Takes an already-constructed parser that defines the desired parsing behavior</li>
     *   <li>Transfers that parser's apply handler to this parser reference</li>
     *   <li>Can only be called once on a given parser reference</li>
     * </ol>
     * <p>
     * This method is primarily used in conjunction with {@link #ref()} to create parsers for
     * recursive grammar structures. It solves the initialization problem for recursive definitions
     * by allowing forward references to parsers whose complete definitions depend on themselves.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Thread-safe with synchronized access to prevent concurrent initialization</li>
     *   <li>Throws an exception if the parser is already initialized</li>
     *   <li>Only transfers the apply handler function, not any other properties</li>
     *   <li>Must be called before the reference parser is used for parsing</li>
     * </ul>
     * <p>
     * Example usage for recursive JSON-like parser:
     * <pre>{@code
     * // Create a forward reference for a JSON value parser
     * Parser<Character, Object> jsonValue = Parser.ref();
     *
     * // Define parsers for different JSON types
     * Parser<Character, String> jsonString = stringLiteral;
     * Parser<Character, Integer> jsonNumber = intr;
     * Parser<Character, Boolean> jsonBoolean = string("true").as(true).or(string("false").as(false));
     * Parser<Character, Object> jsonNull = string("null").as(null);
     *
     * // Define array parser using the value reference
     * Parser<Character, List<Object>> jsonArray =
     *     jsonValue.separatedByZeroOrMany(chr(',')).between('[', ']');
     *
     * // Define object parser using the value reference
     * Parser<Character, Map<String, Object>> jsonObject =
     *     jsonString.skipThen(chr(':')).then(jsonValue)
     *         .map((key, val) -> Map.entry(key, val))
     *         .separatedByZeroOrMany(chr(','))
     *         .between('{', '}')
     *         .map(entries -> {
     *             Map<String, Object> map = new HashMap<>();
     *             entries.forEach(e -> map.put(e.getKey(), e.getValue()));
     *             return map;
     *         });
     *
     * // Finally, initialize the value parser with all possible JSON value types
     * jsonValue.set(jsonString.or(jsonNumber).or(jsonBoolean).or(jsonNull).or(jsonArray).or(jsonObject));
     * }</pre>
     *
     * @param parser the parser whose behavior should be used to initialize this reference
     * @throws IllegalArgumentException if the parser parameter is null
     * @throws IllegalStateException    if this parser has already been initialized
     * @see #ref() for creating an uninitialized parser reference
     * @see #set(Function) for initializing with a custom apply handler
     */
    public synchronized void set(Parser<I, A> parser) {
        if (parser == null) {
            throw new IllegalArgumentException("parser cannot be null");
        }
        if (this.applyHandler != defaultApplyHandler) {
            throw new IllegalStateException("Parser already has an applyHandler");
        }
        this.applyHandler = parser.applyHandler;
    }

    /**
     * Initializes a parser reference with a custom apply handler function.
     * <p>
     * The {@code set} method initializes a parser reference created by {@link #ref()} with a
     * custom function that defines the parsing behavior. This provides more control than
     * {@link #set(Parser)} by allowing direct specification of the parsing logic. The method
     * works as follows:
     * <ol>
     *   <li>Takes a function that defines how the parser should process input</li>
     *   <li>Sets this function as the parser's apply handler</li>
     *   <li>Can only be called once on a given parser reference</li>
     * </ol>
     * <p>
     * This method is primarily used for creating advanced recursive parsers where more control
     * is needed over the parsing behavior than simply using an existing parser. It provides
     * direct access to the core parsing mechanism for implementing custom parser logic.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Thread-safe with synchronized access to prevent concurrent initialization</li>
     *   <li>Throws an exception if the parser is already initialized</li>
     *   <li>The apply handler function should handle input appropriately and return valid results</li>
     *   <li>Must be called before the reference parser is used for parsing</li>
     * </ul>
     * <p>
     * Example usage for a custom recursive parser:
     * <pre>{@code
     * // Create a forward reference for an expression parser
     * Parser<Character, Integer> expr = Parser.ref();
     *
     * // Define parsers for basic components
     * Parser<Character, Integer> number = intr;
     * Parser<Character, Character> plus = chr('+');
     * Parser<Character, Character> openParen = chr('(');
     * Parser<Character, Character> closeParen = chr(')');
     *
     * // Define a custom apply handler for the expression parser
     * expr.set(input -> {
     *     // First try to parse a number
     *     Result<Character, Integer> numResult = number.apply(input);
     *     if (numResult.isSuccess()) {
     *         return numResult;
     *     }
     *
     *     // Then try to parse a parenthesized expression
     *     if (input.atEnd() || input.get() != '(') {
     *         return Result.failure("Expected number or expression", input);
     *     }
     *
     *     // Parse: (expr + expr)
     *     Input<Character> afterOpen = input.advance(1);
     *     Result<Character, Integer> leftResult = expr.apply(afterOpen);
     *     if (!leftResult.isSuccess()) {
     *         return leftResult;
     *     }
     *
     *     Result<Character, Character> opResult = plus.apply(leftResult.getRemaining());
     *     if (!opResult.isSuccess()) {
     *         return Result.failure("Expected '+'", leftResult.getRemaining());
     *     }
     *
     *     Result<Character, Integer> rightResult = expr.apply(opResult.getRemaining());
     *     if (!rightResult.isSuccess()) {
     *         return rightResult;
     *     }
     *
     *     Result<Character, Character> closeResult = closeParen.apply(rightResult.getRemaining());
     *     if (!closeResult.isSuccess()) {
     *         return Result.failure("Expected ')'", rightResult.getRemaining());
     *     }
     *
     *     int result = leftResult.getValue() + rightResult.getValue();
     *     return Result.success(result, closeResult.getRemaining());
     * });
     *
     * // Now expr can parse recursive expressions like "5" or "(1+2)" or "((1+2)+3)"
     * }</pre>
     *
     * @param applyHandler the function that defines the parser's behavior
     * @throws IllegalArgumentException if the applyHandler is null
     * @throws IllegalStateException    if this parser has already been initialized
     * @see #ref() for creating an uninitialized parser reference
     * @see #set(Parser) for initializing with another parser's behavior
     * @see Result for the result type that should be returned by the apply handler
     * @see Input for the input type that will be provided to the apply handler
     */
    public synchronized void set(Function<Input<I>, Result<I, A>> applyHandler) {
        if (applyHandler == null) {
            throw new IllegalArgumentException("applyHandler cannot be null");
        }
        if (this.applyHandler != defaultApplyHandler) {
            throw new IllegalStateException("Parser already has an applyHandler");
        }
        this.applyHandler = applyHandler;
    }

    /**
     * Creates a parser that repeatedly applies this parser as long as the condition evaluates to true.
     * <p>
     * This parser will:
     * <ul>
     *   <li>Check if the condition is true for the current input position</li>
     *   <li>If true, apply this parser and collect the result</li>
     *   <li>Continue until either the condition becomes false, parsing fails, or input is exhausted</li>
     *   <li>Return all collected results as an FList</li>
     * </ul>
     * <p>
     * Unlike {@link #zeroOrMany()}, this parser uses a separate condition parser to determine
     * when to stop collecting items rather than relying on parse failures. This allows for more
     * flexible parsing based on lookahead or contextual conditions.
     * <p>
     * The implementation includes a check to prevent infinite loops in cases where the parser
     * succeeds but doesn't advance the input position.
     *
     * @param condition a parser that returns a boolean indicating whether to continue collecting
     * @return a parser that collects elements while the condition is true
     * @throws IllegalArgumentException if the condition parser is null
     */
    public Parser<I, FList<A>> takeWhile(Parser<I, Boolean> condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition parser cannot be null");
        }

        return new NoCheckParser<>(in -> {
            FList<A> results = new FList<>();
            Input<I> currentInput = in;

            while (!currentInput.isEof()) {
                // Check if the condition is met
                Result<I, Boolean> conditionResult = condition.apply(currentInput);
                if (conditionResult.isError() || !conditionResult.get()) {
                    // Condition not met, stop collecting
                    return Result.success(currentInput, results);
                }

                // Store the current position to check for advancement
                int currentPosition = currentInput.position();

                // Condition met, try to parse an element
                Result<I, A> elementResult = this.apply(currentInput);
                if (elementResult.isError()) {
                    // Failed to parse an element, stop collecting
                    return Result.success(currentInput, results);
                }

                // Add parsed element to results
                results = results.append(elementResult.get());
                currentInput = elementResult.next();

                // Check if we've advanced the position - if not, break to avoid infinite loop
                if (currentInput.position() == currentPosition) {
                    return Result.success(currentInput, results);
                }
            }

            // Reached end of input
            return Result.success(currentInput, results);
        });
    }

    /**
     * Creates a parser that skips whitespace characters before and after applying this parser.
     * <p>
     * The {@code trim} method provides a convenient way to handle whitespace in parsers,
     * especially useful when parsing formats where whitespace is insignificant. The parsing
     * process works as follows:
     * <ol>
     *   <li>Skips all leading whitespace characters from the current input position</li>
     *   <li>Applies this parser to the whitespace-trimmed input</li>
     *   <li>If this parser succeeds, skips all trailing whitespace characters after the result</li>
     *   <li>Returns the original parser's result with the input position advanced past any trailing whitespace</li>
     * </ol>
     * <p>
     * Important implementation details:
     * <ul>
     *   <li>Whitespace is determined using Java's {@link Character#isWhitespace(char)} method</li>
     *   <li>Trimming only occurs when the input elements are of type {@code Character}</li>
     *   <li>If the original parser fails, the failure is returned without attempting to skip trailing whitespace</li>
     *   <li>The parser result value remains unchanged; only the input position is affected</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse an integer, ignoring surrounding whitespace
     * Parser<Character, Integer> parser = intr.trim();
     *
     * // Succeeds with 42 for input "42"
     * // Succeeds with 42 for input "  42  "
     * // Succeeds with 42 for input "\t42\n"
     * }</pre>
     *
     * @return a new parser that skips whitespace before and after applying this parser
     * @see Character#isWhitespace(char) for the definition of whitespace characters
     */
    public Parser<I, A> trim() {
        return new Parser<>(in -> {
            Input<I> trimmedInput = skipWhitespace(in);
            Result<I, A> result = this.apply(trimmedInput);
            if (result.isSuccess()) {
                trimmedInput = skipWhitespace(result.next());
                return Result.success(trimmedInput, result.get());
            }
            return result;
        });
    }

    private Input<I> skipWhitespace(Input<I> in) {
        while (!in.isEof() && in.current() instanceof Character ch && Character.isWhitespace(ch)) {
            in = in.next();
        }
        return in;
    }

    /**
     * Creates a parser that applies this parser zero or more times until a terminator parser succeeds,
     * collecting all successful results into a list.
     * <p>
     * This method is a variant of {@link #zeroOrMany()} that stops collection when a specific
     * terminating pattern is found rather than when this parser fails. The parsing process works as follows:
     * <ol>
     *   <li>At each position, first check if the terminator parser succeeds</li>
     *   <li>If the terminator succeeds, stop collecting and return the results collected so far</li>
     *   <li>If the terminator fails, attempt to apply this parser</li>
     *   <li>If this parser succeeds, add the result to the collection and advance the input position</li>
     *   <li>If this parser fails, return all results collected so far</li>
     *   <li>Repeat until either the terminator succeeds or end of input is reached</li>
     * </ol>
     * <p>
     * Important implementation details:
     * <ul>
     *   <li>The terminator parser is consumed when found (its input position advances)</li>
     *   <li>This parser always succeeds, even with empty input or when no elements match</li>
     *   <li>If this parser fails on the first attempt, an empty list is returned</li>
     *   <li>The parser checks for infinite loops by ensuring input position advances</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a comma-separated list of numbers terminated by a semicolon
     * Parser<Character, Character> digit = chr(Character::isDigit);
     * Parser<Character, Character> comma = chr(',');
     * Parser<Character, Character> semicolon = chr(';');
     *
     * // Collect digits until semicolon is found
     * Parser<Character, FList<Character>> digitList = digit.zeroOrManyUntil(semicolon);
     *
     * // Succeeds with [1,2,3] for input "123;" (consuming all input including semicolon)
     * // Succeeds with [] for input ";" (consuming only the semicolon)
     * // Succeeds with [1,2,3] for input "123" (consuming all input, no semicolon found)
     * }</pre>
     *
     * @param terminator the parser that signals when to stop collecting elements
     * @return a parser that applies this parser zero or more times until the terminator succeeds
     * @throws IllegalArgumentException if the terminator parameter is null
     * @see #manyUntil(Parser) for a version that requires at least one match
     * @see #zeroOrMany() for a version that collects until this parser fails
     */
    public Parser<I, FList<A>> zeroOrManyUntil(Parser<I, ?> terminator) {
        return repeatInternal(0, Integer.MAX_VALUE, terminator);
    }

    /**
     * Creates a parser that applies this parser a specified number of times, collecting results into a list.
     * <p>
     * The {@code repeatInternal} method is a utility for implementing parsers that match a pattern
     * a minimum and/or maximum number of times. It processes the input as follows:
     * <ol>
     *   <li>Attempts to apply this parser repeatedly, starting from the current input position</li>
     *   <li>Stops when the maximum number of repetitions is reached or the parser fails</li>
     *   <li>Ensures that at least the minimum number of repetitions is satisfied</li>
     *   <li>If a terminator parser is provided, stops when the terminator succeeds</li>
     * </ol>
     * <p>
     * This method is used internally by higher-level combinators like {@link #zeroOrMany()},
     * {@link #many()}, and {@link #manyUntil(Parser)}.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Fails if the minimum number of repetitions is not met</li>
     *   <li>Consumes input greedily up to the maximum limit or until the terminator succeeds</li>
     *   <li>Returns a list of all successfully parsed results</li>
     *   <li>Handles edge cases like zero repetitions or infinite maximum limits</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse exactly 3 digits
     * Parser<Character, FList<Character>> threeDigits = digit.repeatInternal(3, 3, null);
     *
     * // Parse 1 to 5 letters
     * Parser<Character, FList<Character>> letters = letter.repeatInternal(1, 5, null);
     *
     * // Parse digits until a semicolon
     * Parser<Character, FList<Character>> digitsUntilSemicolon =
     *     digit.repeatInternal(0, Integer.MAX_VALUE, chr(';'));
     * }</pre>
     *
     * @param min      the minimum number of repetitions (inclusive)
     * @param max      the maximum number of repetitions (inclusive)
     * @param until    an optional parser that terminates the repetition when it succeeds
     * @return a parser that applies this parser the specified number of times
     * @throws IllegalArgumentException if {@code min} is negative, {@code max} is less than {@code min},
     *                                   or {@code until} is null when required
     */
    private Parser<I, FList<A>> repeatInternal(int min, int max, Parser<I, ?> until) {
        if (min < 0 || max < 0) {
            throw new IllegalArgumentException("The number of repetitions cannot be negative");
        }
        if (min > max) {
            throw new IllegalArgumentException("The minimum number of repetitions cannot be greater than the maximum");
        }
        return new Parser<>(in -> {
            List<A> buffer = new ArrayList<>();
            Input<I> current = in;
            int count = 0;

            while (true) {
                // Check terminator (for manyUntil)
                if (until != null) {
                    Result<I, ?> termRes = until.apply(current);
                    if (termRes.isSuccess()) {
                        if (count < min) {
                            return Result.failure(current, "Expected at least " + min + " items");
                        }
                        return Result.success(termRes.next(), new FList<>(buffer));
                    }
                }
                // End-of-input or max reached
                if (current.isEof() || count >= max) {
                    if (count >= min && until == null) {
                        return Result.success(current, new FList<>(buffer));
                    }
                    return Result.failure(current, min + " repetitions");
                }
                // Parse an item
                Result<I, A> res = this.apply(current);
                if (!res.isSuccess()) {
                    if (count >= min) {
                        return Result.success(current, new FList<>(buffer));
                    }
                    return res.cast();
                }
                if (current.position() == res.next().position()) {
                    return Result.failure(current, "Parser consumed no input");
                }
                buffer.add(res.get());
                current = res.next();
                count++;
            }
        });
    }
    protected static final String INFINITE_LOOP_ERROR = "Infinite loop detected";
    private final ThreadLocal<IntObjectMap<Object>> contextLocal = ThreadLocal.withInitial(IntObjectMap::new);
    protected Function<Input<I>, Result<I, A>> applyHandler;
    /**
     * A default apply handler that throws an exception if the parser is not initialized.
     */
    private Function<Input<I>, Result<I, A>> defaultApplyHandler;

    /**
     * Private constructor to create a parser reference that can be initialized later.
     */
    private Parser() {
        this.applyHandler = defaultApplyHandler = in -> {
            throw new IllegalStateException("Parser not initialized");
        };
    }

    /**
     * Creates a new parser with the specified apply handler function.
     * <p>
     * The {@code Parser} constructor creates a parser that uses the provided function to process input and
     * produce results. The apply handler is the core parsing function that defines how this parser
     * transforms input into parsed results. The function works as follows:
     * <ol>
     *   <li>Receives an {@link Input} object representing the current parsing state</li>
     *   <li>Processes the input according to the parser's grammar rules</li>
     *   <li>Returns a {@link Result} object containing either a successful parse result or an error</li>
     * </ol>
     * <p>
     * This constructor is the primary way to create custom parsers with specific parsing logic. Most users
     * will not need to use this constructor directly, instead using combinators and factory methods to
     * build parsers from simpler components.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>The apply handler should return a successful result with the parsed value if parsing succeeds</li>
     *   <li>The apply handler should return a failure result with an error message if parsing fails</li>
     *   <li>The apply handler is responsible for properly advancing the input position on success</li>
     *   <li>Thread safety is maintained as parsers are immutable after construction</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a custom parser that recognizes a specific pattern
     * Parser<Character, String> customParser = new Parser<>(input -> {
     *     if (input.atEnd()) {
     *         return Result.failure("Unexpected end of input", input);
     *     }
     *
     *     // Check for a specific pattern
     *     if (input.get() == 'a' && input.get(1) == 'b') {
     *         return Result.success("ab", input.advance(2));
     *     }
     *
     *     return Result.failure("Expected 'ab'", input);
     * });
     * }</pre>
     *
     * @param applyHandler the function that defines this parser's behavior
     * @throws IllegalArgumentException if the applyHandler is null
     * @see #apply(Input) for the method that uses this handler to parse input
     * @see Result for the result type returned by the apply handler
     * @see Input for the input type consumed by the apply handler
     */
    public Parser(Function<Input<I>, Result<I, A>> applyHandler) {
        if (applyHandler == null) {
            throw new IllegalArgumentException("applyHandler cannot be null");
        }
        this.applyHandler = applyHandler;
    }

    /**
     * Creates a reference to a parser that can be initialized later, enabling recursive grammar definitions.
     * <p>
     * The {@code ref} method addresses the challenge of defining recursive parsers by creating a
     * placeholder parser that can be initialized after its creation. This allows for handling
     * self-referential grammar rules that would otherwise cause initialization issues. The process
     * works as follows:
     * <ol>
     *   <li>Create a parser reference using {@code ref()}</li>
     *   <li>Use this reference in other parser definitions as needed</li>
     *   <li>Later, initialize the reference using {@link #set(Parser)} or {@link #set(Function)}</li>
     * </ol>
     * <p>
     * This technique is essential for parsing recursive structures such as:
     * <ul>
     *   <li>Nested expressions (e.g., arithmetic expressions with parentheses)</li>
     *   <li>Recursive data structures (e.g., JSON objects containing other JSON objects)</li>
     *   <li>Self-referential grammar rules (e.g., a term that can contain other terms)</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Creates a parser with a default handler that throws an exception if used before initialization</li>
     *   <li>Must be initialized with {@link #set(Parser)} or {@link #set(Function)} before use</li>
     *   <li>Thread-safe, allowing for concurrent parser creation and initialization</li>
     *   <li>Cannot be reinitialized after being set once</li>
     * </ul>
     * <p>
     * Example usage for parsing nested arithmetic expressions:
     * <pre>{@code
     * // Create a forward reference for an expression parser
     * Parser<Character, Integer> expr = Parser.ref();
     *
     * // Define a parser for simple numbers
     * Parser<Character, Integer> number = intr;
     *
     * // Define a parser for parenthesized expressions using the reference
     * Parser<Character, Integer> parens = expr.between('(', ')');
     *
     * // Define operators
     * Parser<Character, BinaryOperator<Integer>> addOp =
     *     chr('+').as((a, b) -> a + b);
     *
     * // Now initialize the expression parser with a definition that references itself
     * expr.set(number.or(parens).chainLeftMany(addOp));
     *
     * // Can now parse recursive expressions like "1+(2+3)"
     * }</pre>
     *
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a new uninitialized parser reference
     * @throws IllegalStateException if the parser is used before being initialized
     * @see #set(Parser) for initializing the parser reference with another parser
     * @see #set(Function) for initializing the parser reference with an apply handler
     */
    public static <I, A> Parser<I, A> ref() {
        return new Parser<>();
    }


}
