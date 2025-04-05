package io.github.parseworks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CombinatorsTest {

    @Test
    public void eofSucceedsOnEmptyInput() {
        final Input<Character> input = Input.of("");
        final Parser<Character, Void> parser = Combinators.eof();

        var result = parser.parse(input);
        assertTrue(result.isSuccess());
    }

    @Test
    public void anySucceedsOnNonEmptyInput() {
        final Input<Character> input = Input.of("abc");
        final Parser<Character, Character> parser = Combinators.any(Character.class);

        Result<Character, Character> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals('a', (char) result.get());
    }

    @Test
    public void anyFailsOnEmptyInput() {
        final Input<Character> input = Input.of("");
        final Parser<Character, Character> parser = Combinators.any(Character.class);

        Result<Character, Character> result = parser.parse(input);
        assertFalse(result.isSuccess());
    }


    @Test
    public void regexParserSucceedsOnMatchingInput() {
        final Input<Character> input = Input.of("123abc");
        final Parser<Character, String> parser = Combinators.regex("\\d+");

        Result<Character, String> result = parser.parse(input);
        assertTrue(result.isSuccess());
        assertEquals("123", result.get());
    }

    @Test
    public void regexParserFailsOnNonMatchingInput() {
        final Input<Character> input = Input.of("abc123");
        final Parser<Character, String> parser = Combinators.regex("\\d+");

        Result<Character, String> result = parser.parse(input);
        assertFalse(result.isSuccess());
    }
}