package io.github.parseworks;

import io.github.parseworks.impl.result.NoMatch;
import io.github.parseworks.impl.result.PartialMatch;
import io.github.parseworks.parsers.Lexical;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.parsers.Combinators.attempt;

import static org.junit.jupiter.api.Assertions.*;

public class PartialMatchTest {

    @Test
    public void testPartialMatchInParse() {
        Parser<Character, String> abc = Lexical.string("abc");
        Input<Character> input = Input.of("abcdef");

        // parse(input, false) should return a Match
        Result<Character, String> result1 = abc.parse(input, false);
        assertTrue(result1.matches());
        assertNotSame(ResultType.PARTIAL, result1.type());
        assertEquals("abc", result1.value());
        assertEquals(3, result1.input().position());

        // parse(input, true) should return a PartialMatch now (instead of NoMatch)
        Result<Character, String> result2 = abc.parse(input, true);
        assertFalse(result2.matches());
        assertEquals(ResultType.PARTIAL, result2.type());
        // value() should throw exception for PartialMatch
        assertThrows(RuntimeException.class, result2::value);
        // It still advanced the index to 3
        assertEquals(3, result2.input().position());
    }

    @Test
    public void testFullMatchInParse() {
        Parser<Character, String> abc = Lexical.string("abc");
        Input<Character> input = Input.of("abc");

        Result<Character, String> result = abc.parse(input, true);
        assertTrue(result.matches());
        assertNotSame(ResultType.PARTIAL, result.type());
        assertEquals("abc", result.value());
        assertTrue(result.input().isEof());
    }

    @Test
    public void testPartialStringMatch() {
        Parser<Character, String> abcd = Lexical.string("abcd");
        Input<Character> input = Input.of("abc"); // Missing 'd'

        Result<Character, String> result = abcd.parse(input);
        assertFalse(result.matches());
        assertEquals(ResultType.NO_MATCH, result.type());
        // value() should throw exception for PartialMatch
        assertThrows(RuntimeException.class, result::value);
        assertEquals(0, result.input().position());
    }

    @Test
    public void testNoStringMatch() {
        Parser<Character, String> abcd = Lexical.string("abcd");
        Input<Character> input = Input.of("xyz");

        Result<Character, String> result = abcd.parse(input);
        assertFalse(result.matches());
        assertNotSame(ResultType.PARTIAL, result.type());
        // value() should throw exception for NoMatch
        assertThrows(RuntimeException.class, result::value);
    }

    @Test
    public void testPartialMatch() {
        Parser<Character, String> abcd = Lexical.string("abcd");
        Input<Character> input = Input.of("abc");
        
        Result<Character, String> result = abcd.parse(input);
        
        assertEquals(ResultType.NO_MATCH, result.type());
        NoMatch<Character, String> partial = (NoMatch<Character, String>) result;
        assertEquals(0, partial.input().position());
    }

    @Test
    public void testAttemptBacktrack() {
        Parser<Character, String> abcd = attempt(Lexical.string("abcd"));
        Input<Character> fullInput = Input.of("prefixabcd");
        Input<Character> startInput = fullInput.skip(6); // at 'a'
        
        // Input is "prefixabcX"
        Input<Character> testInput = Input.of("prefixabcX").skip(6);
        
        Result<Character, String> result = abcd.apply(testInput);
        
        assertFalse(result.matches());
        assertEquals(ResultType.PARTIAL, result.type());
        assertEquals(6, result.input().position(), "Should backtrack to 6");
    }
}
