package io.github.parseworks;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class ExtendedCombinatorsTest {

    @Test
    void testExactCharacterParserSucceeds() {
        Parser<Character, Character> parser = Combinators.chr('x');
        Result<Character, Character> result = parser.parse(Input.of("xyz"));
        assertTrue(result.isSuccess(), "Should succeed on 'x'");
        assertEquals('x', (char) result.get());
    }

    @Test
    void testExactCharacterParserFails() {
        Parser<Character, Character> parser = Combinators.chr('x');
        Result<Character, Character> result = parser.parse(Input.of("abc"));
        assertFalse(result.isSuccess(), "Should fail when 'x' is not found");
    }

    @Test
    void testEndOfFileParserSucceeds() {
        Parser<Character, Void> parser = Combinators.eof();
        Result<Character, Void> result = parser.parse(Input.of(""));
        assertTrue(result.isSuccess(), "EOF should succeed on empty input");
    }

    @Test
    void testEndOfFileParserFails() {
        Parser<Character, Void> parser = Combinators.eof();
        Result<Character, Void> result = parser.parse(Input.of("notEmpty"));
        assertFalse(result.isSuccess(), "EOF should fail when input is not empty");
    }

    @Test
    void testRegexParserSucceeds() {
        Parser<Character, String> parser = Combinators.regex("[a-z]+");
        Result<Character, String> result = parser.parse(Input.of("abc123"));
        assertTrue(result.isSuccess(), "Should match consecutive letters");
        assertEquals("abc", result.get());
    }

    @Test
    void testRegexParserFails() {
        Parser<Character, String> parser = Combinators.regex("[0-9]+");
        Result<Character, String> result = parser.parse(Input.of("abc"));
        assertFalse(result.isSuccess(), "Should fail when regex does not match");
    }
}