import io.github.parseworks.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BinaryOperator;

import static io.github.parseworks.Combinators.chr;
import static io.github.parseworks.TextUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssociativityTest {


    @Test
    public void testDouble() {
        Parser<Character, Double> addition = number.then(chr('+')).then(number).map((left, op, right) -> {
            System.out.println("here");
            return Double.sum(left, right);
        });


        var result = addition.parse(Input.of("2+3"));
        System.out.println(result.getOrThrow());

    }


    @Test
    public void testAssociativity() {
        Ref<Character, Double> expression = Parser.ref();
        Ref<Character, Double> term = Parser.ref();

        Parser<Character, Double> addition = term.then(chr('+')).then(expression).map((left, op, right) -> Double.sum(left, right));
        Parser<Character, Double> multiplication = dble.then(chr('*')).then(term).map((left, op, right) -> left * right);
        term.set(multiplication.or(dble));
        expression.set(Combinators.oneOf(List.of(
                addition,
                term
        )));

        var result = expression.parse(Input.of("2*3+4"));
        assertEquals(10.0, result.getOrThrow());
        result = expression.parse(Input.of("2+3*4"));
        assertEquals(14.0, result.getOrThrow());
    }

    @Test
    public void associativity2() {
        Ref<Character, Double> expression = Parser.ref();
        Ref<Character, Double> term = Parser.ref();

        Parser<Character, Double> addition = term.then(chr('+')).then(expression).map((left, op, right) -> Double.sum(left, right));
        Parser<Character, Double> multiplication = dble.then(chr('*')).then(term).map((left, op, right) -> left * right);
        term.set(multiplication.or(dble));
        expression.set(Combinators.oneOf(List.of(
                addition,
                term
        )));

        var result = expression.parse(Input.of("2*3+4"));
        assertEquals(10.0, result.getOrThrow());
        result = expression.parse(Input.of("2+3*4"));
        assertEquals(14.0, result.getOrThrow());
    }

    @Test
    public void testLeftAssociative() {
        BinaryOperator<Integer> add = Integer::sum;
        Parser<Character, Integer> leftAssocParser = number.chainLeftZeroOrMany(chr('+').as(add), 0);

        String input = "1+2+3";
        Result<Character, Integer> result = leftAssocParser.parse(Input.of(input));
        assertEquals(6, result.getOrThrow());
    }

    @Test
    public void testRightAssociative() {
        BinaryOperator<Integer> power = (a, b) -> (int) Math.pow(a, b);
        var rightAssocParser = number.chainRightMany(chr('^').as(power));

        String input = "2^3^2";
        Result<Character, Integer> result = rightAssocParser.parse(Input.of(input));
        assertEquals(512, result.getOrThrow());
    }

}