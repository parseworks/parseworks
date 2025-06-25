package io.github.parseworks;

import io.github.parseworks.impl.Failure.ErrorType;
import io.github.parseworks.impl.parser.NoCheckParser;

import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
     * Creates a parser that accepts any single input element of the specified type.
     * <p>
     * The {@code any(Class<I> type)} method creates a parser that consumes and returns a single input element,
     * regardless of its value, as long as it matches the specified class type. This parser succeeds for any
     * non-empty input, failing only when the input is empty. Passing the {@code Class<I>} argument is necessary
     * to retain type information at runtime, which is otherwise lost due to Java's type erasure. This enables
     * safe chaining and composition of generic parsers, as the type can be checked or inferred where needed.
     * <p>
     * <b>Why pass the class type?</b>
     * <ul>
     *   <li>Java's type erasure removes generic type information at runtime, making it impossible to determine
     *       the actual type parameter {@code I} in generic code.</li>
     *   <li>By explicitly passing {@code Class<I> type}, the parser can retain and use this type information
     *       for runtime checks, casting, or chaining with other parsers that require type safety.</li>
     *   <li>This is especially important when building parser combinators that need to work with generic types
     *       in a type-safe manner.</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser that accepts any character
     * Parser<Character, Character> anyChar = Combinators.any(Character.class);
     *
     * // Chaining so that we accept any character that is not a digit
     * Parser<Character, Character> nondigit = any(Character.class).not(Character::isDigit);
     * }</pre>
     *
     * @param type the class of the input elements (required for type safety due to type erasure)
     * @param <I>  the type of the input elements
     * @return a parser that accepts any single input element of the specified type
     */
    public static <I> Parser<I, I> any(Class<I> type) {
        return new NoCheckParser<>(input -> {
            if (input.isEof()) {
                return Result.failure(input, type.descriptorString(), "end of input").cast();
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
                return Result.failure(in, "one of the expected values", "end of input");
            }
            I current = in.current();
            for (I item : items) {
                if (Objects.equals(current, item)) {
                    return Result.success(in.next(), current);
                }
            }

            // Create a readable list of expected items
            StringBuilder expectedItems = new StringBuilder();
            if (items.length > 0) {
                expectedItems.append(items[0]);
                for (int i = 1; i < items.length; i++) {
                    if (i == items.length - 1) {
                        expectedItems.append(" or ");
                    } else {
                        expectedItems.append(", ");
                    }
                    expectedItems.append(items[i]);
                }
            }

            return Result.failure(in, "one of [" + expectedItems + "]", String.valueOf(current));
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
     * @see #any(Class) for a parser that consumes any single input element
     * @see Parser#thenSkip(Parser) for combining with other parsers while discarding the EOF result
     */
    public static <I> Parser<I, Void> eof() {
        return new NoCheckParser<>(input -> {
            if (input.isEof()) {
                return Result.success(input, null);
            } else {
                // Provide more context about what was found instead of EOF
                String found = input.hasMore() ? String.valueOf(input.current()) : "unknown";
                return Result.expectedEofError(input, found);
            }
        });
    }

    /**
     * Creates a parser that always fails with a generic error message.
     * <p>
     * The {@code fail()} method creates a parser that unconditionally fails regardless of the input.
     * This parser is useful as a base case in recursive parsers, for indicating impossible branches
     * in parser combinators, or as a placeholder during development. The parsing process is simple:
     * <ol>
     *   <li>The parser immediately returns a failure result</li>
     *   <li>The failure contains a generic "to fail" error message</li>
     *   <li>The input position remains unchanged (no input is consumed)</li>
     * </ol>
     * <p>
     * Key features:
     * <ul>
     *   <li>Always fails without examining the input</li>
     *   <li>Never consumes any input elements</li>
     *   <li>Returns the original input position in the failure result</li>
     *   <li>Provides a generic failure message</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Creates a {@link NoCheckParser} that always returns a failure result</li>
     *   <li>Preserves the input state, allowing for recovery with {@link Parser#or}</li>
     *   <li>Uses the simple message "to fail" as the expected value in the error</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser with a fallback
     * Parser<Character, String> fallbackParser = fail().or(string("default"));
     * fallbackParser.parse("default").get();  // Returns "default"
     *
     * // Use in conditional parsing
     * Parser<Character, String> conditional = input -> {
     *     if (someCondition) {
     *         return actualParser.apply(input);
     *     } else {
     *         return fail().apply(input);
     *     }
     * };
     *
     * // As a base case in recursive descent
     * Parser<Character, Integer> expr = Parser.lazy(() ->
     *     number.or(parens).or(fail()));
     *
     * // As a placeholder during development
     * Parser<Character, User> parseUser = regex("[a-zA-Z]+").map(name -> {
     *     // Not yet implemented
     *     return fail().apply(input).cast();
     * });
     * }</pre>
     *
     * @param <I> the type of the input symbols
     * @param <A> the type of the parsed value
     * @return a parser that always fails
     * @see #throwError(Supplier) for throwing exceptions instead of returning failure
     * @see Parser#or(Parser) for providing alternatives when a parser fails
     */
    public static <I, A> Parser<I, A> fail() {
        return new NoCheckParser<>(in -> {
            String found = in.hasMore() ? String.valueOf(in.current()) : "end of input";
            return Result.failure(in, "parser explicitly set to fail", found, ErrorType.GENERIC);
        });
    }

    /**
     * Creates a parser that always fails with a specific error message and type.
     * <p>
     * This allows creating custom failure parsers with specific error types.
     *
     * @param expected  the expected input description
     * @param errorType the type of error to report
     * @param <I>       the type of the input symbols
     * @param <A>       the type of the parsed value
     * @return a parser that always fails with the specified error type
     */
    public static <I, A> Parser<I, A> fail(String expected, ErrorType errorType) {
        return new NoCheckParser<>(in -> {
            String found = in.hasMore() ? String.valueOf(in.current()) : "end of input";
            return Result.failure(in, expected, found, errorType);
        });
    }

    /**
     * Creates a parser that always fails with a syntax error.
     * <p>
     * Use this for errors where the input doesn't match the expected syntax.
     *
     * @param expected the expected syntax description
     * @param <I>      the type of the input symbols
     * @param <A>      the type of the parsed value
     * @return a parser that always fails with a SYNTAX error type
     */
    public static <I, A> Parser<I, A> failSyntax(String expected) {
        return fail(expected, ErrorType.SYNTAX);
    }

    /**
     * Creates a parser that always fails with a validation error.
     * <p>
     * Use this for errors where the input parsed but failed validation.
     *
     * @param constraint the validation constraint description
     * @param <I>        the type of the input symbols
     * @param <A>        the type of the parsed value
     * @return a parser that always fails with a VALIDATION error type
     */
    public static <I, A> Parser<I, A> failValidation(String constraint) {
        return fail(constraint, ErrorType.VALIDATION);
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
                // Provide more context about what was found that shouldn't have matched
                String found = in.hasMore() ? String.valueOf(in.current()) : "end of input";
                return Result.validationError(in, "input that does NOT match the given pattern", found);
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
                return Result.unexpectedEofError(in, "any value except " + value);
            }
            I item = in.current();
            if (Objects.equals(item, value)) {
                return Result.validationError(in, "any value except " + value, String.valueOf(item));
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
     * Parser<Character, String> number = NumericParsers.numeric.many().map(FList::joinChars);
     *
     * // Combine into a single token parser using oneOf
     * Parser<Character, String> token = oneOf(Arrays.asList(
     *     keyword,
     *     identifier,
     *     number
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
                return Result.unexpectedEofError(in, "one of the expected patterns");
            }
            for (Parser<I, A> parser : parsers) {
                Result<I, A> result = parser.apply(in);
                if (result.isSuccess()) {
                    return result;
                }
            }
            String found = in.hasMore() ? String.valueOf(in.current()) : "end of input";
            return Result.syntaxError(in, "one of the expected patterns", found);
        }
        );
    }

    /**
     * Creates a parser that tries each of the provided parsers in order and succeeds with the first one that matches.
     * <p>
     * The {@code oneOf} combinator implements ordered choice: it attempts to apply each parser in the given
     * array to the input, starting from the first. If a parser succeeds, its result is returned and no further
     * parsers are tried. If a parser fails, the next parser is tried with the original input (no input is consumed
     * on failure). If all parsers fail, the resulting parser fails.
     * <p>
     * This method is useful for matching input against multiple possible patterns, such as keywords, operators,
     * or alternative syntactic forms.
     * <p>
     * <b>Behavior:</b>
     * <ul>
     *   <li>Parsers are tried in the order they are provided.</li>
     *   <li>On the first success, parsing stops and the result is returned.</li>
     *   <li>If all parsers fail, the composite parser fails with a generic "one of" error message.</li>
     *   <li>No input is consumed unless a parser succeeds.</li>
     *   <li>If the input is at EOF, the parser fails immediately.</li>
     * </ul>
     * <p>
     * <b>Example usage:</b>
     * <pre>{@code
     * // Parse either "yes", "no", or "maybe"
     * Parser<Character, String> answer = oneOf(
     *     string("yes"),
     *     string("no"),
     *     string("maybe")
     * );
     *
     * // Succeeds for input "yes", "no", or "maybe"
     * // Fails for any other input
     * }</pre>
     *
     * @param parsers the parsers to try in order
     * @param <I>     the type of input elements
     * @param <A>     the type of values produced by the parsers
     * @return a parser that tries each parser in order and succeeds with the first successful result
     * @see #oneOf(List) for a version that takes a list of parsers
     * @see Parser#or(Parser) for binary choice between two parsers
     */
    @SafeVarargs
    public static <I, A> Parser<I, A> oneOf(Parser<I, A>... parsers) {
        return new NoCheckParser<>(in -> {
            if (in.isEof()) {
                return Result.unexpectedEofError(in, "one of the expected patterns");
            }
            for (Parser<I, A> parser : parsers) {
                Result<I, A> result = parser.apply(in);
                if (result.isSuccess()) {
                    return result;
                }
            }
            String found = in.hasMore() ? String.valueOf(in.current()) : "end of input";
            return Result.syntaxError(in, "one of the expected patterns", found);
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
     * Creates a parser that accepts a single character matching the given predicate.
     * <p>
     * The {@code chr(Predicate)} method creates a parser that tests if the current input character
     * satisfies the provided predicate function. This parser succeeds if the predicate returns true
     * for the current character. The parsing process works as follows:
     * <ol>
     *   <li>Checks if the input is not at EOF</li>
     *   <li>Applies the predicate to the current character</li>
     *   <li>If the predicate is satisfied, consumes the character and returns it</li>
     *   <li>If the predicate is not satisfied or at EOF, fails with an error message</li>
     * </ol>
     * <p>
     * This parser is a specialized version of {@link #satisfy(String, Predicate)} for character input.
     * It's a fundamental building block for text-based parsers, allowing character filtering based
     * on arbitrary conditions.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Delegates to {@link #satisfy(String, Predicate)} with a generic "character" expected type</li>
     *   <li>Consumes exactly one character when successful</li>
     *   <li>Returns the matched character as the result</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse any digit character
     * Parser<Character, Character> digit = chr(Character::isDigit);
     *
     * // Parse any uppercase letter
     * Parser<Character, Character> uppercase = chr(Character::isUpperCase);
     *
     * // Parse any whitespace character
     * Parser<Character, Character> whitespace = chr(Character::isWhitespace);
     *
     * // Parse any character except 'x'
     * Parser<Character, Character> notX = chr(c -> c != 'x');
     *
     * // Combining character parsers
     * Parser<Character, String> hexDigit = chr(c ->
     *     Character.isDigit(c) || "ABCDEFabcdef".indexOf(c) >= 0)
     *     .many().map(chars -> chars.stream()
     *         .map(String::valueOf)
     *         .collect(Collectors.joining()));
     * }</pre>
     *
     * @param predicate the condition that characters must satisfy
     * @return a parser that matches a single character based on the predicate
     * @see #satisfy(String, Predicate) for the generic version of this parser
     * @see #chr(char) for matching a specific character
     * @see #oneOf(String) for matching against a set of characters
     */
    public static Parser<Character, Character> chr(Predicate<Character> predicate) {
        return satisfy("<character>", predicate);
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
                return Result.failure(in, expectedType);
            }
            I item = in.current();
            if (predicate.test(item)) {
                return Result.success(in.next(), item);
            } else {
                return Result.failure(in, expectedType);
            }
        });
    }

    /**
     * Creates a parser that matches a specific character.
     * <p>
     * The {@code chr(char)} method creates a parser that tests if the current input character
     * equals the specified character value. This parser succeeds only when the current input
     * character exactly matches the target character. The parsing process works as follows:
     * <ol>
     *   <li>Checks if the input is not at EOF</li>
     *   <li>Compares the current character with the specified character</li>
     *   <li>If they match, consumes the character and returns it</li>
     *   <li>If they don't match or at EOF, fails with an error message</li>
     * </ol>
     * <p>
     * This parser is a specialized version of {@link #is(Object)} for character input. It's
     * commonly used for parsing punctuation, operators, and other specific characters in
     * text-based parsing.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Delegates to {@link #is(Object)} for equality checking</li>
     *   <li>Consumes exactly one character when successful</li>
     *   <li>Returns the matched character as the result</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse specific characters
     * Parser<Character, Character> comma = chr(',');
     * Parser<Character, Character> semicolon = chr(';');
     * Parser<Character, Character> plus = chr('+');
     *
     * // Combine to parse a simple addition expression
     * Parser<Character, Integer> addExpr = digit.then(plus).then(digit)
     *     .map((d1, op, d2) ->
     *         Character.getNumericValue(d1) + Character.getNumericValue(d2));
     *
     * // Parse punctuated list items
     * Parser<Character, List<String>> commaSeparated =
     *     word.separatedByMany(chr(','));
     * }</pre>
     *
     * @param c the specific character to match
     * @return a parser that matches the specified character
     * @see #is(Object) for the generic version of this parser
     * @see #chr(Predicate) for matching characters based on predicates
     * @see #string(String) for matching sequences of characters
     */
    public static Parser<Character, Character> chr(char c) {
        return is(c);
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
                return Result.failure(in, "equivalence");
            }
            I item = in.current();
            if (Objects.equals(item, equivalence)) {
                return Result.success(in.next(), item);
            } else {
                return Result.failure(in, "equivalence");
            }
        });
    }

    /**
     * Creates a parser that matches an exact string of characters.
     * <p>
     * The {@code string()} method creates a parser that attempts to match the input exactly
     * against the provided string. This parser succeeds only if the entire string is matched
     * character by character. The parsing process works as follows:
     * <ol>
     *   <li>Compares each character in the input with the corresponding character in the target string</li>
     *   <li>If all characters match in sequence, returns the entire matched string</li>
     *   <li>If any character differs, fails with information about the mismatch point</li>
     *   <li>If input is too short, fails with an EOF error</li>
     * </ol>
     * <p>
     * Key features:
     * <ul>
     *   <li>Performs exact, case-sensitive string matching</li>
     *   <li>Provides detailed error information showing where the match failed</li>
     *   <li>Optimized for performance with direct character comparison</li>
     *   <li>Special case handling for empty strings</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses character-by-character comparison rather than multiple parsers</li>
     *   <li>Returns the original string rather than rebuilding it from characters</li>
     *   <li>Provides context in error messages showing partial matches</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Match specific keywords
     * Parser<Character, String> ifKeyword = string("if");
     * Parser<Character, String> elseKeyword = string("else");
     *
     * // Match operators
     * Parser<Character, String> plusOperator = string("+");
     * Parser<Character, String> minusOperator = string("-");
     *
     * // Match multi-character tokens
     * Parser<Character, String> arrow = string("->");
     * Parser<Character, String> equality = string("==");
     *
     * // Combine with other parsers
     * Parser<Character, String> helloWorld = string("Hello").thenSkip(string(" ")).then(string("World"))
     *     .map((hello, world) -> hello + " " + world);
     * }</pre>
     *
     * @param str the exact string to match
     * @return a parser that matches the specified string
     * @see #regex(String) for flexible pattern matching
     * @see #chr(char) for single character matching
     */
    public static Parser<Character, String> string(String str) {
        return new NoCheckParser<>(in -> {
            Input<Character> currentInput = in;

            // Handle empty string case
            if (str.isEmpty()) {
                return Result.success(currentInput, "");
            }

            // Check if we have enough characters left in the input
            for (int i = 0; i < str.length(); i++) {
                if (currentInput.isEof()) {
                    return Result.failure(in, str);
                }

                char expected = str.charAt(i);
                char actual = currentInput.current();

                if (expected != actual) {
                    return Result.failure(in, str);
                }

                currentInput = currentInput.next();
            }

            return Result.success(currentInput, str);
        });
    }


    /**
     * Creates a parser that accepts any character that appears in the provided string.
     * <p>
     * The {@code oneOf(String)} method creates a parser that succeeds when the current input
     * character matches any character in the provided string. This parser is useful for defining
     * character classes or sets of acceptable characters. The parsing process works as follows:
     * <ol>
     *   <li>Checks if the input is not at EOF</li>
     *   <li>Tests if the current character appears in the provided string</li>
     *   <li>If the character is found in the string, consumes it and returns it</li>
     *   <li>If the character is not in the string or at EOF, fails with an error message</li>
     * </ol>
     * <p>
     * Performance considerations:
     * <ul>
     *   <li>For small strings (less than 10 characters), uses {@link String#indexOf(int)} for lookup</li>
     *   <li>For larger strings, creates a {@link HashSet} of characters for O(1) lookup performance</li>
     *   <li>This optimization makes character class matching efficient even with large character sets</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Delegates to {@link #satisfy(String, Predicate)} with appropriate predicates</li>
     *   <li>Uses different implementation strategies based on input string length</li>
     *   <li>Consumes exactly one character when successful</li>
     *   <li>Returns the matched character as the result</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse any digit
     * Parser<Character, Character> digit = oneOf("0123456789");
     *
     * // Parse any hexadecimal digit
     * Parser<Character, Character> hexDigit = oneOf("0123456789ABCDEFabcdef");
     *
     * // Parse any vowel
     * Parser<Character, Character> vowel = oneOf("aeiouAEIOU");
     *
     * // Parse any operator
     * Parser<Character, Character> operator = oneOf("+-*\/");
     *
     * // Using with other parsers
     * Parser<Character, String> identifier =
     *     oneOf("_$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ")
     *     .then(oneOf("_$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789").many())
     *     .map((first, rest) -> first + rest.stream()
     *         .map(String::valueOf)
     *         .collect(Collectors.joining()));
     * }</pre>
     *
     * @param str the string containing all acceptable characters
     * @return a parser that accepts any character that appears in the provided string
     * @see #chr(Predicate) for matching characters with arbitrary predicates
     * @see #chr(char) for matching a specific character
     * @see #oneOf(Object...) for the generic version of this parser
     */
    public static Parser<Character, Character> oneOf(String str) {
        // For small strings (under 10 chars), this approach is efficient
        if (str.length() < 10) {
            return satisfy("<oneOf> " + str, c -> str.indexOf(c) != -1);
        }

        // For larger character sets, use a Set for O(1) lookups
        Set<Character> charSet = new HashSet<>();
        for (int i = 0; i < str.length(); i++) {
            charSet.add(str.charAt(i));
        }

        return satisfy("character in set [" + str + "]", charSet::contains);
    }

    /**
     * Creates a parser that matches input against a regular expression pattern with specified flags.
     * <p>
     * The {@code regex()} method creates a parser that incrementally matches characters from the input
     * stream against the provided regular expression pattern, using the specified Pattern flags.
     * This parser handles both start (^) and end ($) anchors appropriately in the context of streaming input.
     * <p>
     * Valid flag values from {@link Pattern} include:
     * <ul>
     *   <li>{@link Pattern#CASE_INSENSITIVE} - Case-insensitive matching</li>
     *   <li>{@link Pattern#MULTILINE} - Multiline mode, affects ^ and $ behavior</li>
     *   <li>{@link Pattern#DOTALL} - Dot matches all characters including line terminators</li>
     *   <li>{@link Pattern#UNICODE_CASE} - Unicode-aware case folding</li>
     *   <li>{@link Pattern#CANON_EQ} - Canonical equivalence</li>
     *   <li>{@link Pattern#LITERAL} - Treat pattern as a literal string</li>
     *   <li>{@link Pattern#UNICODE_CHARACTER_CLASS} - Unicode character classes</li>
     *   <li>{@link Pattern#COMMENTS} - Permits whitespace and comments in pattern</li>
     * </ul>
     * <p>
     * Multiple flags can be combined using the bitwise OR operator (|).
     * <p>
     * Key features:
     * <ul>
     *   <li>Progressively reads characters one by one, building a buffer</li>
     *   <li>Attempts matching after each character to support early success/failure</li>
     *   <li>Properly respects start anchors (^) by only matching from the beginning</li>
     *   <li>Properly respects end anchors ($) by only accepting matches at the end of input</li>
     *   <li>For non-anchored patterns, returns the longest valid match</li>
     *   <li>Efficiently handles streaming input by using {@link Matcher#hitEnd()} for optimization</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Case-insensitive word parser
     * Parser<Character, String> caseInsensitiveWord = regex("[a-z]+", Pattern.CASE_INSENSITIVE);
     * caseInsensitiveWord.parse("Hello").get();  // Returns "Hello"
     *
     * // Multiline regex with DOTALL flag
     * Parser<Character, String> multiline = regex(".*end", Pattern.DOTALL);
     * multiline.parse("line1\nline2\nend").get();  // Returns "line1\nline2\nend"
     *
     * // Combined flags
     * Parser<Character, String> complex = regex("# match digits\n\\d+",
     *     Pattern.COMMENTS | Pattern.UNICODE_CHARACTER_CLASS);
     * }</pre>
     *
     * @param regex the regular expression pattern to match against
     * @param flags the flags to be used with this pattern, as defined in {@link Pattern}
     * @return a parser that matches the input against the given regular expression with specified flags
     * @see Pattern for information about Java regular expression syntax and flag constants
     * @see #regex(String) for the simpler version with default flags
     */
    public static Parser<Character, String> regex(String regex, int flags) {
        boolean hasEndAnchor = regex.endsWith("$") && !regex.endsWith("\\$");
        Pattern pattern = Pattern.compile(regex, flags);

        return new Parser<>(in -> {
            // Special case for empty input
            if (in.isEof()) {
                Matcher emptyMatcher = pattern.matcher("");
                if (emptyMatcher.lookingAt()) {
                    return Result.success(in, emptyMatcher.group());
                }
                return Result.failure(in, regex);
            }

            StringBuilder buffer = new StringBuilder();
            Input<Character> current = in;
            int maxLookAhead = 1000; // Safety limit
            int position = 0;

            // Track best match found so far
            String bestMatch = null;

            // Progressive matching loop
            while (!current.isEof() && position++ < maxLookAhead) {
                char c = current.current();
                buffer.append(c);

                // Get next position to check for EOF
                Input<Character> next = current.next();
                boolean isAtEnd = next.isEof();

                // Try matching at each step
                Matcher matcher = pattern.matcher(buffer);

                if (matcher.lookingAt()) {
                    String match = matcher.group();

                    // For end-anchored patterns, only accept matches at end of input
                    if (!hasEndAnchor || isAtEnd) {
                        bestMatch = match;
                    }
                }

                // If the matcher doesn't benefit from more input, we can stop
                if (!matcher.hitEnd()) {
                    break;
                }

                // Continue reading next character
                current = next;
            }

            // Return the best match found if any
            if (bestMatch != null) {
                return Result.success(in.skip(bestMatch.length()), bestMatch);
            }

            // No match found
            //String preview = buffer.length() > 10 ? buffer.substring(0, 10) + "..." : buffer.toString();
            return Result.failure(in, regex);
        });
    }

    /**
     * Creates a parser that matches input against a regular expression pattern using default flags.
     * <p>
     * This is a convenience method that calls {@link #regex(String, int)} with flags set to 0,
     * which means no special Pattern flags are enabled. The parser incrementally matches characters
     * from the input stream against the provided regular expression pattern.
     * <p>
     * Key features:
     * <ul>
     *   <li>Progressively reads characters one by one, building a buffer</li>
     *   <li>Attempts matching after each character to support early success/failure</li>
     *   <li>Properly respects start anchors (^) by only matching from the beginning</li>
     *   <li>Properly respects end anchors ($) by only accepting matches at the end of input</li>
     *   <li>For non-anchored patterns, returns the longest valid match</li>
     *   <li>Efficiently handles streaming input by using {@link Matcher#hitEnd()} for optimization</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a word (sequence of letters)
     * Parser<Character, String> wordParser = regex("[a-zA-Z]+");
     * wordParser.parse("Hello123").get();  // Returns "Hello"
     *
     * // Parse an identifier (letters, digits, underscore)
     * Parser<Character, String> identifier = regex("[a-zA-Z_][a-zA-Z0-9_]*");
     * identifier.parse("_var123").get();  // Returns "_var123"
     *
     * // Parse a number with optional decimal part
     * Parser<Character, String> number = regex("\\d+(\\.\\d+)?");
     * number.parse("42.5").get();  // Returns "42.5"
     * }</pre>
     *
     * @param regex the regular expression pattern to match against
     * @return a parser that matches the input against the given regular expression
     * @see #regex(String, int) for creating a regex parser with specific flags
     * @see Pattern for information about Java regular expression syntax
     */
    public static Parser<Character, String> regex(String regex) {
        return regex(regex, 0);
    }
}
