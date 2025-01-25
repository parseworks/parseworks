package io.github.parseworks;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.parseworks.Combinators.satisfy;

/**
 * The `Text` class provides a set of parsers for common text parsing tasks,
 * such as parsing specific characters, digits, letters, whitespace, and strings.
 */
public class Text {

    /**
     * A parser that succeeds if the next input symbol is a numeric digit.
     */
    public static final Parser<Character, Character> nonZeroDigit = satisfy(
            c -> c != '0' && Character.isDigit(c), "<nonZeroDigit>");

    /**
     * Parses a sign character.
     */
    public static final Parser<Character, Boolean> sign = Combinators.oneOf(List.of(
            chr('+').skipThen(Parser.pure(true)),
            chr('-').skipThen(Parser.pure(false)),
            Parser.pure(true)
    ));

    private static final Parser<Character, Integer> uintrZero = chr('0').map(zs -> 0);
    private static final Parser<Character, Long> ulngZero = chr('0').map(zs -> 0L);

    /**
     * Parses a single digit character.
     */
    public static Parser<Character, Character> digit = satisfy(Character::isDigit, "<number>");

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

    private static final Parser<Character, Double> floating = digit.zeroOrMore()
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
    public static Parser<Character, Integer> number = digit.oneOrMore().map(chars -> Integer.parseInt(chars.stream()
            .map(String::valueOf)
            .collect(Collectors.joining())));

    /**
     * Parses a single letter character.
     */
    public static Parser<Character, Character> letter = satisfy(Character::isLetter, "<alphabet>");

    /**
     * Parses a single character that satisfies the given predicate.
     *
     * @param predicate the predicate that the character must satisfy
     * @return a parser that parses a single character satisfying the predicate
     */
    public static Parser<Character, Character> chr(Predicate<Character> predicate) {
        return satisfy(predicate, "<character>");
    }

    /**
     * Parses a specific character.
     *
     * @param c the character to parse
     * @return a parser that parses the specified character
     */
    public static Parser<Character, Character> chr(char c) {
        return chr(ch -> ch == c);
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
     * Parses a single alphanumeric character.
     *
     * @return a parser that parses a single alphanumeric character
     */
    public static Parser<Character, Character> alphaNum() {
        return satisfy(Character::isLetterOrDigit, "<alphanumeric>");
    }

    /**
     * Parses a sequence of letters.
     *
     * @return a parser that parses a sequence of letters
     */
    public static Parser<Character, String> word() {
        return letter.oneOrMore().map(chars -> chars.stream()
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
                chr('+').skipThen(Parser.pure(true)),
                chr('-').skipThen(Parser.pure(false)),
                Parser.pure(true))
        ).then(number).map(s -> value -> {
            String sign = s ? "" : "-";
            return Integer.parseInt(sign + value);
        });
    }

    private static <T> Parser<Character, T> nonZeroDigitParser(Function<FList<Integer>, T> converter) {
        return nonZeroDigit.then(digit.zeroOrMore())
                .map(d -> ds -> ds.push(d))
                .map(ds -> ds.map(Character::getNumericValue))
                .map(converter);
    }
}