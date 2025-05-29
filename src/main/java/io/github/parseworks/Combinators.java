package io.github.parseworks;

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
     *     word.sepBy(chr(','));
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
                    return Result.failure(in, "\"" + str + "\"", "eof");
                }

                char expected = str.charAt(i);
                char actual = currentInput.current();

                if (expected != actual) {
                    // Return failure with context about the mismatch
                    String found = i == 0 ? String.valueOf(actual) :
                            str.substring(0, i) + actual + "...";
                    return Result.failure(in, "\"" + str + "\"", found);
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

        return satisfy("<oneOf> " + str, charSet::contains);
    }

    /**
     * Creates a parser that matches input against a regular expression pattern.
     * <p>
     * The {@code regex()} method creates a parser that incrementally matches characters from the input
     * stream against the provided regular expression pattern. This parser handles both start (^) and
     * end ($) anchors appropriately in the context of streaming input.
     * <p>
     * Key features:
     * <ul>
     *   <li>Progressively reads characters one by one, building a buffer</li>
     *   <li>Attempts matching after each character to support early success/failure</li>
     *   <li>Properly respects start anchors (^) by only matching from the beginning</li>
     *   <li>Properly respects end anchors ($) by only accepting matches at the end of input</li>
     *   <li>For non-anchored patterns, returns the longest valid match</li>
     *   <li>Efficiently handles streaming input by using {@link Matcher#hitEnd()} for optimization</li>
     *   <li>Special case handling for empty input</li>
     * </ul>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses {@link Matcher#lookingAt()} to ensure matches start at the beginning of the buffer</li>
     *   <li>Uses a maximum look-ahead limit (1000 characters) for safety</li>
     *   <li>Compiled with {@link Pattern#DOTALL} and {@link Pattern#UNICODE_CHARACTER_CLASS} flags</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Basic word parser
     * Parser<Character, String> word = regex("[a-zA-Z]+");
     * word.parse("Hello").get();  // Returns "Hello"
     * word.parse("Hello123").get();  // Returns "Hello"
     *
     * // Pattern with end anchor - only matches at end of input
     * Parser<Character, String> complete = regex("\\d+$");
     * complete.parse("123").isSuccess();  // true
     * complete.parse("123abc").isSuccess();  // false, digits not at end
     *
     * // Email address parser
     * Parser<Character, String> email = regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
     * email.parse("user@example.com").get();  // Returns "user@example.com"
     * }</pre>
     *
     * @param regex the regular expression pattern to match against
     * @return a parser that matches the input against the given regular expression
     * @see Pattern for information about Java regular expression syntax
     * @see #string(String) for exact string matching
     */
    public static Parser<Character, String> regex(String regex) {
        boolean hasEndAnchor = regex.endsWith("$") && !regex.endsWith("\\$");
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL | Pattern.UNICODE_CHARACTER_CLASS);

        return new Parser<>(in -> {
            // Special case for empty input
            if (in.isEof()) {
                Matcher emptyMatcher = pattern.matcher("");
                if (emptyMatcher.lookingAt()) {
                    return Result.success(in, emptyMatcher.group());
                }
                return Result.failure(in, regex, "eof");
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
            String preview = buffer.length() > 10 ? buffer.substring(0, 10) + "..." : buffer.toString();
            return Result.failure(in, regex, preview);
        });
    }
}
