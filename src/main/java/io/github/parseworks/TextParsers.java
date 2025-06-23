package io.github.parseworks;

import static io.github.parseworks.Combinators.satisfy;

/**
 * The {@code TextUtils} class provides a comprehensive set of parsers for common text parsing tasks.
 * <p>
 * This utility class contains static parser definitions and methods for parsing:
 * <ul>
 *   <li>Numeric values (digits, integers, longs, doubles)</li>
 *   <li>Alphabetic and alphanumeric characters</li>
 *   <li>Whitespace</li>
 *   <li>Words and textual content</li>
 *   <li>Signs and special characters</li>
 * </ul>
 * <p>
 * The parsers in this class are designed to be composable building blocks for creating
 * more complex parsers. They follow functional programming principles, allowing them to
 * be combined using methods like {@code then()}, {@code or()}, {@code many()}, etc.
 * <p>
 * Key parsers include:
 * <ul>
 *   <li>{@link NumericParsers#numeric} - Parses any digit character (0-9)</li>
 *   <li>{@link NumericParsers#nonZeroDigit} - Parses any non-zero digit character (1-9)</li>
 *   <li>{@link #alpha} - Parses any letter character</li>
 *   <li>{@link #alphaNumeric} - Parses any alphanumeric character</li>
 *   <li>{@link #whitespace} - Parses any whitespace character</li>
 *   <li>{@link NumericParsers#unsignedInteger} - Parses an unsigned integer</li>
 *   <li>{@link NumericParsers#integer} - Parses a signed integer</li>
 *   <li>{@link NumericParsers#doubleValue} - Parses a floating-point number</li>
 *   <li>{@link #word} - Parses a sequence of letters</li>
 * </ul>
 * <p>
 * Example usage:
 * <pre>{@code
 * // Parse an integer
 * Parser<Character, Integer> parser = TextUtils.intr;
 * Result<Character, Integer> result = parser.parse("-123");
 * int value = result.get();  // -123
 *
 * // Parse a word
 * Parser<Character, String> wordParser = TextUtils.word;
 * String word = wordParser.parse("Hello123").get();  // "Hello"
 *
 * // Combine parsers
 * Parser<Character, Pair<String, Integer>> nameAndAge =
 *     TextUtils.word.skip(whitespace).then(integer);
 * }</pre>
 *
 * This class works closely with {@link Combinators} which provides the fundamental
 * parser combinators used to build these text parsing utilities.
 *
 * @author jason bailey
 * @version $Id: $Id
 * @see Combinators
 * @see Parser
 */
public class TextParsers {



    /**
     * Creates a parser that matches a single alphabetical character (a-z, A-Z).
     * <p>
     * The {@code alpha} parser recognizes any letter as defined by {@link Character#isLetter(char)},
     * which includes both uppercase and lowercase letters from the ASCII set and other Unicode
     * letter characters. The parsing process works as follows:
     * <ol>
     *   <li>Checks if the current input character is a letter</li>
     *   <li>If it is a letter, returns that character as the result and advances the input position</li>
     *   <li>If it is not a letter, the parser fails</li>
     * </ol>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses Java's {@link Character#isLetter(char)} method to determine what constitutes a letter</li>
     *   <li>Consumes exactly one character on success</li>
     *   <li>Fails if the input is empty or the current character is not a letter</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a single letter
     * Parser<Character, Character> letterParser = alpha();
     *
     * // Succeeds with 'a' for input "abc"
     * // Succeeds with 'Z' for input "Zed"
     * // Fails for input "123" (no letters)
     * // Fails for input "" (empty input)
     * }</pre>
     *
     * @see NumericParsers#numeric for parsing numeric digits
     * @see #alphaNumeric for parsing alphanumeric characters
     * @see Combinators#chr for parsing with a custom character predicate
     */
    public static final Parser<Character, Character> alpha = satisfy("<alphabet>", Character::isLetter);

    /**
     * Parses a single alphanumeric character.
     * This parser will succeed if the next input symbol is a letter or digit.
     */
    public static final Parser<Character, Character> alphaNumeric = satisfy( "<alphanumeric>", Character::isLetterOrDigit);

    /**
     * Creates a parser that trims whitespace from the input before and after parsing.
     * <p>
     * The {@code trim} parser applies the given parser to the input after skipping any leading
     * whitespace, and then skips any trailing whitespace from the result. This is useful for
     * ensuring that whitespace does not interfere with the parsing of the main content.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses {@link #skipWhitespace(Input)} to remove leading and trailing whitespace</li>
     *   <li>Returns a new parser that applies the original parser to the trimmed input</li>
     * </ul>
     *
     * @param parser the parser to apply after trimming whitespace
     * @param <A>    the type of the parsed value
     * @return a new parser that trims whitespace around the original parser
     */
    public static <A> Parser<Character,A>  trim(Parser<Character, A> parser) {
        return new Parser<>(in -> {
            Input<Character> trimmedInput = skipWhitespace(in);
            Result<Character, A> result = parser.apply(trimmedInput);
            if (result.isSuccess()) {
                trimmedInput = skipWhitespace(result.next());
                return Result.success(trimmedInput, result.get());
            }
            return result;
        });
    }

    private static Input<Character> skipWhitespace(Input<Character> in) {
        while (!in.isEof() && Character.isWhitespace(in.current())) {
            in = in.next();
        }
        return in;
    }

    /**
     * Parses a sequence of letters and returns them as a string.
     * <p>
     * The {@code word} parser accepts one or more consecutive letter characters
     * (as defined by {@link Character#isLetter(char)}) and concatenates them into a
     * string result. If no letters are found, it returns an empty string.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses {@link #alpha} parser with {@code many()} to collect letter characters</li>
     *   <li>Efficiently converts the character sequence to a string using StringBuilder</li>
     *   <li>Returns the concatenated string of all matched letters</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a word
     * Parser<Character, String> parser = word;
     * parser.parse("Hello123").get();  // Returns "Hello"
     * parser.parse("").get();          // Returns "" (empty string)
     *
     * // Use with other parsers
     * Parser<Character, String> identifier = word.then(alphaNum.many())
     *     .map((w, rest) -> w + rest.stream()
     *         .map(String::valueOf)
     *         .collect(Collectors.joining()));
     * }</pre>
     *
     * @see #alpha for parsing single letter characters
     * @see #alphaNumeric for parsing alphanumeric characters
     */
    public static final Parser<Character, String> word = alpha.many().map(chars -> {
        StringBuilder sb = new StringBuilder(chars.size());
        for (Character c : chars) {
            sb.append(c);
        }
        return sb.toString();
    });

    public static final Parser<Character,Character> whitespace = satisfy("<whitespace>", Character::isWhitespace);

    //public static final Parser<Character, Void> spaces = whitespace.skipMany();
}
