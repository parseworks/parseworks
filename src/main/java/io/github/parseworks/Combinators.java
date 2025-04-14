package io.github.parseworks;

import io.github.parseworks.impl.parser.NoCheckParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
                return Result.failure(input, "object of "+klass.getName() ,"eof");
            } else {
                return Result.success(input.next(), input.current());
            }
        });
    }

    /**
     * Creates a parser that throws the exception produced by the given supplier.
     * This method uses a sneaky throw mechanism to bypass checked exception requirements.
     *
     * @param supplier the supplier that produces the exception to throw
     * @return a parser that throws an exception
     */
    public static <I,A> Parser<I, A> error(Supplier<? extends Exception> supplier) {
        return new Parser<>(in -> {
            throw sneakyThrow(supplier.get());
        });
    }

    /**
     * Utility method to bypass checked exception requirements.
     */
    @SuppressWarnings("unchecked")
    private static <E extends Throwable> E sneakyThrow(Throwable e) throws E {
        throw (E) e;
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
                return Result.failure(input, "eof", String.valueOf(input.current()));
            }
        });
    }

    /**
     *  Creates a parser that succeeds if the provided parser fails.
     *
     * @param parser the parser to negate
     * @return a parser that succeeds if the provided parser fails
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     */
    public static <I,A> Parser<I, A> not( Parser<I, A> parser) {
        return new Parser<>(in -> {
            Result<I, A> result = parser.apply(in);
            if (result.isSuccess()) {
                return Result.failure(in, "not", String.valueOf(result.get()));
            } else {
                return Result.success(in, null);
            }
        });
    }

    /**
     * Creates a parser that succeeds if the current input item is not equal to the provided value.
     * <p>
     * This parser attempts to match the current input item against the specified value using
     * {@link Objects#equals(Object, Object)}. If the input is at the end of the file (EOF), it returns a
     * failure result with an "inequality" expected type and "eof" as the found value. If the current item
     * equals the provided value, it returns a failure result indicating what was found instead.
     * <p>
     * This method is useful for creating parsers that match specific input values.
     *
     * @param value the value to compare against
     * @param <I>   the type of the input symbols
     * @return a parser that succeeds if the current input item is not equal to the provided value
     */
    public static <I> Parser<I, I> isNot(I value) {
        return new NoCheckParser<>(in -> {
            if (in.isEof()) {
                return Result.failure(in, "inequality", "eof");
            }
            I item = in.current();
            if (Objects.equals(item, value)) {
                return Result.failure(in, "inequality", String.valueOf(item));
            } else {
                return Result.success(in.next(), item);
            }
        });
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
        return new Parser<>(in -> {
                    if (in.isEof()) {
                        return Result.failure(in, "one of");
                    }
                    for (Parser<I,A> parser : parsers) {
                        Result<I, A> result = parser.apply(in);
                        if (result.isSuccess()) {
                            return result;
                        }
                    }
                    return Result.failure(in, "one of");
                }
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
                    return result.cast();
                }
                results.add(result.get());
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
     * <p>
     * This parser attempts to parse a single item from the input and checks if it satisfies the provided predicate.
     * If the input is at the end of the file (EOF), it returns a failure result with an "Unexpected end of input" message.
     * If the item satisfies the predicate, it returns a successful result with the parsed item.
     * If the item does not satisfy the predicate, it returns a failure result with a message indicating the expected type.
     * <p>
     * This method is useful for creating parsers that need to validate input items against specific conditions.
     *
     * @param expectedType the error message to use if the predicate is not satisfied
     * @param predicate    the predicate that the parsed item must satisfy
     * @param <I>          the type of the input symbols
     * @return a parser that parses a single item that satisfies the given predicate
     */
    public static <I> Parser<I, I> satisfy(String expectedType, Predicate<I> predicate) {
        return new NoCheckParser<>(in -> {
            if (in.isEof()) {
                return Result.failure(in, expectedType, "eof");
            }
            I item = in.current();
            if (predicate.test(item)) {
                return Result.success(in.next(), item);
            } else {
                return Result.failure(in, expectedType, String.valueOf(in.current()));
            }
        });
    }

    /**
     * Creates a parser that succeeds if the current input item equals the provided value.
     * <p>
     * This parser attempts to match the current input item against the specified equivalence value using
     * {@link Objects#equals(Object, Object)}. If the input is at the end of the file (EOF), it returns a
     * failure result with an "equivalence" expected type and "eof" as the found value. If the current item
     * equals the equivalence value, it returns a successful result with the matched item. Otherwise, it
     * returns a failure result indicating what was found instead.
     * <p>
     * This method is useful for creating parsers that match specific input values.
     *
     * @param equivalence the value to match against the current input item
     * @param <I>         the type of the input symbols
     * @return a parser that succeeds if the current input item equals the provided value
     */
    public static <I> Parser<I, I> is(I equivalence) {
        return new NoCheckParser<>(in -> {
            if (in.isEof()) {
                return Result.failure(in, "equivalence", "eof");
            }
            I item = in.current();
            if (Objects.equals(item, equivalence)) {
                return Result.success(in.next(), item);
            } else {
                return Result.failure(in, "equivalence", String.valueOf(item));
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
        return satisfy("<character>", predicate);
    }

    /**
     * Parses a specific character.
     *
     * @param c the character to parse
     * @return a parser that parses the specified character
     */
    public static Parser<Character, Character> chr(char c) {
        return is(c);
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
        return satisfy("<oneOf> " + str, c -> str.indexOf(c) != -1);
    }

    /**
     * Creates a parser that matches the input against the given regular expression.
     * The parser attempts to match the input from the beginning using the provided regex pattern.
     * If the match is successful, the parser returns the matched string.
     * If the match fails, the parser returns a failure result with an appropriate error message.
     * <p>
     * This method is useful for parsing input that needs to conform to a specific pattern,
     * such as numbers, identifiers, or other structured text.
     *
     * @param regex the regular expression pattern to match against the input
     * @return a parser that matches the input against the given regular expression
     */
    public static Parser<Character,String> regex(String regex) {
        Pattern pattern = Pattern.compile(regex.charAt(0) == '^' ? regex : "^" + regex);
        return new Parser<>(in -> {
            var input = in;
            StringBuilder result = new StringBuilder();
            while (!input.isEof()) {
                result.append(input.current());
                input = input.next();
            }
            var string =  result.toString();
            Matcher matcher = pattern.matcher(string);
            if (matcher.find()) {
                return Result.success(in.skip(matcher.end()), matcher.group());
            }
            return Result.failure(in, regex, string.substring(0, Math.min(10, string.length())));
        });
    }

}
