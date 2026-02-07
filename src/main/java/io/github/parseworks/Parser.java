package io.github.parseworks;

import io.github.parseworks.impl.IntObjectMap;
import io.github.parseworks.impl.result.Match;
import io.github.parseworks.impl.result.NoMatch;
import io.github.parseworks.impl.result.PartialMatch;
import io.github.parseworks.parsers.Chains;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.github.parseworks.parsers.Chains.chain;
import static io.github.parseworks.parsers.Combinators.is;
/**
 * Core parser class for consuming input of type {@code I} and producing results of type {@code A}.
 * <pre>{@code
 * Parser<Character, Integer> parser = Numeric.integer;
 * }</pre>
 *
 * @param <I> input type
 * @param <A> result type
 */
public class Parser<I, A> {



    /**
     * Replaces the result of this parser with a constant value on success.
     * <pre>{@code
     * Lexical.string("true").as(true).value(); // true
     * }</pre>
     *
     * @param value constant value to return
     * @param <R>   result type
     * @return a parser returning the constant value
     */
    public <R> Parser<I, R> as(R value) {
        return this.skipThen(pure(value));
    }

    /**
     * Creates a parser that always succeeds with the given value without consuming input.
     * <pre>{@code
     * Parser.pure(42).parse("any").value(); // 42
     * }</pre>
     *
     * @param value value to return
     * @param <I>   input type
     * @param <A>   result type
     * @return a parser that always succeeds with the given value
     */
    public static <I, A> Parser<I, A> pure(A value) {
        return new Parser<>(input -> new Match<>(value, input));
    }

    /**
     * Creates a parser that matches an expression enclosed by matching bracket symbols.
     * <p>
     * This is equivalent to {@code between(bracket, bracket)} and is useful for
     * symmetric delimiters like parentheses, brackets, or quotes.
     *
     * @param bracket the symbol used as both opening and closing delimiter
     * @return a parser that matches content between matching bracket symbols
     */
    public Parser<I, A> between(I bracket) {
        return between(bracket, bracket);
    }

    /**
     * Creates a parser that matches an expression between distinct opening and closing symbols.
     * <p>
     * This is useful for asymmetric delimiters like XML tags or different bracket styles.
     *
     * @param open the opening delimiter symbol
     * @param close the closing delimiter symbol
     * @return a parser that matches content between the specified delimiters
     */

    public Parser<I, A> between(I open, I close) {
        return is(open).skipThen(this).thenSkip(is(close));
    }

    /**
     * Applies two parsers in sequence and returns the result of this parser.
     * <pre>{@code
     * Parser<Character, Integer> p = Numeric.integer.thenSkip(Lexical.chr(';'));
     * p.parse("42;").value(); // 42
     * }</pre>
     *
     * @param pb  next parser
     * @param <B> result type of pb
     * @return a parser returning this parser's result
     */
    public <B> Parser<I, A> thenSkip(Parser<I, B> pb) {
        return new Parser<>(in -> {
            Result<I, A> res = this.apply(in);
            if (!res.matches()) return res;
            Result<I, B> res2 = pb.apply(res.input());
            if (!res2.matches()) {
                if (res2.input().position() > in.position()) {
                    return new PartialMatch<>(res2.input(), (Failure<I, A>) res2.cast());
                }
                return res2.cast();
            }
            return new Match<>(res.value(), res2.input());
        });
    }


    /**
     * Applies two parsers in sequence and returns the result of the second parser.
     * <pre>{@code
     * Parser<Character, Integer> p = Lexical.string("key:").skipThen(Numeric.integer);
     * p.parse("key:42").value(); // 42
     * }</pre>
     *
     * @param pb  next parser
     * @param <B> result type of pb
     * @return a parser returning pb's result
     */
    public <B> Parser<I, B> skipThen(Parser<I, B> pb) {
        return new Parser<>(in -> {
            Result<I, A> res = this.apply(in);
            if (!res.matches()) return res.cast();
            Result<I, B> res2 = pb.apply(res.input());
            if (!res2.matches()) {
                if (res2.input().position() > in.position()) {
                    return new PartialMatch<>(res2.input(), (Failure<I, B>) res2);
                }
            }
            return res2;
        });
    }


    /**
     * Applies two parsers in sequence and returns an {@link ApplyBuilder} for combining results.
     * <pre>{@code
     * Parser<Character, String> p = Lexical.chr('a').then(Lexical.chr('b'))
     *                                      .map((a, b) -> "" + a + b);
     * p.parse("ab").value(); // "ab"
     * }</pre>
     *
     * @param next next parser
     * @param <B>  result type of next
     * @return an ApplyBuilder
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
     * The {@code chainLeftZeroOrMany} method extends {@link #chainLeftOneOrMore(Parser)} to handle the case
     * where no operands are present in the input by providing a default value. It processes the input as follows:
     * <ol>
     *   <li>First attempts to parse a left-associative operator expression using {@code chainLeftOneOrMore}</li>
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
     *   <li>Combines {@link #chainLeftOneOrMore(Parser)} with {@link #or(Parser)} and {@link #pure(Object)}</li>
     *   <li>No input is consumed if the expression cannot be parsed</li>
     *   <li>Always succeeds, either with the parsed result or the default value</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse arithmetic expressions with left-associative subtraction
     * Parser<Character, Integer> number = Numeric.integer;
     * Parser<Character, BinaryOperator<Integer>> subtract =
     *     Lexical.chr('-').as((a, b) -> a - b);
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
     * @see #chainLeftOneOrMore(Parser) for the version that requires at least one operand
     * @see #chainRightZeroOrMore(Parser, Object) for the right-associative equivalent
     * @see Chains.Associativity for associativity options
     */
    public Parser<I, A> chainLeftZeroOrMore(Parser<I, BinaryOperator<A>> op, A a) {
        return this.chainLeftOneOrMore(op).or(pure(a));
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
     *   <li>First applies this parser to value the initial operand</li>
     *   <li>Then repeatedly tries to parse an operator followed by another operand</li>
     *   <li>Combines the results from left to right using the binary operators</li>
     *   <li>Fails if no valid expression is found</li>
     * </ol>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse arithmetic expressions with left-associative subtraction
     * Parser<Character, Integer> number = Numeric.integer;
     * Parser<Character, BinaryOperator<Integer>> subtract =
     *     Lexical.chr('-').as((a, b) -> a - b);
     *
     * Parser<Character, Integer> expression = number.chainLeftOneOrMore(subtract);
     *
     * // Parses "5-3-2" as (5-3)-2 = 0
     * // Parses "7" as simply 7
     * // Fails for empty input
     * }</pre>
     *
     * @param op the parser that recognizes and returns binary operators
     * @return a parser that handles left-associative expressions with at least one operand
     * @throws IllegalArgumentException if the operator parser is null
     * @see io.github.parseworks.parsers.Chains#chain(Parser, Parser, Chains.Associativity) for the more general method with explicit associativity
     * @see #chainLeftZeroOrMore(Parser, Object) for a version that provides a default value
     * @see #chainRightOneOrMore(Parser) for the right-associative equivalent
     */
    public Parser<I, A> chainLeftOneOrMore(Parser<I, BinaryOperator<A>> op) {
        return chain(this, op, Chains.Associativity.LEFT);
    }

    /**
     * Creates a parser for right-associative operator expressions that succeeds even when no operands are found.
     * <p>
     * The {@code chainRightZeroOrMany} method extends {@link #chainRightOneOrMore(Parser)} to handle the case
     * where no operands are present in the input by providing a default value. It processes the input as follows:
     * <ol>
     *   <li>First attempts to parse a right-associative operator expression using {@code chainRightOneOrMore}</li>
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
     *   <li>Combines {@link #chainRightOneOrMore(Parser)} with {@link #or(Parser)} and {@link #pure(Object)}</li>
     *   <li>No input is consumed if the expression cannot be parsed</li>
     *   <li>Always succeeds, either with the parsed result or the default value</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse arithmetic expressions with right-associative exponentiation
     * Parser<Character, Integer> number = Numeric.integer;
     * Parser<Character, BinaryOperator<Integer>> power =
     *     Lexical.chr('^').as((base, exp) -> (int)Math.pow(base, exp));
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
     * @see #chainRightOneOrMore(Parser) for the version that requires at least one operand
     * @see #chainLeftZeroOrMore(Parser, Object) for the left-associative equivalent
     * @see Chains.Associativity for associativity options
     */
    public Parser<I, A> chainRightZeroOrMore(Parser<I, BinaryOperator<A>> op, A a) {
        return this.chainRightOneOrMore(op).or(pure(a));
    }

    /**
     * Tries this parser first, and if it fails, tries the alternative parser.
     * <pre>{@code
     * Parser<Character, Integer> p = Numeric.integer.or(Parser.pure(0));
     * p.parse("42").value(); // 42
     * p.parse("abc").value(); // 0
     * }</pre>
     *
     * @param other alternative parser
     * @return a choice parser
     */
    public Parser<I, A> or(Parser<I, A> other) {
        return new Parser<>(in -> {
            Result<I, A> result = this.apply(in);
            return result.matches() ? result : other.apply(in);
        });
    }

    /**
     * Creates a parser for right-associative operator expressions requiring at least one operand.
     * <p>
     * The {@code chainRightOneOrMore} method provides specialized support for parsing expressions
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
     *   <li>First applies this parser to value the initial operand</li>
     *   <li>Then repeatedly tries to parse an operator followed by another operand</li>
     *   <li>Combines the results from right to left using the binary operators</li>
     *   <li>Fails if no valid expression is found</li>
     * </ol>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse arithmetic expressions with right-associative exponentiation
     * Parser<Character, Integer> number = Numeric.integer;
     * Parser<Character, BinaryOperator<Integer>> power =
     *     Lexical.chr('^').as((base, exp) -> (int)Math.pow(base, exp));
     *
     * Parser<Character, Integer> expression = number.chainRightOneOrMore(power);
     *
     * // Parses "2^3^2" as 2^(3^2) = 2^9 = 512
     * // Parses "5" as simply 5
     * // Fails for empty input
     * }</pre>
     *
     * @param op the parser that recognizes and returns binary operators
     * @return a parser that handles right-associative expressions with at least one operand
     * @throws IllegalArgumentException if the operator parser is null
     * @see io.github.parseworks.parsers.Chains#chain(Parser, Parser, Chains.Associativity) for the more general method with explicit associativity
     * @see #chainRightZeroOrMore(Parser, Object) for a version that provides a default value
     * @see #chainLeftOneOrMore(Parser) for the left-associative equivalent
     */
    public Parser<I, A> chainRightOneOrMore(Parser<I, BinaryOperator<A>> op) {
        return chain(this, op, Chains.Associativity.RIGHT);
    }

    /**
     * Collects zero or more matches into a list.
     * <pre>{@code
     * Lexical.chr('a').zeroOrMore().parse("aaa").value(); // ['a', 'a', 'a']
     * Lexical.chr('a').zeroOrMore().parse("bbb").value(); // []
     * }</pre>
     *
     * @return a list parser
     */
    public Parser<I, List<A>> zeroOrMore() {
        return repeatInternal(0, Integer.MAX_VALUE, null);
    }

    /**
     * Collects one or more matches into a list.
     * <pre>{@code
     * Lexical.chr('a').oneOrMore().parse("aaa").value(); // ['a', 'a', 'a']
     * Lexical.chr('a').oneOrMore().parse("bbb").matches(); // false
     * }</pre>
     *
     * @return a list parser
     */
    public Parser<I, List<A>> oneOrMore() {
        return repeatInternal(1, Integer.MAX_VALUE, null);
    }

    /**
     * Collects one or more matches until the terminator succeeds.
     * <pre>{@code
     * Lexical.chr('a').oneOrMoreUntil(Lexical.chr(';')).parse("aaa;").value(); // ['a', 'a', 'a']
     * }</pre>
     *
     * @param until terminator parser
     * @return a list parser
     */
    public Parser<I, List<A>> oneOrMoreUntil(Parser<I, ?> until) {
        return repeatInternal(1, Integer.MAX_VALUE, until);
    }


    /**
     * Succeeds only if validation succeeds without consuming input.
     * <pre>{@code
     * Parser<Character, Integer> p = Numeric.integer.onlyIf(Lexical.chr('+'));
     * p.parse("+123").value(); // 123
     * p.parse("-123").matches(); // false
     * }</pre>
     *
     * @param validation validation parser
     * @param <B>        validation result type
     * @return a conditional parser
     */
    public <B> Parser<I, A> onlyIf(Parser<I, B> validation) {
        return new Parser<>(input -> {
            Result<I, B> validationResult = validation.apply(input);
            if (!validationResult.matches()) {
                return validationResult.cast();
            }
            return this.apply(input);
        });
    }

    /**
     * Succeeds if followed by lookahead without consuming lookahead input.
     * <pre>{@code
     * Parser<Character, String> p = Lexical.word.peek(Lexical.chr('='));
     * p.parse("id=42").value(); // "id"
     * }</pre>
     *
     * @param lookahead lookahead parser
     * @param <B>       lookahead result type
     * @return a lookahead parser
     */
    public <B> Parser<I, A> peek(Parser<I, B> lookahead) {
        return new Parser<>(input -> {
            Result<I, A> result = this.apply(input);
            if (!result.matches()) {
                return result;
            }
            Result<I, B> peek = lookahead.apply(result.input());
            if (!peek.matches()) {
                return new NoMatch<>(input, "Expected 'peek' to succeed", (NoMatch<?, ?>) peek);
            }
            return result;
        });
    }


    /**
     * Creates a parser that logs its progress and results to standard output while behaving exactly like this parser.
     * <p>
     * The {@code logSystemOut} method wraps this parser with logging functionality that prints information about:
     * <ul>
     *   <li>The input position where parsing starts</li>
     *   <li>Whether parsing succeeded or failed</li>
     *   <li>The parsed value (on success) or error message (on failure)</li>
     * </ul>
     * <p>
     * This method is particularly useful for:
     * <ul>
     *   <li>Debugging parser behavior</li>
     *   <li>Understanding why certain inputs fail to parse</li>
     *   <li>Tracing the execution of complex parser combinations</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser for integers with logging
     * Parser<Character, Integer> debugParser = Numeric.integer.logSystemOut();
     *
     * // When parsing "123", outputs:
     * // Parser starting at position: 0 succeeded with value: 123
     *
     * // When parsing "abc", outputs:
     * // Parser starting at position: 0 failed: Expected digit but found 'a'
     * }</pre>
     *
     * @return a new parser that logs its progress while behaving like this parser
     * @see Result for the structure of success and failure results that are logged
     */
    public Parser<I, A> logSystemOut() {
        return new Parser<>(input -> {
            System.out.print("Parser starting at position: " + input.position());
            Result<I, A> result = this.apply(input);
            if (result.matches()) {
                System.out.println(" succeeded with value: " + result.value());
            } else {
                System.out.println(" failed: " + result.error());
            }
            return result;
        });
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
     * // Parse signed numbers
     * Parser<Character, Integer> number = Numeric.integer;
     * Parser<Character, Integer> signedNumber = Lexical.chr('-').optional().then(number)
     *     .map((sign, num) -> sign.isPresent() ? -num : num);
     *
     * // Succeeds with 42 for input "42"
     * // Succeeds with -42 for input "-42"
     * // Fails for input "abc" (no number found)
     * }</pre>
     *
     * @return a parser that always succeeds, returning either an Optional containing this
     * parser's result or an empty Optional
     * @see #orElse(Object) for providing a default value instead of an Optional
     * @see #zeroOrMore() for collecting zero or more occurrences of a pattern
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
     * Parser<Character, Integer> parser = Numeric.integer.orElse(0);
     *
     * // Succeeds with 42 for input "42"
     * // Succeeds with 0 for input "abc"
     * }</pre>
     *
     * @param other the default value to return if this parser fails
     * @return a parser that returns either the successful parse result or the default value
     */
    public Parser<I, A> orElse(A other) {
        return new Parser<>(in -> {
            Result<I, A> result = this.apply(in);
            if (!result.matches()) {
                return new Match<>(other, in);
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
     * Creates an iterator that incrementally parses the input, allowing for streaming processing of parse results.
     * <p>
     * The {@code iterateParse} method provides a way to parse input incrementally by returning an iterator
     * that processes the input one element at a time. This is particularly useful when:
     * <ul>
     *   <li>Processing large inputs that shouldn't be parsed all at once</li>
     *   <li>Implementing streaming or lazy parsing scenarios</li>
     *   <li>Searching for multiple occurrences of a pattern in the input</li>
     * </ul>
     * <p>
     * The iterator works as follows:
     * <ol>
     *   <li>Attempts to parse the input at the current position using this parser</li>
     *   <li>If parsing succeeds, returns the parsed value and advances the input position</li>
     *   <li>If parsing fails, skips one character and tries again at the next position</li>
     *   <li>Continues until the end of input is reached</li>
     * </ol>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>The iterator maintains its own input position state</li>
     *   <li>Results are computed lazily when {@code hasNext()} or {@code next()} is called</li>
     *   <li>Failed parse attempts are skipped automatically</li>
     *   <li>The iterator follows the standard Java Iterator contract</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser that matches integers
     * Parser<Character, Integer> intParser = integer;
     *
     * // Create an input from a string containing mixed content
     * Input<Character> input = Input.fromString("123 abc 456 def 789");
     *
     * // Iterate over all integers in the input
     * Iterator<Integer> numbers = intParser.iterateParse(input);
     * while (numbers.hasNext()) {
     *     Integer number = numbers.next();
     *     System.out.println(number); // Prints: 123, 456, 789
     * }
     * }</pre>
     *
     * @param input the input to parse
     * @return an iterator that yields parse results one at a time
     * @throws IllegalArgumentException if the input is null
     * @see Input for input handling
     * @see Result for parse result handling
     */
    public Iterator<A> iterateParse(Input<I> input) {
        final Parser<I,A> parser = this;
        return new Iterator<>() {
            private Input<I> currentInput = input;
            private Result<I, A> nextResult = null;


            @Override
            public boolean hasNext() {
                if (currentInput.isEof()) {
                    return false;
                }

                if (nextResult != null) {
                    return true;
                }

                // Try to find the next valid result
                while (!currentInput.isEof()) {
                    Result<I, A> result = parser.parse(currentInput, false);
                    if (result.matches()) {
                        nextResult = result;
                        return true;
                    }
                    // Skip one character and try again
                    currentInput = currentInput.next();
                }

                return false;
            }

            @Override
            public A next() {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more elements to parse");
                }

                // We already have the next result from hasNext()
                A value = nextResult.value();
                currentInput = nextResult.input();
                nextResult = null;
                return value;
            }
        };
    }

    /**
     * Creates a Stream that lazily parses the input, providing a modern streaming interface for parse results.
     * <p>
     * The {@code streamParse} method converts the parsing process into a Java Stream, allowing for:
     * <ul>
     *   <li>Functional-style processing of parse results</li>
     *   <li>Lazy evaluation of parse operations</li>
     *   <li>Integration with the Java Stream API</li>
     *   <li>Composition with other stream operations</li>
     * </ul>
     * <p>
     * The returned Stream has the following characteristics:
     * <ul>
     *   <li>ORDERED - elements are processed in the order they appear in the input</li>
     *   <li>NONNULL - all elements are guaranteed to be non-null</li>
     *   <li>Non-parallel - parsing is performed sequentially</li>
     *   <li>Unknown size - the number of elements is not known in advance</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser for integers
     * Parser<Character, Integer> intParser = intr;
     * Input<Character> input = Input.fromString("123 456 789");
     *
     * // Process integers using stream operations
     * intParser.streamParse(input)
     *         .filter(n -> n > 200)
     *         .map(n -> n * 2)
     *         .forEach(System.out::println);
     *
     * // Collect all numbers into a list
     * List<Integer> numbers = intParser.streamParse(input)
     *                                 .collect(Collectors.toList());
     * }</pre>
     * <p>
     * Note that this method internally uses {@link #iterateParse(Input)} and wraps it in a Stream.
     * The stream will automatically close any resources associated with the parsing process when
     * the stream is closed or fully consumed.
     *
     * @param input the input to parse
     * @return a Stream containing the parse results
     * @throws IllegalArgumentException if the input is null
     * @see #iterateParse(Input) for the underlying iterator implementation
     * @see java.util.stream.Stream for available stream operations
     */
    public  Stream<A> streamParse(Input<I> input) {
        return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(
                iterateParse(input),
                Spliterator.ORDERED | Spliterator.NONNULL
            ),
            false
        );
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
     * // result.matches() == true only if the entire JSON document was valid and fully consumed
     *
     * // Parse just a number from the beginning of input
     * Parser<Character, Integer> numberParser = intr;
     * Input<Character> partialInput = Input.of("42 and more text");
     * Result<Character, Integer> partialResult = numberParser.parse(partialInput, false);
     * // partialResult.matches() == true, partialResult.value() == 42
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
        if (consumeAll && result.matches()) {
            if (!result.input().isEof()) {
                return new PartialMatch<>(result.input(), new NoMatch<>(result.input(), "end of input"));
            }
        }
        return result;
    }

    /**
     * Applies the specified input to the handler and returns the result.
     *
     * @param in the input to process of type {@code Input<I>}
     * @return the result of processing the input, encapsulated in a {@code Result<I, A>}
     */
    public Result<I, A> apply(Input<I> in) {
        return applyHandler.apply(in);
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
     * Parser<Character, Integer> intParser = Numeric.integer;
     * Input<Character> input = Input.of("123");
     * Result<Character, Integer> result = intParser.parseAll(input);
     *
     * if (result.matches()) {
     *     Integer value = result.value(); // Successfully parsed value
     * } else {
     *     String error = result.error(); // Error message for failure
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
     *   <li>If successful for all iterations, returns all results collected in a {@code List}</li>
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
     *   <li>Unlike {@link #oneOrMore()}, this parser requires exactly the specified number of matches</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse exactly 3 digits
     * Parser<Character, Character> digit = Lexical.chr(Character::isDigit);
     * Parser<Character, List<Character>> threeDigits = digit.repeat(3);
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
     * @see #oneOrMore() for matching one or more occurrences without an upper limit
     * @see #zeroOrMore() for matching zero or more occurrences
     */
    public Parser<I, List<A>> repeat(int target) {
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
     *   <li>Returns all results collected in a {@code List}</li>
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
     * Parser<Character, Character> digit = Lexical.chr(Character::isDigit);
     * Parser<Character, List<Character>> digits = digit.repeat(2, 4);
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
     * @see #oneOrMore() for matching one or more occurrences without an upper limit
     * @see #zeroOrMore() for matching zero or more occurrences without an upper limit
     */
    public Parser<I, List<A>> repeat(int min, int max) {
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
     *   <li>Returns all results collected in a {@code List}</li>
     *   <li>If this parser fails before reaching the minimum count, the entire parser fails</li>
     * </ol>
     * <p>
     * This method combines the behavior of {@link #repeat(int)} and {@link #oneOrMore()}, requiring
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
     * Parser<Character, Character> digit = Lexical.chr(Character::isDigit);
     * Parser<Character, List<Character>> twoOrMoreDigits = digit.repeatAtLeast(2);
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
     * @see #oneOrMore() for matching one or more occurrences (equivalent to repeatAtLeast(1))
     * @see #zeroOrMore() for matching zero or more occurrences (equivalent to repeatAtLeast(0))
     */
    public Parser<I, List<A>> repeatAtLeast(int target) {
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
     *   <li>Returns all successful results collected in a {@code List}</li>
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
     *   <li>Unlike {@link #zeroOrMore()}, this parser has an upper limit on matches</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse up to 3 digits
     * Parser<Character, Character> digit = Lexical.chr(Character::isDigit);
     * Parser<Character, List<Character>> upToThreeDigits = digit.repeatAtMost(3);
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
     * @see #zeroOrMore() for matching zero or more occurrences without an upper limit
     */
    public Parser<I, List<A>> repeatAtMost(int max) {
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
     *   <li>Returns an empty list if no elements are found (unlike {@link #oneOrMoreSeparatedBy(Parser)} which requires at least one element)</li>
     * </ol>
     * <p>
     * This method is particularly useful for parsing common data formats like comma-separated lists,
     * space-delimited tokens, or any syntax that involves items separated by delimiters, where empty
     * collections are valid. Examples include empty parameter lists, empty arrays, or optional sequences.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Builds on {@link #oneOrMoreSeparatedBy(Parser)} but succeeds even when no elements are found</li>
     *   <li>Only the elements are collected; separator values are discarded</li>
     *   <li>Always succeeds, returning an empty list if no elements match</li>
     *   <li>The input position remains unchanged if no elements are found</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a comma-separated list of numbers, allowing empty lists
     * Parser<Character, Integer> number = Numeric.integer;
     * Parser<Character, Character> comma = Lexical.chr(',');
     * Parser<Character, List<Integer>> optionalList = number.zeroOrMoreSeparatedBy(comma);
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
     * @see #oneOrMoreSeparatedBy(Parser) for a version that requires at least one element
     * @see #zeroOrMore() for collecting repeated elements without separators
     * @see #repeat(int, int) for collecting a specific range of elements
     */
    public <SEP> Parser<I, List<A>> zeroOrMoreSeparatedBy(Parser<I, SEP> sep) {
        return this.oneOrMoreSeparatedBy(sep).map(l -> l).or(pure(Collections.emptyList()));
    }

    /**
     * Transforms the result of this parser using the given function.
     * <pre>{@code
     * Parser<Character, Integer> p = Lexical.chr('5').map(Character::getNumericValue);
     * p.parse("5").value(); // 5
     * }</pre>
     *
     * @param func transformation function
     * @param <R>  transformed result type
     * @return a transformed parser
     */
    public <R> Parser<I, R> map(Function<A, R> func) {
        return new Parser<>(in -> apply(in).map(func));
    }

    /**
     * Creates a parser that parses a non-empty sequence of elements separated by a delimiter.
     * <p>
     * The {@code oneOrMoreSeparatedBy} method creates a parser that matches elements using this parser,
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
     * Parser<Character, Integer> number = Numeric.integer;
     * Parser<Character, Character> comma = Lexical.chr(',');
     * Parser<Character, List<Integer>> numberList = number.oneOrMoreSeparatedBy(comma);
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
     * @see #zeroOrMoreSeparatedBy(Parser) for a version that allows empty sequences
     * @see #oneOrMore() for collecting repeated elements without separators
     * @see #repeat(int, int) for collecting a specific range of elements
     */
    public <SEP> Parser<I, List<A>> oneOrMoreSeparatedBy(Parser<I, SEP> sep) {
        return this.then(sep.skipThen(this).zeroOrMore()).map(a -> l -> Lists.prepend(a, l));
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
     *     if (numResult.matches()) {
     *         return numResult;
     *     }
     *
     *     // Then try to parse a parenthesized expression
     *     if (input.atEnd() || input.value() != '(') {
     *         return new NoMatch<>(input, "Expected number or expression");
     *     }
     *
     *     // Parse: (expr + expr)
     *     Input<Character> afterOpen = input.advance(1);
     *     Result<Character, Integer> leftResult = expr.apply(afterOpen);
     *     if (!leftResult.matches()) {
     *         return leftResult;
     *     }
     *
     *     Result<Character, Character> opResult = plus.apply(leftResult.input());
     *     if (!opResult.matches()) {
     *         return new NoMatch<>(leftResult.input(), "Expected '+'");
     *     }
     *
     *     Result<Character, Integer> rightResult = expr.apply(opResult.input());
     *     if (!rightResult.matches()) {
     *         return rightResult;
     *     }
     *
     *     Result<Character, Character> closeResult = closeParen.apply(rightResult.input());
     *     if (!closeResult.matches()) {
     *         return new NoMatch<>(rightResult.input(), "Expected ')'");
     *     }
     *
     *     int resultValue = leftResult.value() + rightResult.value();
     *     return new Match<>(resultValue, closeResult.input());
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
     * Unlike {@link #zeroOrMore()}, this parser uses a separate condition parser to determine
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
    public Parser<I, List<A>> takeWhile(Parser<I, Boolean> condition) {
        if (condition == null) {
            throw new IllegalArgumentException("Condition parser cannot be null");
        }

        return new Parser<>(in -> {
            List<A> results = new ArrayList<>();
            Input<I> currentInput = in;

            while (!currentInput.isEof()) {
                // Check if the condition is met
                Result<I, Boolean> conditionResult = condition.apply(currentInput);
                if (!conditionResult.matches()) {
                    // Condition not met, stop collecting
                    return new Match<>(Collections.unmodifiableList(results), currentInput);
                }

                // Store the current position to check for advancement
                int currentPosition = currentInput.position();

                // Condition met, try to parse an element
                Result<I, A> elementResult = this.apply(currentInput);
                if (!elementResult.matches()) {
                    // Failed to parse an element, stop collecting
                    return new Match<>(Collections.unmodifiableList(results), currentInput);
                }

                // Add parsed element to results
                results.add(elementResult.value());
                currentInput = elementResult.input();

                // Check if we've advanced the position - if not, break to avoid infinite loop
                if (currentInput.position() == currentPosition) {
                    return new Match<>(Collections.unmodifiableList(results), currentInput);
                }
            }

            return new Match<>(Collections.unmodifiableList(results), currentInput);
        });
    }

    /**
     * Creates a repeating parser that applies this parser zero or more times until a terminator parser succeeds,
     * collecting all successful results into a list.
     * <p>
     * This method is a variant of {@link #zeroOrMore()} that stops collection when a specific
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
     * Parser<Character, Character> digit = Lexical.chr(Character::isDigit);
     * Parser<Character, Character> comma = Lexical.chr(',');
     * Parser<Character, Character> semicolon = Lexical.chr(';');
     *
     * // Collect digits until semicolon is found
     * Parser<Character, List<Character>> digitList = digit.zeroOrManyUntil(semicolon);
     *
     * // Succeeds with [1,2,3] for input "123;" (consuming all input including semicolon)
     * // Succeeds with [] for input ";" (consuming only the semicolon)
     * // Succeeds with [1,2,3] for input "123" (consuming all input, no semicolon found)
     * }</pre>
     *
     * @param terminator the parser that signals when to stop collecting elements
     * @return a parser that applies this parser zero or more times until the terminator succeeds
     * @throws IllegalArgumentException if the terminator parameter is null
     * @see #oneOrMoreUntil(Parser) for a version that requires at least one match
     * @see #zeroOrMore() for a version that collects until this parser fails
     */
    public Parser<I, List<A>> zeroOrMoreUntil(Parser<I, ?> terminator) {
        return repeatInternal(0, Integer.MAX_VALUE, terminator);
    }

    /**
     * Creates a repeating parser that applies this parser a specified number of times, collecting results into a list.
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
     * This method is used internally by higher-level combinators like {@link #zeroOrMore()},
     * {@link #oneOrMore()}, and {@link #oneOrMoreUntil(Parser)}.
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
     * Parser<Character, List<Character>> threeDigits = digit.repeatInternal(3, 3, null);
     *
     * // Parse 1 to 5 letters
     * Parser<Character, List<Character>> letters = letter.repeatInternal(1, 5, null);
     *
     * // Parse digits until a semicolon
     * Parser<Character, List<Character>> digitsUntilSemicolon =
     *     digit.repeatInternal(0, Integer.MAX_VALUE, Lexical.chr(';'));
     * }</pre>
     *
     * @param min      the minimum number of repetitions (inclusive)
     * @param max      the maximum number of repetitions (inclusive)
     * @param until    an optional parser that terminates the repetition when it succeeds
     * @return a parser that applies this parser the specified number of times
     * @throws IllegalArgumentException if {@code min} is negative, {@code max} is less than {@code min},
     *                                   or {@code until} is null when required
     */
    private Parser<I, List<A>> repeatInternal(int min, int max, Parser<I, ?> until) {
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
                // Check terminator (for one or moreUntil)
                if (until != null) {
                    Result<I, ?> termRes = until.apply(current);
                    if (termRes.matches()) {
                        if (count < min) {
                            // Provide more context about the error
                            return new NoMatch<>(
                                current, 
                                "expected at least " + min + " items (found only " + count + " before terminator)");
                        }
                        return new Match<>(Collections.unmodifiableList(buffer), termRes.input());
                    }
                }
                // End-of-input or max reached
                if (current.isEof() || count >= max) {
                    if (count >= min && until == null) {
                        return new Match<>(Collections.unmodifiableList(buffer), current);
                    }
                    // Provide more context about the error
                    String reason = current.isEof() ? "end of input reached" : "maximum repetitions reached";
                    return new NoMatch<>(current, min + " repetitions (" + reason + ")");
                }
                // Parse an item
                Result<I, A> res = this.apply(current);
                if (!res.matches()) {
                    // If the parser consumed input before failing, it's a hard error
                    //if (res.input() != null && res.input().position() > current.position()) {
                    //    return res.cast();
                    //}
                    if (res.type() == ResultType.PARTIAL){
                        return res.cast();
                    }
                    // If we have a terminator, we MUST reach it
                    if (until != null) {
                        return res.cast();
                    }

                    if (count >= min) {
                        return new Match<>(Collections.unmodifiableList(buffer), current);
                    }
                    
                    if (current.position() > in.position()) {
                        return new PartialMatch<>(current, new NoMatch<>(current,
                                "at least " + min + " repetition(s)",
                                (NoMatch<?, ?>) res
                        ));
                    }

                    // Pass through the original error with more context
                    // pass literal failure as part of new failure to create a nested response
                    return new NoMatch<>(current,
                        "at least " + min + " repetition(s)",
                        (NoMatch<?, ?>) res
                    );
                }
                if (current.position() == res.input().position()) {
                    // Provide more context about the error when parser doesn't consume input
                    return new NoMatch<>(
                        current, 
                        "parser to consume input during repetition"
                    );
                }
                buffer.add(res.value());
                current = res.input();
                count++;
            }
        });
    }

    /**
     * A function that defines how this parser applies to an input.
     * <p>
     * This function is the core of the parser, defining how it processes input and produces results.
     * It takes an {@link Input} object representing the current parsing state and returns a {@link Result}
     * object containing either a successful parse result or an error.
     */
    protected Function<Input<I>, Result<I, A>> applyHandler;
    /**
     * A default apply handler that throws an exception if the parser is not initialized.
     */
    private Function<Input<I>, Result<I, A>> defaultApplyHandler;


    /**
     * Private constructor to create a parser reference that can be initialized later.
     */
    protected Parser() {
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
     *   <li>Returns a {@link Result} object containing either a Match or a NoMatch result</li>
     * </ol>
     * <p>
     * This constructor is the primary way to create custom parsers with specific parsing logic. Most users
     * will not need to use this constructor directly, instead using combinators and factory methods to
     * build parsers from simpler components.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>The apply handler should return a Match result with the parsed value if parsing succeeds</li>
     *   <li>The apply handler should return a NoMatch result with an error message if parsing fails</li>
     *   <li>The apply handler is responsible for properly advancing the input position on success</li>
     *   <li>Thread safety is maintained as parsers are immutable after construction</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a custom parser that recognizes a specific pattern
     * Parser<Character, String> customParser = new Parser<>(input -> {
     *     if (input.atEnd()) {
     *         return new NoMatch<>(input, "Unexpected end of input");
     *     }
     *
     *     // Check for a specific pattern
     *     if (input.value() == 'a' && input.value(1) == 'b') {
     *         return new Match<>("ab", input.advance(2));
     *     }
     *
     *     return new NoMatch<>(input, "Expected 'ab'");
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
     * expr.set(number.or(parens).chainLeftOneOrMore(addOp));
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
        return new CheckParser<>();
    }

    private static class CheckParser<I, A> extends Parser<I, A> {

        private final ThreadLocal<IntObjectMap<ArrayDeque<Parser<?, ?>>>> contextLocal =
            ThreadLocal.withInitial(IntObjectMap::new);

        @Override
        public Result<I, A> apply(Input<I> in) {
            int pos = in.position();
            IntObjectMap<ArrayDeque<Parser<?, ?>>> ctx = contextLocal.get();

            ArrayDeque<Parser<?, ?>> stack = ctx.get(pos);
            if (stack == null) {
                stack = new ArrayDeque<>();
                ctx.put(pos, stack);
            }

            for (Parser<?, ?> p : stack) {
                if (p == this) {
                    return new NoMatch<>(in, "no infinite recursion");
                }
            }

            stack.push(this);
            try {
                return applyHandler.apply(in);
            } catch (RuntimeException e) {
                return new NoMatch<>(in, "parser to function correctly");
            } finally {
                stack.pop();
                if (stack.isEmpty()) {
                    ctx.remove(pos);
                }
            }
        }
    }

    /**
     * Creates a parser that attempts to recover from errors by trying an alternative parser.
     * <p>
     * If this parser succeeds, its result is returned. If it fails, the recovery parser is applied
     * to the same input position.
     * <p>
     * This is useful for error recovery in situations where there are multiple valid alternatives,
     * and you want to try them in sequence.
     *
     * @param recovery the parser to try if this parser fails
     * @param <B> the result type of the recovery parser
     * @return a new parser that tries the recovery parser if this parser fails
     */
    public <B> Parser<I, B> recover(Parser<I, B> recovery) {
        return new Parser<>(input -> {
            Result<I, A> result = this.apply(input);
            if (result.matches()) {
                return result.cast();
            }
            return recovery.apply(input);
        });
    }

    /**
     * Creates a parser that attempts to recover from errors by applying a function
     * to the failure result.
     * <p>
     * If this parser succeeds, its result is returned. If it fails, the recovery function is applied
     * to the failure result to produce a new result.
     * <p>
     * This is useful for custom error recovery strategies, such as:
     * <ul>
     *   <li>Transforming errors into default values</li>
     *   <li>Implementing domain-specific error recovery logic</li>
     * </ul>
     *
     * @param recovery the function to apply to the failure result
     * @param <B> the result type of the recovery function
     * @return a new parser that applies the recovery function if this parser fails
     */
    public <B> Parser<I, B> recoverWith(Function<NoMatch<I, A>, Result<I, B>> recovery) {
        return new Parser<>(input -> {
            Result<I, A> result = this.apply(input);
            if (result.matches()) {
                return result.cast();
            }
            return recovery.apply((NoMatch<I, A>) result);
        });
    }

    /**
     * Labels this parser with a human-readable expectation for error messages.
     * <pre>{@code
     * Parser<Character, String> p = Lexical.word.expecting("identifier");
     * p.parse("123").matches(); // false, error: "Expected identifier"
     * }</pre>
     *
     * @param label descriptive label
     * @return a labeled parser
     */
    public <B> Parser<I, A> expecting(String label) {
        return new Parser<>(input -> {
            Result<I, A> result = this.apply(input);
            if (result.matches()) return result;
            return new NoMatch<>(input, label, (Failure<?, ?>) result);
        });
    }

    /**
     * Sequences this parser with another parser chosen based on this parser's result.
     * <pre>{@code
     * Parser<Character, String> p = Numeric.unsignedInteger.flatMap(n -> 
     *     Lexical.chr(',').skipThen(Lexical.chr('a').repeat(n)).map(Lists::join)
     * );
     * p.parse("3,aaa").value(); // "aaa"
     * }</pre>
     *
     * @param f   function returning the next parser
     * @param <B> next result type
     * @return a monadic parser
     */
    public <B> Parser<I, B> flatMap(java.util.function.Function<A, Parser<I, B>> f) {
        if (f == null) {
            throw new IllegalArgumentException("flatMap function cannot be null");
        }
        return new Parser<>(in -> {
            Result<I, A> r = this.apply(in);
            if (!r.matches()) {
                // propagate the original failure
                return r.cast();
            }
            Parser<I, B> next = f.apply(r.value());
            if (next == null) {
                // be defensive to help users diagnose nulls
                return new NoMatch<I, B>(r.input(), "parser to function correctly").cast();
            }
            Result<I, B> rb = next.apply(r.input());
            if (!rb.matches()) {
                return new PartialMatch<>(rb.input(), (Failure<I, B>) rb);
            }
            return rb;
        });
    }

}
