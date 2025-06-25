package io.github.parseworks;

import org.junit.jupiter.api.Test;

import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import static io.github.parseworks.Combinators.chr;
import static io.github.parseworks.Combinators.oneOf;
import static io.github.parseworks.NumericParsers.integer;
import static io.github.parseworks.NumericParsers.number;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ReadMeTest {

    @Test
    public void summ() {
        Parser<Character, String> expr = Parser.ref();
        Parser<Character, String> temp = chr('X').or(
                chr('a')).then(expr).then(chr('b')).map(a -> e -> b -> a + e + b);

        expr.set(temp);

        Result<Character, String> result = expr.parse(Input.of("ABCD"));
        // Handle success or failure
        var response = result.handle(
                Result::get,
                failure -> "Error: " + failure.error()
        );

        assertTrue(response.contains("Error: Parse error at position 0:"), "Message was " + response);

        // This is a test class for the README.md file.
        // It is used to validate the code snippets in the README.md file.
        Parser<Character, Integer> sum =
                number.thenSkip(chr('+')).then(number).map(Integer::sum);

        int sumResult = sum.parse(Input.of("1+2")).get();
        assertEquals(3, sumResult); // 3

        //sum.parse(Input.of("1+z")).errorOptional().ifPresent(System.out::println);

        var response2 = sum.parse(Input.of("1+z")).handle(
                success -> "Success: no way!",
                failure -> "Error: " + failure.error()
        );
        assertTrue(response2.contains("Error: Parse error at position 2: Expected at least 1 repetitions (found only 0) but found Parse error at position 2: Expected <number> but found z"));
    }

    @Test
    public void solvingForX(){
        enum BinOp {
            ADD { BinaryOperator<Integer> op() { return Integer::sum; } },
            SUB { BinaryOperator<Integer> op() { return (a, b) -> a - b; } },
            MUL { BinaryOperator<Integer> op() { return (a, b) -> a * b; } },
            DIV { BinaryOperator<Integer> op() { return (a, b) -> a / b; } };
            abstract BinaryOperator<Integer> op();
        }

        Parser<Character, UnaryOperator<Integer>> expr = Parser.ref();

        Parser<Character, UnaryOperator<Integer>> var = chr('x').map(x -> v -> v);
        Parser<Character, UnaryOperator<Integer>> num = integer.map(i -> v -> i);
        Parser<Character, BinOp> binOp = oneOf(
                chr('+').as(BinOp.ADD),
                chr('-').as(BinOp.SUB),
                chr('*').as(BinOp.MUL),
                chr('/').as(BinOp.DIV)
        );

        Parser<Character, UnaryOperator<Integer>> binExpr = chr('(')
                .skipThen(expr)
                .then(binOp)
                .then(expr.thenSkip(chr(')')))
                .map(left -> op -> right -> x ->  op.op().apply(left.apply(x), right.apply(x)));

        expr.set(oneOf(var, num, binExpr));
        /// comment line
        UnaryOperator<Integer> eval = expr.parse(Input.of("(x*((x/2)+x))")).get();
        int result = eval.apply(4);
        assert result == 24;

    }

}
