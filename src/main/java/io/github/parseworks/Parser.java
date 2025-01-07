package io.github.parseworks;

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
        return new Parser<>() {
            @Override
            public Result<I, B> apply(Input<I> in) {
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
            }
        };
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
        final Parser<I, A> parserAndEof = this.andL(Combinators.eof());
        return parserAndEof.apply(in);
    }

    public Result<I, A> apply(Input<I> in) {
        Result<I, A> result = null;
        if (this.position == in.position()) {
            result = Result.failure(in, "Infinite loop detected");
        } else {
            this.position = in.position();
            result = applyHandler.apply(in);
        }
        this.position = -1;
        return result;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean accepts(Parser<I, A> other, int position) {
        return !(this == other && this.position == position);
    }

    public <B> Parser<I, A> andL(Parser<I, B> pb) {
        return this.and(pb).map((a, b) -> a);
    }

    public <B> Parser<I, B> andR(Parser<I, B> pb) {
        return new Parser<>() {
            @Override
            public Result<I, B> apply(Input<I> in) {
                Result<I, A> bounce = Parser.this.apply(in);
                if (!bounce.isSuccess()) {
                    return bounce.cast();
                }
                Input<I> in2 = bounce.next();
                return pb.apply(in2);
            }
        };
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
        return open.andR(this).andL(close);
    }

    public Parser<I, FList<A>> many() {
        return new Parser<>( in -> {
            FList<A> results = new FList<>();
            Input<I> currentInput = in;
            while (true) {
                Result<I, A> result = this.apply(currentInput);
                if (!result.isSuccess()) {
                    break;
                }
                results.add(result.getOrThrow());
                currentInput = result.next();
            }
            return Result.success(currentInput, results);
        });
    }

    /**
     * Parse left-associative operator expressions.
     *
     * @param op the parser for the binary operator
     * @return a parser that parses left-associative operator expressions
     */
    public Parser<I, A> chainl1(Parser<I, BinaryOperator<A>> op) {
        final Parser<I, UnaryOperator<A>> plo =
                op.and(this)
                        .map((f, y) -> x -> f.apply(x, y));
        return this.and(plo.many())
                .map((a, lf) -> {
                    if (lf.isEmpty()) {
                        return a;
                    }
                    return lf.foldLeft(a, (acc, f) -> f.apply(acc));
                });
    }

    public <B> ApplyBuilder<I, A, B> and(Parser<I, B> pb) {
        return ApplyBuilder.of(this, pb);
    }

    public <R> Parser<I, R> map(Function<A, R> f) {
        return new Parser<>( in -> apply(in).map(f));
    }

    /**
     * Parse right-associative operator expressions.
     *
     * @param binaryOperatorParser the parser for the binary operator
     * @return a parser that parses right-associative operator expressions
     */
    public Parser<I, A> chainr1(Parser<I, BinaryOperator<A>> binaryOperatorParser) {
        return this.and(
                binaryOperatorParser.and(this)
                        .map(Pair::new)
                        .many()
        ).map((a, tuples) ->
                tuples.stream()
                        .reduce(a, (acc, tuple) -> tuple.first().apply(tuple.second(), acc), (a1, a2) -> a1)
        );
    }

    /**
     * A parser that applies this parser one or more times until it fails,
     * and then returns a non-empty list of the results.
     * If this parser fails on the first attempt, the parser fails.
     *
     * @return a parser that applies this parser repeatedly until it fails
     */
    public Parser<I, FList<A>> oneOrMore() {
        return this.and(this.many())
                .map(a -> l -> l.push(a));
    }

    public Parser<I, A> or(Parser<I, A> other) {
        return new Parser<>( in -> {
            Result<I, A> result = this.apply(in);
            if (result.isSuccess()) {
                return result;
            }
            return other.apply(in);
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
    public <SEP> Parser<I, FList<A>> sepBy(Parser<I, SEP> sep) {
        return this.sepBy1(sep).map(l -> l)
                .or(pure(new FList<>()));
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
        return this.and(sep.andR(this).many())
                .map(a -> l -> {l.add(a); return l;});
    }

    public Parser<I, A> chainl(Parser<I, BinaryOperator<A>> op, A i) {
        return this.chainl1(op).or(pure(i));
    }

    public Parser<I, A> chainr(Parser<I, BinaryOperator<A>> op, A i) {
        return this.chainr1(op).or(pure(i));
    }

    public Parser<I, Optional<A>> optional() {
        return this.map(Optional::of).or(pure(Optional.empty()));
    }
}