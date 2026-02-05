package io.github.parseworks.parsers;

import io.github.parseworks.Lists;
import io.github.parseworks.Parser;

import java.util.List;
import java.util.function.Function;

import static io.github.parseworks.Parser.pure;
import static io.github.parseworks.parsers.Combinators.satisfy;
import static io.github.parseworks.parsers.Lexical.chr;

public class Numeric {

    /**
     * Matches a single non-zero digit (1-9).
     * <p>
     * Useful for parsing numbers without leading zeros.
     * <pre>{@code
     * nonZeroDigit.parse("5").value();    // '5'
     * nonZeroDigit.parse("0").matches();  // false
     * }</pre>
     *
     * @see #numeric for any digit including '0'
     * @see #unsignedInteger for complete integer parsing
     */
    public static final Parser<Character, Character> nonZeroDigit = satisfy( "<nonZeroDigit>", c -> c != '0' && Character.isDigit(c));


    /**
     * Matches a single digit (0-9).
     * <pre>{@code
     * numeric.parse("5abc").value();             // '5'
     * numeric.oneOrMore().parse("123abc");       // ['1', '2', '3']
     * numeric.map(Character::getNumericValue);   // converts to int
     * }</pre>
     *
     * @see #nonZeroDigit for digits 1-9 only
     * @see #number for multi-digit parsing
     */
    public static final Parser<Character, Character> numeric = satisfy("<number>", Character::isDigit);


    /**
     * Matches an optional sign (+ or -), defaulting to positive.
     * <p>
     * Returns {@code true} for positive (+) or no sign, {@code false} for negative (-).
     * <pre>{@code
     * sign.parse("+123").value();  // true
     * sign.parse("-123").value();  // false
     * sign.parse("123").value();   // true (default positive)
     * }</pre>
     *
     * @see #integer for signed integer parsing
     */
    public static final Parser<Character, Boolean> sign = Combinators.oneOf(
            chr('+').as(true),
            chr('-').as(false),
            pure(true)
    );

    /** Matches '0' and returns 0. */
    private static final Parser<Character, Integer> unsignedIntegerZero = chr('0').as(0);

    /** Matches '0' and returns 0L. */
    private static final Parser<Character, Long> unsignedLongZero = chr('0').as( 0L);


    private static final Parser<Character, Integer> unSignedIntegerNotZero = nonZeroDigitParser(
            ds -> Lists.foldLeft(ds, 0, (acc, x) -> acc * 10 + x)
    );

    private static final Parser<Character, Long> unsignedLongNotZero = nonZeroDigitParser(
            ds -> Lists.foldLeft(ds, 0L, (acc, x) -> acc * 10L + x)
    );

    /**
     * Matches an unsigned integer without leading zeros.
     * <p>
     * Accepts "0" or a digit 1-9 followed by any digits.
     * <pre>{@code
     * unsignedInteger.parse("123").value();  // 123
     * unsignedInteger.parse("0").value();    // 0
     * unsignedInteger.parse("007").value();  // 0 (stops after first '0')
     * }</pre>
     *
     * @see #integer for signed integers
     * @see #number for multi-digit parsing that accepts leading zeros
     */
    public static final Parser<Character, Integer> unsignedInteger = unsignedIntegerZero.or(unSignedIntegerNotZero);

    /**
     * Matches a signed integer with optional sign (+/-).
     * <pre>{@code
     * integer.parse("123").value();   // 123
     * integer.parse("+123").value();  // 123
     * integer.parse("-123").value();  // -123
     * }</pre>
     *
     * @see #unsignedInteger for unsigned parsing
     * @see #longValue for larger integers
     */
    public static final Parser<Character, Integer> integer = sign.then(unsignedInteger)
            .map((sign, i) -> sign ? i : -i);

    private static final Parser<Character, Integer> exponent = (chr('e').or(chr('E')))
            .skipThen(integer);

    /**
     * Matches an unsigned long integer without leading zeros.
     * <p>
     * Similar to {@link #unsignedInteger} but returns {@code Long}.
     * <pre>{@code
     * unsignedLong.parse("9223372036854775807").value();  // Long.MAX_VALUE
     * unsignedLong.parse("0").value();                    // 0L
     * }</pre>
     *
     * @see #longValue for signed longs
     * @see #unsignedInteger for Integer range
     */
    public static final Parser<Character, Long> unsignedLong = unsignedLongZero.or(unsignedLongNotZero);

    /**
     * Matches a signed long integer with optional sign (+/-).
     * <pre>{@code
     * longValue.parse("9223372036854775807").value();  // Long.MAX_VALUE
     * longValue.parse("-42").value();                  // -42L
     * }</pre>
     *
     * @see #unsignedLong for unsigned parsing
     * @see #integer for Integer range
     */
    public static final Parser<Character, Long> longValue = sign.then(unsignedLong)
            .map((sign, i) -> sign ? i : -i);

    private static final Parser<Character, Double> floating = numeric.zeroOrMore()
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
     * Matches a double-precision floating point number with optional sign, decimal, and exponent.
     * <p>
     * Supports scientific notation (e.g., "6.022E23").
     * <pre>{@code
     * doubleValue.parse("123.45").value();   // 123.45
     * doubleValue.parse("-3.14").value();    // -3.14
     * doubleValue.parse("6.022E23").value(); // 6.022 × 10²³
     * doubleValue.parse("42").value();       // 42.0
     * }</pre>
     *
     * @see #integer for integer parsing
     * @see #longValue for long integer parsing
     */
    public static final Parser<Character, Double> doubleValue = sign.then(unsignedLong)
            .then((chr('.').skipThen(floating)).optional())
            .then(exponent.optional())
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

    public static final Parser<Character, Integer> number = numeric.oneOrMore().map(chars -> {
        int result = 0;
        for (Character c : chars) {
            result = result * 10 + Character.getNumericValue(c);
        }
        return result;
    });

    private static final Parser<Character, String> hexDigits = satisfy("<hexDigit>",
            (Character c) -> (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))
            .oneOrMore()
            .map(Lists::join)
            .expecting("hex value");
 
    /**
     * Matches a hexadecimal integer with "0x" or "0X" prefix.
     * <pre>{@code
     * hex.parse("0xFF").value();   // 255
     * hex.parse("0x2A").value();   // 42
     * }</pre>
     */
    public static final Parser<Character, Integer>  hex = chr('0').skipThen(chr('x').or(chr('X')))
        .skipThen(hexDigits)
        .map(hexStr -> Integer.parseInt(hexStr, 16));

    /**
     * A parser that parses a non-zero digit followed by zero or more digits.
     * This parser will succeed if the next input symbols form a non-zero digit followed by zero or more digits,
     * and will return the parsed result converted by the given converter function.
     *
     * @param converter the function to convert the parsed digits
     * @param <T>       the type of the parsed value
     * @return a parser that parses a non-zero digit followed by zero or more digits and converts the result
     */
    private static <T> Parser<Character, T> nonZeroDigitParser(Function<List<Integer>, T> converter) {
        return nonZeroDigit.then(numeric.zeroOrMore())
                .map(d -> ds ->
                    converter.apply(Lists.prepend(Character.getNumericValue(d), Lists.map(ds, Character::getNumericValue))));
    }
}