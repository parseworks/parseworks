import io.github.parseworks.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BinaryOperator;

import static io.github.parseworks.Text.chr;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CalculatorParserTest
 * deliberately constructs a parser with recursion. This is to demonstrate the
 * ability for the parser to detect infinite recursion and fail gracefully.
 */
public class RecursionProtectionTest {

    public static Parser<Character, Integer> number() {
        return Text.digit.map(Character::getNumericValue);
    }

    public static Parser<Character, BinaryOperator<Integer>> operator() {
        return Combinators.choice(List.of(
                chr('+').as(Integer::sum),
                chr('-').as((a, b) -> a - b),
                chr('*').as((a, b) -> a * b),
                chr('/').as((a, b) -> a / b)
        ));
    }

    public static Ref<Character, Integer> term = Parser.ref();
    public static Parser<Character, Integer> expression = term.opChainLeft(operator());

    public static Parser<Character, Integer> term2 = Combinators.choice(List.of(
            term,
            number(),
            expression.between(chr('('),chr(')'))));

    @Test
    public void calculator() {
        term.set(term2);
        Input<Character> input = Input.of("3+(2*4)-5");
        Result<Character, Integer> result = expression.parse(input);
        assertEquals(result.getOrThrow(), 6);
    }

}
