import io.github.parseworks.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.BinaryOperator;

import static io.github.parseworks.Combinators.chr;
import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {

    @Test
    public void testPure() {
        Parser<Character, String> parser = Parser.pure("test");
        Input<Character> input = Input.of("");
        Result<Character, String> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals("test", result.getOrThrow());
    }

    @Test
    public void testZeroOrMany() {
        Parser<Character, List<Character>> parser = chr(Character::isLetter).zeroOrMany().then(chr(Character::isDigit).zeroOrMany()).map((letters, digits) -> {
            for (var digit : digits) {
                ((List<Character>) letters).add(digit);
            }
            return letters;
        });
        Input<Character> input = Input.of("abc123");
        Result<Character, List<Character>> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(6, result.getOrThrow().size());
    }

    @Test
    public void testChainr1() {
        Parser<Character, Integer> number = TextUtils.number;
        Parser<Character, BinaryOperator<Integer>> plus = chr('+').map(op -> Integer::sum);
        Parser<Character, Integer> parser = number.chainRightMany(plus);
        Input<Character> input = Input.of("1+2+3");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(6, result.getOrThrow());
    }


    @Test
    public void testBetween() {
        Parser<Character, String> content = chr(Character::isLetter).many().map(chars -> {
            StringBuilder sb = new StringBuilder();
            for (var c : chars) {
                sb.append(c);
            }
            return sb.toString();
        });
        String test = "compute";
        Parser<Character, String> parser = content.between('(', ')');
        Input<Character> input = Input.of("(" + test + ")");
        Result<Character, String> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(test, result.getOrThrow());
    }

    @Test
    public void testDigit() {
        Parser<Character, Character> parser = TextUtils.digit;
        Input<Character> input = Input.of("5");
        Result<Character, Character> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals('5', result.getOrThrow());
    }

    @Test
    public void testNumber() {
        Parser<Character, Integer> parser = TextUtils.number;
        Input<Character> input = Input.of("12345");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(12345, result.getOrThrow());
    }

    @Test
    public void testFailure() {
        Parser<Character, Character> parser = chr('a');
        Input<Character> input = Input.of("b");
        Result<Character, Character> result = parser.parse(input);
        assertFalse(result.isSuccess());
    }

    @Test
    public void testChoice() {
        Parser<Character, Character> parser = chr('a').or(chr('b'));
        Input<Character> input = Input.of("b");
        Result<Character, Character> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals('b', result.getOrThrow());
    }

    @Test
    public void testChainl1() {
        Parser<Character, Integer> number = TextUtils.number;
        Parser<Character, BinaryOperator<Integer>> plus = chr('+').map(op -> Integer::sum);
        Parser<Character, Integer> parser = number.chainLeftMany(plus);
        Input<Character> input = Input.of("1+2+3");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(6, result.getOrThrow());
    }

    @Test
    public void testChainl() {
        Parser<Character, Integer> number = TextUtils.number;
        Parser<Character, BinaryOperator<Integer>> plus = chr('-').map(op -> (a, b) -> a - b);
        Parser<Character, Integer> parser = number.chainLeftMany(plus);
        Input<Character> input = Input.of("1-2-3");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(-4, result.getOrThrow());
    }

    @Test
    public void testChainr() {
        Parser<Character, Integer> number = TextUtils.number;
        Parser<Character, BinaryOperator<Integer>> plus = chr('-').map(op -> (a, b) -> a - b);
        Parser<Character, Integer> parser = number.chainRightMany(plus);
        Input<Character> input = Input.of("1-2-3");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getOrThrow());
    }

    @Test
    public void testChainr2() {
        Parser<Character, Integer> number = TextUtils.number;
        Parser<Character, BinaryOperator<Integer>> plus = chr('-').as((a, b) -> a - b);
        Parser<Character, Integer> parser = number.chainRightMany(plus);
        Input<Character> input = Input.of("1-2-3");
        Result<Character, Integer> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getOrThrow());
    }

    @Test
    public void testSeparatedByZeroOrMany() {
        Parser<Character, FList<Character>> parser = chr(Character::isLetter).separatedByZeroOrMany(chr(','));
        Input<Character> input = Input.of("a,b,c");
        Result<Character, FList<Character>> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getOrThrow().size());
    }

    @Test
    public void testOptional() {
        Parser<Character, Optional<Character>> parser = chr('a').optional();
        Input<Character> input = Input.of("a");
        Result<Character, Optional<Character>> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertTrue(result.getOrThrow().isPresent());
        assertEquals('a', result.getOrThrow().get());
    }

    @Test
    public void testBetweenDifferentContent() {
        Parser<Character, Character> open = chr('[');
        Parser<Character, Character> close = chr(']');
        Parser<Character, String> content = chr(Character::isLetter).many().map(chars -> {
            StringBuilder sb = new StringBuilder();
            for (var c : chars) {
                sb.append(c);
            }
            return sb.toString();
        });
        String test = "example";
        Parser<Character, String> parser = content.between('[', ']');
        Input<Character> input = Input.of("[" + test + "]");
        Result<Character, String> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(test, result.getOrThrow());
    }

    @Test
    public void testRepeat() {
        Parser<Character, FList<Character>> parser = chr('a').repeat(3);
        Input<Character> input = Input.of("aaa");
        Result<Character, FList<Character>> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getOrThrow().size());
    }

    @Test
    public void testRepeatAtLeast() {
        Parser<Character, FList<Character>> parser = chr('a').repeatAtLeast(2);
        Input<Character> input = Input.of("aaa");
        Result<Character, FList<Character>> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getOrThrow().size());
    }

    @Test
    public void testRepeatBetween() {
        Parser<Character, FList<Character>> parser = chr('a').repeat(2, 4);
        Input<Character> input = Input.of("aaa");
        Result<Character, FList<Character>> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals(3, result.getOrThrow().size());
    }


}