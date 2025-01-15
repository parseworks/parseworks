import io.github.parseworks.Combinators;
import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import io.github.parseworks.Ref;
import org.junit.jupiter.api.Test;

import java.util.function.BinaryOperator;

import static io.github.parseworks.Text.chr;
import static io.github.parseworks.Text.dble;

public class ArithmeticParserTest {

    public static Ref<Character, Double> term = Parser.ref();
    public static Parser<Character, Double> expression = term
            .then(Combinators.oneOf(
                    chr('+').as(Double::sum),
                    chr('-').as((BinaryOperator<Double>) (left, right) -> left - right)
            )).then(term).map((left, op, right) -> op.apply(left, right)).or(term).trim();
    public static Parser<Character, Double> factor = Combinators.oneOf(
            dble,
            expression.between('(', ')')
    ).trim();
    public static Parser<Character, Double> term2 = factor
            .then(Combinators.oneOf(
                    chr('*').as((left, right) -> left * right),
                    chr('/').as((BinaryOperator<Double>) (left, right) -> left / right)
            )).then(factor).map(left -> op -> right -> op.apply(left, right)).or(factor).trim();

    @Test
    public void mathTest() {
        term.set(term2);
        String input = "3 + 5 * (2 * -8)";
        double result = expression.parse(Input.of(input)).getOrThrow();
        System.out.println("Result: " + result);
    }


}