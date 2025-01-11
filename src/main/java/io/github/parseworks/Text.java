package io.github.parseworks;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.parseworks.Combinators.choice;
import static io.github.parseworks.Combinators.satisfy;

/**
 * The `Text` class provides a set of parsers for common text parsing tasks,
 * such as parsing specific characters, digits, letters, whitespace, and strings.
 */
public class Text {

    /**
     * Parses a single character that satisfies the given predicate.
     *
     * @param predicate the predicate that the character must satisfy
     * @return a parser that parses a single character satisfying the predicate
     */
    public static Parser<Character, Character> chr(Predicate<Character> predicate) {
        return new Parser<>(input -> {
            if (input.isEof()) {
                return Result.failure(input, "End of input");
            }
            char c = input.get();
            if (predicate.test(c)) {
                return Result.success(input.next(), c);
            } else {
                return Result.failure(input, "Unexpected character: " + c);
            }
        });
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
     * Parses a single digit character.
     *
     * @return a parser that parses a single digit character
     */
    public static Parser<Character, Character> digit =  chr(Character::isDigit);

    /**
     * Parses a number.
     *
     * @return a parser that parses a number and returns it as an integer
     */
    public static Parser<Character, Integer> number = digit.oneOrMore().map(chars -> Integer.parseInt(chars.stream()
                .map(String::valueOf)
                .collect(Collectors.joining())));


    /**
     * Parses a single letter character.
     *
     * @return a parser that parses a single letter character
     */
    public static Parser<Character, Character> letter= chr(Character::isLetter);


    /**
     * Parses a single whitespace character.
     *
     * @return a parser that parses a single whitespace character
     */
    public static Parser<Character, Character> whitespace = chr(Character::isWhitespace);


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
        return chr(Character::isLetterOrDigit);
    }

    /**
     * Parses one or more whitespace characters.
     *
     * @return a parser that parses one or more whitespace characters
     */
    public static Parser<Character, String> space() {
        return whitespace.oneOrMore().map(chars -> chars.stream()
                .map(String::valueOf)
                .collect(Collectors.joining()));
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

    public static int digitToInt(Character c) {
        return Character.getNumericValue(c);
    }

    /**
     * Parses an integer, including optional leading sign.
     *
     * @return a parser that parses an integer
     */
    public static Parser<Character, Integer> integer() {
        return choice(List.of(
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

    /**
     * A parser that succeeds if the next input symbol is a numeric digit.
     */
    public static final Parser<Character, Character> nonZeroDigit = satisfy(
            c -> c != '0' && Character.isDigit(c),"nonZeroDigit");

    private static final Parser<Character, Integer> uintrNotZero = nonZeroDigitParser(
            ds -> ds.foldLeft(0, (acc, x) -> acc * 10 + x)
    );
    public static final Parser<Character, Boolean> sign =
            choice(List.of(
                    chr('+').skipThen(Parser.pure(true)),
                    chr('-').skipThen(Parser.pure(false)),
                    Parser.pure(true)
                    ));
    private static final Parser<Character, Integer> uintrZero =
            chr('0').map(zs -> 0);

    /**
     * A parser for an unsigned integer.
     */
    public static final Parser<Character, Integer> uintr = uintrZero.or(uintrNotZero);
    /**
     * A parser for a signed integer.
     */
    public static final Parser<Character, Integer> intr =
            sign.then(uintr)
                    .map((sign, i) -> sign ? i : -i);
    private static final Parser<Character, Integer> expnt =
            (chr('e').or(chr('E')))
                    .skipThen(intr);

    private static final Parser<Character, Long> ulngNotZero = nonZeroDigitParser(
            ds -> ds.foldLeft(0L, (acc, x) -> acc * 10L + x)
    );
    private static final Parser<Character, Long> ulngZero =
            chr('0').map(zs -> 0L);
    /**
     * A parser for an unsigned long.
     */
    public static final Parser<Character, Long> ulng = ulngZero.or(ulngNotZero);
    /**
     * A parser for an unsigned long.
     */
    public static final Parser<Character, Long> lng =
            sign.then(ulng)
                    .map((sign, i) -> sign ? i : -i);

    private static final Parser<Character, Double> floating =
            digit.zeroOrMore()
                    .map(ds -> ds.map(Character::getNumericValue))
                    .map(l -> l.foldRight(0.0, (d, acc) -> d + acc / 10.0) / 10.0);
    /**
     * A parser for a floating point number.
     */
    public static final Parser<Character, Double> dble =
            sign.then(ulng)
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
}