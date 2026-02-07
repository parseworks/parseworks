package io.github.parseworks;

import io.github.parseworks.impl.inputs.CharSequenceInput;
import io.github.parseworks.parsers.Lexical;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class CoreParserTest {

    @Test
    public void testBetween() {
        Parser<Character, Character> p = Lexical.chr('a').between('(', ')');
        assertTrue(p.parse("(a)").matches());
        assertFalse(p.parse("a").matches());
        assertFalse(p.parse("(a").matches());
        assertFalse(p.parse("a)").matches());
    }

    @Test
    public void testAs() {
        Parser<Character, String> p = Lexical.chr('a').as("found a");
        Result<Character, String> result = p.parse("a");
        assertTrue(result.matches());
        assertEquals("found a", result.value());
    }

    @Test
    public void testPure() {
        Parser<Character, String> p = Parser.pure("pure value");
        Result<Character, String> result = p.parse("anything");
        assertTrue(result.matches());
        assertEquals("pure value", result.value());
        assertEquals(0, result.input().position());
    }

    @Test
    public void testOptional() {
        Parser<Character, Optional<Character>> p = Lexical.chr('a').optional();
        Result<Character, Optional<Character>> r1 = p.parse("a");
        assertTrue(r1.matches());
        assertEquals(Optional.of('a'), r1.value());

        Result<Character, Optional<Character>> r2 = p.parse("b");
        assertTrue(r2.matches());
        assertEquals(Optional.empty(), r2.value());
    }

    @Test
    public void testOrElse() {
        Parser<Character, Character> p = Lexical.chr('a').orElse('b');
        assertEquals('a', p.parse("a").value());
        assertEquals('b', p.parse("x").value());
    }

    @Test
    public void testRepeatAtLeast() {
        Parser<Character, List<Character>> p = Lexical.chr('a').repeatAtLeast(2);
        assertFalse(p.parse("a").matches());
        assertTrue(p.parse("aa").matches());
        assertTrue(p.parse("aaa").matches());
        assertEquals(3, p.parse("aaa").value().size());
    }

    @Test
    public void testRepeatAtMost() {
        Parser<Character, List<Character>> p = Lexical.chr('a').repeatAtMost(2);
        assertTrue(p.parse("").matches());
        assertTrue(p.parse("a").matches());
        assertTrue(p.parse("aa").matches());
        Result<Character, List<Character>> res = p.parse("aaa");
        assertTrue(res.matches());
        assertEquals(2, res.value().size());
        assertEquals(2, res.input().position());
    }

    @Test
    public void testExpecting() {
        Parser<Character, Character> p = Lexical.chr('a').expecting("the letter a");
        Result<Character, Character> res = p.parse("b");
        assertFalse(res.matches());
        assertTrue(res.error().contains("the letter a"));
    }

    @Test
    public void testRecover() {
        Parser<Character, Character> p = Lexical.chr('a').recover(Lexical.chr('b'));
        assertEquals('a', p.parse("a").value());
        assertEquals('b', p.parse("b").value());
    }
}
