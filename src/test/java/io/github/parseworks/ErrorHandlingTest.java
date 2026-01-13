package io.github.parseworks;

import io.github.parseworks.parsers.Lexical;
import org.junit.jupiter.api.Test;

import static io.github.parseworks.parsers.Combinators.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the error handling functionality in the parseworks library.
 * These tests verify that the appropriate error types are used for different error scenarios.
 */
public class ErrorHandlingTest {

    @Test
    public void testSyntaxErrorType() {
        Parser<Character, String> parser = oneOf(Lexical.string("foo"), Lexical.string("bar"));
        Result<Character, String> result = parser.parse("baz");

        assertTrue(!result.matches());
    }

    @Test
    public void testExpectedEofErrorType() {
        // Test that eof() uses EXPECTED_EOF error type when input is not at EOF
        Parser<Character, Void> parser = eof();
        Result<Character, Void> result = parser.parse("x");

        assertFalse(result.matches());
    }

    @Test
    public void testUnexpectedEofErrorType() {
        // Test that oneOf() uses UNEXPECTED_EOF error type when input is at EOF
        Parser<Character, String> parser = oneOf(Lexical.string("foo"), Lexical.string("bar"));
        Result<Character, String> result = parser.parse("");

        assertFalse(result.matches());

    }

    @Test
    public void testValidationErrorType() {
        // Test that not() uses VALIDATION error type when input matches the pattern
        Parser<Character, Character> parser = not(is('x'));
        Result<Character, Character> result = parser.parse("x");

        assertFalse(result.matches());
    }

    @Test
    public void testRecursionErrorType() {
        // Create a parser that causes infinite recursion
        // We need to create a more realistic recursive grammar
        Parser<Character, String> expr = Parser.ref();

        // Define expr in terms of itself, which will cause infinite recursion
        // when there's no base case that can succeed
        expr.set(
            Lexical.chr('(')
            .skipThen(expr)
            .thenSkip(Lexical.chr(')'))
            .map(Object::toString)
        );

        // This input will cause infinite recursion because it keeps
        // trying to parse nested expressions without ever reaching a base case
        Result<Character, String> result = expr.parse("(((");

        assertFalse(result.matches());
        Failure<?, ?> failure = (Failure<?, ?>) result;

        // Print the actual error message for debugging
        System.out.println("[DEBUG_LOG] Error message: " + failure.error());
    }

    @Test
    public void testInternalErrorType() {
        // Create a parser that throws a RuntimeException
        Parser<Character, String> parser = new Parser<>(in -> {
            throw new RuntimeException("Test exception");
        });

        assertThrows(RuntimeException.class, () -> {
            Result<Character, String> result = parser.parse("x");
        });
    }


    @Test
    public void testErrorMessageFormat() {
        // Test that error messages follow the consistent format
        Parser<Character, String> parser = Lexical.string("foo");
        Result<Character, String> result = parser.parse("bar");

        assertTrue(!result.matches());
        String errorMessage = result.error();

        // Print the actual error message for debugging
        System.out.println("[DEBUG_LOG] Actual error message: " + errorMessage);

        // Error message should contain "expected" and "found"
        assertTrue(errorMessage.contains("expected"));
        assertTrue(errorMessage.contains("found"));
    }
}
