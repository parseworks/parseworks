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

    Function<Input<I>, Result<I, A>> applyHandler;
    int position = -1;

    public Parser() {
        this.applyHandler = in -> Result.failure(in, "Parser not initialized");
    }

    public Parser(Function<Input<I>, Result<I, A>> applyHandler) {
        this.applyHandler = applyHandler;
    }

    public static <I, A> Ref<I, A> ref() {
        return new Ref<>();
    }

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

    public static <I, A, B> Parser<I, B> ap(Function<A, B> f, Parser<I, A> pa) {
        return ap(pure(f), pa);
    }

    public static <I, A, B> Parser<I, B> ap(Parser<I, Function<A, B>> pf, A a) {
        return ap(pf, pure(a));
    }

    public static <I, A> Parser<I, A> pure(A value) {
        return new Parser<>( in -> Result.success(in, value));
    }

    public Result<I, A> parse(Input<I> in) {
        return this.thenSkip(Combinators.eof()).apply(in);
    }

    public Result<I, A> apply(Input<I> in) {
        if (this.position == in.position()) {
            return Result.failure(in, "Infinite loop detected");
        }
        this.position = in.position();
        Result<I, A> result = applyHandler.apply(in);
        this.position = -1;
        return result;
    }

    public <B> Parser<I, A> thenSkip(Parser<I, B> pb) {
        return this.then(pb).map((a, b) -> a);
    }

    public <B> Parser<I, B> skipThen(Parser<I, B> pb) {
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
        return open.skipThen(this).thenSkip(close);
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

    public <B> ApplyBuilder<I, A, B> then(Parser<I, B> next) {
        return ApplyBuilder.of(this, next);
    }

    public <R> Parser<I, R> map(Function<A, R> func) {
        return new Parser<>( in -> apply(in).map(func));
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
        return this.then(sep.skipThen(this).zeroOrMore()).map(a -> l -> {l.add(a); return l;});
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