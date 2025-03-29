package io.github.parseworks;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.github.parseworks.Combinators.chr;
import static io.github.parseworks.Combinators.satisfy;
import static io.github.parseworks.Parser.pure;

/**
 * The `TextUtils` class provides a set of parsers for common text parsing tasks,
 * such as parsing specific characters, digits, letters, whitespace, and strings.
 *
 * @author jason bailey
 * @version $Id: $Id
 */
public class TextUtils {

    /**
     * A parser that succeeds if the next input symbol is a numeric digit.
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
    public static Parser<Character, Character> digit = satisfy("<number>", Character::isDigit);

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

    private static final Parser<Character, Double> floating = digit.zeroOrMany()
            .map(ds -> ds.map(Character::getNumericValue))
            .map(l -> l.foldRight(0.0, (d, acc) -> d + acc / 10.0) / 10.0);

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
    public static Parser<Character, Integer> number = digit.many().map(chars -> Integer.parseInt(chars.stream()
            .map(String::valueOf)
            .collect(Collectors.joining())));

    /**
     * Parses a single letter character.
     */
    public static Parser<Character, Character> letter = satisfy("<alphabet>", Character::isLetter);



    /**
     * Parses a single alphanumeric character.
     *
     * @return a parser that parses a single alphanumeric character
     */
    public static Parser<Character, Character> alphaNum() {
        return satisfy( "<alphanumeric>", Character::isLetterOrDigit);
    }

    /**
     * Parses a sequence of letters.
     *
     * @return a parser that parses a sequence of letters
     */
    public static Parser<Character, String> word() {
        return letter.many().map(chars -> chars.stream()
                .map(String::valueOf)
                .collect(Collectors.joining()));
    }


    /**
     * Parses an integer, including optional leading sign.
     *
     * @return a parser that parses an integer
     */
    public static Parser<Character, Integer> integer() {
        return Combinators.oneOf(List.of(
                chr('+').skipThen(pure(true)),
                chr('-').skipThen(pure(false)),
                pure(true))
        ).then(number).map(s -> value -> {
            String sign = s ? "" : "-";
            return Integer.parseInt(sign + value);
        });
    }

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
        return nonZeroDigit.then(digit.zeroOrMany())
                .map(d -> ds -> ds.push(d))
                .map(ds -> ds.map(Character::getNumericValue))
                .map(converter);
    }
}
