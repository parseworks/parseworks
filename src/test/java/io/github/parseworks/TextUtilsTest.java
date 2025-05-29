package io.github.parseworks;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TextUtilsTest {

    @Test
    public void testAlphaNum() {
        Parser<Character, Character> parser = TextUtils.alphaNum;
        assertTrue(parser.parse(Input.of("a")).isSuccess());
        assertTrue(parser.parse(Input.of("1")).isSuccess());
        assertFalse(parser.parse(Input.of("!")).isSuccess());
    }

    @Test
    public void testWord() {
        Parser<Character, String> parser = TextUtils.word;
        assertTrue(parser.parse(Input.of("hello")).isSuccess());
        assertFalse(parser.parseAll(Input.of("hello1")).isSuccess());
        assertFalse(parser.parse(Input.of("123")).isSuccess());
    }

    @Test
    public void testInteger() {
        Parser<Character, Integer> parser = TextUtils.intr;
        assertEquals(123, parser.parse(Input.of("123")).get());
        assertEquals(-123, parser.parse(Input.of("-123")).get());
        assertEquals(123, parser.parse(Input.of("+123")).get());
        assertFalse(parser.parse(Input.of("abc")).isSuccess());
    }
}