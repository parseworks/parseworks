import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import io.github.parseworks.Ref;
import io.github.parseworks.Result;
import io.github.parseworks.impl.Success;
import org.junit.jupiter.api.Test;

import java.io.CharArrayReader;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

import static io.github.parseworks.Combinators.*;
import static io.github.parseworks.TextUtils.*;

public class ReadMeTest {

    @Test
    public void summ() {
        Ref<Character, String> expr = Parser.ref();
        Parser<Character, String> temp = chr('X').or(
                chr('a')).then(expr).then(chr('b')).map(a -> e -> b -> a + e + b);

        expr.set(temp);
        char[] charData = { 'A', 'B', 'C', 'D' };

        // Construct Input from a char array
        Input<Character> chrArrInput = Input.of(charData);
        // Construct Input from a String
        Input<Character> strInput = Input.of("ABCD");
        // Construct Input from a Reader
        Input<Character> rdrInput = Input.of(new CharArrayReader(charData));

        Result<Character, String> result = expr.parse(Input.of("ABCD"));

        // Handle success or failure
        result.handle(
                success -> System.out.println(success.getOrThrow()),
                failure -> System.out.println("Error: " + failure.getFullErrorMessage())
        );
        // This is a test class for the README.md file.
        // It is used to validate the code snippets in the README.md file.
        Parser<Character, Integer> sum =
                number.thenSkip(chr('+')).then(number).map(Integer::sum);

        int sumResult = sum.parse(Input.of("1+2")).getOrThrow();
        System.out.println(sumResult); // 3

        try {
            sum.parse(Input.of("1+z")).getOrThrow();
        } catch (Exception e) {
            System.out.println(e.getMessage()); // Failure at position 2, saw 'z', expected <number>
        }

        sum.parse(Input.of("1+z")).handle(
                success -> System.out.println("Success: no way!"),
                failure -> System.out.println("Error: " + failure.getError())
        );
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

        Ref<Character, UnaryOperator<Integer>> expr = Parser.ref();

        Parser<Character, UnaryOperator<Integer>> var = chr('x').map(x -> v -> v);
        Parser<Character, UnaryOperator<Integer>> num = intr.map(i -> v -> i);
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
        UnaryOperator<Integer> eval = expr.parse(Input.of("(x*((x/2)+x))")).getOrThrow();
        int result = eval.apply(4);
        assert result == 24;

    }

}
