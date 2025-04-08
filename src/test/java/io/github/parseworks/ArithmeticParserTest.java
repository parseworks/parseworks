package io.github.parseworks;

import org.junit.jupiter.api.Test;

import java.util.function.BinaryOperator;

import static io.github.parseworks.Combinators.chr;
import static io.github.parseworks.TextUtils.dble;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for arithmetic expression parsing using the Combinators library.
 */
public class ArithmeticParserTest {

    /**
     * A reference to a parser for terms in arithmetic expressions.
     */
    public static Parser<Character, Double> term = Parser.ref();

    /**
     * A parser for arithmetic expressions, supporting addition and subtraction.
     */
    public static Parser<Character, Double> expression = term
            .then(Combinators.oneOf(
                    chr('+').as(Double::sum),
                    chr('-').as((BinaryOperator<Double>) (left, right) -> left - right)
            )).then(term).map((left, op, right) -> op.apply(left, right)).or(term).trim();

    /**
     * A parser for factors in arithmetic expressions, supporting nested expressions and double values.
     */
    public static Parser<Character, Double> factor = Combinators.oneOf(
            dble,
            expression.between('(', ')')
    ).trim();

    static {
        term.set(factor
                .then(Combinators.oneOf(
                        chr('*').as((left, right) -> left * right),
                        chr('/').as((BinaryOperator<Double>) (left, right) -> left / right)
                )).then(factor).map(left -> op -> right -> op.apply(left, right)).or(factor).trim());
    }

    /**
     * Tests the parsing of a complex arithmetic expression.
     */
    @Test
    public void mathTest() {
        String input = "3 + 5 * (2 * -8)";
        double result = expression.parse(Input.of(input)).get();
        assertEquals(-77, result, "Parsing failed for expression: " + input);
    }
}