package io.github.parseworks;

import java.util.List;
import java.util.function.Function;

import static io.github.parseworks.Combinators.chr;
import static io.github.parseworks.Combinators.satisfy;
import static io.github.parseworks.Parser.pure;

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
 *   <li>{@link #numeric} - Parses any digit character (0-9)</li>
 *   <li>{@link #nonZeroDigit} - Parses any non-zero digit character (1-9)</li>
 *   <li>{@link #alpha} - Parses any letter character</li>
 *   <li>{@link #alphaNum} - Parses any alphanumeric character</li>
 *   <li>{@link #whitespace} - Parses any whitespace character</li>
 *   <li>{@link #uintr} - Parses an unsigned integer</li>
 *   <li>{@link #intr} - Parses a signed integer</li>
 *   <li>{@link #dble} - Parses a floating-point number</li>
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
public class TextUtils {

    /**
     * Creates a parser that matches any digit character except '0'.
     * <p>
     * The {@code nonZeroDigit} parser accepts any character that is both a digit (0-9)
     * and not the digit '0', effectively matching digits 1-9. The parsing process works as follows:
     * <ol>
     *   <li>Checks if the input is not at EOF</li>
     *   <li>Tests if the current character is a digit character (using {@link Character#isDigit(char)})</li>
     *   <li>Tests if the current character is not '0'</li>
     *   <li>If both conditions are satisfied, consumes the character and returns it</li>
     *   <li>If either condition fails or at EOF, fails with an error message</li>
     * </ol>
     * <p>
     * This parser is particularly useful for parsing numerical formats where leading zeros
     * are not allowed, such as certain number formats in programming languages or data exchange formats.
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Delegates to {@link Combinators#satisfy(String, java.util.function.Predicate)} with a compound predicate</li>
     *   <li>Uses a combination of digit checking and zero exclusion in a single predicate</li>
     *   <li>Consumes exactly one character when successful</li>
     *   <li>Returns the matched character (1-9) as the result</li>
     * </ul>
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a non-zero digit
     * Parser<Character, Character> firstDigit = nonZeroDigit;
     * firstDigit.parse("123").get();  // Returns '1'
     * firstDigit.parse("0123").isSuccess();  // false, starts with '0'
     *
     * // Parse a number that doesn't start with zero
     * Parser<Character, Integer> naturalNumber = nonZeroDigit
     *     .then(numeric.many())
     *     .map((first, rest) -> {
     *         String numStr = first + rest.stream()
     *             .map(String::valueOf)
     *             .collect(Collectors.joining());
     *         return Integer.parseInt(numStr);
     *     });
     *
     * // Parse multiple non-zero digits
     * Parser<Character, List<Character>> nonZeroDigits = nonZeroDigit.many();
     * }</pre>
     *
     * @see #numeric for parsing any digit (0-9)
     * @see #number for parsing multi-digit numbers
     * @see #uintr for parsing unsigned integers
     */
    public static final Parser<Character, Character> nonZeroDigit = satisfy( "<nonZeroDigit>", c -> c != '0' && Character.isDigit(c));

    /**
     * A parser that parses a sign character.
     * This parser will succeed if the next input symbol is a '+' or '-' character,
     * returning `true` for '+' and `false` for '-'. If no sign character is present,
     * it will default to `true`.
     */
    public static final Parser<Character, Boolean> sign = Combinators.oneOf(List.of(
            chr('+').as(true),
            chr('-').as(false),
            pure(true)
    ));

    /**
     * A parser that parses the character '0' and returns the integer 0.
     */
    private static final Parser<Character, Integer> uintrZero = chr('0').as(0);

    /**
     * A parser that parses the character '0' and returns the long 0.
     */
    private static final Parser<Character, Long> ulngZero = chr('0').as( 0L);

    /**
     * Parses a single digit character.
     */
    public static Parser<Character, Character> numeric = satisfy("<number>", Character::isDigit);


    /**
     * A parser that parses a non-zero unsigned integer.
     * This parser will succeed if the next input symbols form a non-zero unsigned integer,
     * and will return the parsed integer value.
     */
    private static final Parser<Character, Integer> uintrNotZero = nonZeroDigitParser(
            ds -> ds.foldLeft(0, (acc, x) -> acc * 10 + x)
    );

    /**
     * A parser for an unsigned integer.
     */
    public static final Parser<Character, Integer> uintr = uintrZero.or(uintrNotZero);

    /**
     * A parser for a signed integer.
     */
    public static final Parser<Character, Integer> intr = sign.then(uintr)
            .map((sign, i) -> sign ? i : -i);

    /**
     * A parser that parses an exponent part of a floating-point number.
     * This parser will succeed if the next input symbol is 'e' or 'E', followed by an integer.
     */
    private static final Parser<Character, Integer> expnt = (chr('e').or(chr('E')))
            .skipThen(intr);

    private static final Parser<Character, Long> ulngNotZero = nonZeroDigitParser(
            ds -> ds.foldLeft(0L, (acc, x) -> acc * 10L + x)
    );

    /**
     * A parser for an unsigned long.
     */
    public static final Parser<Character, Long> ulng = ulngZero.or(ulngNotZero);

    /**
     * A parser for a signed long.
     */
    public static final Parser<Character, Long> lng = sign.then(ulng)
            .map((sign, i) -> sign ? i : -i);

    private static final Parser<Character, Double> floating = numeric.zeroOrMany()
            .map(digits -> {
                double result = 0.0;
                double factor = 0.1;
                for (Character c : digits) {
                    result += Character.getNumericValue(c) * factor;
                    factor *= 0.1;
                }
                return result;
            });

    /**
     * A parser for a floating point number.
     */
    public static final Parser<Character, Double> dble = sign.then(ulng)
            .then((chr('.').skipThen(floating)).optional())
            .then(expnt.optional())
            .map((sn, i, f, exp) -> {
                double r = i.doubleValue();
                if (f.isPresent()) {
                    r += f.get();
                }
                if (exp.isPresent()) {
                    r = r * Math.pow(10.0, exp.get());
                }
                return sn ? r : -r;
            });

    /**
     * Parses a number.
     */
    public static Parser<Character, Integer> number = numeric.many().map(chars -> {
        int result = 0;
        for (Character c : chars) {
            result = result * 10 + Character.getNumericValue(c);
        }
        return result;
    });

    /**
     * Parses a single letter character.
     */
    public static Parser<Character, Character> alpha = satisfy("<alphabet>", Character::isLetter);

    /**
     * Parses a single alphanumeric character.
     * This parser will succeed if the next input symbol is a letter or digit.
     */
    public static Parser<Character, Character> alphaNum = satisfy( "<alphanumeric>", Character::isLetterOrDigit);


    /**
     * Parses a sequence of letters.
     * This parser will succeed if the next input symbols form a sequence of letters,
     * and will return the parsed result converted by the given converter function.
     */
    public static Parser<Character, String> word = alpha.many().map(chars -> {
        StringBuilder sb = new StringBuilder(chars.size());
        for (Character c : chars) {
            sb.append(c);
        }
        return sb.toString();
    });


    public static Parser<Character,Character> whitespace = satisfy("<whitespace>", Character::isWhitespace);

    //public static final Parser<Character, Void> spaces = whitespace.skipMany();

    /**
     * Parses an integer, including optional leading sign.
     * This parser will succeed if the next input symbols form a valid integer,
     * and will return the parsed result converted by the given converter function.
     */
    public static Parser<Character, Integer> integer = sign.then(number)
            .map((sign, value) -> sign ? value : -value);

    /**
     * A parser that parses a non-zero digit followed by zero or more digits.
     * This parser will succeed if the next input symbols form a non-zero digit followed by zero or more digits,
     * and will return the parsed result converted by the given converter function.
     *
     * @param converter the function to convert the parsed digits
     * @param <T>       the type of the parsed value
     * @return a parser that parses a non-zero digit followed by zero or more digits and converts the result
     */
    private static <T> Parser<Character, T> nonZeroDigitParser(Function<FList<Integer>, T> converter) {
        return nonZeroDigit.then(numeric.zeroOrMany())
                .map(d -> ds -> ds.prepend(d))
                .map(ds -> ds.map(Character::getNumericValue))
                .map(converter);
    }
}
