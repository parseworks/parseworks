package io.github.parseworks;

import io.github.parseworks.impl.NoCheckParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.github.parseworks.Parser.pure;
import static io.github.parseworks.Utils.failure;

/**
 * The `Combinators` class provides a set of combinator functions for creating complex parsers
 * by combining simpler ones. These combinators include choice, sequence, many, and satisfy.
 */
public class Combinators {

    /**
     * Choice combinator: tries each parser in the list until one succeeds.
     *
     * @param parsers the list of parsers to try
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a parser that tries each parser in the list until one succeeds
     */
    public static <I, A> Parser<I, A> choice(List<Parser<I, A>> parsers) {
        return new Parser<>(in ->
            parsers.stream().map(p -> p.apply(in)).filter(Result::isSuccess).findFirst().orElseGet(()-> failure(in))
        );
    }

    /**
     * Choice combinator: tries each parser in the list until one succeeds.
     *
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a parser that tries each parser in the list until one succeeds
     */
    public static <I, A> Parser<I, A> choice(Parser<I, A> parserA, Parser<I, A> parserB) {
        return choice(List.of(parserA,parserB));
    }

    /**
     * Choice combinator: tries each parser in the list until one succeeds.
     *
     * @param parserA the first parser to try
     * @param parserB the second parser to try
     * @param parserC the third parser to try
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a parser that tries each parser in the list until one succeeds
     */
    public static <I, A> Parser<I, A> choice(Parser<I, A> parserA, Parser<I, A> parserB, Parser<I, A> parserC) {
        return choice(List.of(parserA, parserB, parserC));
    }

    /**
     * Choice combinator: tries each parser in the list until one succeeds.
     *
     * @param parserA the first parser to try
     * @param parserB the second parser to try
     * @param parserC the third parser to try
     * @param parserD the fourth parser to try
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a parser that tries each parser in the list until one succeeds
     */
    public static <I, A> Parser<I, A> choice(Parser<I, A> parserA, Parser<I, A> parserB, Parser<I, A> parserC, Parser<I, A> parserD) {
        return choice(List.of(parserA, parserB, parserC, parserD));
    }

    /**
     * Choice combinator: tries each parser in the list until one succeeds.
     *
     * @param parserA the first parser to try
     * @param parserB the second parser to try
     * @param parserC the third parser to try
     * @param parserD the fourth parser to try
     * @param parserE the fifth parser to try
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a parser that tries each parser in the list until one succeeds
     */
    public static <I, A> Parser<I, A> choice(Parser<I, A> parserA, Parser<I, A> parserB, Parser<I, A> parserC, Parser<I, A> parserD, Parser<I, A> parserE) {
        return choice(List.of(parserA, parserB, parserC, parserD, parserE));
    }

    /**
     * Choice combinator: tries each parser in the list until one succeeds.
     *
     * @param parserA the first parser to try
     * @param parserB the second parser to try
     * @param parserC the third parser to try
     * @param parserD the fourth parser to try
     * @param parserE the fifth parser to try
     * @param parserF the sixth parser to try
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a parser that tries each parser in the list until one succeeds
     */
    public static <I, A> Parser<I, A> choice(Parser<I, A> parserA, Parser<I, A> parserB, Parser<I, A> parserC, Parser<I, A> parserD, Parser<I, A> parserE, Parser<I, A> parserF) {
        return choice(List.of(parserA, parserB, parserC, parserD, parserE, parserF));
    }

    /**
     * Creates a parser that succeeds if the input is at the end of the file (EOF).
     * This parser will return a successful result with a value of `null` if the input is at EOF,
     * and will fail with an error message "Expected end of file" if the input is not at EOF.
     *
     * @param <I> the type of input symbols
     * @return a parser that succeeds if the input is at EOF
     */
    public static <I> Parser<I, Void> eof() {
        return new Parser<>(input -> {
            if (input.isEof()) {
                return Result.success(input, null);
            } else {
                return Result.failure(input, "Expected end of file");
            }
        });
    }

    /**
     * Sequence combinator: applies each parser in sequence and collects the results.
     *
     * @param parsers the list of parsers to apply in sequence
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a parser that applies each parser in sequence and collects the results
     */
    public static <I, A> Parser<I, List<A>> sequence(List<Parser<I, A>> parsers) {
        return new Parser<>(in -> {
            List<A> results = new ArrayList<>();
            Input<I> currentInput = in;
            for (Parser<I, A> parser : parsers) {
                Result<I, A> result = parser.apply(currentInput);
                if (!result.isSuccess()) {
                    return failure(currentInput);
                }
                results.add(result.getOrThrow());
                currentInput = result.next();
            }
            return Result.success(currentInput, results);
        });
    }

    /**
     * Sequence combinator: applies each parser in sequence and collects the results.
     *
     * @param parserA the first parser to apply in sequence
     * @param parserB the second parser to apply in sequence
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a parser that applies each parser in sequence and collects the results
     */
    public static <I, A> ApplyBuilder<I, A, A> sequence(Parser<I, A> parserA, Parser<I, A> parserB) {
        return parserA.then(parserB);
    }

    /**
     * Sequence combinator: applies each parser in sequence and collects the results.
     *
     * @param parserA the first parser to apply in sequence
     * @param parserB the second parser to apply in sequence
     * @param parserC the second parser to apply in sequence
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a parser that applies each parser in sequence and collects the results
     */
    public static <I, A> ApplyBuilder<I, A, A>.ApplyBuilder3<A> sequence(Parser<I, A> parserA, Parser<I, A> parserB, Parser<I, A> parserC) {
        return parserA.then(parserB).then(parserC);
    }

    /**
     * ZeroOrMore combinator: applies the parser zero or more times and collects the results.
     *
     * @param parser the parser to apply repeatedly
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a parser that applies the given parser zero or more times and collects the results
     */
    public static <I, A> Parser<I, FList<A>> zeroOrMore(Parser<I, A> parser) {
        return new Parser<>( in -> {
            FList<A> results = new FList<>();
            for (Input<I> currentInput = in; ; ) {
                Result<I, A> result = parser.apply(currentInput);
                if (!result.isSuccess()) {
                    return Result.success(currentInput, results);
                }
                results.add(result.getOrThrow());
                currentInput = result.next();
            }
        });
    }


    /**
     * Satisfy combinator: parses a single item that satisfies the given predicate.
     *
     * @param predicate the predicate that the parsed item must satisfy
     * @param errorMessage the error message to use if the predicate is not satisfied
     * @param <I> the type of the input symbols
     * @return a parser that parses a single item that satisfies the given predicate
     */
    public static <I> Parser<I, I> satisfy(Predicate<I> predicate, Function<I,String> errorMessage) {
        return new NoCheckParser<>(in -> {
            if (in.isEof()) {
                return Result.failureEof(in, "Unexpected end of input");
            }
            I item = in.current();
            if (predicate.test(item)) {
                return Result.success(in.next(), item);
            } else {
                return Result.failure(in, errorMessage.apply(item));
            }
        });
    }

    /**
     * Satisfy combinator: parses a single item that satisfies the given predicate.
     *
     * @param predicate the predicate that the parsed item must satisfy
     * @param expectedType the error message to use if the predicate is not satisfied
     * @param <I> the type of the input symbols
     * @return a parser that parses a single item that satisfies the given predicate
     */
    public static <I> Parser<I, I> satisfy(Predicate<I> predicate, String expectedType) {
        return new NoCheckParser<>(in -> {
            if (in.isEof()) {
                return Result.failureEof(in, "Unexpected end of input. expected type "+expectedType);
            }
            I item = in.current();
            if (predicate.test(item)) {
                return Result.success(in.next(), item);
            } else {
                return Result.failure(in,"Failure at position %s, saw '%s', expected %s".formatted(in.position(),String.valueOf(in.current()),expectedType));
            }
        });
    }

    /**
     * Many1 combinator: applies the parser one or more times and collects the results.
     *
     * @param parser the parser to apply repeatedly
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a parser that applies the given parser one or more times and collects the results
     */
    public static <I, A> Parser<I, FList<A>> oneOrMore(Parser<I, A> parser) {
        return parser.then(zeroOrMore(parser)).map(a -> l -> { l.add(0, a); return l; });
    }

    /**
     * Optional combinator: tries to apply the parser and returns an Optional result.
     *
     * @param parser the parser to apply optionally
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a parser that tries to apply the given parser and returns an Optional result
     */
    public static <I, A> Parser<I, Optional<A>> optional(Parser<I, A> parser) {
        return parser.map(Optional::of).or(pure(Optional.empty()));
    }

    /**
     * thenSkip combinator: applies two parsers in sequence and returns the result of the left parser and skips the right.
     *
     * @param left the left parser to apply
     * @param right the right parser to apply
     * @param <I> the type of the input symbols
     * @param <A> the type of the left parsed value
     * @param <B> the type of the right parsed value
     * @return a parser that applies two parsers in sequence and returns the result of the left parser
     */
    public static <I, A, B> Parser<I, A> thenSkip(Parser<I, A> left, Parser<I, B> right) {
        return left.thenSkipRight(right);
    }


    /**
     * Not combinator: succeeds if the given parser fails, and fails if the given parser succeeds.
     *
     * @param parser the parser to negate
     * @param <I> the type of the input symbols
     * @return a parser that succeeds if the given parser fails, and fails if the given parser succeeds
     */
    public static <I> Parser<I, Void> not(Parser<I, ?> parser) {
        return new Parser<>(in -> {
            Result<I, ?> result = parser.apply(in);
            if (result.isSuccess()) {
                return Result.failure(in, "Unexpected success");
            } else {
                return Result.success(in, null);
            }
        });
    }
}