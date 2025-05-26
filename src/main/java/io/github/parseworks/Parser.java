package io.github.parseworks;

import io.github.parseworks.impl.IntObjectMap;
import io.github.parseworks.impl.parser.NoCheckParser;

import java.util.*;
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

    protected Function<Input<I>, Result<I, A>> applyHandler;
    protected static final String INFINITE_LOOP_ERROR = "Infinite loop detected";
    /**
     * A default apply handler that throws an exception if the parser is not initialized.
     */
    private Function<Input<I>, Result<I, A>> defaultApplyHandler;

    protected final ThreadLocal<IntObjectMap<Object>> contextLocal = ThreadLocal.withInitial(IntObjectMap::new);
    private static final ThreadLocal<int[]> POSITIONS = ThreadLocal.withInitial(() -> new int[64]);
    private static final ThreadLocal<Object[]> PARSERS = ThreadLocal.withInitial(() -> new Object[64]);
    private static final ThreadLocal<Integer> SIZE = ThreadLocal.withInitial(() -> 0);

    /**
     * Private constructor to create a parser reference that can be initialized later.
     */
    private Parser() {
        this.applyHandler = defaultApplyHandler = in -> { throw new IllegalStateException("Parser not initialized"); };
    }

    /**
     * Creates a reference to a parser that can be initialized later.
     *
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a new parser reference
     */
    public static <I,A> Parser<I,A> ref() {
        return new Parser<>();
    }

    /**
     * Constructor for the Parser class.
     *
     * @param applyHandler a {@link java.util.function.Function} object
     */
    public Parser(Function<Input<I>, Result<I, A>> applyHandler) {
        if (applyHandler == null) {
            throw new IllegalArgumentException("applyHandler cannot be null");
        }
        this.applyHandler = applyHandler;
    }

    /**
     * Sets the applyHandler to the provided parser's applyHandler, for an uninitialized parser.
     *
     * @param parser the parser to set
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
     * Sets the applyHandler to the provided function, for an uninitialized parser.
     *
     * @param applyHandler the function to set as the applyHandler
     */
    public synchronized void set(Function<Input<I>, Result<I, A>> applyHandler){
        if (applyHandler == null) {
            throw new IllegalArgumentException("applyHandler cannot be null");
        }
        if (this.applyHandler != defaultApplyHandler) {
            throw new IllegalStateException("Parser already has an applyHandler");
        }
        this.applyHandler = applyHandler;
    }

    /**
     * Applies a function provided by one parser to the result of another parser.
     *
     * @param functionProvider the parser that provides the function
     * @param valueParser      the parser that provides the value
     * @param <I>              the type of the input symbols
     * @param <A>              the type of the parsed value
     * @param <B>              the type of the result of the function
     * @return a parser that applies the function to the value
     */
    public static <I, A, B> Parser<I, B> apply(Parser<I, Function<A, B>> functionProvider, Parser<I, A> valueParser) {
        return new NoCheckParser<>(in -> {
            Result<I, Function<A, B>> functionResult = functionProvider.apply(in);
            if (functionResult.isError()) {
                return functionResult.cast();
            }
            Function<A, B> func = functionResult.get();
            Input<I> in2 = functionResult.next();
            Result<I, A> valueResult = valueParser.apply(in2);
            if (valueResult.isError()) {
                return valueResult.cast();
            }
            return valueResult.map(func);
        });
    }

    /**
     * Applies a function to the result of a parser.
     *
     * @param f   the function to apply
     * @param pa  the parser that provides the value
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @param <B> the type of the result of the function
     * @return a parser that applies the function to the value
     */
    public static <I, A, B> Parser<I, B> apply(Function<A, B> f, Parser<I, A> pa) {
        return apply(pure(f), pa);
    }

    /**
     * Applies a function provided by a parser to a constant value.
     *
     * @param pf  the parser that provides the function
     * @param a   the constant value
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @param <B> the type of the result of the function
     * @return a parser that applies the function to the value
     */
    public static <I, A, B> Parser<I, B> apply(Parser<I, Function<A, B>> pf, A a) {
        return apply(pf, pure(a));
    }

    /**
     * Creates a parser that always succeeds with the given value.
     *
     * @param value the value to return
     * @param <I>   the type of the input symbols
     * @param <A>   the type of the parsed value
     * @return a parser that always succeeds with the given value
     */
    public static <I, A> Parser<I, A> pure(A value) {
        return new NoCheckParser<>(in -> Result.success(in, value));
    }


    /**
     * Parses the input and optionally ensures that the entire input is consumed.
     * This is the core parsing method that other parse methods delegate to.
     *
     * @param in         the input to parse
     * @param consumeAll whether to consume the entire input
     * @return the result of parsing the input
     */
    public Result<I, A> parse(Input<I> in, boolean consumeAll) {
        Result<I, A> result = this.apply(in);
        if (consumeAll && result.isSuccess()) {
            result = result.next().isEof() ? result : Result.failure(result.next(), "eof");
        }
        return result;
    }

    /**
     * Parses the input string and optionally ensures that the entire input is consumed.
     *
     * @param input      the input string to parse
     * @param consumeAll whether to consume the entire input
     * @return the result of parsing the input string
     */
    @SuppressWarnings("unchecked")
    public Result<I, A> parse(String input, boolean consumeAll) {
        Input<I> in = (Input<I>) Input.of(input);
        return this.parse(in, consumeAll);
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
     * Parses the input string without requiring that the entire input is consumed.
     *
     * @param input the input string to parse
     * @return the result of parsing the input string
     */
    @SuppressWarnings("unchecked")
    public Result<I, A> parse(String input) {
        Input<I> in = (Input<I>) Input.of(input);
        return parse(in);
    }

    /**
     * Parses the input and ensures that the entire input is consumed.
     *
     * @param in the input to parse
     * @return the result of parsing the input
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
    public Result<I, A> parseAll(String input) {
        Input<I> in = (Input<I>) Input.of(input);
        return parseAll(in);
    }

    /**
     * Applies this parser to the given input.
     * <p>
     * This method attempts to parse the provided input using the parser's `applyHandler` function.
     * It first checks for an infinite loop by comparing the current input position with the last
     * recorded position for this parser. If an infinite loop is detected, it returns a failure result.
     * <p>
     * If no infinite loop is detected, it updates the last recorded position to the current input
     * position and applies the `applyHandler` function to the input. After parsing, it resets the
     * last recorded position and returns the result of the parsing operation.
     * <p>
     * This method is typically used internally by other parsing methods to perform the actual
     * parsing logic.
     *
     * @param in the input to parse
     * @return the result of parsing the input, which can be either a success or a failure
     */

    public Result<I, A> apply(Input<I> in) {
        int position = in.position();
        int[] positions = POSITIONS.get();
        Object[] parsers = PARSERS.get();
        int size = SIZE.get();

        // Linear search is faster for small arrays (typically the case for parser depth)
        for (int i = 0; i < size; i++) {
            if (positions[i] == position && parsers[i] == this) {
                return Result.failure(in, null, INFINITE_LOOP_ERROR);
            }
        }

        // Resize if needed
        if (size == positions.length) {
            int newLength = positions.length * 2;
            POSITIONS.set(Arrays.copyOf(positions, newLength));
            PARSERS.set(Arrays.copyOf(parsers, newLength));
            positions = POSITIONS.get();
            parsers = PARSERS.get();
        }

        // Add current position
        positions[size] = position;
        parsers[size] = this;
        SIZE.set(size + 1);

        try {
            return applyHandler.apply(in);
        } finally {
            SIZE.set(size); // Restore previous size
        }
    }
    /**
     * Chains this parser with another parser, applying them in sequence.
     * The result of the first parser is returned, and the result of the second parser is ignored.
     *
     * @param pb  the next parser to apply in sequence
     * @param <B> the type of the result of the next parser
     * @return a parser that applies this parser and then the next parser, returning the result of this parser
     */
    public <B> Parser<I, A> thenSkip(Parser<I, B> pb) {
        return this.then(pb).map((a, b) -> a);
    }

    /**
     * Chains this parser with another parser, applying them in sequence.
     * The result of the first parser is ignored, and the result of the second parser is returned.
     *
     * @param pb  the next parser to apply in sequence
     * @param <B> the type of the result of the next parser
     * @return a parser that applies this parser and then the next parser, returning the result of the next parser
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
     * A parser that applies this parser zero or more times until it fails,
     * and then returns a list of the results.
     * If this parser fails on the first attempt, an empty list is returned.
     * <p>
     * This method repeatedly applies the parser to the input until it fails.
     * The results of each successful application are collected into a list.
     * If the parser fails on the first attempt, an empty list is returned.
     * <p>
     * This method is useful for parsing sequences of elements where the number
     * of elements is not known in advance.
     *
     * @return a parser that applies this parser repeatedly until it fails
     */
    public Parser<I, FList<A>> zeroOrMany() {
        return repeatInternal(0, Integer.MAX_VALUE, null);
    }

    /**
     * A parser that applies this parser zero or more times until the until parser succeeds,
     * and then returns a list of the results.
     * The terminator is consumed when found.
     * If this parser fails on the first attempt, an empty list is returned.
     *
     * @param terminator the parser that signals when to stop collecting
     * @return a parser that applies this parser repeatedly until the until parser succeeds
     */
    public Parser<I, FList<A>> zeroOrManyUntil(Parser<I, ?> terminator) {
        return repeatInternal(0, Integer.MAX_VALUE, terminator);
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
     * Chains this parser with another parser, applying them in sequence.
     *
     * @param next the next parser to apply in sequence
     * @param <B>  the type of the result of the next parser
     * @return an ApplyBuilder that allows further chaining of parsers
     */
    public <B> ApplyBuilder<I, A, B> then(Parser<I, B> next) {
        return ApplyBuilder.of(this, next);
    }

    /**
     * Trims leading and trailing whitespace from the input, before and after applying this parser.
     *
     * @return a parser that trims leading and trailing whitespace
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
     * Transforms the result of this parser using the given function.
     *
     * @param func the function to apply to the parsed result
     * @param <R>  the type of the transformed result
     * @return a parser that applies the given function to the parsed result
     */
    public <R> Parser<I, R> map(Function<A, R> func) {
        return new Parser<>(in -> apply(in).map(func));
    }

    /**
     * Transforms the result of this parser to a constant value.
     *
     * @param value the constant value to return
     * @param <R>   the type of the constant value
     * @return a parser that returns the constant value regardless of the input
     */
    public <R> Parser<I, R> as(R value) {
        return this.skipThen(pure(value));
    }

    /**
     * If this parser fails, it returns the other value.
     *
     * @param other the value to return if this parser fails
     * @return a parser that returns the other value if this parser fails
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
     *
     *
     * @param parser
     * @return
     * @param <B>
     */
    public <B> Parser<I, A> not(Parser<I, B> parser) {
        return new Parser<>(in -> {
            Result<I,B> result = parser.apply(in);
            if (result.isSuccess()) {
                return Result.failure(in, "Parser to fail", String.valueOf(result.get()));
            }
            return this.apply(in);
        });
    }

    /**
     * Checks if the result of this parser is not equal to the given value.
     *
     * @param value the value to check against
     * @return a parser that succeeds if the result of the parser is not equal to the given value
     */
    public Parser<I, A> isNot(I value) {
        return this.not(is(value));
    }

    /**
     * Chains this parser with an operator parser, applying them in sequence based on the specified associativity.
     * The result of the first parser is combined with the results of subsequent parsers using the operator.
     *
     * @param op            the parser for the binary operator
     * @param associativity the associativity of the operator (LEFT or RIGHT)
     * @return a parser that applies this parser and the operator parser in sequence
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
     * A parser for an operand, followed by zero or more operands that are separated by operators.
     * The operators are right-associative.
     *
     * @param op the parser for the operator
     * @param a  the value to return if there are no operands
     * @return a parser for operator expressions
     */
    public Parser<I, A> chainRightZeroOrMany(Parser<I, BinaryOperator<A>> op, A a) {
        return this.chainRightMany(op).or(pure(a));
    }

    /**
     * Parse right-associative operator expressions.
     *
     * @param op the parser for the binary operator
     * @return a parser that parses right-associative operator expressions
     */
    public Parser<I, A> chainRightMany(Parser<I, BinaryOperator<A>> op) {
        return chain(op, Associativity.RIGHT);
    }


    /**
     * A parser for an operand, followed by zero or more operands that are separated by operators.
     * The operators are left-associative.
     * This can, for example, be used to eliminate left recursion
     * which typically occurs in expression grammars.
     *
     * @param op the parser for the operator
     * @param a  the value to return if there are no operands
     * @return a parser for operator expressions
     */
    public Parser<I, A> chainLeftZeroOrMany(Parser<I, BinaryOperator<A>> op, A a) {
        return this.chainLeftMany(op).or(pure(a));
    }


    /**
     * A parser for an operand, followed by one or more operands that are separated by operators.
     * The operators are left-associative.
     * This can, for example, be used to eliminate left recursion
     * which typically occurs in expression grammars.
     *
     * @param op the parser for the operator
     * @return a parser for operator expressions
     */
    public Parser<I, A> chainLeftMany(Parser<I, BinaryOperator<A>> op) {
        return chain(op, Associativity.LEFT);
    }


    /**
     * A parser that applies this parser one or more times until it fails,
     * and then returns a non-empty list of the results.
     * If this parser fails on the left attempt, the parser fails.
     *
     * @return a parser that applies this parser repeatedly until it fails
     */
    public Parser<I, FList<A>> many() {
        return repeatInternal(1, Integer.MAX_VALUE, null);
    }

    /**
     * A parser that applies this parser one or more times until the until parser succeeds,
     * and then returns a non-empty list of the results.
     * The terminator is consumed when found.
     * If this parser fails before any items are collected, the parser fails.
     *
     * @param until the parser that signals when to stop collecting
     * @return a parser that applies this parser repeatedly until the until parser succeeds
     */
    public Parser<I, FList<A>> manyUntil(Parser<I, ?> until) {
        return repeatInternal(1, Integer.MAX_VALUE, until);
    }

    /**
     * Creates a parser that tries this parser first, and if it fails, tries the other parser.
     * The result of the first successful parser is returned.
     *
     * @param other the other parser to try if this parser fails
     * @return a parser that tries this parser first, and if it fails, tries the other parser
     */
    public Parser<I, A> or(Parser<I, A> other) {
        return new Parser<>(in -> {
            Result<I, A> result = this.apply(in);
            return result.isSuccess() ? result : other.apply(in);
        });
    }

    /**
     * A parser that applies this parser the `target` number of times
     * If the parser fails before reaching the target of repetitions, the parser fails.
     * If the parser succeeds at least `target` times, the results are collected in a list and returned by the parser.
     *
     * @param target the target number of times to apply this parser
     * @return a parser that applies this parser the 'target' number of times
     * @throws java.lang.IllegalArgumentException if the target of repetitions is negative or if target is greater than max
     */
    public Parser<I, FList<A>> repeat(int target) {
        return repeatInternal(target, target, null);
    }

    /**
     * A parser that applies this parser the `target` number of times
     * If the parser fails before reaching the target of repetitions, the parser fails.
     * If the parser succeeds at least `target` times, the results are collected in a list and returned by the parser.
     *
     * @param target the target number of times to apply this parser
     * @return a parser that applies this parser the 'target' number of times
     * @throws java.lang.IllegalArgumentException if the target of repetitions is negative or if target is greater than max
     */
    public Parser<I, FList<A>> repeatAtLeast(int target) {
        return repeatInternal(target, Integer.MAX_VALUE, null);
    }

    /**
     * A parser that applies this parser at most `max` times
     * If the parser fails before reaching the target of repetitions, the parser passes.
     * If the parser succeeds at most `max` times, the results are collected in a list and returned by the parser.
     *
     * @param max the maximum number of times to apply this parser
     * @return a parser that applies this parser at most 'max' number of times
     * @throws java.lang.IllegalArgumentException if the number of repetitions is negative or if min is greater than max
     */
    public Parser<I, FList<A>> repeatAtMost(int max) {
        return repeatInternal(0, max, null);
    }


    /**
     * A parser that applies this parser between `min` and `max` times.
     * If the parser fails before reaching the minimum number of repetitions, the parser fails.
     * If the parser succeeds at least `min` times and at most `max` times, the results are collected in a list and returned by the parser.
     *
     * @param min the minimum number of times to apply this parser
     * @param max the maximum number of times to apply this parser
     * @return a parser that applies this parser between `min` and `max` times
     * @throws java.lang.IllegalArgumentException if the number of repetitions is negative or if min is greater than max
     */
    public Parser<I, FList<A>> repeat(int min, int max) {
        return repeatInternal(min, max, null);
    }

    private Parser<I, FList<A>> repeatInternal(int min, int max, Parser<I, ?> terminator) {
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
                if (terminator != null) {
                    Result<I, ?> termRes = terminator.apply(current);
                    if (termRes.isSuccess()) {
                        if (count < min) {
                            return Result.failure(current, "Expected at least " + min + " items");
                        }
                        return Result.success(termRes.next(), new FList<>(buffer));
                    }
                }
                // End-of-input or max reached
                if (current.isEof() || count >= max) {
                    if (count >= min && terminator == null) {
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
    /**
     * A parser that applies this parser zero or more times until it fails,
     * alternating with calls to the separator parser.
     * The results of this parser are collected in a list and returned by the parser.
     *
     * @param sep   the separator parser
     * @param <SEP> the separator type
     * @return a parser that applies this parser zero or more times alternated with the separator parser
     */
    public <SEP> Parser<I, FList<A>> separatedByZeroOrMany(Parser<I, SEP> sep) {
        return this.separatedByMany(sep).map(l -> l).or(pure(new FList<>()));
    }

    /**
     * A parser that applies this parser one or more times until it fails,
     * alternating with calls to the separator parser.
     * The results of this parser are collected in a non-empty list and returned by the parser.
     *
     * @param sep   the separator parser
     * @param <SEP> the separator type
     * @return a parser that applies this parser one or more times alternated with the separator parser
     */
    public <SEP> Parser<I, FList<A>> separatedByMany(Parser<I, SEP> sep) {
        return this.then(sep.skipThen(this).zeroOrMany()).map(a -> l -> l.prepend(a));
    }

    /**
     * Creates a parser that optionally applies this parser.
     * If this parser succeeds, the result is wrapped in an {@link java.util.Optional}.
     * If this parser fails, an empty {@link java.util.Optional} is returned.
     *
     * @return a parser that optionally applies this parser
     */
    public Parser<I, Optional<A>> optional() {
        return this.map(Optional::of).or(pure(Optional.empty()));
    }
}
