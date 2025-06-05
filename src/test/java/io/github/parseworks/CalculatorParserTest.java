package io.github.parseworks;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BinaryOperator;

import static io.github.parseworks.Combinators.chr;
import static io.github.parseworks.NumericParsers.numeric;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CalculatorParserTest {

    public static Parser<Character, Integer> term = Parser.ref();
    public static Parser<Character, Integer> expression = term.chainLeft(operator(), 0);
    public static Parser<Character, Integer> term2 = Combinators.oneOf(List.of(
            number(),
            expression.between('(', ')')));

    public static Parser<Character, Integer> number() {
        return numeric.map(Character::getNumericValue);
    }

    public static Parser<Character, BinaryOperator<Integer>> operator() {
        return Combinators.oneOf(List.of(
                chr('+').map(op -> Integer::sum),
                chr('-').map(op -> (a, b) -> a - b),
                chr('*').map(op -> (a, b) -> a * b),
                chr('/').map(op -> (a, b) -> a / b)
        ));
    }

    @Test
    public void calculator() {
        term.set(term2);
        Input<Character> input = Input.of("3+(2*4)-5");
        Result<Character, Integer> result = expression.parse(input);
        assertEquals(3 + (2 * 4) - 5, result.get(), "Parsing failed for expression: " + input);
    }


}