package io.github.parseworks;

import io.github.parseworks.parsers.Combinators;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.BinaryOperator;

import static io.github.parseworks.parsers.Combinators.chr;
import static io.github.parseworks.parsers.Numeric.doubleValue;
import static io.github.parseworks.parsers.Numeric.number;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssociativityTest {


    @Test
    public void testDouble() {
        Parser<Character, Double> addition = number.then(chr('+')).then(number).map((left, op, right) -> Double.sum(left, right));


        var result = addition.parse(Input.of("2+3"));
        assertEquals(5.0, result.get(), "Expected 5.0 but got " + result.get());

    }


    @Test
    public void testAssociativity() {
        Parser<Character, Double> expression = Parser.ref();
        Parser<Character, Double> term = Parser.ref();

        Parser<Character, Double> addition = term.then(chr('+')).then(expression).map((left, op, right) -> Double.sum(left, right));
        Parser<Character, Double> multiplication = doubleValue.then(chr('*')).then(term).map((left, op, right) -> left * right);
        term.set(multiplication.or(doubleValue));
        expression.set(Combinators.oneOf(List.of(
                addition,
                term
        )));

        var result = expression.parse(Input.of("2*3+4"));
        assertEquals(10.0, result.get());
        result = expression.parse(Input.of("2+3*4"));
        assertEquals(14.0, result.get());
    }

    @Test
    public void associativity2() {
        Parser<Character, Double> expression = Parser.ref();
        Parser<Character, Double> term = Parser.ref();

        Parser<Character, Double> addition = term.then(chr('+')).then(expression).map((left, op, right) -> Double.sum(left, right));
        Parser<Character, Double> multiplication = doubleValue.then(chr('*')).then(term).map((left, op, right) -> left * right);
        term.set(multiplication.or(doubleValue));
        expression.set(Combinators.oneOf(List.of(
                addition,
                term
        )));

        var result = expression.parse(Input.of("2*3+4"));
        assertEquals(10.0, result.get());
        result = expression.parse(Input.of("2+3*4"));
        assertEquals(14.0, result.get());
    }

    @Test
    public void testLeftAssociative() {
        BinaryOperator<Integer> add = Integer::sum;
        Parser<Character, Integer> leftAssocParser = number.chainLeft(chr('+').as(add), 0);

        String input = "1+2+3";
        Result<Character, Integer> result = leftAssocParser.parse(Input.of(input));
        assertEquals(6, result.get());
    }

    @Test
    public void testRightAssociative() {
        BinaryOperator<Integer> power = (a, b) -> (int) Math.pow(a, b);
        var rightAssocParser = number.chainRightMany(chr('^').as(power));

        String input = "2^3^2";
        Result<Character, Integer> result = rightAssocParser.parse(Input.of(input));
        assertEquals(512, result.get());
    }

}