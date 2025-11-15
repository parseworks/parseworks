package io.github.parseworks;

import org.junit.jupiter.api.Test;

import static io.github.parseworks.parsers.Lexical.chr;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for all potential error message types in the parseworks library.
 * is properly generated and has the correct error message format.
 */
public class ErrorMessageTest {

    @Test
    public void testUnexpectedEofError() {
        // Test unexpected EOF error when input ends prematurely
        Parser<Character, Character> parser = chr('a').then(chr('b')).map((a, b) -> a);
        Result<Character, Character> result = parser.parse("a");

        assertTrue(!result.matches());
        String errorMessage = result.error();
        assertTrue(errorMessage.contains("Unexpected end of input") || 
                   errorMessage.contains("reached end of input"));
    }

    @Test
    public void testExpectedEofError() {
        // Test expected EOF error when input has trailing content
        Parser<Character, Character> parser = chr('a');
        Result<Character, Character> result = parser.parseAll("ab");

        assertTrue(!result.matches());
        String errorMessage = result.error();
        assertTrue(errorMessage.contains("Expected end of input") || 
                   errorMessage.contains("expected end of input"));
    }

}
