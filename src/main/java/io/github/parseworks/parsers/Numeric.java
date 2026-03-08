package io.github.parseworks.parsers;

import io.github.parseworks.*;
import io.github.parseworks.impl.result.Match;
import io.github.parseworks.impl.result.NoMatch;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

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
    public static final Parser<Character, Character> nonZeroDigit = satisfy( "<nonZeroDigit>", (CharPredicate) (c -> c != '0' && Character.isDigit(c)));


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
    public static final Parser<Character, Character> numeric = satisfy("<number>", (CharPredicate) Character::isDigit);


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

    /**
     * Matches a double-precision floating point number with optional sign, decimal, and exponent.
     * <p>
     * Supports scientific notation (e.g., "6.022E23").
     * <pre>{@code
     * doubleValue.parse("123.45").value();   // 123.45
     * doubleValue.parse("-3.14").value();    // -3.14
     * doubleValue.parse("6.022E23").value(); // 6.022 × 10²³
     * doubleValue.parse("42").value();       // 42.0
     * doubleValue.parse(".5").value();       // 0.5
     * }</pre>
     *
     * @see #integer for integer parsing
     * @see #longValue for long integer parsing
     */
    public static final Parser<Character, Double> doubleValue = new Parser<>(in -> {
        CharSequence data = in.data();
        int start = in.position();
        int length = data.length();
        int current = start;

        if (current < length && (data.charAt(current) == '+' || data.charAt(current) == '-')) {
            current++;
        }

        int startOfNumber = current;
        boolean hasDigits = false;

        // Integer part
        while (current < length && Character.isDigit(data.charAt(current))) {
            current++;
            hasDigits = true;
        }

        // Fractional part
        if (current < length && data.charAt(current) == '.') {
            current++;
            while (current < length && Character.isDigit(data.charAt(current))) {
                current++;
                hasDigits = true;
            }
        }

        if (!hasDigits) {
            return new NoMatch<>(in, "doubleValue");
        }

        // Exponent part
        if (current < length && (data.charAt(current) == 'e' || data.charAt(current) == 'E')) {
            int exponentStart = current;
            current++;
            if (current < length && (data.charAt(current) == '+' || data.charAt(current) == '-')) {
                current++;
            }
            boolean hasExpDigits = false;
            while (current < length && Character.isDigit(data.charAt(current))) {
                current++;
                hasExpDigits = true;
            }
            if (!hasExpDigits) {
                // Not a valid exponent, back up
                current = exponentStart;
            }
        }

        String numStr = data.subSequence(start, current).toString();
        try {
            double value = Double.parseDouble(numStr);
            return new Match<>(value, in.skip(current - start));
        } catch (NumberFormatException e) {
            return new NoMatch<>(in, "doubleValue");
        }
    });

    public static final Parser<Character, Long> number = numeric.oneOrMore().map(chars -> {
        long result = 0;
        for (Character c : chars) {
            result = result * 10 + Character.getNumericValue(c);
            // Result is cast to int at the end, which may overflow but matches documented/previous behavior.
            // Using long for intermediate calculation prevents premature overflow of smaller bits.
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
     * <p>
     * Supports up to 64-bit hexadecimal values, returning an {@code Integer} (truncated if necessary).
     * For full 64-bit precision, use a parser that returns {@code Long}.
     * <pre>{@code
     * hex.parse("0xFF").value();   // 255
     * hex.parse("0x2A").value();   // 42
     * }</pre>
     */
    public static final Parser<Character, Long>  hex = new Parser<>(in -> {
        CharSequence data = in.data();
        int start = in.position();
        int length = data.length();
        int current = start;

        if (current + 2 > length) return new NoMatch<>(in, "hex");
        if (data.charAt(current) != '0') return new NoMatch<>(in, "hex");
        char prefix = data.charAt(current + 1);
        if (prefix != 'x' && prefix != 'X') return new NoMatch<>(in, "hex");
        
        current += 2;
        int digitsStart = current;
        while (current < length) {
            char c = data.charAt(current);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F'))) {
                break;
            }
            current++;
        }

        if (current == digitsStart) return new NoMatch<>(in, "hex value");

        String hexStr = data.subSequence(digitsStart, current).toString();
        try {
            long value = Long.parseLong(hexStr, 16);
            return new Match<>(value, in.skip(current - start));
        } catch (NumberFormatException e) {
            return new NoMatch<>(in, "hex value");
        }
    });

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