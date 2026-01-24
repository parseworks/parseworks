package io.github.parseworks;

import io.github.parseworks.parsers.Combinators;
import io.github.parseworks.parsers.Numeric;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BinaryOperator;

import static io.github.parseworks.parsers.Lexical.chr;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CalculatorParserTest
 * deliberately constructs a parser with recursion. This is to demonstrate the
 * ability for the parser to detect infinite recursion and fail gracefully.
 */
public class RecursionProtectionTest {

    public static Parser<Character, Integer> term = Parser.ref();
    public static Parser<Character, Integer> expression = term.chainLeftOneOrMore(operator());
    public static Parser<Character, Integer> term2 = Combinators.oneOf(List.of(
            term,
            number(),
            expression.between('(', ')')
    ));

    public static Parser<Character, Integer> number() {
        return Numeric.numeric.map(Character::getNumericValue);
    }

    public static Parser<Character, BinaryOperator<Integer>> operator() {
        return Combinators.oneOf(List.of(
                chr('+').as(Integer::sum),
                chr('-').as((a, b) -> a - b),
                chr('*').as((a, b) -> a * b),
                chr('/').as((a, b) -> a / b)
        ));
    }

    @Test
    public void calculator() {
        term.set(term2);
        Input<Character> input = Input.of("3+(2*4)-5");
        Result<Character, Integer> result = expression.parse(input);
        assertEquals(6, result.value());
    }

}
