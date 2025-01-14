package io.github.parseworks;

import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * The `Parser` class represents a parser that can parse input of type `I` and produce a result of type `A`.
 *
 * @param <I> the type of the input symbols
 * @param <A> the type of the parsed value
 */
public class Parser<I, A> {

    protected Function<Input<I>, Result<I, A>> applyHandler;
    int index = -1;

    public Parser() {
        this.applyHandler = in -> Result.failure(in, "Parser not initialized");
    }

    public Parser(Function<Input<I>, Result<I, A>> applyHandler) {
        this.applyHandler = applyHandler;
    }

    /**
     * Creates a new reference to a parser.
     *
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a new reference to a parser
     */
    public static <I, A> Ref<I, A> ref() {
        return new Ref<>();
    }

    /**
     * Applies a function provided by one parser to the result of another parser.
     *
     * @param functionProvider the parser that provides the function
     * @param valueParser the parser that provides the value
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @param <B> the type of the result of the function
     * @return a parser that applies the function to the value
     */
    public static <I, A, B> Parser<I, B> ap(Parser<I, Function<A, B>> functionProvider, Parser<I, A> valueParser) {
        return new Parser<>(in -> {
            Result<I, Function<A, B>> functionResult = functionProvider.apply(in);
            if (!functionResult.isSuccess()) {
                return functionResult.cast();
            }
            Function<A, B> func = functionResult.getOrThrow();
            Input<I> in2 = functionResult.next();
            Result<I, A> valueResult = valueParser.apply(in2);
            if (!valueResult.isSuccess()) {
                return valueResult.cast();
            }
            return valueResult.map(func);
        });
    }

    /**
     * Applies a function to the result of a parser.
     *
     * @param f the function to apply
     * @param pa the parser that provides the value
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @param <B> the type of the result of the function
     * @return a parser that applies the function to the value
     */
    public static <I, A, B> Parser<I, B> ap(Function<A, B> f, Parser<I, A> pa) {
        return ap(pure(f), pa);
    }

    /**
     * Applies a function provided by a parser to a constant value.
     *
     * @param pf the parser that provides the function
     * @param a the constant value
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @param <B> the type of the result of the function
     * @return a parser that applies the function to the value
     */
    public static <I, A, B> Parser<I, B> ap(Parser<I, Function<A, B>> pf, A a) {
        return ap(pf, pure(a));
    }

    /**
     * Creates a parser that always succeeds with the given value.
     *
     * @param value the value to return
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a parser that always succeeds with the given value
     */
    public static <I, A> Parser<I, A> pure(A value) {
        return new Parser<>(in -> Result.success(in, value));
    }

    /**
     * Parses the input and ensures that the entire input is consumed.
     *
     * @param in the input to parse
     * @return the result of parsing the input
     */
    public Result<I, A> parse(Input<I> in) {
        return this.thenSkipRight(Combinators.eof()).apply(in);
    }

    /**
     * Applies this parser to the given input.
     * If the parser detects an infinite loop, it returns a failure result.
     *
     * @param in the input to parse
     * @return the result of parsing the input
     */
    public Result<I, A> apply(Input<I> in) {
        if (this.index == in.position()) {
            return Result.failure(in, "Infinite loop detected");
        }
        this.index = in.position();
        Result<I, A> result = applyHandler.apply(in);
        this.index = -1;
        return result;
    }

    public <B> Parser<I, A> thenSkipRight(Parser<I, B> pb) {
        return this.then(pb).map((a, b) -> a);
    }

    public <B> Parser<I, B> skipLeftThen(Parser<I, B> pb) {
        return new Parser<>(in -> {
            Result<I, A> left = this.apply(in);
            if (!left.isSuccess()) {
                return left.cast();
            }
            return pb.apply(left.next());
        });
    }

    /**
     * A parser for expressions with enclosing symbols.
     * Applies the open parser, then this parser, and then the close parser.
     * If all three succeed, the result of this parser is returned.
     *
     * @param open    the open symbol parser
     * @param close   the close symbol parser
     * @param <OPEN>  the open parser result type
     * @param <CLOSE> the close parser result type
     * @return a parser for expressions with enclosing symbols
     */
    public <OPEN, CLOSE> Parser<I, A> between(Parser<I, OPEN> open, Parser<I, CLOSE> close) {
        return open.skipLeftThen(this).thenSkipRight(close);
    }

    /**
     * A parser for expressions with enclosing bracket symbols .
     * Applies the open parser, then this parser, and then the close parser.
     * If all three succeed, the result of this parser is returned.
     *
     * @param bracket   the close symbol parser
     * @param <B> the close parser result type
     * @return a parser for expressions with enclosing symbols
     */
    public <B> Parser<I, A> between(Parser<I, B> bracket ) {
        return between(bracket, bracket);
    }

    public static <I, A> Parser<I, A> fail() {
        return new Parser<>(in -> Result.failure(in, "Parser failed"));
    }

    /**
     * OneOrMore combinator: applies the parser one or more times and collects the results.
     *
     * @param parser the parser to apply repeatedly
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a parser that applies the given parser one or more times and collects the results
     */
    public static <I,A> Parser<I,FList<A>> oneOrMore(Parser<I,A> parser) {
        return parser.then(parser.zeroOrMore()).map((a, l) -> l.push(a));
    }


    public Parser<I, FList<A>> zeroOrMore() {
        return new Parser<>( in -> {
            FList<A> results = new FList<>();
            for (Input<I> currentInput = in; ; ) {
                Result<I, A> result = this.apply(currentInput);
                if (!result.isSuccess()) {
                    return Result.success(currentInput, results);
                }
                results.add(result.getOrThrow());
                currentInput = result.next();
            }
        });
    }

    /**
     * Parse left-associative operator expressions.
     *
     * @param op the parser for the binary operator
     * @return a parser that parses left-associative operator expressions
     */
    public Parser<I, A> opChainLeft(Parser<I, BinaryOperator<A>> op) {
        Parser<I, UnaryOperator<A>> plo = op.then(this).map((f, y) -> x -> f.apply(x, y));
        return this.then(plo.zeroOrMore()).map((a, lf) -> lf.isEmpty() ? a : lf.foldLeft(a, (acc, f) -> f.apply(acc)));
    }

    /**
     * Chains this parser with another parser, applying them in sequence.
     * The result of the first parser is passed to the second parser.
     *
     * @param next the next parser to apply in sequence
     * @param <B> the type of the result of the next parser
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
                return Result.success(trimmedInput, result.getOrThrow());
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
     * @param <R> the type of the transformed result
     * @return a parser that applies the given function to the parsed result
     */
    public <R> Parser<I, R> map(Function<A, R> func) {
        return new Parser<>( in -> apply(in).map(func));
    }

    /**
     * Transforms the result of this parser to a constant value.
     *
     * @param value the constant value to return
     * @param <R> the type of the constant value
     * @return a parser that returns the constant value regardless of the input
     */
    public <R> Parser<I, R> as(R value) {
        return this.skipLeftThen(pure(value));
    }

    /**
     * Wraps the 'this' parser to only call it if the provided parser returns a fail
     *
     * @param parser the parser to negate
     * @return a parser that succeeds if the given parser fails, and fails if the given parser succeeds
     */
    public Parser<I, A> not(Parser<I, A> parser) {
        return new Parser<>(in -> {
            Result<I, A> result = parser.apply(in);
            if (result.isSuccess()) {
                return Result.failure(in, "Parse was successful for what we didn't want");
            }
            return this.apply(in);
        });
    }

    /**
     * Parse right-associative operator expressions.
     *
     * @param binaryOperatorParser the parser for the binary operator
     * @return a parser that parses right-associative operator expressions
     */
    public Parser<I, A> chainr1(Parser<I, BinaryOperator<A>> binaryOperatorParser) {
        return this.then(binaryOperatorParser.then(this).map(Pair::new).zeroOrMore())
                .map((a, tuples) -> tuples.stream().reduce(a, (acc, tuple) -> tuple.left().apply(tuple.right(), acc), (a1, a2) -> a1));
    }

    public Parser<I, A> zeroOrMoreOp(Parser<I, BinaryOperator<A>> binaryOperatorParser) {
        return this.then(binaryOperatorParser.then(this).map(Pair::new).optional())
                .map((a, optPair) -> optPair.map(pair -> pair.left().apply(a, pair.right())).orElse(a));
    }

    public enum Associative{
        RIGHT,LEFT
    }

    public Parser<I, A> zeroOrMoreOp(List<Pair<Parser<I, BinaryOperator<A>>, Associative>> binaryOperators) {
        return new Parser<>(in -> {
            Input<I> currentInput = in;
            Result<I, A> firstResult = this.apply(currentInput);
            if (!firstResult.isSuccess()) {
                return Result.failure(in, "No initial value parsed");
            }

            A result = firstResult.getOrThrow();
            currentInput = firstResult.next();

            while (true) {
                boolean matched = false;
                for (Pair<Parser<I, BinaryOperator<A>>, Associative> pair : binaryOperators) {
                    Result<I, BinaryOperator<A>> opResult = pair.left().apply(currentInput);
                    if (opResult.isSuccess()) {
                        BinaryOperator<A> operator = opResult.getOrThrow();
                        Input<I> nextInput = opResult.next();
                        Result<I, A> valueResult = this.apply(nextInput);
                        if (valueResult.isSuccess()) {
                            A value = valueResult.getOrThrow();
                            result = pair.right() == Associative.LEFT ? operator.apply(result, value) : operator.apply(value, result);
                            currentInput = valueResult.next();
                            matched = true;
                            break;
                        }
                    }
                }
                if (!matched) {
                    break;
                }
            }
            return Result.success(currentInput, result);
        });
    }

    public Parser<I, A> rightAssociative(Parser<I, BinaryOperator<A>> binaryOperatorParser) {
        return this.then(binaryOperatorParser.then(this).map(Pair::new).optional())
                .map((a, optPair) -> optPair.map(pair -> pair.left().apply(a, pair.right())).orElse(a));
    }

    public Parser<I, A> leftAssociative(Parser<I, BinaryOperator<A>> binaryOperatorParser) {
        return this.then(binaryOperatorParser.then(this).map(Pair::new).optional())
                .map((a, optPair) -> optPair.map(pair -> pair.left().apply(a, pair.right())).orElse(a));
    }

    /**
     * A parser that applies this parser one or more times until it fails,
     * and then returns a non-empty list of the results.
     * If this parser fails on the left attempt, the parser fails.
     *
     * @return a parser that applies this parser repeatedly until it fails
     */
    public Parser<I, FList<A>> oneOrMore() {
        return this.then(this.zeroOrMore()).map(a -> l -> l.push(a));
    }

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
     * @throws IllegalArgumentException if the target of repetitions is negative or if target is greater than max
     */
    public Parser<I, FList<A>> repeatAtLeast(int target) {
        if (target < 0) {
            throw new IllegalArgumentException("The target of repetitions cannot be negative");
        }
        return new Parser<>(in -> {
            FList<A> accumulator = new FList<>();
            Input<I> currentInput = in;
            int count = 0;
            while (true) {
                if (currentInput.isEof() && count < target) {
                    return Result.failureEof(currentInput, "Unexpected end of input");
                }
                Result<I, A> result = this.apply(currentInput);
                if (result.isSuccess()) {
                    accumulator.add(result.getOrThrow());
                    currentInput = result.next();
                    count++;
                } else {
                    return count >= target ? Result.success(currentInput, accumulator) : Result.failure(currentInput, "Parsing failed before reaching the required target of repetitions");
                }
            }
        });
    }

    /**
     * A parser that applies this parser the `target` number of times
     * If the parser fails before reaching the target of repetitions, the parser fails.
     * If the parser succeeds at least `target` times, the results are collected in a list and returned by the parser.
     *
     * @param target the target number of times to apply this parser
     * @return a parser that applies this parser the 'target' number of times
     * @throws IllegalArgumentException if the target of repetitions is negative or if target is greater than max
     */
    public Parser<I, FList<A>> repeat(int target) {
        if (target < 0) {
            throw new IllegalArgumentException("The target of repetitions cannot be negative");
        }
        return new Parser<>(in -> {
            FList<A> accumulator = new FList<>();
            Input<I> currentInput = in;
            int count = 0;
            while (count < target) {
                if (currentInput.isEof()) {
                    return Result.failureEof(currentInput, "Unexpected end of input");
                }
                Result<I, A> result = this.apply(currentInput);
                if (result.isSuccess()) {
                    accumulator.add(result.getOrThrow());
                    currentInput = result.next();
                    count++;
                } else {
                    return Result.failure(currentInput, "Parsing failed before reaching the required target of repetitions");
                }
            }
            return Result.success(currentInput,accumulator);
        });
    }

    /**
     * A parser that applies this parser between `min` and `max` times.
     * If the parser fails before reaching the minimum number of repetitions, the parser fails.
     * If the parser succeeds at least `min` times and at most `max` times, the results are collected in a list and returned by the parser.
     *
     * @param min the minimum number of times to apply this parser
     * @param max the maximum number of times to apply this parser
     * @return a parser that applies this parser between `min` and `max` times
     * @throws IllegalArgumentException if the number of repetitions is negative or if min is greater than max
     */
    public Parser<I, FList<A>> repeat(int min, int max) {
        if (min < 0 || max < 0) {
            throw new IllegalArgumentException("The number of repetitions cannot be negative");
        }
        if (min > max) {
            throw new IllegalArgumentException("The minimum number of repetitions cannot be greater than the maximum");
        }
        return new Parser<>(in -> {
            FList<A> accumulator = new FList<>();
            Input<I> currentInput = in;
            int count = 0;
            while (count < max) {
                if (currentInput.isEof()) {
                    if (count >= min) {
                        return Result.success(currentInput,accumulator);
                    } else {
                        return Result.failureEof(currentInput, "Unexpected end of input");
                    }
                }
                Result<I, A> result = this.apply(currentInput);
                if (result.isSuccess()) {
                    accumulator.add(result.getOrThrow());
                    currentInput = result.next();
                    count++;
                } else {
                    if (count >= min) {
                        return Result.success(currentInput,accumulator);
                    } else {
                        return Result.failure(currentInput, "Parsing failed before reaching the required number of repetitions");
                    }
                }
            }
            return Result.success(currentInput,accumulator);
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
    public <SEP> Parser<I, FList<A>> separatedBy(Parser<I, SEP> sep) {
        return this.sepBy1(sep).map(l -> l).or(pure(new FList<>()));
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
    public <SEP> Parser<I, FList<A>> sepBy1(Parser<I, SEP> sep) {
        return this.then(sep.skipLeftThen(this).zeroOrMore()).map(a -> l -> {l.add(a); return l;});
    }

    public Parser<I, A> opChainLeft(Parser<I, BinaryOperator<A>> op, A i) {
        return this.opChainLeft(op).or(pure(i));
    }

    public Parser<I, A> opChainRight(Parser<I, BinaryOperator<A>> op, A i) {
        return this.chainr1(op).or(pure(i));
    }

    public Parser<I, Optional<A>> optional() {
        return this.map(Optional::of).or(pure(Optional.empty()));
    }
}