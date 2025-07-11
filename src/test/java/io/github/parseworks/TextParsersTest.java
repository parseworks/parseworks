package io.github.parseworks;

import io.github.parseworks.parsers.NumericParsers;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TextParsersTest {

    @Test
    public void testAlphaNum() {
        Parser<Character, Character> parser = TextParsers.alphaNumeric;
        assertTrue(parser.parse(Input.of("a")).isSuccess());
        assertTrue(parser.parse(Input.of("1")).isSuccess());
        assertFalse(parser.parse(Input.of("!")).isSuccess());
    }

    @Test
    public void testWord() {
        Parser<Character, String> parser = TextParsers.word;
        assertTrue(parser.parse(Input.of("hello")).isSuccess());
        assertFalse(parser.parseAll(Input.of("hello1")).isSuccess());
        assertFalse(parser.parse(Input.of("123")).isSuccess());
    }

    @Test
    public void testInteger() {
        Parser<Character, Integer> parser = NumericParsers.integer;
        assertEquals(123, parser.parse(Input.of("123")).get());
        assertEquals(-123, parser.parse(Input.of("-123")).get());
        assertEquals(123, parser.parse(Input.of("+123")).get());
        assertFalse(parser.parse(Input.of("abc")).isSuccess());
    }
}