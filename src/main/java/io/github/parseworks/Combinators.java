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
     * Creates a parser that accepts any single input element.
     * <p>
     * The {@code any()} method creates a parser that consumes and returns a single input element
     * regardless of its value. This parser succeeds for any non-empty input,
     * failing only when the input is empty. The parsing process works as follows:
     * <ol>
     *   <li>Checks if there is at least one element remaining in the input</li>
     *   <li>If input is not empty, consumes and returns the next element</li>
     *   <li>If input is empty, the parser fails with a "not end of file" message</li>
     * </ol>
     * <p>
     * This parser is a fundamental building block for more complex parsers, serving as the simplest
     * form of element consumption. It can be combined with other parsers using methods like
     * or {@link Parser#not} to create more specific parsers.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Consumes exactly one element from the input when successful</li>
     *   <li>Returns the consumed element as the parse result</li>
     *   <li>Works with any input element type {@code I}</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser that accepts any character
     * Parser<Character, Character> anyChar = Combinators.any();
     *
     * // Succeeds with 'a' for input "abc" (consuming only 'a')
     * // Succeeds with '1' for input "123" (consuming only '1')
     * // Fails for empty input ""
     *
     * // Create a digit parser by combining any() with filter()
     * Parser<Character, Character> digit = any().filter(Character::isDigit);
     * }</pre>
     *
     * @param <I> the type of the input elements
     * @return a parser that accepts any single input element
     */
    public static <I> Parser<I, I> any() {
        return new Parser<>(input -> {
            if (input.isEof()) {
                return Result.failure(input, "not end of file", "eof");
            } else {
                return Result.success(input.next(), input.current());
            }
        });
    }

    /**
     * Creates a parser that always throws an exception supplied by the provided supplier.
     * <p>
     * The {@code throwError} method creates a parser that unconditionally throws the exception
     * created by the supplied function, regardless of the input. This is useful for handling
     * critical error conditions where normal parser failure is insufficient and execution
     * should be immediately terminated.
     * <p>
     * Unlike {@code error()} which returns a failure result, this parser throws an actual
     * exception that will propagate up the call stack, potentially terminating parsing
     * completely unless caught by a try-catch block.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>The parser never consumes any input</li>
     *   <li>The parser always throws the exception created by the supplier</li>
     *   <li>Uses a technique to bypass checked exception requirements</li>
     *   <li>Cannot be recovered from using {@link Parser#or(Parser)} since it throws rather than returns</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser that throws a custom exception for an unrecoverable error
     * Parser<Character, Object> critical = throwError(() ->
     *     new IllegalStateException("Unrecoverable parsing error"));
     *
     * // Or throw based on some condition
     * Parser<Character, String> strictJson =
     *     jsonParser.or(throwError(() -> new JsonParseException("Invalid JSON structure")));
     *
     * // Will throw the exception when this parser is applied
     * try {
     *     critical.parse("any input"); // Throws IllegalStateException
     * } catch (IllegalStateException e) {
     *     // Handle exception
     * }
     * }</pre>
     *
     * @param supplier a supplier that creates the exception to throw
     * @param <I>      the type of the input elements
     * @return a parser that always throws the specified exception
     * @see #fail() for a parser that fails without throwing an exception
     * @see Parser#or(Parser) for combining parsers with fallback behavior (not applicable to throwError)
     */
    public static <I> Parser<I, ? super Object> throwError(Supplier<? extends Exception> supplier) {
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


    @SafeVarargs
    public static <I> Parser<I, I> oneOf(I... items) {
        return new NoCheckParser<>(in -> {
            if (in.isEof()) {
                return Result.failure(in, "one of", "eof");
            }
            I current = in.current();
            for (I item : items) {
                if (Objects.equals(current, item)) {
                    return Result.success(in.next(), current);
                }
            }
            return Result.failure(in, "one of", String.valueOf(current));
        });
    }

    /**
     * Creates a parser that succeeds if the input is at the end of the file (EOF).
     * <p>
     * The {@code eof()} method creates a parser that checks if there are no more input elements
     * to process. This parser is useful for ensuring that the entire input has been consumed
     * after parsing or for detecting when parsing should stop. The parsing process works as follows:
     * <ol>
     *   <li>Checks if the input position is at EOF</li>
     *   <li>If at EOF, succeeds with a null value</li>
     *   <li>If not at EOF, fails with an error message indicating the current element</li>
     * </ol>
     * <p>
     * This parser is often used as the final parser in a sequence to ensure that no unparsed
     * input remains, or to mark the boundary between different parsed sections.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Does not consume any input elements</li>
     *   <li>Returns null as the parse result when successful</li>
     *   <li>Uses the {@link Void} type to indicate no meaningful return value</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser that ensures complete input consumption
     * Parser<Character, String> completeParser = stringParser.thenSkip(eof());
     *
     * // Succeeds only if entire input is parsed
     * Result<Character, String> result = completeParser.parse("hello");
     *
     * // Validate JSON with no trailing content
     * Parser<Character, JsonNode> strictJson = jsonParser.thenSkip(eof());
     *
     * // Fails if there's remaining input
     * strictJson.parse("{\"key\":\"value\"} extra"); // Fails at "extra"
     * }</pre>
     *
     * @param <I> the type of the input elements
     * @return a parser that succeeds if the input is at EOF
     * @see #any() for a parser that consumes any single input element
     * @see Parser#thenSkip(Parser) for combining with other parsers while discarding the EOF result
     */
    public static <I> Parser<I, Void> eof() {
        return new NoCheckParser<>(input -> {
            if (input.isEof()) {
                return Result.success(input, null);
            } else {
                return Result.failure(input, "eof", String.valueOf(input.current()));
            }
        });
    }

    /**
     * Creates a parser that always fails with a generic error message.
     *
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a parser that always fails
     */
    public static <I, A> Parser<I, A> fail() {
        return new NoCheckParser<>(in -> Result.failure(in, "to fail"));
    }

    /**
     * Creates a parser that succeeds if the provided parser fails.
     *
     * @param parser the parser to negate
     * @param <I>    the type of the input symbols
     * @param <A>    the type of the parsed value
     * @return a parser that succeeds if the provided parser fails
     */
    public static <I, A> Parser<I, A> not(Parser<I, A> parser) {
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
     * Creates a parser that tries multiple parsers in sequence until one succeeds.
     * <p>
     * The {@code oneOf} method provides a way to choose between multiple alternative
     * parsing strategies, similar to the logical OR operation or choice operator in
     * formal grammars. The parsing process works as follows:
     * <ol>
     *   <li>Parsers in the list are tried in order, from first to last</li>
     *   <li>When a parser succeeds, its result is returned and no further parsers are tried</li>
     *   <li>If a parser fails, the next parser in the list is tried with the original input</li>
     *   <li>If all parsers fail, the composite parser fails</li>
     * </ol>
     * <p>
     * Important implementation details:
     * <ul>
     *   <li>This implements ordered choice - parsers are tried in the order they appear in the list</li>
     *   <li>When a parser fails, no input is consumed before trying the next parser</li>
     *   <li>If the list is empty, the resulting parser always fails</li>
     *   <li>All parsers in the list must have the same input and output types</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse one of three different types of token
     * Parser<Character, String> keyword = string("if").or(string("else")).or(string("while"));
     * Parser<Character, String> identifier = regex("[a-zA-Z][a-zA-Z0-9]*");
     * Parser<Character, Integer> number = intr;
     *
     * // Combine into a single token parser using oneOf
     * Parser<Character, Object> token = oneOf(Arrays.asList(
     *     keyword.cast(),
     *     identifier.cast(),
     *     number.cast()
     * ));
     *
     * // Succeeds with "if" for input "if"
     * // Succeeds with "myVar" for input "myVar"
     * // Succeeds with 42 for input "42"
     * // Fails for input "+" or empty input
     * }</pre>
     *
     * @param parsers the list of parsers to try in sequence
     * @param <I>     the type of input elements
     * @param <A>     the type of values produced by the parsers
     * @return a parser that tries each parser in the list until one succeeds
     * @throws IllegalArgumentException if the parsers list is null
     * @see Parser#or(Parser) for choosing between two parsers
     */
    public static <I, A> Parser<I, A> oneOf(List<Parser<I, A>> parsers) {
        return new NoCheckParser<>(in -> {
            if (in.isEof()) {
                return Result.failure(in, "one of");
            }
            for (Parser<I, A> parser : parsers) {
                Result<I, A> result = parser.apply(in);
                if (result.isSuccess()) {
                    return result;
                }
            }
            return Result.failure(in, "one of");
        }
        );
    }

    @SafeVarargs
    public static <I, A> Parser<I, A> oneOf(Parser<I, A>... parsers) {
        return new NoCheckParser<>(in -> {
            if (in.isEof()) {
                return Result.failure(in, "one of");
            }
            for (Parser<I, A> parser : parsers) {
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
     * Creates a parser that matches input against a regular expression pattern.
     * <p>
     * The {@code regex()} method creates a parser that matches characters from the input stream
     * against the provided regular expression pattern. The parser works progressively, attempting
     * to match the pattern as characters are consumed one by one. The parsing process works as follows:
     * <ol>
     *   <li>Reads input characters incrementally, building a buffer</li>
     *   <li>Tries to match the regex pattern against the buffer after each character</li>
     *   <li>Returns successfully as soon as a complete match is found</li>
     *   <li>Fails if no match is possible with the current input</li>
     * </ol>
     * <p>
     * This parser is designed to be efficient with streaming input by validating progressively
     * rather than reading the entire input at once, allowing for early success or failure.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Automatically anchors the pattern to the start of input (adds ^ if not present)</li>
     *   <li>Uses a maximum look-ahead limit (1000 characters) to prevent excessive processing</li>
     *   <li>Returns the first match found as a String</li>
     *   <li>Only consumes the portion of input that matches the pattern</li>
     *   <li>Uses {@link Matcher#hitEnd()} to optimize early determination of match failure</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Email pattern parser
     * Parser<Character, String> emailParser = regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
     *
     * // Parse a valid email
     * Result<Character, String> result = emailParser.parse("user@example.com");
     * // result.isSuccess() == true, result.get() == "user@example.com"
     *
     * // Parse an email with trailing content
     * Result<Character, String> partialResult = emailParser.parse("user@example.com and more text");
     * // partialResult.isSuccess() == true, partialResult.get() == "user@example.com"
     *
     * // Number parser with explicit anchor
     * Parser<Character, String> phoneParser = regex("^\\d{3}-\\d{3}-\\d{4}");
     * phoneParser.parse("555-123-4567"); // Succeeds with "555-123-4567"
     * }</pre>
     *
     * @param regex the regular expression pattern to match against
     * @return a parser that matches the input against the given regular expression
     * @see #string(String) for exact string matching
     * @see #satisfy(String, Predicate) for character-by-character validation
     * @see Pattern for more information about regular expression syntax
     */
    public static Parser<Character, String> regex(String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS);
        return new Parser<>(in -> {
            // Special case for empty input
            if (in.isEof()) {
                Matcher emptyMatcher = pattern.matcher("");
                if (emptyMatcher.find() && emptyMatcher.start() == 0) {
                    return Result.success(in, emptyMatcher.group());
                }
                return Result.failure(in, regex, "eof");
            }

            StringBuilder buffer = new StringBuilder();
            Input<Character> current = in;
            int maxLookAhead = 1000; // Safety limit
            int position = 0;

            // Track last successful match
            String lastMatch = null;
            int lastMatchPos = 0;

            // Progressive matching loop
            while (!current.isEof() && position < maxLookAhead) {
                char c = current.current();
                buffer.append(c);
                position++;

                // Try matching at each step
                Matcher matcher = pattern.matcher(buffer);
                if (matcher.find() && matcher.start() == 0) {
                    // Store successful match
                    lastMatch = matcher.group();
                }
                if (!matcher.hitEnd()) {
                    // No more matches possible, break and return last match
                    break;
                }

                // Continue reading next character
                current = current.next();
            }

            // Return the last successful match if found
            if (lastMatch != null) {
                return Result.success(in.skip(lastMatch.length()), lastMatch);
            }

            // No match found
            String preview = buffer.length() > 10 ? buffer.substring(0, 10) + "..." : buffer.toString();
            return Result.failure(in, regex, preview);
        });
    }
}
