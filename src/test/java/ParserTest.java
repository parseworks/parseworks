import io.github.parseworks.*;
import org.junit.jupiter.api.Test;

import java.util.function.BinaryOperator;

import static io.github.parseworks.Text.chr;
import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {

    @Test
    public void testPure() {
        Parser<Character, String> parser = Combinators.pure("test");
        Input<Character> input = Input.of("");
        Result<Character, String> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals("test", result.getOrThrow());
    }

    @Test
    public void testMany() {
        Parser<Character, IList<Character>> parser = chr(Character::isLetter).many();
        Input<Character> input = Input.of("abc123");
        Result<Character, IList<Character>> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getOrThrow());
    }

    @Test
    public void testChainr1() {
        Parser<Character, Integer> number = Text.number();
        Parser<Character, BinaryOperator<Integer>> plus = chr('+').map(op -> Integer::sum);
        Parser<Character, Integer> parser = number.chainr1(plus);
        Input<Character> input = Input.of("1+2+3");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(6, result.getOrThrow());
    }


    @Test
    public void testBetween() {
        Parser<Character, Character> open = chr('(');
        Parser<Character, Character> close = chr(')');
        Parser<Character, String> content = chr(Character::isLetter).many1().map(chars -> {
            StringBuilder sb = new StringBuilder();
            while (chars.hasNext()) {
                sb.append(chars.next());
            }
            return sb.toString();
        });
        Parser<Character, String> parser = content.between(open, close);
        Input<Character> input = Input.of("(content)");
        Result<Character, String> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals("content", result.getOrThrow());
    }
}