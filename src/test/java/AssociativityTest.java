
import io.github.parseworks.Input;
import io.github.parseworks.Parser;
import io.github.parseworks.Ref;
import org.junit.jupiter.api.Test;

import java.util.List;


import static io.github.parseworks.Combinators.choice;
import static io.github.parseworks.Text.*;
import static org.junit.jupiter.api.Assertions.*;

public class AssociativityTest {


    @Test
    public void testDouble(){
        Parser<Character, Double> addition = number.then(chr('+')).then(number).map((left, op, right) -> {
            System.out.println("here");
            return Double.sum(left, right);
        });


        var result = addition.parse(Input.of("2+3"));
        System.out.println(result.getOrThrow());

    }


    @Test
    public void testAssociativity(){
        Ref<Character,Double> expression = Parser.ref();
        Ref <Character,Double> term = Parser.ref();

        Parser<Character, Double> addition = term.then(chr('+')).then(expression).map((left, op, right) -> Double.sum(left, right));
        Parser<Character,Double> multiplication = dble.then(chr('*')).then(term).map((left, op,right) -> left * right);
        term.set(multiplication.or(dble));
        expression.set(choice(List.of(
                addition,
                term
        )));

        var result = expression.parse(Input.of("2*3+4"));
        assertEquals(10.0,result.getOrThrow());
        result = expression.parse(Input.of("2+3*4"));
        assertEquals(14.0,result.getOrThrow());
    }

    public void testAssociativity2(){
        Ref<Character,Double> expression = Parser.ref();
        Ref <Character,Double> term = Parser.ref();

        Parser<Character, Double> addition = term.then(chr('+')).then(expression).map((left, op, right) -> Double.sum(left, right));
        Parser<Character,Double> multiplication = dble.then(chr('*')).then(term).map((left, op,right) -> left * right);
        term.set(multiplication.or(dble));
        expression.set(choice(List.of(
                addition,
                term
        )));

        var result = expression.parse(Input.of("2*3+4"));
        assertEquals(10.0,result.getOrThrow());
        result = expression.parse(Input.of("2+3*4"));
        assertEquals(14.0,result.getOrThrow());
    }

}