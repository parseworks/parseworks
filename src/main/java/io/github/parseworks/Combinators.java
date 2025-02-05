package io.github.parseworks;

import io.github.parseworks.impl.parser.NoCheckParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.parseworks.Parser.pure;
import static io.github.parseworks.Utils.failure;

/**
 * The `Combinators` class provides a set of combinator functions for creating complex parsers
 * by combining simpler ones. These combinators include choice, sequence, many, and satisfy.
 *
 * @author jason bailey
 * @version $Id: $Id
 */
public class Combinators {

    private Combinators() {
    }

    /**
     * Creates a parser that succeeds if the input is not at the end of the file (EOF).
     * This parser will return a successful result with the current input symbol if the input is not at EOF,
     * and will fail with an error message "Unexpected end of input" if the input is at EOF.
     *
     * @param klass a {@link java.lang.Class} object
     * @param <I> Type of the input symbols
     * @return a {@link io.github.parseworks.Parser} object
     */
    @SuppressWarnings("unused")
    public static <I> Parser<I, I> any(Class<I> klass) {
        return new Parser<>(input -> {
            if (input.isEof()) {
                return Result.failure(input, "Unexpected end of input");
            } else {
                return Result.success(input.next(), input.current());
            }
        });
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
     * Applies the parser one or more times and collects the results in a `FList`.
     * This parser will succeed if the given parser succeeds at least once, and will return a non-empty list of the results.
     * If the parser fails on the first attempt, the parser fails.
     *
     * @param parser the parser to apply repeatedly
     * @param <I>    the type of the input symbols
     * @param <A>    the type of the parsed value
     * @return a parser that applies the given parser one or more times and collects the results in a `FList`
     */
    public static <I, A> Parser<I, FList<A>> many(Parser<I, A> parser) {
        return parser.then(parser.zeroOrMany()).map((a, l) -> l.push(a));
    }

    /**
     * Optional combinator: tries to apply the parser and returns an `Optional` result.
     *
     * @param parser the parser to apply optionally
     * @param <I>    the type of the input symbols
     * @param <A>    the type of the parsed value
     * @return a parser that tries to apply the given parser and returns an `Optional` result
     */
    public static <I, A> Parser<I, Optional<A>> optional(Parser<I, A> parser) {
        return parser.map(Optional::of).or(pure(Optional.empty()));
    }

    /**
     * Choice combinator: tries each parser in the list until one succeeds.
     *
     * @param parsers the list of parsers to try
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a parser that tries each parser in the list until one succeeds
     */
    public static <I, A> Parser<I, A> oneOf(List<Parser<I, A>> parsers) {
        return new Parser<>(in ->
                parsers.stream().map(p -> p.apply(in)).filter(Result::isSuccess).findFirst().orElseGet(() -> failure(in))
        );
    }

    /**
     * Choice combinator: tries each parser in the list until one succeeds.
     *
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @param parserA a {@link io.github.parseworks.Parser} object
     * @param parserB a {@link io.github.parseworks.Parser} object
     * @return a parser that tries each parser in the list until one succeeds
     */
    public static <I, A> Parser<I, A> oneOf(Parser<I, A> parserA, Parser<I, A> parserB) {
        return oneOf(List.of(parserA, parserB));
    }

    /**
     * Choice combinator: tries each parser in the list until one succeeds.
     *
     * @param parserA the first parser to try
     * @param parserB the second parser to try
     * @param parserC the third parser to try
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a parser that tries each parser in the list until one succeeds
     */
    public static <I, A> Parser<I, A> oneOf(Parser<I, A> parserA, Parser<I, A> parserB, Parser<I, A> parserC) {
        return oneOf(List.of(parserA, parserB, parserC));
    }

    /**
     * Choice combinator: tries each parser in the list until one succeeds.
     *
     * @param parserA the first parser to try
     * @param parserB the second parser to try
     * @param parserC the third parser to try
     * @param parserD the fourth parser to try
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a parser that tries each parser in the list until one succeeds
     */
    public static <I, A> Parser<I, A> oneOf(Parser<I, A> parserA, Parser<I, A> parserB, Parser<I, A> parserC, Parser<I, A> parserD) {
        return oneOf(List.of(parserA, parserB, parserC, parserD));
    }

    /**
     * Choice combinator: tries each parser in the list until one succeeds.
     *
     * @param parserA the first parser to try
     * @param parserB the second parser to try
     * @param parserC the third parser to try
     * @param parserD the fourth parser to try
     * @param parserE the fifth parser to try
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a parser that tries each parser in the list until one succeeds
     */
    public static <I, A> Parser<I, A> oneOf(Parser<I, A> parserA, Parser<I, A> parserB, Parser<I, A> parserC, Parser<I, A> parserD, Parser<I, A> parserE) {
        return oneOf(List.of(parserA, parserB, parserC, parserD, parserE));
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
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a parser that tries each parser in the list until one succeeds
     */
    public static <I, A> Parser<I, A> oneOf(Parser<I, A> parserA, Parser<I, A> parserB, Parser<I, A> parserC, Parser<I, A> parserD, Parser<I, A> parserE, Parser<I, A> parserF) {
        return oneOf(List.of(parserA, parserB, parserC, parserD, parserE, parserF));
    }

    /**
     * Sequence combinator: applies each parser in sequence and collects the results in a `List`.
     *
     * @param parsers the list of parsers to apply in sequence
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a parser that applies each parser in sequence and collects the results in a `List`
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
     * Satisfy combinator: parses a single item that satisfies the given predicate.
     *
     * @param predicate    the predicate that the parsed item must satisfy
     * @param errorMessage the error message to use if the predicate is not satisfied
     * @param <I>          the type of the input symbols
     * @return a parser that parses a single item that satisfies the given predicate
     */
    public static <I> Parser<I, I> satisfy(Predicate<I> predicate, Function<I, String> errorMessage) {
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
     * <p>
     * This parser attempts to parse a single item from the input and checks if it satisfies the provided predicate.
     * If the input is at the end of the file (EOF), it returns a failure result with an "Unexpected end of input" message.
     * If the item satisfies the predicate, it returns a successful result with the parsed item.
     * If the item does not satisfy the predicate, it returns a failure result with a message indicating the expected type.
     * <p>
     * This method is useful for creating parsers that need to validate input items against specific conditions.
     *
     * @param predicate    the predicate that the parsed item must satisfy
     * @param expectedType the error message to use if the predicate is not satisfied
     * @param <I>          the type of the input symbols
     * @return a parser that parses a single item that satisfies the given predicate
     */
    public static <I> Parser<I, I> satisfy(Predicate<I> predicate, String expectedType) {
        return new NoCheckParser<>(in -> {
            if (in.isEof()) {
                return Result.failureEof(in, "Unexpected end of input. expected type " + expectedType);
            }
            I item = in.current();
            if (predicate.test(item)) {
                return Result.success(in.next(), item);
            } else {
                return Result.failure(in, "Failure at position %s, saw '%s', expected %s".formatted(in.position(), String.valueOf(in.current()), expectedType));
            }
        });
    }

    /**
     * Parses a single character that satisfies the given predicate.
     *
     * @param predicate the predicate that the character must satisfy
     * @return a parser that parses a single character satisfying the predicate
     */
    public static Parser<Character, Character> chr(Predicate<Character> predicate) {
        return satisfy(predicate, "<character>");
    }

    /**
     * Parses a specific character.
     *
     * @param c the character to parse
     * @return a parser that parses the specified character
     */
    public static Parser<Character, Character> chr(char c) {
        return chr(ch -> ch == c);
    }

    /**
     * Parses a specific string.
     *
     * @param str the string to parse
     * @return a parser that parses the specified string
     */
    public static Parser<Character, String> string(String str) {
        return Combinators.sequence(str.chars()
                        .mapToObj(c -> chr((char) c))
                        .toList())
                .map(chars -> chars.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining()));
    }

    /**
     * Parses a single character from a set of characters.
     *
     * @param str the set of characters to parse
     * @return a parser that parses a single character from the specified set
     */
    public static Parser<Character, Character> oneOf(String str) {
        return satisfy(c -> str.indexOf(c) != -1, "<oneOf> " + str);
    }



}
