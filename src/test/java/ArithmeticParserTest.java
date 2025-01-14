import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import io.github.parseworks.Ref;
import io.github.parseworks.Result;
import org.junit.jupiter.api.Test;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static io.github.parseworks.Combinators.choice;

public class ArithmeticParserTest {

    @Test
    public void mathTest() {
        term.set(term2);
        Parser<Character, Double> parser = expression;
        String input = "3 + 5 * (2 - 8)";
        double result = parser.parse(Input.of(input)).getOrThrow();
        System.out.println("Result: " + result);
    }

    public static Ref<Character, Double> term = Parser.ref();

    public static Parser<Character, Double> expression = term
                .then(choice(
                        chr('+').skipLeftThen(term).map(right -> left -> left + right),
                        chr('-').skipLeftThen(term).map(right -> (Function<Double, Double>) left  -> left - right)
                )).map((left, right) -> right.apply(left)).or(term).trim();

    public static Parser<Character, Double> factor = choice(
            number(),
            expression.between(chr('('),chr(')'))).trim();

    public static Parser<Character, Double> term2 = factor
                .then(choice(
                        chr('*').skipLeftThen(factor).map(right -> left -> left * right),
                        chr('/').skipLeftThen(factor).map(right -> (Function<Double, Double>) left  -> left / right)
                )).map((left, right) -> right.apply(left)).or(factor).trim();


    public static Parser<Character, Double> number() {
        return digit().oneOrMore().map(chars -> Double.parseDouble(chars.stream()
                .map(String::valueOf)
                .collect(Collectors.joining()))).trim();
    }

    public static Parser<Character, Character> digit() {
        return satisfy(Character::isDigit, "<digit>");
    }

    public static Parser<Character, Character> whitespace() {
        return satisfy(Character::isWhitespace, "<whitespace>");
    }

    public static Parser<Character, Character> chr(char c) {
        return satisfy(ch -> ch == c, "<'" + c + "'>");
    }

    public static <I> Parser<I, I> satisfy(Predicate<I> predicate, String expectedType) {
        return new Parser<>(in -> {
            if (in.isEof()) {
                return Result.failureEof(in, "Unexpected end of input. Expected " + expectedType);
            }
            I item = in.current();
            if (predicate.test(item)) {
                return Result.success(in.next(), item);
            } else {
                return Result.failure(in, "Failure at position " + in.position() + ", saw '" + item + "', expected " + expectedType);
            }
        });
    }
}