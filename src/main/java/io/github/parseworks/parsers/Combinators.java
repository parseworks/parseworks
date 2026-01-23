package io.github.parseworks.parsers;

import io.github.parseworks.*;
import io.github.parseworks.impl.result.NoMatch;
import io.github.parseworks.impl.result.PartialMatch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The `Combinators` class provides a set of combinator functions for creating complex parsers
 * by combining simpler ones. These combinators include choice, sequence, and satisfy.
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
     * Parser<Character, Character> nonDigit = Combinators.not(Lexical.chr(Character::isDigit));
     * }</pre>
     *
     * @param type the class of the input elements (required for type safety due to type erasure)
     * @param <I>  the type of the input elements
     * @return a parser that accepts any single input element of the specified type
     */
    public static <I> Parser<I, I> any(Class<I> type) {
        return new Parser<>(input -> {
            if (input.isEof()) {
                return Result.failure(input, type.descriptorString()).cast();
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
     * @see #fail() for a parser that results in a NoMatch without throwing an exception
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


    /**
     * Creates a parser that accepts any input element that equals one of the provided items.
     * <p>
     * The {@code oneOf(I... items)} method creates a parser that matches the current input element
     * against a set of possible values using {@link Objects#equals(Object, Object)}. This parser
     * succeeds if the current input element equals any of the provided items. The parsing process
     * works as follows:
     * <ol>
     *   <li>Checks if the input is not at EOF</li>
     *   <li>Compares the current input element with each of the provided items</li>
     *   <li>If a match is found, consumes the element and returns it</li>
     *   <li>If no match is found or at EOF, fails with an error message</li>
     * </ol>
     * <p>
     * This method is useful for creating parsers that match against a fixed set of possible values,
     * such as keywords, operators, or specific tokens.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse one of the digits 1, 2, or 3
     * Parser<Character, Character> digit123 = oneOf('1', '2', '3');
     *
     * // Parse one of the boolean literals
     * Parser<String, String> boolLiteral = oneOf("true", "false");
     *
     * // Parse one of several arithmetic operators
     * Parser<Character, Character> operator = oneOf('+', '-', '*', '/');
     * }</pre>
     *
     * @param items the items to match against
     * @param <I>   the type of the input elements
     * @return a parser that accepts any input element that equals one of the provided items
     * @see #oneOf(Parser...) for choosing between multiple parsers
     * @see #is(Object) for matching a single specific value
     */
    @SafeVarargs
    public static <I> Parser<I, I> oneOf(I... items) {
        return new Parser<>(in -> {
            if (in.isEof()) {
                return Result.failure(in, "one of the expected values");
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

            return Result.failure(in, "one of [" + expectedItems + "]");
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
        return new Parser<>(input -> {
            if (input.isEof()) {
                return Result.success(input, null);
            } else {
                return Result.expectedEofError(input);
            }
        });
    }

    /**
     * Creates a parser that unconditionally fails, consuming no input.
     * <p>
     * This parser is useful for:
     * <ul>
     *   <li>Providing a base case in recursive parsers</li>
     *   <li>Creating explicit failure points in parsing logic</li>
     *   <li>Implementing temporary placeholders during development</li>
     *   <li>Building fallback branches in parser combinators</li>
     * </ul>
     * <p>
     * When applied, this parser will:
     * <ul>
     *   <li>Always return a NoMatch result</li>
     *   <li>Not consume any input</li>
     *   <li>Include a generic "fail" message in the NoMatch result</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Using fail as a base case
     * Parser<Character, Integer> number = digit.many1().map(Integer::parseInt)
     *     .orElse(fail());
     *
     * // Using fail for unimplemented features
     * Parser<Character, User> parseUser =
     *     string("user:").skipThen(fail());  // Not yet implemented
     * }</pre>
     *
     * @param <I> the type of input symbols
     * @param <A> the type of the parser result
     * @return a parser that always fails
     * @see Parser#or(Parser) for providing alternatives when failure occurs
     */
    public static <I, A> Parser<I, A> fail() {
        return new Parser<>(in -> Result.failure(in, "parser explicitly set to fail"));
    }

    /**
     * Creates a parser that always fails with a specific error message.
     * <p>
     * The {@code fail(String)} method extends the basic {@link #fail()} method by allowing
     * you to specify the expected input description. This gives
     * you more control over the error message, which can be useful for:
     * <ol>
     *   <li>Creating domain-specific error messages that are more meaningful to users</li>
     *   <li>Providing more context about why parsing failed</li>
     * </ol>
     * <p>
     * The parsing process is simple:
     * <ol>
     *   <li>The parser immediately returns a NoMatch result</li>
     *   <li>The failure contains the specified expected description</li>
     *   <li>The input position remains unchanged (no input is consumed)</li>
     * </ol>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Always fails without examining the input</li>
     *   <li>Never consumes any input elements</li>
     *   <li>Returns the original input position in the failure result</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Create a parser that fails with a specific message
     * Parser<Character, String> syntaxError =
     *     fail("properly formatted JSON object");
     *
     * // Create a parser that fails with a message
     * Parser<Character, Integer> validationError =
     *     fail("positive integer");
     *
     * // Use in conditional parsing
     * Parser<Character, User> parseUser = input -> {
     *     if (isAuthenticated) {
     *         return userParser.apply(input);
     *     } else {
     *         return fail("authentication").apply(input);
     *     }
     * };
     * }</pre>
     *
     * @param expected the expected input description
     * @param <I>      the type of the input symbols
     * @param <A>      the type of the parsed value
     * @return a parser that always fails with a NoMatch result
     * @see #fail() for a generic NoMatch parser
     */
    public static <I, A> Parser<I, A> fail(String expected) {
        return new Parser<>(in -> Result.failure(in, expected));
    }

    /**
     * Creates a parser that succeeds if the provided parser fails, returning the current input element.
     * <p>
     * The {@code not} method creates a negation parser that inverts the Match/NoMatch behavior of another parser.
     * This is useful for creating parsers that match anything except a specific pattern. The parsing process
     * works as follows:
     * <ol>
     *   <li>Applies the provided parser to the input without consuming any input</li>
     *   <li>If the provided parser results in a NoMatch, this parser succeeds (Match) and returns the current input element</li>
     *   <li>If the provided parser results in a Match, this parser fails (NoMatch) with a validation error</li>
     *   <li>If the input is at EOF, this parser fails (NoMatch)</li>
     * </ol>
     * <p>
     * This method is particularly useful for creating parsers that exclude certain patterns or for
     * implementing "not followed by" lookahead assertions in parsing grammars.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>The parser does not consume any input when checking the negation condition</li>
     *   <li>When successful, returns the current input element and advances the input position</li>
     *   <li>When the negated parser succeeds, fails with a validation error</li>
     *   <li>Cannot succeed at EOF since there's no current element to return</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse any character that is not a digit
     * Parser<Character, Character> notDigit = not(Lexical.chr(Character::isDigit));
     *
     * // Parse an identifier that doesn't start with a reserved word
     * Parser<Character, String> keyword = oneOf(
     *     Lexical.string("if"), Lexical.string("else"), Lexical.string("while")
     * );
     * Parser<Character, String> identifier = not(keyword).skipThen(
     *     Lexical.regex("[a-zA-Z][a-zA-Z0-9]*")
     * );
     *
     * // Succeeds with 'a' for input "a"
     * // Fails for input "1" (matches the negated parser)
     * // Fails for empty input (no current element)
     * }</pre>
     *
     * @param parser the parser to negate
     * @param <I>    the type of the input symbols
     * @param <A>    the type of the parsed value
     * @return a parser that succeeds if the provided parser fails, returning the current input element
     * @throws IllegalArgumentException if the parser parameter is null
     * @see #isNot(Object) for negating equality with a specific value
     */
    public static <I, A> Parser<I, I> not(Parser<I, A> parser) {
        return new Parser<>(in -> {
            Result<I, A> result = parser.apply(in);
            if (result.matches() || !result.input().hasMore()) {
                // Provide more context about what was found that shouldn't have matched
                String found = result.input().hasMore() ? String.valueOf(in.current()) : "end of input";
                return Result.failure(in, "parser succeeded when we wanted it to fail");
            }
            return Result.success(in.next(), in.current());

        });
    }

    /**
     * Creates a parser that succeeds if the current input element is not equal to the provided value.
     * <p>
     * The {@code isNot} method creates a parser that checks if the current input element is different
     * from a specific value. This is useful for creating parsers that exclude specific tokens or
     * characters. The parsing process works as follows:
     * <ol>
     *   <li>Checks if the input is not at EOF</li>
     *   <li>Compares the current input element with the provided value using {@link Objects#equals(Object, Object)}</li>
     *   <li>If they are not equal, consumes the element and returns it</li>
     *   <li>If they are equal or at EOF, fails with an appropriate error message</li>
     * </ol>
     * <p>
     * This method is particularly useful for creating parsers that need to match any input except
     * specific values, such as parsing until a delimiter or excluding certain characters.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses {@link Objects#equals(Object, Object)} for comparison, handling null values correctly</li>
     *   <li>When successful, consumes one input element</li>
     *   <li>When the input matches the excluded value, fails with a validation error</li>
     *   <li>When at EOF, fails with an unexpected EOF error</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse any character that is not a semicolon
     * Parser<Character, Character> notSemicolon = isNot(';');
     *
     * // Parse any token except "end"
     * Parser<String, String> notEnd = isNot("end");
     *
     * // Parse content until a closing bracket
     * Parser<Character, String> content = isNot(']').oneOrMore().map(chars ->
     *     chars.stream().map(String::valueOf).collect(Collectors.joining()));
     *
     * // Succeeds with 'a' for input "a"
     * // Fails for input ";" (matches the excluded value)
     * // Fails for empty input (no current element)
     * }</pre>
     *
     * @param value the value to exclude (the parser fails if the current input equals this)
     * @param <I>   the type of the input symbols
     * @return a parser that succeeds if the current input element is not equal to the provided value
     * @see #not(Parser) for negating a parser rather than a specific value
     * @see #is(Object) for the opposite operation (matching a specific value)
     */
    public static <I> Parser<I, I> isNot(I value) {
        return new Parser<>(in -> {
            if (in.isEof()) {
                return Result.unexpectedEofError(in, "any value except " + value);
            }
            I item = in.current();
            if (Objects.equals(item, value)) {
                return Result.failure(in, "any value except " + value);
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
     *   <li>If a parser returns a {@code PARTIAL} failure, it is returned immediately and no further alternatives are tried.</li>
     *   <li>If a parser fails with {@code NO_MATCH}, the next parser in the list is tried with the original input</li>
     *   <li>If all parsers fail with {@code NO_MATCH}, a combined failure is returned containing all branch failures</li>
     * </ol>
     * <p>
     * Important implementation details:
     * <ul>
     *   <li>This implements ordered choice - parsers are tried in the order they appear in the list</li>
     *   <li>When a parser fails with {@code NO_MATCH}, no input is consumed before trying the next parser</li>
     *   <li>If the list is empty, the resulting parser always fails</li>
     *   <li>All parsers in the list must have the same input and output types</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse one of three different types of token
     * Parser<Character, String> keyword = string("if").or(string("else")).or(string("while"));
     * Parser<Character, String> identifier = regex("[a-zA-Z][a-zA-Z0-9]*");
     * Parser<Character, String> number = Numeric.numeric.oneOrMore().map(Lists::join);
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
        if (parsers.isEmpty()) {
            throw new IllegalArgumentException("There must be at least one parser defined");
        }
        return new Parser<>(in -> {
            if (in.isEof()) {
                return Result.unexpectedEofError(in, "eof before `oneOf` parser");
            }
            List<Failure<I, A>> failures = null;

            for (Parser<I, A> parser : parsers) {
                Result<I, A> result = parser.apply(in);
                if (result.matches()) {
                    return result;
                }
                
                // If it's a hard failure (consumed input), stop and return it
                if (result.type() == ResultType.PARTIAL && result.input().position() > in.position()) {
                    return result;
                }

                if (failures == null){
                    failures = new ArrayList<>();
                }
                failures.add((Failure<I, A>) result);
            }
            assert failures != null;
            return Result.failure(failures);
        });
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
        return oneOf(Arrays.asList(parsers));
    }

    /**
     * Creates a parser that applies multiple parsers in sequence and collects their results in a list.
     * <p>
     * The {@code sequence} method is a fundamental combinator for parsing ordered sequences of elements.
     * It applies each parser in the provided list in order, collecting their results into a single list.
     * The parsing process works as follows:
     * <ol>
     *   <li>Applies the first parser to the input</li>
     *   <li>If successful, applies the second parser to the remaining input</li>
     *   <li>Continues applying parsers in order until all succeed or one fails</li>
     *   <li>If all parsers succeed, returns a list containing all parsed values</li>
     *   <li>If any parser fails, the entire sequence fails</li>
     * </ol>
     * <p>
     * This method is useful for parsing fixed-format data where multiple elements appear in a specific order,
     * such as record structures, command sequences, or multi-part tokens.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Each parser is applied to the input remaining after the previous parser</li>
     *   <li>Results are collected in order in an {@link ArrayList}</li>
     *   <li>The sequence fails if any parser fails</li>
     *   <li>An empty list of parsers will always succeed with an empty result list</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a date in "YYYY-MM-DD" format
     * Parser<Character, Integer> year = regex("\\d{4}").map(Integer::parseInt);
     * Parser<Character, Integer> month = regex("\\d{2}").map(Integer::parseInt);
     * Parser<Character, Integer> day = regex("\\d{2}").map(Integer::parseInt);
     *
     * Parser<Character, List<Integer>> dateComponents = sequence(Arrays.asList(
     *     year,
     *     string("-").skipThen(month),
     *     string("-").skipThen(day)
     * ));
     *
     * // Succeeds with [2023, 4, 15] for input "2023-04-15"
     * // Fails for input "2023/04/15" (wrong separator)
     * // Fails for input "2023-4-15" (month needs two digits)
     * }</pre>
     *
     * @param parsers the list of parsers to apply in sequence
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return a parser that applies each parser in sequence and collects the results in a list
     * @see #sequence(Parser, Parser) for a specialized version with two parsers
     * @see #sequence(Parser, Parser, Parser) for a specialized version with three parsers
     * @see Parser#then(Parser) for combining just two parsers
     */
    public static <I, A> Parser<I, List<A>> sequence(List<Parser<I, A>> parsers) {
        return new Parser<>(in -> {
            List<A> results = new ArrayList<>();
            Input<I> currentInput = in;
            for (Parser<I, A> parser : parsers) {
                Result<I, A> result = parser.apply(currentInput);
                if (!result.matches()) {
                    return result.cast();
                }
                results.add(result.value());
                currentInput = result.input();
            }
            return Result.success(currentInput, results);
        });
    }

    /**
     * Creates a parser that applies two parsers in sequence and returns an ApplyBuilder for further composition.
     * <p>
     * This specialized version of the {@code sequence} method applies two parsers in sequence and returns
     * an {@link ApplyBuilder} that allows for further composition and transformation of the results.
     * The parsing process works as follows:
     * <ol>
     *   <li>Applies the first parser to the input</li>
     *   <li>If successful, applies the second parser to the remaining input</li>
     *   <li>If both parsers succeed, returns an ApplyBuilder containing both results</li>
     *   <li>If either parser fails, the entire sequence fails</li>
     * </ol>
     * <p>
     * The returned ApplyBuilder allows for mapping the two results to a combined value using the
     * {@link ApplyBuilder#map(BiFunction)} method.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a key-value pair separated by a colon
     * Parser<Character, String> key = regex("[a-zA-Z]+");
     * Parser<Character, String> value = regex("[0-9]+");
     * Parser<Character, Pair<String, String>> keyValue =
     *     sequence(key, string(":").skipThen(value))
     *         .map((k, v) -> new Pair<>(k, v));
     *
     * // Succeeds with Pair("age", "30") for input "age:30"
     * // Fails for input "age=30" (wrong separator)
     * }</pre>
     *
     * @param parserA the first parser to apply in sequence
     * @param parserB the second parser to apply in sequence
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return an ApplyBuilder that allows for further composition of the two parser results
     * @see #sequence(List) for a version that works with a list of parsers
     * @see #sequence(Parser, Parser, Parser) for a version that works with three parsers
     * @see Parser#then(Parser) for the underlying implementation
     */
    public static <I, A> ApplyBuilder<I, A, A> sequence(Parser<I, A> parserA, Parser<I, A> parserB) {
        return parserA.then(parserB);
    }

    /**
     * Creates a parser that applies three parsers in sequence and returns an ApplyBuilder3 for further composition.
     * <p>
     * This specialized version of the {@code sequence} method applies three parsers in sequence and returns
     * an {@link ApplyBuilder.ApplyBuilder3} that allows for further composition and transformation of the results.
     * The parsing process works as follows:
     * <ol>
     *   <li>Applies the first parser to the input</li>
     *   <li>If successful, applies the second parser to the remaining input</li>
     *   <li>If successful, applies the third parser to the remaining input</li>
     *   <li>If all three parsers succeed, returns an ApplyBuilder3 containing all three results</li>
     *   <li>If any parser fails, the entire sequence fails</li>
     * </ol>
     * <p>
     * The returned ApplyBuilder3 allows for mapping the three results to a combined value using the
     * {@link ApplyBuilder.ApplyBuilder3#map} method with a three-argument function.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a date in "YYYY-MM-DD" format
     * Parser<Character, Integer> year = regex("\\d{4}").map(Integer::parseInt);
     * Parser<Character, Integer> month = regex("\\d{2}").map(Integer::parseInt);
     * Parser<Character, Integer> day = regex("\\d{2}").map(Integer::parseInt);
     *
     * Parser<Character, Date> dateParser =
     *     sequence(
     *         year,
     *         string("-").skipThen(month),
     *         string("-").skipThen(day)
     *     ).map((y, m, d) -> new Date(y - 1900, m - 1, d));
     *
     * // Succeeds with Date object for input "2023-04-15"
     * // Fails for input with incorrect format
     * }</pre>
     *
     * @param parserA the first parser to apply in sequence
     * @param parserB the second parser to apply in sequence
     * @param parserC the third parser to apply in sequence
     * @param <I>     the type of the input symbols
     * @param <A>     the type of the parsed value
     * @return an ApplyBuilder3 that allows for further composition of the three parser results
     * @see #sequence(List) for a version that works with a list of parsers
     * @see #sequence(Parser, Parser) for a version that works with two parsers
     * @see Parser#then(Parser) for the underlying implementation
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
        return new Parser<>(in -> {
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
        return new Parser<>(in -> {
            if (in.isEof()) {
                return Result.failure(in, String.valueOf(equivalence));
            }
            I item = in.current();
            if (Objects.equals(item, equivalence)) {
                return Result.success(in.next(), item);
            } else {
                return Result.failure(in, String.valueOf(equivalence));
            }
        });
    }


    /**
     * Creates a parser that backtracks on failure.
     * <p>
     * If the parser fails, it will report the failure at the original input position,
     * effectively undoing any input consumption that occurred before the failure.
     *
     * @param parser the parser to make atomic
     * @param <I>    the type of the input symbols
     * @param <A>    the type of the parsed value
     * @return an atomic version of the provided parser
     */
    public static <I, A> Parser<I, A> attempt(Parser<I, A> parser) {
        return new Parser<>(in -> {
            Result<I, A> res = parser.apply(in);
            if (res.matches()) return res;
            return new NoMatch<>(in, "parse attempt", (Failure<?, ?>) res);
        });
    }

}
