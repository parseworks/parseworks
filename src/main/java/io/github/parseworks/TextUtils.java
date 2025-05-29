package io.github.parseworks;

import java.util.List;
import java.util.function.Function;

import static io.github.parseworks.Combinators.*;
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
     * Creates a parser that recognizes an optional sign character (+ or -).
     * <p>
     * The {@code sign} parser attempts to match a sign character from the input and
     * returns a boolean value representing the sign:
     * <ul>
     *   <li>{@code true} for a '+' character</li>
     *   <li>{@code false} for a '-' character</li>
     *   <li>{@code true} if no sign character is present (default)</li>
     * </ul>
     * <p>
     * The parsing process works as follows:
     * <ol>
     *   <li>Attempts to match a '+' character, returning {@code true} if successful</li>
     *   <li>If that fails, attempts to match a '-' character, returning {@code false} if successful</li>
     *   <li>If both attempts fail, succeeds without consuming any input and returns {@code true}</li>
     * </ol>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses {@link Combinators#oneOf(List)} to try multiple parser alternatives in order</li>
     *   <li>Consumes exactly one character when a sign is present</li>
     *   <li>Consumes no characters when no sign is present</li>
     *   <li>Always succeeds since the default case is included</li>
     * </ul>
     * <p>
     * This parser is particularly useful when parsing numeric values where a sign is optional,
     * such as integers and floating-point numbers.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a sign followed by a number
     * Parser<Character, Integer> signedNumber = sign.then(uintr)
     *     .map((signValue, number) -> signValue ? number : -number);
     *
     * signedNumber.parse("+123").get();  // Returns 123
     * signedNumber.parse("-123").get();  // Returns -123
     * signedNumber.parse("123").get();   // Returns 123 (default positive)
     * }</pre>
     *
     * @see #intr for parsing signed integers
     * @see #dble for parsing floating-point numbers that use this sign parser
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
     * Creates a parser that matches any digit character (0-9).
     * <p>
     * The {@code numeric} parser accepts any single character that is a digit according to
     * {@link Character#isDigit(char)}. The parsing process works as follows:
     * <ol>
     *   <li>Checks if the input is not at EOF</li>
     *   <li>Tests if the current character is a digit character</li>
     *   <li>If the condition is satisfied, consumes the character and returns it</li>
     *   <li>If the condition fails or input is at EOF, fails with an error message</li>
     * </ol>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Delegates to {@link Combinators#satisfy(String, java.util.function.Predicate)} with the digit predicate</li>
     *   <li>Uses {@link Character#isDigit(char)} to determine if a character is a digit</li>
     *   <li>Consumes exactly one character when successful</li>
     *   <li>Returns the matched digit character as the result</li>
     * </ul>
     * <p>
     * This parser is a fundamental building block for parsing numbers and is used by other
     * numeric parsers in this class.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a single digit
     * Parser<Character, Character> digitParser = numeric;
     * digitParser.parse("5abc").get();  // Returns '5'
     * digitParser.parse("abc").isSuccess();  // false, doesn't start with a digit
     *
     * // Parse multiple digits
     * Parser<Character, List<Character>> digits = numeric.many();
     * digits.parse("123abc").get();  // Returns List containing '1', '2', '3'
     *
     * // Convert parsed digits to an integer
     * Parser<Character, Integer> digitAsInt = numeric.map(c -> Character.getNumericValue(c));
     * }</pre>
     *
     * @see #nonZeroDigit for parsing digits 1-9
     * @see #number for parsing multi-digit numbers
     * @see #uintr for parsing unsigned integers
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
     * A parser that recognizes and parses an unsigned integer.
     * <p>
     * The {@code uintr} parser matches either a single '0' character or a sequence starting with
     * a non-zero digit (1-9) followed by zero or more digits (0-9), and converts the entire sequence
     * into an integer value. This parser handles all valid unsigned integer formats.
     * <p>
     * The parsing process works as follows:
     * <ol>
     *   <li>First attempts to match a single '0' character (using {@link #uintrZero})</li>
     *   <li>If that fails, attempts to match a non-zero digit followed by additional digits
     *       (using {@link #uintrNotZero})</li>
     *   <li>Converts the matched sequence into an integer value</li>
     * </ol>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Combines two parsers with {@link Parser#or(Parser)} to handle both zero and non-zero cases</li>
     *   <li>The {@code uintrZero} parser matches exactly the character '0' and returns 0</li>
     *   <li>The {@code uintrNotZero} parser builds multi-digit integers starting with a non-zero digit</li>
     *   <li>Always returns a non-negative integer value</li>
     * </ul>
     * <p>
     * This parser is a building block for other numeric parsers and can be used anywhere an
     * unsigned integer is required.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse simple unsigned integers
     * Parser<Character, Integer> parser = uintr;
     * parser.parse("123").get();      // Returns 123
     * parser.parse("0").get();        // Returns 0
     * parser.parse("01").get();       // Returns 0 (only consumes the '0')
     *
     * // Use in combination with other parsers
     * Parser<Character, Integer> hex = chr('0').then(chr('x').skipThen(
     *     regex("[0-9a-fA-F]+").map(s -> Integer.parseInt(s, 16))
     * ));
     *
     * // Invalid inputs
     * parser.parse("").isSuccess();   // false, empty input
     * parser.parse("abc").isSuccess(); // false, doesn't start with a digit
     * }</pre>
     *
     * @see #intr for parsing signed integers
     * @see #uintrZero for parsing just the '0' digit
     * @see #uintrNotZero for parsing non-zero integers
     */
    public static final Parser<Character, Integer> uintr = uintrZero.or(uintrNotZero);

    /**
     * A parser that recognizes and parses a signed integer.
     * <p>
     * The {@code intr} parser matches an optional sign character ('+' or '-') followed by
     * an unsigned integer, and converts the entire sequence into a signed integer value.
     * This parser handles all valid integer formats including positive and negative numbers.
     * <p>
     * The parsing process works as follows:
     * <ol>
     *   <li>First parses an optional sign using the {@link #sign} parser</li>
     *   <li>Then parses an unsigned integer using the {@link #uintr} parser</li>
     *   <li>Combines the results, applying the sign to the unsigned integer value</li>
     * </ol>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Combines the {@link #sign} and {@link #uintr} parsers using {@link Parser#then(Parser)}</li>
     *   <li>Maps the result to apply the sign: positive if {@code true}, negative if {@code false}</li>
     *   <li>Returns the resulting signed integer value</li>
     * </ul>
     * <p>
     * This parser is useful for parsing integer values in various contexts where
     * both positive and negative numbers need to be handled.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse signed integers
     * Parser<Character, Integer> parser = intr;
     * parser.parse("123").get();      // Returns 123
     * parser.parse("+123").get();     // Returns 123
     * parser.parse("-123").get();     // Returns -123
     * parser.parse("0").get();        // Returns 0
     * parser.parse("-0").get();       // Returns 0
     *
     * // Invalid inputs
     * parser.parse("").isSuccess();   // false, empty input
     * parser.parse("abc").isSuccess(); // false, doesn't start with a digit or sign
     * parser.parse("12.3").get();     // Returns 12, consumes only the integer part
     * }</pre>
     *
     * @see #sign for parsing optional sign characters
     * @see #uintr for parsing unsigned integers
     * @see #dble for parsing floating-point numbers
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
     * A parser that recognizes and parses an unsigned long integer.
     * <p>
     * The {@code ulng} parser matches either a single '0' character or a sequence starting with
     * a non-zero digit (1-9) followed by zero or more digits (0-9), and converts the entire sequence
     * into a long value. This parser handles all valid unsigned long integer formats.
     * <p>
     * The parsing process works as follows:
     * <ol>
     *   <li>First attempts to match a single '0' character (using {@link #ulngZero})</li>
     *   <li>If that fails, attempts to match a non-zero digit followed by additional digits
     *       (using {@link #ulngNotZero})</li>
     *   <li>Converts the matched sequence into a long value</li>
     * </ol>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Combines two parsers with {@link Parser#or(Parser)} to handle both zero and non-zero cases</li>
     *   <li>The {@code ulngZero} parser matches exactly the character '0' and returns 0L</li>
     *   <li>The {@code ulngNotZero} parser builds multi-digit long integers starting with a non-zero digit</li>
     *   <li>Always returns a non-negative long value</li>
     * </ul>
     * <p>
     * This parser is similar to {@link #uintr} but operates on the larger range of values supported by
     * the long data type, making it suitable for parsing larger numeric values.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse simple unsigned longs
     * Parser<Character, Long> parser = ulng;
     * parser.parse("9223372036854775807").get();  // Returns 9223372036854775807L (Long.MAX_VALUE)
     * parser.parse("0").get();                    // Returns 0L
     * parser.parse("01").get();                   // Returns 0L (only consumes the '0')
     *
     * // Invalid inputs
     * parser.parse("").isSuccess();               // false, empty input
     * parser.parse("abc").isSuccess();            // false, doesn't start with a digit
     * }</pre>
     *
     * @see #lng for parsing signed long integers
     * @see #uintr for parsing unsigned integers (with smaller range)
     * @see #dble for parsing floating-point numbers
     */
    public static final Parser<Character, Long> ulng = ulngZero.or(ulngNotZero);

    /**
     * A parser that recognizes and parses a signed long integer.
     * <p>
     * The {@code lng} parser matches an optional sign character ('+' or '-') followed by
     * an unsigned long integer, and converts the entire sequence into a signed long value.
     * This parser handles all valid long integer formats including positive and negative numbers.
     * <p>
     * The parsing process works as follows:
     * <ol>
     *   <li>First parses an optional sign using the {@link #sign} parser</li>
     *   <li>Then parses an unsigned long using the {@link #ulng} parser</li>
     *   <li>Combines the results, applying the sign to the unsigned long value</li>
     * </ol>
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Combines the {@link #sign} and {@link #ulng} parsers using {@link Parser#then(Parser)}</li>
     *   <li>Maps the result to apply the sign: positive if {@code true}, negative if {@code false}</li>
     *   <li>Returns the resulting signed long value</li>
     * </ul>
     * <p>
     * This parser is useful for parsing long integer values in various contexts where
     * both positive and negative numbers with larger ranges need to be handled.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse signed longs
     * Parser<Character, Long> parser = lng;
     * parser.parse("9223372036854775807").get();  // Returns Long.MAX_VALUE
     * parser.parse("+42").get();                  // Returns 42L
     * parser.parse("-9223372036854775808").get(); // Returns Long.MIN_VALUE
     * parser.parse("0").get();                    // Returns 0L
     * parser.parse("-0").get();                   // Returns 0L
     *
     * // Invalid inputs
     * parser.parse("").isSuccess();               // false, empty input
     * parser.parse("abc").isSuccess();            // false, doesn't start with a digit or sign
     * parser.parse("42.5").get();                 // Returns 42L, consumes only the integer part
     * }</pre>
     *
     * @see #sign for parsing optional sign characters
     * @see #ulng for parsing unsigned long integers
     * @see #intr for parsing integers with smaller range
     * @see #dble for parsing floating-point numbers
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
     * A parser that parses a double-precision floating point number from character input.
     * <p>
     * The {@code dble} parser combines a sign parser with an unsigned long parser to read
     * numeric values with optional signs. It operates as follows:
     * <ol>
     *   <li>First parses an optional sign (+ or -) using the {@code sign} parser</li>
     *   <li>Then parses the numeric portion using the {@code ulng} parser</li>
     *   <li>Combines these values to produce a signed double value</li>
     * </ol>
     * <p>
     * This parser handles standard decimal notation for floating point numbers, including
     * optional signs, integer and fractional components, and scientific notation.
     * <p>
     * Example usage:
     * <pre>{@code
     * // Parse a double value
     * Result<Character, Double> result = dble.parse("123.45");
     *
     * // Succeeds with 123.45 for input "123.45"
     * // Succeeds with -42.0 for input "-42"
     * // Succeeds with 3.14159 for input "3.14159"
     * // Succeeds with 6.022E23 for input "6.022E23"
     * // Fails for input "abc" (not a valid number)
     * }</pre>
     *
     * @see #sign for the parser that handles the optional sign character
     * @see #ulng for the parser that handles the numeric portion
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
    public static final Parser<Character, Integer> number = numeric.many().map(chars -> {
        int result = 0;
        for (Character c : chars) {
            result = result * 10 + Character.getNumericValue(c);
        }
        return result;
    });

    /**
     * Parses a single letter character.
     */
    public static final Parser<Character, Character> alpha = satisfy("<alphabet>", Character::isLetter);

    /**
     * Parses a single alphanumeric character.
     * This parser will succeed if the next input symbol is a letter or digit.
     */
    public static final Parser<Character, Character> alphaNum = satisfy( "<alphanumeric>", Character::isLetterOrDigit);


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
     * @see #alphaNum for parsing alphanumeric characters
     */
    public static final Parser<Character, String> word = alpha.many().map(chars -> {
        StringBuilder sb = new StringBuilder(chars.size());
        for (Character c : chars) {
            sb.append(c);
        }
        return sb.toString();
    });


    /**
     * A parser that recognizes and parses a hexadecimal integer.
     * <p>
     * The {@code hex} parser matches a "0x" or "0X" prefix followed by one or more
     * hexadecimal digits (0-9, a-f, A-F) and converts the sequence into an integer value.
     * <p>
     * Example: "0x1A" parses to 26
     */
    public static final Parser<Character, Integer> hex = string("0x").or(string("0X"))
            .skipThen(regex("[0-9a-fA-F]+")
                    .map(hexStr -> Integer.parseInt(hexStr, 16)));

    public static final Parser<Character,Character> whitespace = satisfy("<whitespace>", Character::isWhitespace);

    //public static final Parser<Character, Void> spaces = whitespace.skipMany();


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
